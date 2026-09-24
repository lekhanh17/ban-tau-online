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
 * Mot thread phuc vu mot client, trao doi bang DOI TUONG thay vi chuoi text.
 */
public class ClientHandler implements Runnable {

    private static final Pattern NAME_RULE = Pattern.compile(Protocol.NAME_PATTERN);

    private final Socket socket;
    private final ObjectOutputStream out;
    private final ObjectInputStream in;

    /** null nghia la CHUA dang nhap. */
    private volatile String username;

    /**
     * THU TU TAO LUONG RAT QUAN TRONG.
     *
     * <p>Constructor cua ObjectOutputStream ghi mot "header" vao luong ra.
     * Constructor cua ObjectInputStream thi DUNG CHO doc header cua ben kia.
     * Neu ca hai ben cung tao ObjectInputStream truoc thi ca hai cung ngoi cho
     * nhau - chuong trinh treo vinh vien, khong bao loi gi.
     *
     * <p>Quy tac: tao ObjectOutputStream truoc, goi flush() de day header di,
     * roi moi tao ObjectInputStream. Ca server va client deu lam nhu vay.
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

    /** Gui mot ban tin. Dong bo vi nhieu thread cung goi khi broadcast. */
    public synchronized void send(Packet packet) {
        try {
            out.writeObject(packet);
            out.flush();
            // reset() xoa bo nho dem cac doi tuong da gui.
            // Neu khong goi, ObjectOutputStream se gui "tham chieu toi doi tuong cu"
            // thay vi noi dung moi khi gap doi tuong da tung gui - ben nhan se
            // nhan duoc du lieu cu. Loi nay rat kho tim.
            out.reset();
        } catch (IOException e) {
            // Ghi that bai nghia la socket da hong, khong lam gi them duoc.
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
            // Client dong ket noi mot cach binh thuong (goi socket.close()).
            System.out.println(getUsername() + " da dong ket noi.");

        } catch (SocketException e) {
            // Client bi tat dot ngot - he dieu hanh gui goi RST.
            System.out.println(getUsername() + " mat ket noi dot ngot: " + e.getMessage());

        } catch (IOException | ClassNotFoundException e) {
            // ClassNotFoundException: ben kia gui mot lop ma ben nay khong co.
            // Thuong do server va client duoc bien dich tu hai phien ban code khac nhau.
            System.out.println(getUsername() + " loi: " + e);

        } finally {
            donDep();
        }
    }

    /** Phan giai ban tin. Chia hai vung: truoc va sau dang nhap. */
    private void handle(Packet packet) {

        // ----- VUNG 1: chua dang nhap, chi cho phep LOGIN -----
        if (username == null) {
            if (packet.type() == PacketType.LOGIN) {
                xuLyDangNhap(packet.arg(0));
            } else {
                sendError(Protocol.E_NOT_LOGGED_IN, "Ban phai dang nhap truoc");
            }
            return;
        }

        // ----- VUNG 2: da dang nhap -----
        switch (packet.type()) {
            case LOGIN ->
                    sendError(Protocol.E_BAD_STATE, "Ban da dang nhap voi ten " + username);

            case CHAT -> {
                String noiDung = packet.arg(0);
                if (!noiDung.isBlank()) {
                    ServerMain.broadcast(Packet.of(PacketType.CHAT_MSG, username, noiDung));
                }
            }

            case WHO ->
                    send(Packet.of(PacketType.WHO_LIST, ServerMain.onlineNames()));

            // Cac loai con lai la ban tin chi server moi duoc gui.
            // Client binh thuong khong gui duoc, chi client tu viet lai moi gui.
            default ->
                    sendError(Protocol.E_UNKNOWN_CMD,
                            "Client khong duoc phep gui ban tin loai " + packet.type());
        }
    }

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
                        + "Lenh: CHAT <noi dung>, WHO, QUIT"));
        ServerMain.broadcast(Packet.of(PacketType.SYSTEM, n + " da vao phong"));
        System.out.println("Dang nhap: " + n + " (online: " + ServerMain.onlineCount() + ")");
    }

    /** Luon chay du thoat binh thuong hay do loi. */
    private void donDep() {
        String ten = username;
        if (ten != null) {
            ServerMain.unregisterUser(ten);
            ServerMain.broadcast(Packet.of(PacketType.SYSTEM, ten + " da roi di"));
        }
        try {
            socket.close();
        } catch (IOException ignored) {
            // khong con gi de lam
        }
        System.out.println(getUsername() + " da ngat. Con lai "
                + ServerMain.onlineCount() + " nguoi.");
    }
}