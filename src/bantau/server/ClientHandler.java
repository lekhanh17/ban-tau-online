package bantau.server;

import bantau.common.Board;
import bantau.common.Packet;
import bantau.common.PacketType;
import bantau.common.Protocol;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * Mot thread phuc vu mot client.
 */
public class ClientHandler implements Runnable {

    private static final Pattern NAME_RULE = Pattern.compile(Protocol.NAME_PATTERN);

    private final Socket socket;
    private final ObjectOutputStream out;
    private final ObjectInputStream in;

    /** null nghia la CHUA dang nhap. */
    private volatile String username;
    /** null nghia la dang o lobby. */
    private volatile Room room;
    /** Ket noi da dong - khong con gui duoc gi nua. */
    private volatile boolean closed;

    /**
     * THU TU TAO LUONG BAT BUOC: ObjectOutputStream truoc, flush(), roi moi
     * ObjectInputStream. Nguoc lai hai ben cung cho header cua nhau - treo
     * vinh vien ma khong bao loi.
     */
    public ClientHandler(Socket socket) throws IOException {
        this.socket = socket;
        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.out.flush();
        this.in = new ObjectInputStream(socket.getInputStream());
    }

    public String getUsername() {
        return username == null ? "?" : username;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public synchronized void send(Packet packet) {
        if (closed) {
            return;
        }
        try {
            out.writeObject(packet);
            out.flush();
            // Xoa bo dem doi tuong. Bo dong nay thi lan sau gui cung mot
            // doi tuong se chi gui tham chieu cu - ben nhan nhan du lieu cu.
            out.reset();
        } catch (IOException e) {
            System.out.println("Khong gui duoc toi " + getUsername() + ": " + e.getMessage());
        }
    }

    private void sendError(String code, String moTa) {
        send(Packet.of(PacketType.ERROR, code, moTa));
    }

    /* ------------------------------------------------------------------ */
    /* Vong doi                                                           */
    /* ------------------------------------------------------------------ */

    @Override
    public void run() {
        System.out.println("Ket noi moi tu " + socket.getRemoteSocketAddress());
        try {
            send(Packet.of(PacketType.SYSTEM, "Chao mung! Hay dang nhap bang lenh LOGIN."));

            while (true) {
                Object obj = in.readObject();
                if (!(obj instanceof Packet packet)) {
                    continue;
                }
                System.out.println("  <-- " + getUsername() + " : " + packet);

                if (packet.type() == PacketType.QUIT) {
                    break;
                }
                handle(packet);
            }

        } catch (EOFException e) {
            System.out.println(getUsername() + " da dong ket noi.");
        } catch (SocketException e) {
            System.out.println(getUsername() + " mat ket noi dot ngot: " + e.getMessage());
        } catch (IOException | ClassNotFoundException e) {
            System.out.println(getUsername() + " loi: " + e);
        } finally {
            donDep();
        }
    }

    private void handle(Packet packet) {

        // ----- Chua dang nhap: chi cho phep LOGIN va REGISTER -----
        if (username == null) {
            switch (packet.type()) {
                case LOGIN -> xuLyDangNhap(packet.arg(0), packet.arg(1));
                case REGISTER -> xuLyDangKy(packet.arg(0), packet.arg(1));
                default -> sendError(Protocol.E_NOT_LOGGED_IN, "Ban phai dang nhap truoc");
            }
            return;
        }

        // ----- Da dang nhap -----
        switch (packet.type()) {
            case LOGIN ->
                    sendError(Protocol.E_BAD_STATE, "Ban da dang nhap voi ten " + username);

            case CHAT -> xuLyChat(packet.arg(0));

            case WHO ->
                    send(Packet.of(PacketType.WHO_LIST, ServerMain.onlineNames()));

            case REGISTER ->
                    sendError(Protocol.E_BAD_STATE, "Ban da dang nhap roi");

            case ROOM_LIST -> ServerMain.rooms().sendRoomList(this);

            case RANK_LIST -> send(Packet.withPayload(PacketType.RANK_DATA,
                    new ArrayList<>(ServerMain.players().bangXepHang(Protocol.RANK_TOP))));

            case HISTORY_LIST -> send(Packet.withPayload(PacketType.HISTORY_DATA,
                    new ArrayList<>(ServerMain.matches()
                            .lichSuCua(username, Protocol.HISTORY_LIMIT))));

            case ROOM_CREATE -> xuLyTaoPhong(packet.arg(0));

            case ROOM_JOIN -> xuLyVaoPhong(packet.intArg(0, -1));

            case ROOM_LEAVE -> xuLyRoiPhong();

            case READY -> xuLySanSang(packet);

            case FIRE -> xuLyBan(packet.intArg(0, -1), packet.intArg(1, -1));

            default ->
                    sendError(Protocol.E_UNKNOWN_CMD,
                            "Client khong duoc phep gui ban tin loai " + packet.type());
        }
    }

    /* ------------------------------------------------------------------ */
    /* Cac lenh cu the                                                    */
    /* ------------------------------------------------------------------ */

    /**
     * Kiem tra ten va mat khau co dung khuon dang khong.
     *
     * @return null neu hop le, nguoc lai la ma loi
     */
    private String kiemTraKhuonDang(String ten, String matKhau) {
        if (!NAME_RULE.matcher(ten).matches()) {
            return Protocol.E_NAME_INVALID;
        }
        if (matKhau == null || matKhau.length() < Protocol.PASS_MIN
                || matKhau.length() > Protocol.PASS_MAX) {
            return Protocol.E_PASS_INVALID;
        }
        return null;
    }

    /** Doi ma loi thanh cau giai thich cho nguoi choi doc. */
    private String moTaLoi(String code) {
        return switch (code) {
            case Protocol.E_NAME_INVALID -> "Ten phai dai " + Protocol.NAME_MIN + "-"
                    + Protocol.NAME_MAX + " ky tu, chi gom chu cai, so va dau gach duoi";
            case Protocol.E_PASS_INVALID -> "Mat khau phai dai " + Protocol.PASS_MIN + "-"
                    + Protocol.PASS_MAX + " ky tu";
            case Protocol.E_NAME_EXISTS -> "Ten nay da co nguoi dang ky";
            case Protocol.E_NO_ACCOUNT -> "Chua co tai khoan nay, hay bam Dang ky truoc";
            case Protocol.E_WRONG_PASSWORD -> "Sai mat khau";
            case Protocol.E_DB_ERROR -> "Loi CSDL phia server, thu lai sau";
            default -> "Khong dang nhap duoc";
        };
    }

    private void xuLyDangKy(String ten, String matKhau) {
        String n = ten == null ? "" : ten.trim();

        String loi = kiemTraKhuonDang(n, matKhau);
        if (loi != null) {
            sendError(loi, moTaLoi(loi));
            return;
        }

        String err = ServerMain.players().dangKy(n, matKhau);
        if (err != null) {
            sendError(err, moTaLoi(err));
            return;
        }

        send(Packet.of(PacketType.REGISTER_OK, n));
        System.out.println("Tao tai khoan moi: " + n);
    }

    private void xuLyDangNhap(String ten, String matKhau) {
        String n = ten == null ? "" : ten.trim();

        String loi = kiemTraKhuonDang(n, matKhau);
        if (loi != null) {
            sendError(loi, moTaLoi(loi));
            return;
        }

        // Kiem tra tai khoan trong CSDL truoc...
        String err = ServerMain.players().kiemTraDangNhap(n, matKhau);
        if (err != null) {
            sendError(err, moTaLoi(err));
            return;
        }

        // ...roi moi xem ten do co dang duoc dung o may khac khong.
        if (!ServerMain.registerUser(n, this)) {
            sendError(Protocol.E_NAME_TAKEN,
                    "Tai khoan '" + n + "' dang dang nhap o mot may khac");
            return;
        }

        username = n;
        send(Packet.of(PacketType.LOGIN_OK, n));
        send(Packet.of(PacketType.SYSTEM,
                "Dang co " + ServerMain.onlineCount() + " nguoi online."));
        ServerMain.rooms().sendRoomList(this);
        System.out.println("Dang nhap: " + n + " (online: " + ServerMain.onlineCount() + ")");
    }

    private void xuLyChat(String noiDung) {
        if (noiDung == null || noiDung.isBlank()) {
            return;
        }
        Packet msg = Packet.of(PacketType.CHAT_MSG, username, noiDung);
        Room r = room;
        if (r != null) {
            r.broadcast(msg);
        } else {
            ServerMain.broadcast(msg);
        }
    }

    private void xuLyTaoPhong(String tenPhong) {
        if (room != null) {
            sendError(Protocol.E_ALREADY_IN_ROOM, "Ban dang o trong mot phong");
            return;
        }
        String n = (tenPhong == null || tenPhong.isBlank())
                ? "Phong cua " + username
                : tenPhong.trim();
        if (n.length() > Protocol.ROOM_NAME_MAX) {
            n = n.substring(0, Protocol.ROOM_NAME_MAX);
        }
        Room r = ServerMain.rooms().createRoom(n);
        String err = r.join(this);
        if (err != null) {
            sendError(err, "Khong vao duoc phong vua tao");
        }
    }

    private void xuLyVaoPhong(int roomId) {
        if (room != null) {
            sendError(Protocol.E_ALREADY_IN_ROOM, "Ban dang o trong mot phong");
            return;
        }
        Room r = ServerMain.rooms().getRoom(roomId);
        if (r == null) {
            sendError(Protocol.E_ROOM_NOT_FOUND, "Phong #" + roomId + " khong ton tai");
            return;
        }
        String err = r.join(this);
        if (err != null) {
            sendError(err, "Khong vao duoc phong #" + roomId);
        }
    }

    private void xuLyRoiPhong() {
        Room r = room;
        if (r == null) {
            sendError(Protocol.E_NOT_IN_ROOM, "Ban chua o trong phong nao");
            return;
        }
        r.leave(this);
        ServerMain.rooms().sendRoomList(this);
    }

    /**
     * Nhan so do dat tau. Ban do den duoi dang doi tuong Board trong payload.
     *
     * <p>payload(Board.class) ep kieu AN TOAN: neu client gui sai kieu thi
     * tra ve null chu khong nem ClassCastException lam sap thread.
     */
    private void xuLySanSang(Packet packet) {
        Room r = room;
        if (r == null) {
            sendError(Protocol.E_NOT_IN_ROOM, "Ban chua o trong phong nao");
            return;
        }
        Board board = packet.payload(Board.class);
        String err = r.handleReady(this, board);
        if (err != null) {
            sendError(err, "So do dat tau khong hop le hoac sai thoi diem");
        }
    }

    private void xuLyBan(int x, int y) {
        Room r = room;
        if (r == null) {
            sendError(Protocol.E_NOT_IN_ROOM, "Ban chua o trong phong nao");
            return;
        }
        String err = r.handleFire(this, x, y);
        if (err != null) {
            sendError(err, "Khong ban duoc vao o (" + x + "," + y + ")");
        }
    }

    private void donDep() {
        closed = true;
        Room r = room;
        if (r != null) {
            // Client tat dot ngot van phai duoc go khoi phong, neu khong
            // phong se ket vinh vien voi mot nguoi da chet.
            r.leave(this);
        }
        String ten = username;
        if (ten != null) {
            ServerMain.unregisterUser(ten);
        }
        try {
            socket.close();
        } catch (IOException ignored) {
            // khong con gi de lam
        }
        System.out.println(getUsername() + " da ngat. Con lai "
                + ServerMain.onlineCount() + " nguoi, "
                + ServerMain.rooms().roomCount() + " phong.");
    }
}
