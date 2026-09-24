package bantau.server;

import bantau.common.Packet;
import bantau.common.PacketType;
import bantau.common.Protocol;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.regex.Pattern;

/**
 * Mot thread phuc vu mot client.
 *
 * <p>Handler co HAI TRANG THAI chong len nhau:
 * <ul>
 *   <li>Chua dang nhap / da dang nhap</li>
 *   <li>Dang o lobby / dang o trong mot phong</li>
 * </ul>
 */
public class ClientHandler implements Runnable {

    private static final Pattern NAME_RULE = Pattern.compile(Protocol.NAME_PATTERN);

    private final Socket socket;
    private final ObjectOutputStream out;
    private final ObjectInputStream in;

    /** null nghia la CHUA dang nhap. */
    private volatile String username;
    /** null nghia la dang o lobby, chua vao phong nao. */
    private volatile Room room;
    /** Danh dau ket noi da dong - khong con gui duoc gi nua. */
    private volatile boolean closed;

    /**
     * THU TU TAO LUONG RAT QUAN TRONG: ObjectOutputStream truoc, flush(),
     * roi moi ObjectInputStream. Nguoc lai se treo vinh vien khong bao loi.
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

    /** Gui mot ban tin. Dong bo vi nhieu thread cung goi khi broadcast. */
    public synchronized void send(Packet packet) {
        // Client da ngat thi bo qua, tranh in day man hinh loi "Broken pipe".
        if (closed) {
            return;
        }
        try {
            out.writeObject(packet);
            out.flush();
            // Xoa bo dem doi tuong, tranh gui lai tham chieu cu.
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

        // ----- Chua dang nhap: chi cho phep LOGIN -----
        if (username == null) {
            if (packet.type() == PacketType.LOGIN) {
                xuLyDangNhap(packet.arg(0));
            } else {
                sendError(Protocol.E_NOT_LOGGED_IN, "Ban phai dang nhap truoc");
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

            case ROOM_LIST ->
                    ServerMain.rooms().sendRoomList(this);

            case ROOM_CREATE -> xuLyTaoPhong(packet.arg(0));

            case ROOM_JOIN -> xuLyVaoPhong(packet.intArg(0, -1));

            case ROOM_LEAVE -> xuLyRoiPhong();

            default ->
                    sendError(Protocol.E_UNKNOWN_CMD,
                            "Client khong duoc phep gui ban tin loai " + packet.type());
        }
    }

    /* ------------------------------------------------------------------ */
    /* Cac lenh cu the                                                    */
    /* ------------------------------------------------------------------ */

    private void xuLyDangNhap(String ten) {
        String n = ten == null ? "" : ten.trim();

        if (!NAME_RULE.matcher(n).matches()) {
            sendError(Protocol.E_NAME_INVALID,
                    "Ten phai dai " + Protocol.NAME_MIN + "-" + Protocol.NAME_MAX
                            + " ky tu, chi gom chu cai, so va dau gach duoi");
            return;
        }
        if (!ServerMain.registerUser(n, this)) {
            sendError(Protocol.E_NAME_TAKEN, "Ten '" + n + "' dang co nguoi khac dung");
            return;
        }

        username = n;
        send(Packet.of(PacketType.LOGIN_OK, n));
        send(Packet.of(PacketType.SYSTEM,
                "Dang co " + ServerMain.onlineCount() + " nguoi online. "
                        + "Lenh: ROOMS, CREATE <ten>, JOIN <id>, LEAVE, CHAT <noi dung>, WHO, QUIT"));
        ServerMain.rooms().sendRoomList(this);
        System.out.println("Dang nhap: " + n + " (online: " + ServerMain.onlineCount() + ")");
    }

    /**
     * Chat: neu dang o trong phong thi chi nguoi cung phong nghe thay,
     * neu dang o lobby thi tat ca nguoi o lobby nghe thay.
     */
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

    /** Luon chay du thoat binh thuong hay do loi. */
    private void donDep() {
        closed = true;
        Room r = room;
        if (r != null) {
            // Rat quan trong: client tat dot ngot van phai duoc go khoi phong,
            // neu khong phong se ket vinh vien voi mot nguoi da chet.
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