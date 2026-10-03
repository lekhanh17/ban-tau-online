package bantau.bot;

import bantau.common.BaoMat;
import bantau.common.Board;
import bantau.common.FireResult;
import bantau.common.Packet;
import bantau.common.PacketType;
import bantau.common.Protocol;
import bantau.common.RoomInfo;
import bantau.common.RoomState;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
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
 * {@link Socket} noi vao server, tu dang ky tai khoan, tu dang nhap, tu vao
 * phong, tu gui READY va FIRE - bang dung nhung ban tin BSP ma client Swing
 * dang dung. Server khong he biet day la may hay nguoi.
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
 * <p><b>Thuat toan:</b> san - diet (hunt and target), khong phai hoc may.
 * Xem {@link #chonOBan()}.
 */
public final class BotClient implements Runnable {

    private static final SecureRandom NGAU_NHIEN_AN_TOAN = new SecureRandom();
    private static int demBot;

    private final String host;
    private final int cong;
    private final int roomId;
    private final String ten;
    private final String matKhau;

    private final Random rnd = new Random();

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    /** Ban do that cua bot, de biet minh con tau nao. */
    private final Board banDoMinh = new Board();

    /** O nao bot da ban roi - khong ban lai. */
    private final boolean[][] daBan = new boolean[Board.SIZE][Board.SIZE];

    /**
     * Hang doi o can thu tiep theo. Khi ban TRUNG mot o, bon o ke no duoc
     * day vao day de thu - vi tau nam lien nhau.
     */
    private final Deque<int[]> oCanThu = new ArrayDeque<>();

    /** Van dau da tung bat dau chua - de biet khi nao nen roi phong. */
    private boolean daVaoTran;

    public BotClient(String host, int cong, int roomId) {
        this.host = host;
        this.cong = cong;
        this.roomId = roomId;

        // Ten ngau nhien cho moi lan: tranh dung do khi nguoi choi mo nhieu
        // phong co bot cung luc. Mat khau cung ngau nhien va chi ton tai
        // trong bo nho tien trinh - khong ai dang nhap duoc bang tai khoan
        // nay vi khong ai biet mat khau.
        demBot++;
        this.ten = Protocol.BOT_PREFIX + demBot + "_" + chuoiNgauNhien(4);
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
        System.out.println("[BOT " + ten + "] da ket noi, dang dang nhap.");
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
            case FIRE_RESULT -> ghiNhanKetQua(
                    p.intArg(0, -1), p.intArg(1, -1), p.arg(2));

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
        for (int x = 0; x < Board.SIZE; x++) {
            for (int y = 0; y < Board.SIZE; y++) {
                daBan[x][y] = false;
            }
        }
        oCanThu.clear();
    }

    /* ------------------------------------------------------------------ */
    /* Chon nuoc di - THUAT TOAN SAN VA DIET                               */
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
        int[] o = chonOBan();
        if (o == null) {
            return;
        }
        daBan[o[0]][o[1]] = true;
        gui(Packet.of(PacketType.FIRE, String.valueOf(o[0]), String.valueOf(o[1])));
    }

    /**
     * CHON O DE BAN - hai che do.
     *
     * <p><b>DIET:</b> neu hang doi {@link #oCanThu} con o, ban vao do truoc.
     * Day la cac o ke mot o vua ban trung - tau nam lien nhau nen phan con
     * lai cua no chac chan o mot trong bon huong.
     *
     * <p><b>SAN:</b> het o can thu thi ban mo moi, nhung khong ban bua. Chi
     * ban vao cac o co {@code (x + y)} chan - kieu ban co.
     *
     * <p>Vi sao ban kieu ban co: con tau ngan nhat dai 2 o, nen moi con tau
     * deu chac chan che it nhat mot o thuoc luoi ban co. Chi can quet nua
     * ban do la tim duoc het tau, thay vi quet ca 100 o. Het o ban co moi
     * ban sang nhung o con lai.
     *
     * @return toa do {x, y}, hoac null neu het o
     */
    private int[] chonOBan() {
        // --- Che do DIET ---
        while (!oCanThu.isEmpty()) {
            int[] o = oCanThu.poll();
            if (hopLe(o[0], o[1])) {
                return o;
            }
        }

        // --- Che do SAN ---
        List<int[]> banCo = new ArrayList<>();
        List<int[]> conLai = new ArrayList<>();
        for (int x = 0; x < Board.SIZE; x++) {
            for (int y = 0; y < Board.SIZE; y++) {
                if (!hopLe(x, y)) {
                    continue;
                }
                if ((x + y) % 2 == 0) {
                    banCo.add(new int[] { x, y });
                } else {
                    conLai.add(new int[] { x, y });
                }
            }
        }
        List<int[]> nguon = banCo.isEmpty() ? conLai : banCo;
        if (nguon.isEmpty()) {
            return null;
        }
        Collections.shuffle(nguon, rnd);
        return nguon.get(0);
    }

    private boolean hopLe(int x, int y) {
        return Board.inBounds(x, y) && !daBan[x][y];
    }

    /**
     * Nhan ket qua phat ban cua minh va cap nhat ke hoach.
     *
     * <p>TRUNG thi day bon o ke vao hang doi de thu tiep. CHIM thi xoa hang
     * doi: con tau do xong roi, nhung o ke con lai khong con y nghia, giu
     * lai chi lam bot phi dan.
     */
    private void ghiNhanKetQua(int x, int y, String ketQua) {
        if (!Board.inBounds(x, y)) {
            return;
        }
        daBan[x][y] = true;

        if (FireResult.SUNK.name().equals(ketQua)) {
            oCanThu.clear();
            return;
        }
        if (!FireResult.HIT.name().equals(ketQua)) {
            return;
        }
        int[][] ke = { { x + 1, y }, { x - 1, y }, { x, y + 1 }, { x, y - 1 } };
        for (int[] o : ke) {
            if (hopLe(o[0], o[1])) {
                oCanThu.add(o);
            }
        }
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
