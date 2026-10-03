package bantau.server;

import bantau.common.Board;
import bantau.common.Packet;
import bantau.common.PacketType;
import bantau.common.Protocol;
import bantau.common.RoomInfo;
import bantau.common.RoomState;

/**
 * MOT PHONG CHOI - toi da 2 nguoi, chua mot van dau.
 *
 * <p>May trang thai cua phong:
 * <pre>
 *   WAITING  --du 2 nguoi-->  PLACING  --ca hai READY-->  PLAYING
 *      ^                          |                          |
 *      +--------- mot nguoi roi phong -------------------- --+
 *                                 ^                          |
 *                                 +---- het van, choi lai ----+
 * </pre>
 *
 * <p>Moi phuong thuc thay doi trang thai deu synchronized vi cac thread
 * ClientHandler khac nhau cung truy cap vao doi tuong nay.
 */
public class Room {

    private final int id;
    private final String name;
    private final RoomManager manager;

    private ClientHandler p1;
    private ClientHandler p2;

    private RoomState state = RoomState.WAITING;
    private GameSession game;

    /**
     * Ten nguoi dang bi mat ket noi ma phong con cho vao lai.
     * null nghia la khong cho ai.
     */
    private String tenChoVaoLai;
    /** Moc thoi gian bat dau cho, de biet khi nao het han an han. */
    private long batDauChoMs;

