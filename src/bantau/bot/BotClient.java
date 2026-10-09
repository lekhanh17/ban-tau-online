package bantau.bot;

import bantau.common.BaoMat;
import bantau.common.Board;
import bantau.common.FireResult;
import bantau.common.MucDoBot;
import bantau.common.Packet;
import bantau.common.PacketType;
import bantau.common.Protocol;
import bantau.common.RoomInfo;
import bantau.common.RoomState;
import bantau.common.ShipType;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Random;

/**
 * DOI THU MAY - MOT CLIENT TCP DOC LAP, KHONG PHAI LOGIC TRONG SERVER
 *
 * <p>Day la diem thiet ke quan trong nhat cua lop nay, va cung la ly do no
 * co gia tri voi mon Lap trinh mang may tinh.
 *
 * <p>Cach de nhat de lam doi thu may la cho server tu danh thay: trong
 * {@code GameSession}, den luot "may" thi goi mot ham tinh nuoc di roi cap
 * nhat ban do luon. Lam vay thi khong co goi tin nao di qua mang ca - do
 * thuan tuy la logic game, khong lien quan gi den mang.
 *
 * <p>Lop nay lam nguoc lai. No la <b>mot client that</b>: tu mo
 * {@link Socket} noi vao server, tu dang nhap, tu vao phong, tu gui READY va
 * FIRE - bang dung nhung ban tin BSP ma client Swing dang dung. Server khong
 * he biet day la may hay nguoi.
 *
 * <pre>
 *   ClientMain (Swing)  ---BSP/TCP--->  Server  &lt;---BSP/TCP---  BotClient
 * </pre>
 *
 * <p>Nho vay bot chung minh duoc ba dieu ma khong phan nao khac chung minh
 * duoc:
 * <ol>
 *   <li><b>Giao thuc BSP day du va doc lap voi giao dien.</b> Mot chuong
 *       trinh khong co mot dong Swing nao van choi tron ven mot van chi bang
 *       cac ban tin da dinh nghia.</li>
 *   <li><b>Server xu ly dung nhieu ket noi dong thoi.</b> Bot la mot ket noi
 *       that, chiem mot thread that trong pool.</li>
 *   <li><b>Co the do tai.</b> Bat nhieu bot cung luc la co ngay cong cu do
 *       hieu nang, khong can viet them gi.</li>
 * </ol>
 *
 * <h2>Lop nay KHONG chua thuat toan</h2>
 *
 * <p>Viec chon o ban duoc giao cho {@link ChienThuatBan} - ba muc do la ba
 * lop khac nhau. Lop nay chi lo phan mang: dang nhap, vao phong, gui va nhan
 * ban tin. Nho tach nhu vay ma do duoc so lieu thuat toan bang
 * {@code bantau.tools.MoPhongBot} ma khong can dung server.
 */
public final class BotClient implements Runnable {

    private static final SecureRandom NGAU_NHIEN_AN_TOAN = new SecureRandom();
    private static int demBot;

    private final String host;
    private final int cong;
    private final int roomId;
    private final String ten;
    private final String matKhau;
    private final MucDoBot mucDo;

    private final Random rnd = new Random();

    /** Cach chon o ban, tuy theo muc do kho. */
    private final ChienThuatBan chienThuat;

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    /** Ban do that cua bot, de biet minh con tau nao. */
    private final Board banDoMinh = new Board();

    /**
     * O nao da GUI phat ban di - khong phai o nao da biet ket qua.
     *
     * <p>Hai thu nay khac nhau, va day la cho de sinh loi. Chien thuat chi
     * cap nhat hieu biet khi NHAN duoc {@code FIRE_RESULT}. Neu server tu
     * choi phat ban (sai luot, o da ban) thi khong co ket qua nao ve, chien
     * thuat van tuong o do chua ban va co the chon lai dung o do - lap vo
     * han. Mang nay chan truong hop do.
     */
    private final boolean[][] daGui = new boolean[Board.SIZE][Board.SIZE];

    /** Van dau da tung bat dau chua - de biet khi nao nen roi phong. */
    private boolean daVaoTran;

    public BotClient(String host, int cong, int roomId) {
        this(host, cong, roomId, MucDoBot.THUONG);
    }

