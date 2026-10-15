package bantau.server;

import bantau.common.Board;
import bantau.common.MucDoBot;
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
     * Thoi diem cuoi cung nghe thay client (bat ky goi tin nao, ke ca PONG).
     *
     * <p>Dung {@link System#currentTimeMillis()} chu khong phai gio he thong
     * vi chi can do KHOANG CACH thoi gian.
     */
    private volatile long lanCuoiNgheThay = System.currentTimeMillis();

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

    /**
     * Bao lau roi khong nghe thay gi tu client nay, tinh bang giay.
     * Thread canh gac o {@link ServerMain} dung de biet ai da chet.
     */
    public long soGiayImLang() {
        return (System.currentTimeMillis() - lanCuoiNgheThay) / 1000;
    }

    /** Dong ket noi tu phia server - dung khi client khong con tra loi. */
    public void ngatKetNoi(String lyDo) {
        System.out.println("Ngắt kết nối " + getUsername() + ": " + lyDo);
        try {
            socket.close();   // lam readObject() o thread kia bat loi va don dep
        } catch (IOException ignored) {
        }
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
            System.out.println("Không gửi được tới " + getUsername() + ": " + e.getMessage());
        }
    }

    private void sendError(String code, String moTa) {
        send(Packet.of(PacketType.ERROR, code, moTa));
    }

    /* ------------------------------------------------------------------ */
    /* Vòng đời                                                          */
    /* ------------------------------------------------------------------ */

    @Override
    public void run() {
        System.out.println("Kết nối mới từ " + socket.getRemoteSocketAddress());
        try {
            send(Packet.of(PacketType.SYSTEM, "Chào mừng! Hãy đăng nhập bằng lệnh LOGIN."));

            while (true) {
                Object obj = in.readObject();
                if (!(obj instanceof Packet packet)) {
                    continue;
                }

                // Nghe thay bat ky goi tin nao cung nghia la client con song.
                lanCuoiNgheThay = System.currentTimeMillis();

                // PONG chi de bao con song, khong in ra cho do roi man hinh.
                if (packet.type() == PacketType.PONG) {
                    continue;
                }
                System.out.println("  <-- " + getUsername() + " : " + packet);

                if (packet.type() == PacketType.QUIT) {
                    break;
                }
                handle(packet);
            }

        } catch (EOFException e) {
            System.out.println(getUsername() + " đã đóng kết nối.");
        } catch (SocketException e) {
            System.out.println(getUsername() + " mất kết nối đột ngột: " + e.getMessage());
        } catch (IOException | ClassNotFoundException e) {
            System.out.println(getUsername() + " lỗi: " + e);
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
                default -> sendError(Protocol.E_NOT_LOGGED_IN, "Bạn phải đăng nhập trước");
            }
            return;
        }

        // ----- Da dang nhap -----
        switch (packet.type()) {
            case LOGIN ->
                    sendError(Protocol.E_BAD_STATE, "Bạn đã đăng nhập với tên " + username);

            case CHAT -> xuLyChat(packet.arg(0));

            case WHO ->
                    send(Packet.of(PacketType.WHO_LIST, ServerMain.onlineNames()));

            case REGISTER ->
                    sendError(Protocol.E_BAD_STATE, "Bạn đã đăng nhập rồi");

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

            case ADD_BOT -> xuLyThemBot(packet.arg(0));

            default ->
                    sendError(Protocol.E_UNKNOWN_CMD,
                            "Client không được phép gửi bản tin loại " + packet.type());
        }
    }

    /* ------------------------------------------------------------------ */
    /* Các lệnh cụ thể                                                   */
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
            case Protocol.E_NAME_INVALID -> "Tên phải dài " + Protocol.NAME_MIN + "-"
                    + Protocol.NAME_MAX + " ký tự, chỉ gồm chữ cái, số và dấu gạch dưới";
            case Protocol.E_PASS_INVALID -> "Mật khẩu phải dài " + Protocol.PASS_MIN + "-"
                    + Protocol.PASS_MAX + " ký tự";
            case Protocol.E_NAME_EXISTS -> "Tên này đã có người đăng ký";
            case Protocol.E_NO_ACCOUNT -> "Chưa có tài khoản này, hãy bấm Đăng ký trước";
            case Protocol.E_WRONG_PASSWORD -> "Sai mật khẩu";
            case Protocol.E_DB_ERROR -> " lỗi CSDL phía server, thử lại sau";
            default -> "Không đăng nhập được";
        };
    }

    private void xuLyDangKy(String ten, String matKhau) {
        String n = ten == null ? "" : ten.trim();

        String loi = kiemTraKhuonDang(n, matKhau);
        if (loi != null) {
            sendError(loi, moTaLoi(loi));
            return;
        }

        // Tien to nay danh rieng cho doi thu may. Khong chan thi nguoi choi
        // co the dang ky ten giong bot, va luat "khong tinh thanh tich tran
        // voi may" se bi loi dung de giau nhung tran thua.
        if (n.toLowerCase().startsWith(Protocol.BOT_PREFIX.toLowerCase())) {
            sendError(Protocol.E_NAME_RESERVED,
                    "Tên bắt đầu bằng \"" + Protocol.BOT_PREFIX
                            + "\" dành riêng cho đối thủ máy");
            return;
        }

        String err = ServerMain.players().dangKy(n, matKhau);
        if (err != null) {
            sendError(err, moTaLoi(err));
            return;
        }

        send(Packet.of(PacketType.REGISTER_OK, n));
        System.out.println("Tạo tài khoản mới: " + n);
    }

    private void xuLyDangNhap(String ten, String matKhau) {
        String n = ten == null ? "" : ten.trim();

        String loi = kiemTraKhuonDang(n, matKhau);
        if (loi != null) {
            sendError(loi, moTaLoi(loi));
            return;
        }

        // Kiem tra tai khoan trong CSDL truoc
        String err = ServerMain.players().kiemTraDangNhap(n, matKhau);
        if (err != null) {
            sendError(err, moTaLoi(err));
            return;
        }

        // ...roi moi xem ten do co dang duoc dung o may khac khong.
        if (!ServerMain.registerUser(n, this)) {
            sendError(Protocol.E_NAME_TAKEN,
                    "Tài khoản '" + n + "' đang đăng nhập ở một máy khác");
            return;
        }

        username = n;
        send(Packet.of(PacketType.LOGIN_OK, n));
        send(Packet.of(PacketType.SYSTEM,
                "Đang có " + ServerMain.onlineCount() + " người online."));
        System.out.println("Đăng nhập: " + n + " (online: " + ServerMain.onlineCount() + ")");

        // Truoc do co bi rot mang giua van khong? Neu co va chua het thoi
        // gian an han thi dua thang vao lai van dang do, khong qua lobby.
        Room cho = ServerMain.rooms().timPhongChoVaoLai(n);
        if (cho != null) {
            String loiVaoLai = cho.vaoLai(this);
            if (loiVaoLai == null) {
                send(Packet.of(PacketType.SYSTEM,
                        "Đã khôi phục ván đấu dang dở của bạn."));
                return;
            }
            System.out.println("Không đưa " + n + " vào lại được: " + loiVaoLai);
        }

        ServerMain.rooms().sendRoomList(this);
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
            sendError(Protocol.E_ALREADY_IN_ROOM, "Bạn đang ở trong một phòng");
            return;
        }
        String n = (tenPhong == null || tenPhong.isBlank())
                ? "Phòng của " + username
                : tenPhong.trim();
        if (n.length() > Protocol.ROOM_NAME_MAX) {
            n = n.substring(0, Protocol.ROOM_NAME_MAX);
        }
        Room r = ServerMain.rooms().createRoom(n);
        String err = r.join(this);
        if (err != null) {
            sendError(err, "Không vào được phòng vừa tạo");
        }
    }

    private void xuLyVaoPhong(int roomId) {
        if (room != null) {
            sendError(Protocol.E_ALREADY_IN_ROOM, "Bạn đang ở trong một phòng");
            return;
        }
        Room r = ServerMain.rooms().getRoom(roomId);
        if (r == null) {
            sendError(Protocol.E_ROOM_NOT_FOUND, "Phòng #" + roomId + " không tồn tại");
            return;
        }
        String err = r.join(this);
        if (err != null) {
            sendError(err, "Không vào được phòng #" + roomId);
        }
    }

    private void xuLyRoiPhong() {
        Room r = room;
        if (r == null) {
            sendError(Protocol.E_NOT_IN_ROOM, "Bạn chưa ở trong phòng nào");
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
            sendError(Protocol.E_NOT_IN_ROOM, "Bạn chưa ở trong phòng nào");
            return;
        }
        Board board = packet.payload(Board.class);
        String err = r.handleReady(this, board);
        if (err != null) {
            sendError(err, "Sơ đồ đặt tàu không hợp lệ hoặc sai thời điểm");
        }
    }

    private void xuLyBan(int x, int y) {
        Room r = room;
        if (r == null) {
            sendError(Protocol.E_NOT_IN_ROOM, "Bạn chưa ở trong phòng nào");
            return;
        }
        String err = r.handleFire(this, x, y);
        if (err != null) {
            sendError(err, "Không bắn được vào ô (" + x + "," + y + ")");
        }
    }

    /**
     * Them mot doi thu may vao phong dang o.
     *
     * <p>Server KHONG tu danh thay. No khoi dong mot client bot rieng, va
     * client do ket noi NGUOC LAI vao chinh server nay qua TCP, dang nhap
     * va vao phong bang dung nhung ban tin ma client Swing dung. Server khong
     * he biet day la may hay nguoi - va do chinh la diem manh: no chung minh
     * giao thuc BSP day du va doc lap voi giao dien.
     *
     * <p>Noi vao {@code 127.0.0.1} chu khong phai dia chi ngoai: bot chay
     * trong cung tien trinh server nen luon di duoc duong loopback, va khong
     * phu thuoc vao cau hinh mang cua may.
     *
     * @param maMucDo ten muc do kho tu args[0] cua ADD_BOT; chuoi rong hoac
     *                khong hop le thi dung {@link MucDoBot#THUONG}
     */
    private void xuLyThemBot(String maMucDo) {
        Room r = room;
        if (r == null) {
            sendError(Protocol.E_NOT_IN_ROOM, "Bạn chưa ở trong phòng nào");
            return;
        }
        String loi = r.coTheThemBot();
        if (loi != null) {
            sendError(loi, "Không thêm đối thủ máy vào lúc này được");
            return;
        }
        MucDoBot mucDo = MucDoBot.tuChuoi(maMucDo);
        try {
            bantau.bot.BotClient bot = new bantau.bot.BotClient(
                    "127.0.0.1", ServerMain.congDangChay(), r.getId(), mucDo);

            // Dang ky tai khoan cho bot TU BEN TRONG server, qua thang
            // PlayerDao. Bot khong the tu gui REGISTER vi duong REGISTER
            // cong khai chan moi ten bat dau bang BOT_PREFIX.
            String loiDangKy = ServerMain.players()
                    .dangKy(bot.ten(), bot.matKhau());
            if (loiDangKy != null) {
                sendError(Protocol.E_BOT_FAILED,
                        "Không tạo được tài khoản cho đối thủ máy");
                return;
            }

            bot.chay();
            send(Packet.of(PacketType.SYSTEM,
                    "Đang gọi đối thủ máy (" + bot.ten() + ", mức "
                            + mucDo.ten() + ") vào phòng..."));
            System.out.println("Thêm bot " + bot.ten() + " mức " + mucDo.ten()
                    + " vào phòng #" + r.getId()
                    + " theo yêu cầu của " + username);
        } catch (RuntimeException e) {
            sendError(Protocol.E_BOT_FAILED,
                    "Không khởi động được đối thủ máy: " + e.getMessage());
        }
    }

    private void donDep() {
        closed = true;
        Room r = room;
        if (r != null) {
            // Client tat dot ngot van phai duoc go khoi phong, neu khong
            // phong se ket vinh vien voi mot nguoi da chet.
            //
            // Nhung neu dang danh do thi KHONG xu thua ngay: ketNoiBiMat()
            // giu nguyen ban do va cho nguoi do dang nhap lai trong
            // Protocol.RECONNECT_SECONDS giay.
            r.ketNoiBiMat(this);
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
        System.out.println(getUsername() + " đã ngắt kết nối. Còn lại "
                + ServerMain.onlineCount() + " người, "
                + ServerMain.rooms().roomCount() + " phòng.");
    }
}