    public Room(int id, String name, RoomManager manager) {
        this.id = id;
        this.name = name;
        this.manager = manager;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public synchronized RoomState getState() {
        return state;
    }

    /**
     * So cho da bi chiem trong phong.
     *
     * <p>Nguoi dang rot mang ma phong con cho vao lai VAN TINH LA MOT NGUOI,
     * du handler cua ho da bi go ra. Nho vay danh sach phong ngoai lobby hien
     * dung "2/2" va khong ai vao chiem cho cua ho.
     */
    public synchronized int playerCount() {
        return (p1 != null ? 1 : 0) + (p2 != null ? 1 : 0)
                + (tenChoVaoLai != null ? 1 : 0);
    }

    public synchronized boolean isEmpty() {
        return p1 == null && p2 == null;
    }

    public synchronized boolean isFull() {
        return playerCount() >= Protocol.MAX_PLAYERS_PER_ROOM;
    }

    public synchronized boolean contains(ClientHandler c) {
        return c == p1 || c == p2;
    }

    public synchronized RoomInfo info() {
        return new RoomInfo(id, name, playerCount(), state);
    }

    /**
     * Ten hien thi cho mot cho trong phong.
     *
     * <p>Cho dang bo trong VI NGUOI DO ROT MANG thi van hien ten ho, de nguoi
     * con lai biet minh dang cho ai vao lai chu khong phai doi thu bo di.
     */
    private String tenCua(ClientHandler c) {
        if (c != null) {
            return c.getUsername();
        }
        return tenChoVaoLai != null ? tenChoVaoLai : "-";
    }

    public synchronized void broadcast(Packet packet) {
        if (p1 != null) {
            p1.send(packet);
        }
        if (p2 != null) {
            p2.send(packet);
        }
    }

    private void guiTrangThai() {
        broadcast(Packet.withPayload(PacketType.ROOM_STATE, info(),
                tenCua(p1), tenCua(p2)));
    }

    /* ------------------------------------------------------------------ */
    /* Vao / roi phong                                                    */
    /* ------------------------------------------------------------------ */

    /** @return null neu thanh cong, nguoc lai la ma loi */
    public synchronized String join(ClientHandler c) {
        if (contains(c)) {
            return Protocol.E_ALREADY_IN_ROOM;
        }
        if (isFull()) {
            return Protocol.E_ROOM_FULL;
        }
        // Cho duy nhat mot nguoi vao lai thi khong duoc cho nguoi khac
        // nhay vao chiem suot - ban do cua nguoi cu con nguyen trong do.
        if (tenChoVaoLai != null) {
            return Protocol.E_ROOM_FULL;
        }

        if (p1 == null) {
            p1 = c;
        } else {
            p2 = c;
        }
        c.setRoom(this);
        c.send(Packet.withPayload(PacketType.ROOM_JOINED, info(), c == p1 ? "1" : "0"));

        if (isFull()) {
            batDauDatTau();
        } else {
            state = RoomState.WAITING;
            guiTrangThai();
        }
        manager.broadcastRoomList();
        return null;
    }

    /** Chuyen sang giai doan dat tau. Goi khi du 2 nguoi hoac khi choi lai. */
    private void batDauDatTau() {
        state = RoomState.PLACING;
        game = new GameSession(p1, p2);
        guiTrangThai();
        broadcast(Packet.of(PacketType.PLACE_PHASE));
    }

    /** Mot nguoi roi phong - chu dong bam roi, hoac mat ket noi. */
    public synchronized void leave(ClientHandler c) {
        if (!contains(c)) {
            return;
        }
        ClientHandler conLai = (c == p1) ? p2 : p1;

        if (c == p1) {
            p1 = null;
        } else {
            p2 = null;
        }
        c.setRoom(null);
        c.send(Packet.of(PacketType.ROOM_LEFT));

        // Dang danh do ma bo di thi nguoi con lai duoc xu thang.
        if (game != null && conLai != null) {
            game.abortBecauseLeft(c);
        }
        game = null;

        // Nguoi con lai cung bo di trong luc dang cho doi thu vao lai:
        // khong con ai, huy luon viec cho va bo phong. Van dau khong duoc
        // ghi vao CSDL vi ca hai ben deu khong con o day.
        if (tenChoVaoLai != null) {
            System.out.println("Phong #" + id + ": huy cho " + tenChoVaoLai
                    + " vao lai, nguoi con lai da roi phong.");
            tenChoVaoLai = null;
        }

        if (conLai != null) {
            p1 = conLai;
            p2 = null;
            state = RoomState.WAITING;
            conLai.send(Packet.of(PacketType.SYSTEM,
                    c.getUsername() + " da roi phong. Ban tro thanh chu phong."));
            guiTrangThai();
        } else {
            manager.removeRoom(this);
        }
        manager.broadcastRoomList();
    }

    /* ------------------------------------------------------------------ */
    /* Rot mang va vao lai van                                            */
    /* ------------------------------------------------------------------ */

    /**
     * MOT KET NOI VUA CHET - quyet dinh giu phong hay bo phong.
     *
     * <p>{@link ClientHandler#donDep()} goi ham nay thay vi goi thang
     * {@link #leave(ClientHandler)}. Co hai duong di:
     *
     * <ul>
     *   <li><b>Dang danh do</b> ({@link RoomState#PLAYING}, van dau da bat
     *       dau va chua ket thuc): KHONG xu thua. Go handler ra khoi cho
     *       nhung giu nguyen ban do, tam dung dong ho, chuyen phong sang
     *       {@link RoomState#PAUSED} va cho nguoi do dang nhap lai trong
     *       {@link Protocol#RECONNECT_SECONDS} giay.</li>
     *   <li><b>Moi truong hop khac</b> (dang cho nguoi choi, dang dat tau,
     *       van da ket thuc): xu ly nhu roi phong binh thuong. Luc nay chua
     *       co gi dang de giu lai, va giu phong chi lam ro danh sach lobby.
     *   </li>
     * </ul>
     *
     * <p>Chu y thu tu: phai kiem tra {@code state} va {@code game} TRUOC khi
     * go handler ra, vi go ra roi thi khong con biet ai la nguoi con lai.
     */
    public synchronized void ketNoiBiMat(ClientHandler c) {
        if (!contains(c)) {
            return;
        }
        boolean dangDanhDo = state == RoomState.PLAYING
                && game != null && game.isStarted() && !game.isFinished();
        if (!dangDanhDo) {
            leave(c);
            return;
        }

        ClientHandler conLai = (c == p1) ? p2 : p1;
        if (conLai == null) {
            // Khong con ai de cho - giu phong lam gi.
            leave(c);
            return;
        }

        if (c == p1) {
            p1 = null;
        } else {
            p2 = null;
        }
        c.setRoom(null);

        tenChoVaoLai = c.getUsername();
        batDauChoMs = System.currentTimeMillis();
        state = RoomState.PAUSED;
        game.tamDung();

        conLai.send(Packet.of(PacketType.OPPONENT_LOST, tenChoVaoLai,
                String.valueOf(Protocol.RECONNECT_SECONDS)));
        guiTrangThai();
        manager.broadcastRoomList();

        System.out.println("Phong #" + id + ": " + tenChoVaoLai
                + " mat ket noi giua van. Tam dung, cho vao lai trong "
                + Protocol.RECONNECT_SECONDS + " giay.");
    }

    /**
     * Phong nay co the them doi thu may vao duoc khong.
     *
     * <p>Chi cho khi dang con dung mot cho trong va chua vao van: them bot
     * giua van dau hoac khi dang cho nguoi rot mang vao lai deu vo nghia.
     *
     * @return null neu duoc, nguoc lai la ma loi
     */
    public synchronized String coTheThemBot() {
        if (tenChoVaoLai != null) {
            return Protocol.E_BAD_STATE;
        }
        if (state != RoomState.WAITING) {
            return Protocol.E_BAD_STATE;
        }
        if (isFull()) {
            return Protocol.E_ROOM_FULL;
        }
        return null;
    }

    /** Phong nay co dang cho dung nguoi ten {@code ten} vao lai khong. */
    public synchronized boolean dangChoVaoLai(String ten) {
        return tenChoVaoLai != null && ten != null
                && tenChoVaoLai.equalsIgnoreCase(ten);
    }

    /**
     * NGUOI ROT MANG DA DANG NHAP LAI - khoi phuc nguyen van van dau.
     *
     * <p>Goi tu {@link ClientHandler} ngay sau khi dang nhap thanh cong.
     * Trinh tu phai dung thu tu nay:
     * <ol>
     *   <li>Dat handler moi vao cho trong, bao no thuoc phong nay.</li>
     *   <li>{@link GameSession#thayNguoiChoi} - doi tham chieu nguoi choi
     *       trong van dau sang handler moi.</li>
     *   <li>Gui ROOM_JOINED de giao dien client chuyen sang man hinh phong.
     *   </li>
     *   <li>Gui RESUME_DATA de ve lai hai ban co.</li>
     *   <li>{@link GameSession#tiepTuc} - chay lai dong ho va gui TURN cho
     *       CA HAI ben.</li>
     * </ol>
     *
     * <p>Gui RESUME_DATA truoc TURN la bat buoc: TURN bat dong ho dem nguoc
     * tren man hinh, ma dong ho chi co nghia khi ban co da ve xong.
     *
     * @return null neu thanh cong, nguoc lai la ma loi
     */
    public synchronized String vaoLai(ClientHandler c) {
        if (!dangChoVaoLai(c.getUsername())) {
            return Protocol.E_BAD_STATE;
        }
        if (game == null || game.isFinished()) {
            return Protocol.E_BAD_STATE;
        }

        if (p1 == null) {
            p1 = c;
        } else if (p2 == null) {
            p2 = c;
        } else {
            return Protocol.E_ROOM_FULL;
        }
        c.setRoom(this);

        String ten = tenChoVaoLai;
        tenChoVaoLai = null;
        state = RoomState.PLAYING;

        if (!game.thayNguoiChoi(ten, c)) {
            return Protocol.E_BAD_STATE;
        }

        c.send(Packet.withPayload(PacketType.ROOM_JOINED, info(),
                c == p1 ? "1" : "0"));
        game.guiLaiTrangThai(c);

        ClientHandler conLai = (c == p1) ? p2 : p1;
        if (conLai != null) {
            conLai.send(Packet.of(PacketType.OPPONENT_BACK, ten));
        }
        guiTrangThai();
        game.tiepTuc();
        manager.broadcastRoomList();

        System.out.println("Phong #" + id + ": " + ten
                + " da vao lai van dang do.");
        return null;
    }

    /**
     * HET THOI GIAN AN HAN CHUA - thread canh gac goi moi giay.
     *
     * <p>Qua {@link Protocol#RECONNECT_SECONDS} giay ma nguoi rot mang khong
     * vao lai thi nguoi con lai duoc xu thang, tran dau duoc ghi vao CSDL,
     * va phong tro ve {@link RoomState#WAITING} de ho tim doi thu moi.
     */
    public synchronized void kiemTraChoVaoLai() {
        if (state != RoomState.PAUSED || tenChoVaoLai == null) {
            return;
        }
        long daCho = (System.currentTimeMillis() - batDauChoMs) / 1000;
        if (daCho < Protocol.RECONNECT_SECONDS) {
            return;
        }

        ClientHandler conLai = (p1 != null) ? p1 : p2;
        String ten = tenChoVaoLai;
        tenChoVaoLai = null;

        System.out.println("Phong #" + id + ": " + ten + " khong vao lai trong "
                + Protocol.RECONNECT_SECONDS + " giay, xu thua.");

        if (game != null) {
            game.ketThucViKhongVaoLai(conLai);
        }
        game = null;

        if (conLai == null) {
            manager.removeRoom(this);
        } else {
            p1 = conLai;
            p2 = null;
            state = RoomState.WAITING;
            conLai.send(Packet.of(PacketType.SYSTEM,
                    ten + " khong vao lai duoc. Ban duoc xu thang."));
            guiTrangThai();
        }
        manager.broadcastRoomList();
    }

    /* ------------------------------------------------------------------ */
    /* Trong van dau                                                      */
    /* ------------------------------------------------------------------ */

    /**
     * Nhan so do dat tau cua mot nguoi choi.
     *
     * <p>Day la cho server KHONG DUOC TIN client. Ban do nhan ve phai qua
     * {@link Board#kiemTraHopLe()} truoc khi chap nhan.
     *
     * @return null neu hop le, nguoc lai la ma loi
     */
    public synchronized String handleReady(ClientHandler c, Board board) {
        if (!contains(c)) {
            return Protocol.E_NOT_IN_ROOM;
        }
        if (state != RoomState.PLACING || game == null) {
            return Protocol.E_BAD_STATE;
        }
        if (board == null) {
            return Protocol.E_BAD_PLACEMENT;
        }

        String loi = board.kiemTraHopLe();
        if (loi != null) {
            System.out.println("Tu choi so do cua " + c.getUsername() + ": " + loi);
            return Protocol.E_BAD_PLACEMENT;
        }

        boolean caHaiSanSang = game.setBoard(c, board);
        c.send(Packet.of(PacketType.READY_OK));
        c.send(Packet.of(PacketType.SYSTEM, "So do hop le. Dang cho doi thu..."));

        if (caHaiSanSang) {
            state = RoomState.PLAYING;
            guiTrangThai();
            game.start();
            manager.broadcastRoomList();
        }
        return null;
    }

    /**
     * Kiem tra nguoi dang danh co het gio suy nghi chua.
     * Thread canh gac ben {@link ServerMain} goi moi giay.
     */
    public synchronized void kiemTraHetGio() {
        if (state == RoomState.PLAYING && game != null) {
            game.kiemTraHetGio();
        }
    }

    /** @return null neu hop le, nguoc lai la ma loi */
    public synchronized String handleFire(ClientHandler c, int x, int y) {
        if (!contains(c)) {
            return Protocol.E_NOT_IN_ROOM;
        }
        if (state != RoomState.PLAYING || game == null) {
            return Protocol.E_BAD_STATE;
        }

        String loi = game.fire(c, x, y);
        if (loi != null) {
            return loi;
        }

        // Van dau vua ket thuc: mo luon mot van moi cho hai nguoi choi lai.
        if (game.isFinished()) {
            batDauDatTau();
            manager.broadcastRoomList();
        }
        return null;
    }
}