    public BotClient(String host, int cong, int roomId, MucDoBot mucDo) {
        this.host = host;
        this.cong = cong;
        this.roomId = roomId;
        this.mucDo = mucDo == null ? MucDoBot.THUONG : mucDo;
        this.chienThuat = ChienThuatBan.tao(this.mucDo);

        // Ten ngau nhien cho moi lan: tranh dung do khi nguoi choi mo nhieu
        // phong co bot cung luc. Mat khau cung ngau nhien va chi ton tai
        // trong bo nho tien trinh - khong ai dang nhap duoc bang tai khoan
        // nay vi khong ai biet mat khau.
        //
        // Ten co kem mot chu cho muc do (D/T/K) de nhin nhat ky hay anh chup
        // la biet ngay van do danh voi muc nao. Phai dung chu viet tat vi
        // Protocol.NAME_MAX chi cho 16 ky tu.
        demBot++;
        this.ten = Protocol.BOT_PREFIX + this.mucDo.name().charAt(0)
                + demBot + "_" + chuoiNgauNhien(4);
        this.matKhau = chuoiNgauNhien(16);
    }

    private static String chuoiNgauNhien(int doDai) {
        String bang = "abcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder(doDai);
        for (int i = 0; i < doDai; i++) {
            sb.append(bang.charAt(NGAU_NHIEN_AN_TOAN.nextInt(bang.length())));
        }
        return sb.toString();
    }

    public String ten() {
        return ten;
    }

    public String matKhau() {
        return matKhau;
    }

    public MucDoBot mucDo() {
        return mucDo;
    }

    /**
     * Khoi dong bot tren thread rieng kieu daemon: server khong phai cho no,
     * va no khong giu server song khi server dung.
     *
     * <p><b>Goi ham nay SAU khi da dang ky tai khoan cho bot.</b> Bot khong
     * tu gui REGISTER duoc, vi server chan moi ten bat dau bang
     * {@link Protocol#BOT_PREFIX} o duong REGISTER cong khai. Viec dang ky
     * phai lam tu ben trong server, qua thang {@code PlayerDao}.
     */
    public void chay() {
        Thread t = new Thread(this, "bot-" + ten);
        t.setDaemon(true);
        t.start();
    }

    /* ------------------------------------------------------------------ */
    /* Vong doi                                                            */
    /* ------------------------------------------------------------------ */

    @Override
    public void run() {
        try {
            ketNoi();
            vongLapDoc();
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("[BOT " + ten + "] ket thuc: " + e.getMessage());
        } finally {
            dong();
        }
    }

    /**
     * Mo ket noi va dang nhap.
     *
     * <p>Dung chung {@link BaoMat#taoSocket} voi client Swing, nen bot tu
     * dong chay dung che do ma hoa ma server dang chay - khong phai cau hinh
     * gi them.
     */
    private void ketNoi() throws IOException {
        socket = BaoMat.taoSocket(host, cong, 5000);
        socket.setTcpNoDelay(true);

        // THU TU BAT BUOC: luong ra truoc, flush(), roi moi luong vao.
        out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        in = new ObjectInputStream(socket.getInputStream());

        // Tai khoan da duoc server dang ky san truoc khi chay bot, nen o day
        // chi can dang nhap. Bot KHONG tu gui REGISTER duoc vi server chan
        // moi ten bat dau bang BOT_PREFIX o duong REGISTER cong khai.
        gui(Packet.of(PacketType.LOGIN, ten, matKhau));
        System.out.println("[BOT " + ten + "] da ket noi (muc " + mucDo.ten()
                + ", thuat toan: " + chienThuat.ten() + "), dang dang nhap.");
    }

    private void vongLapDoc() throws IOException, ClassNotFoundException {
        while (true) {
            Object o = in.readObject();
            if (o instanceof Packet p && !xuLy(p)) {
                return;
            }
        }
    }

    /** @return false neu bot nen dung han */
    private boolean xuLy(Packet p) {
        switch (p.type()) {
            // Tra loi ngay, neu khong server coi nhu bot da chet va ngat.
            case PING -> gui(Packet.of(PacketType.PONG));

            case LOGIN_OK -> {
                gui(Packet.of(PacketType.ROOM_JOIN, String.valueOf(roomId)));
                System.out.println("[BOT " + ten + "] dang nhap xong, xin vao"
                        + " phong #" + roomId + ".");
            }

            case PLACE_PHASE -> datTau();

            case GAME_START -> daVaoTran = true;

            case TURN -> {
                if (ten.equals(p.arg(0))) {
                    banMotPhat();
                }
            }

            // Ket qua phat ban cua chinh bot - dung de chon o ban tiep theo.
            case FIRE_RESULT -> ghiNhanKetQua(p);

            // Doi thu ban vao bot - cap nhat ban do cua minh cho dung.
            case INCOMING -> banDoMinh.fire(p.intArg(0, -1), p.intArg(1, -1));

            case GAME_OVER -> {
                System.out.println("[BOT " + ten + "] het van: " + p.arg(0));
                xoaTrangThaiVan();
            }

            // Phong quay ve WAITING sau khi da danh xong nghia la nguoi choi
            // da roi di. Khong con ai de choi, bot thoat luon - neu khong
            // phong se ton tai mai voi mot con bot ngoi cho.
            case ROOM_STATE -> {
                RoomInfo info = p.payload(RoomInfo.class);
                if (daVaoTran && info != null && info.state() == RoomState.WAITING) {
                    System.out.println("[BOT " + ten + "] nguoi choi da roi"
                            + " phong, bot thoat.");
                    gui(Packet.of(PacketType.QUIT));
                    return false;
                }
            }

            case ROOM_LEFT -> {
                return false;
            }

            case ERROR -> {
                System.out.println("[BOT " + ten + "] loi tu server: "
                        + p.arg(0) + " - " + p.arg(1));
                // Khong vao duoc phong thi khong co viec gi de lam nua.
                if (Protocol.E_ROOM_FULL.equals(p.arg(0))
                        || Protocol.E_ROOM_NOT_FOUND.equals(p.arg(0))) {
                    return false;
                }
            }

            default -> {
                // Cac ban tin khac bot khong quan tam.
            }
        }
        return true;
    }

    /* ------------------------------------------------------------------ */
    /* Dat tau                                                             */
    /* ------------------------------------------------------------------ */

    private void datTau() {
        xoaTrangThaiVan();
        banDoMinh.randomPlace(rnd);
        gui(Packet.withPayload(PacketType.READY, banDoMinh));
    }

    private void xoaTrangThaiVan() {
        for (boolean[] cot : daGui) {
            Arrays.fill(cot, false);
        }
        chienThuat.batDauVanMoi();
    }

    /* ------------------------------------------------------------------ */
    /* Ban                                                                 */
    /* ------------------------------------------------------------------ */

    /**
     * Ban mot phat, co cho mot chut cho nguoi choi kip nhin.
     *
     * <p>Cho bang {@link Thread#sleep} o day la AN TOAN vi bot chay tren
     * thread rieng cua no, khong phai thread giao dien. Lam vay trong client
     * Swing thi treo ca cua so.
     */
    private void banMotPhat() {
        try {
            Thread.sleep(Protocol.BOT_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
        int[] o = chonOChuaGui();
        if (o == null) {
            return;
        }
        daGui[o[0]][o[1]] = true;
        gui(Packet.of(PacketType.FIRE, String.valueOf(o[0]), String.valueOf(o[1])));
    }

    /**
     * Xin chien thuat chon o, va bo qua nhung o da gui phat ban di roi.
     *
     * <p>Thu vai lan vi chien thuat co the chon lai dung o do khi no chua
     * nhan duoc ket qua. Het luot thu thi ban bat ky o nao chua gui - mien
     * la van di tiep, khong treo.
     */
    private int[] chonOChuaGui() {
        for (int lan = 0; lan < 5; lan++) {
            int[] o = chienThuat.chonO(rnd);
            if (o == null) {
                break;
            }
            if (Board.inBounds(o[0], o[1]) && !daGui[o[0]][o[1]]) {
                return o;
            }
        }
        for (int x = 0; x < Board.SIZE; x++) {
            for (int y = 0; y < Board.SIZE; y++) {
                if (!daGui[x][y]) {
                    return new int[] { x, y };
                }
            }
        }
        return null;
    }

    /**
     * Chuyen {@code FIRE_RESULT} cho chien thuat.
     *
     * <p>args: x, y, ten {@link FireResult}, va ma tau neu tau vua chim.
     * Ma tau la thong tin quan trong voi muc KHO: biet tau nao da chim thi
     * loai duoc no khoi phep dem cac cach dat.
     */
    private void ghiNhanKetQua(Packet p) {
        int x = p.intArg(0, -1);
        int y = p.intArg(1, -1);
        if (!Board.inBounds(x, y)) {
            return;
        }
        daGui[x][y] = true;

        FireResult kq;
        try {
            kq = FireResult.valueOf(p.arg(2));
        } catch (IllegalArgumentException e) {
            return;
        }

        ShipType tauChim = null;
        String ma = p.arg(3);
        if (kq == FireResult.SUNK && !ma.isEmpty()) {
            tauChim = ShipType.fromCode(ma.charAt(0));
        }
        chienThuat.ghiKetQua(x, y, kq, tauChim);
    }

    /* ------------------------------------------------------------------ */

    private synchronized void gui(Packet p) {
        if (out == null) {
            return;
        }
        try {
            out.writeObject(p);
            out.flush();
            // Bat buoc: khong reset thi lan sau gui cung doi tuong se chi
            // gui tham chieu cu, ben nhan doc ra du lieu cu.
            out.reset();
        } catch (IOException e) {
            dong();
        }
    }

    private void dong() {
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ignored) {
            // het viec
        }
    }
}
