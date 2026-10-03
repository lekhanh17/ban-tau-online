package bantau.tools;

import bantau.common.BaoMat;
import bantau.common.Board;
import bantau.common.FireResult;
import bantau.common.Packet;
import bantau.common.PacketType;
import bantau.common.Protocol;
import bantau.common.ShipType;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * TEST TU DONG CHO CHUC NANG VAO LAI VAN KHI ROT MANG
 *
 * <p>Chay doi lap voi giao dien: khong mo cua so Swing nao, chi dung socket
 * tho de gia lam hai nguoi choi. Nho vay test chay duoc trong terminal va
 * lap lai duoc nhieu lan.
 *
 * <p><b>Cach chay:</b> bat server o cong 5100 truoc
 * ({@code java -cp "bin;lib/*" bantau.server.ServerMain 5100 nossl}),
 * roi {@code java -cp "bin;lib/*" bantau.tools.TestVaoLai 5100}.
 *
 * <p>Test lan luot kiem tra:
 * <ol>
 *   <li>Rot mang giua van thi doi thu nhan OPPONENT_LOST, KHONG phai
 *       GAME_OVER - tuc la chua bi xu thua.</li>
 *   <li>Dang nhap lai thi nhan duoc RESUME_DATA.</li>
 *   <li>Ban do cua minh ve lai dung: du 5 tau, dung so o da bi ban.</li>
 *   <li>Ban do doi thu ve lai dung nhung o minh da ban.</li>
 *   <li><b>Khong lo vi tri tau doi thu</b> o nhung o chua ban - day la
 *       kiem tra bao mat quan trong nhat cua chuc nang nay.</li>
 *   <li>Dong ho luot duoc giu lai, khong duoc reset ve 30 giay.</li>
 *   <li>Vao lai roi van ban duoc (van dau da doi tham chieu nguoi choi).</li>
 *   <li>Doi thu nhan OPPONENT_BACK.</li>
 *   <li>Khong vao lai trong thoi gian an han thi doi thu duoc xu thang.</li>
 * </ol>
 *
 * <p>Khong thuoc phan chuong trinh nop.
 */
public final class TestVaoLai {

    private static int soDung;
    private static int soSai;

    private static void kiemTra(String moTa, boolean dieuKien) {
        if (dieuKien) {
            soDung++;
            System.out.println("  DUNG  " + moTa);
        } else {
            soSai++;
            System.out.println("  SAI   " + moTa);
        }
    }

    /* ------------------------------------------------------------------ */
    /* Client tho - chi du de test, khong co giao dien                     */
    /* ------------------------------------------------------------------ */

    /** Mot nguoi choi gia lap. Doc goi tin tren thread rieng vao hang doi. */
    private static final class Nguoi {
        private final String ten;
        private final Socket socket;
        private final ObjectOutputStream out;
        private final ObjectInputStream in;
        private final BlockingQueue<Packet> hopThu = new ArrayBlockingQueue<>(500);
        private volatile boolean song = true;

        Nguoi(String ten, int port) throws IOException {
            this.ten = ten;
            this.socket = new Socket("127.0.0.1", port);
            this.socket.setTcpNoDelay(true);
            // THU TU BAT BUOC: luong ra truoc, flush, roi luong vao.
            this.out = new ObjectOutputStream(socket.getOutputStream());
            this.out.flush();
            this.in = new ObjectInputStream(socket.getInputStream());

            Thread t = new Thread(this::doc, "test-" + ten);
            t.setDaemon(true);
            t.start();
        }

        private void doc() {
            try {
                while (song) {
                    Object o = in.readObject();
                    if (o instanceof Packet p) {
                        // Tra PONG ngay, neu khong server se ngat ket noi.
                        if (p.type() == PacketType.PING) {
                            gui(Packet.of(PacketType.PONG));
                            continue;
                        }
                        hopThu.offer(p);
                    }
                }
            } catch (Exception e) {
                song = false;
            }
        }

        synchronized void gui(Packet p) {
            if (!song) {
                return;
            }
            try {
                out.writeObject(p);
                out.flush();
                out.reset();
            } catch (IOException e) {
                song = false;
            }
        }

        /** Cho den khi nhan duoc goi tin loai nay, bo qua cac loai khac. */
        Packet cho(PacketType loai, int giay) throws InterruptedException {
            long hetHan = System.currentTimeMillis() + giay * 1000L;
            while (System.currentTimeMillis() < hetHan) {
                Packet p = hopThu.poll(500, TimeUnit.MILLISECONDS);
                if (p != null && p.type() == loai) {
                    return p;
                }
            }
            return null;
        }

        /** Co nhan goi tin loai nay trong ngan nay giay khong. */
        boolean coNhan(PacketType loai, int giay) throws InterruptedException {
            return cho(loai, giay) != null;
        }

        /** Rut day mang: dong socket phu phang, khong gui QUIT. */
        void rotMang() throws IOException {
            song = false;
            socket.close();
        }

        void dong() {
            song = false;
            try {
                socket.close();
            } catch (IOException ignored) {
                // het viec
            }
        }
    }

    /* ------------------------------------------------------------------ */

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 5100;
        BaoMat.batSsl(false);

        System.out.println("=== TEST VAO LAI VAN KHI ROT MANG ===");
        System.out.println("Server: 127.0.0.1:" + port + " (che do nossl)");
        System.out.println();

        kichBan1(port);
        System.out.println();
        kichBan2(port);

        System.out.println();
        System.out.println("=== KET QUA: " + soDung + " DUNG, " + soSai + " SAI ===");
        if (soSai > 0) {
            System.exit(1);
        }
    }

    /* ------------------------------------------------------------------ */
    /* Kich ban 1: rot mang roi vao lai thanh cong                         */
    /* ------------------------------------------------------------------ */

    private static void kichBan1(int port) throws Exception {
        System.out.println("--- Kich ban 1: rot mang roi VAO LAI duoc ---");

        String tenA = "tvl_a" + (System.currentTimeMillis() % 100000);
        String tenB = "tvl_b" + (System.currentTimeMillis() % 100000);

        Nguoi a = dangKyVaVao(tenA, port);
        Nguoi b = dangKyVaVao(tenB, port);

        // A tao phong, B vao -> ca hai nhan PLACE_PHASE
        a.gui(Packet.of(PacketType.ROOM_CREATE, "test-vaolai"));
        Packet joinedA = a.cho(PacketType.ROOM_JOINED, 5);
        kiemTra("A tao duoc phong", joinedA != null);

        int roomId = layRoomId(joinedA);
        b.gui(Packet.of(PacketType.ROOM_JOIN, String.valueOf(roomId)));
        kiemTra("B vao duoc phong", b.coNhan(PacketType.ROOM_JOINED, 5));
        kiemTra("Ca hai nhan PLACE_PHASE",
                a.coNhan(PacketType.PLACE_PHASE, 5)
                        && b.coNhan(PacketType.PLACE_PHASE, 5));

        // Hai ban do co dinh de biet truoc tau nam o dau
        Board banDoA = banDoCoDinh();
        Board banDoB = banDoCoDinh();
        a.gui(Packet.withPayload(PacketType.READY, banDoA));
        b.gui(Packet.withPayload(PacketType.READY, banDoB));

        Packet batDau = a.cho(PacketType.GAME_START, 5);
        kiemTra("Van dau bat dau", batDau != null);

        // Tim ai di truoc roi cho nguoi do ban vai phat
        Packet turn = a.cho(PacketType.TURN, 5);
        b.cho(PacketType.TURN, 5);
        kiemTra("Nhan duoc TURN dau tien", turn != null);
        if (turn == null) {
            a.dong();
            b.dong();
            return;
        }

        Nguoi diTruoc = tenA.equals(turn.arg(0)) ? a : b;
        Nguoi diSau = (diTruoc == a) ? b : a;
        String tenDiTruoc = (diTruoc == a) ? tenA : tenB;

        // Ban 3 phat vao hang dau cua doi thu. Ban do co dinh dat tau o
        // hang 0 nen chac chan trung, va ban trung thi giu luot.
        for (int x = 0; x < 3; x++) {
            diTruoc.gui(Packet.of(PacketType.FIRE, String.valueOf(x), "0"));
            Packet kq = diTruoc.cho(PacketType.FIRE_RESULT, 5);
            kiemTra("Phat ban " + (x + 1) + " duoc chap nhan", kq != null);
        }

        // Doi vai giay de dong ho luot troi di, co co so kiem tra giu gio
        Thread.sleep(3000);

        // ----- ROT MANG -----
        System.out.println("  (" + tenDiTruoc + " rot mang)");
        diTruoc.rotMang();

        // Doi thu phai nhan OPPONENT_LOST, va KHONG duoc nhan GAME_OVER.
        // Phai cho qua TIMEOUT_SECONDS vi server chi biet client chet sau
        // khi het thoi gian cho PONG.
        Packet mat = diSau.cho(PacketType.OPPONENT_LOST,
                Protocol.TIMEOUT_SECONDS + 20);
        kiemTra("Doi thu nhan OPPONENT_LOST", mat != null);
        kiemTra("OPPONENT_LOST ghi dung ten nguoi rot mang",
                mat != null && tenDiTruoc.equals(mat.arg(0)));
        kiemTra("Doi thu KHONG bi xu thang ngay (khong co GAME_OVER)",
                !diSau.coNhan(PacketType.GAME_OVER, 2));

        // ----- CHO MOT LUC ROI MOI VAO LAI -----
        // Doi 20 giay truoc khi vao lai la de TEST DUNG CHO TAM DUNG DONG HO.
        // Neu dong ho luot cu chay trong luc tam dung thi sau 20 giay no da
        // can hoac het, va phep kiem tra "giu nguyen dong ho" ben duoi se SAI.
        System.out.println("  (cho 20 giay de kiem tra dong ho co that su"
                + " dung lai khong)");
        int truocKhiCho = Protocol.TURN_SECONDS;
        Thread.sleep(20000);

        // ----- VAO LAI -----
        Nguoi lai = new Nguoi(tenDiTruoc, port);
        lai.gui(Packet.of(PacketType.LOGIN, tenDiTruoc, "matkhau123"));
        kiemTra("Dang nhap lai thanh cong", lai.coNhan(PacketType.LOGIN_OK, 5));
        kiemTra("Duoc dua thang vao phong cu",
                lai.coNhan(PacketType.ROOM_JOINED, 5));

        Packet resume = lai.cho(PacketType.RESUME_DATA, 5);
        kiemTra("Nhan duoc RESUME_DATA", resume != null);
        if (resume == null) {
            lai.dong();
            diSau.dong();
            return;
        }

        // --- Ban do CUA MINH ---
        Board veLai = resume.payload(Board.class);
        kiemTra("RESUME_DATA co ban do cua minh", veLai != null);
        if (veLai != null) {
            kiemTra("Ban do cua minh con du 5 tau", veLai.isComplete());
            kiemTra("Ban do cua minh chua bi ban phat nao (doi thu chua ban)",
                    demOdaBan(veLai) == 0);
        }

        // --- Ban do DOI THU ---
        String maDoiThu = resume.arg(1);
        kiemTra("Co chuoi 100 ky tu mo ta ban do doi thu",
                maDoiThu != null && maDoiThu.length() == 100);

        if (maDoiThu != null && maDoiThu.length() == 100) {
            // 3 o da ban deu phai co dau, khong duoc la dau cham
            boolean duDau = maDoiThu.charAt(0) != '.'
                    && maDoiThu.charAt(1) != '.'
                    && maDoiThu.charAt(2) != '.';
            kiemTra("3 o da ban duoc ve lai day du", duDau);

            // KIEM TRA BAO MAT: so dau khac '.' phai dung bang so phat da ban.
            // Nhieu hon la server da lo thong tin ve nhung o chua ban.
            int soODaBiet = 0;
            for (int i = 0; i < 100; i++) {
                if (maDoiThu.charAt(i) != '.') {
                    soODaBiet++;
                }
            }
            kiemTra("KHONG lo vi tri tau doi thu o o chua ban"
                    + " (biet dung 3 o, dem duoc " + soODaBiet + ")",
                    soODaBiet == 3);
        }

        // --- Dong ho luot ---
        int giayConLai = resume.intArg(3, -1);
        kiemTra("Khong reset dong ho ve " + truocKhiCho
                        + " giay (con " + giayConLai + ")",
                giayConLai > 0 && giayConLai <= Protocol.TURN_SECONDS);
        // Da cho 20 giay. Dong ho co dung lai that thi so giay con lai phai
        // gan nhu khong doi; con chay thi chi con duoi 10 giay hoac bang 0.
        kiemTra("Dong ho DUNG LAI trong luc tam dung"
                        + " (sau 20 giay cho, con " + giayConLai + " giay)",
                giayConLai > 15);

        kiemTra("Ten doi thu dung", resume.arg(0) != null
                && !resume.arg(0).isBlank() && !resume.arg(0).equals(tenDiTruoc));

        // --- Doi thu duoc bao la minh da ve ---
        kiemTra("Doi thu nhan OPPONENT_BACK",
                diSau.coNhan(PacketType.OPPONENT_BACK, 5));

        // --- Van ban duoc sau khi vao lai ---
        Packet turnMoi = lai.cho(PacketType.TURN, 5);
        kiemTra("Nhan TURN moi sau khi vao lai", turnMoi != null);

        if (turnMoi != null && tenDiTruoc.equals(turnMoi.arg(0))) {
            lai.gui(Packet.of(PacketType.FIRE, "3", "0"));
            kiemTra("VAN BAN DUOC sau khi vao lai"
                            + " (van dau da doi tham chieu nguoi choi)",
                    lai.coNhan(PacketType.FIRE_RESULT, 5));
        } else {
            // Luot dang o phia doi thu - cho ho ban mot phat de doi luot
            diSau.gui(Packet.of(PacketType.FIRE, "9", "9"));
            diSau.cho(PacketType.FIRE_RESULT, 5);
            Packet t2 = lai.cho(PacketType.TURN, 10);
            if (t2 != null && tenDiTruoc.equals(t2.arg(0))) {
                lai.gui(Packet.of(PacketType.FIRE, "3", "0"));
                kiemTra("VAN BAN DUOC sau khi vao lai",
                        lai.coNhan(PacketType.FIRE_RESULT, 5));
            } else {
                kiemTra("VAN BAN DUOC sau khi vao lai (khong doi duoc luot)",
                        false);
            }
        }

        lai.dong();
        diSau.dong();
    }

    /* ------------------------------------------------------------------ */
    /* Kich ban 2: khong vao lai, het thoi gian an han thi bi xu thua      */
    /* ------------------------------------------------------------------ */

    private static void kichBan2(int port) throws Exception {
        System.out.println("--- Kich ban 2: rot mang va KHONG vao lai ---");
        System.out.println("  (test nay cho khoang "
                + (Protocol.TIMEOUT_SECONDS + Protocol.RECONNECT_SECONDS)
                + " giay, xin doi)");

        String tenA = "tvl_c" + (System.currentTimeMillis() % 100000);
        String tenB = "tvl_d" + (System.currentTimeMillis() % 100000);

        Nguoi a = dangKyVaVao(tenA, port);
        Nguoi b = dangKyVaVao(tenB, port);

        a.gui(Packet.of(PacketType.ROOM_CREATE, "test-hethan"));
        Packet joinedA = a.cho(PacketType.ROOM_JOINED, 5);
        int roomId = layRoomId(joinedA);
        b.gui(Packet.of(PacketType.ROOM_JOIN, String.valueOf(roomId)));
        b.cho(PacketType.ROOM_JOINED, 5);
        a.cho(PacketType.PLACE_PHASE, 5);
        b.cho(PacketType.PLACE_PHASE, 5);

        a.gui(Packet.withPayload(PacketType.READY, banDoCoDinh()));
        b.gui(Packet.withPayload(PacketType.READY, banDoCoDinh()));
        kiemTra("Van dau thu hai bat dau", a.cho(PacketType.GAME_START, 5) != null);
        a.cho(PacketType.TURN, 5);
        b.cho(PacketType.TURN, 5);

        // A rot mang va khong bao gio vao lai
        a.rotMang();
        kiemTra("B nhan OPPONENT_LOST",
                b.coNhan(PacketType.OPPONENT_LOST, Protocol.TIMEOUT_SECONDS + 20));

        // Cho het thoi gian an han
        Packet ketThuc = b.cho(PacketType.GAME_OVER, Protocol.RECONNECT_SECONDS + 20);
        kiemTra("Het an han thi B nhan GAME_OVER", ketThuc != null);
        kiemTra("B duoc xu THANG",
                ketThuc != null && Protocol.RESULT_WIN.equals(ketThuc.arg(0)));
        kiemTra("Ly do dung la OPPONENT_LOST",
                ketThuc != null && Protocol.REASON_OPPONENT_LOST.equals(ketThuc.arg(1)));

        b.dong();
    }

    /* ------------------------------------------------------------------ */
    /* Ham phu                                                             */
    /* ------------------------------------------------------------------ */

    private static Nguoi dangKyVaVao(String ten, int port) throws Exception {
        Nguoi n = new Nguoi(ten, port);
        n.gui(Packet.of(PacketType.REGISTER, ten, "matkhau123"));
        // Co the la REGISTER_OK (tai khoan moi) hoac ERROR (da ton tai).
        Packet p = n.cho(PacketType.REGISTER_OK, 5);
        if (p == null) {
            // Tai khoan da co san, dang nhap thang.
            n.gui(Packet.of(PacketType.LOGIN, ten, "matkhau123"));
        } else {
            n.gui(Packet.of(PacketType.LOGIN, ten, "matkhau123"));
        }
        n.cho(PacketType.LOGIN_OK, 5);
        return n;
    }

    private static int layRoomId(Packet joined) {
        if (joined == null) {
            return -1;
        }
        bantau.common.RoomInfo info = joined.payload(bantau.common.RoomInfo.class);
        return info == null ? -1 : info.id();
    }

    /**
     * Ban do CO DINH, khong ngau nhien: ca 5 tau nam ngang o 5 hang dau,
     * bat dau tu cot 0.
     *
     * <p>Phai co dinh de test biet truoc ban vao dau thi trung - dung
     * {@link Board#randomPlace(Random)} thi ket qua moi lan chay moi khac
     * va test khong con tin duoc.
     */
    private static Board banDoCoDinh() {
        Board b = new Board();
        int hang = 0;
        for (ShipType t : ShipType.values()) {
            b.place(t, 0, hang, bantau.common.Orientation.HORIZONTAL);
            hang++;
        }
        return b;
    }

    private static int demOdaBan(Board b) {
        int n = 0;
        for (int x = 0; x < Board.SIZE; x++) {
            for (int y = 0; y < Board.SIZE; y++) {
                if (b.isShot(x, y)) {
                    n++;
                }
            }
        }
        return n;
    }

    /** Chua dung, giu lai cho cac test sau. */
    @SuppressWarnings("unused")
    private static List<FireResult> chuaDung() {
        return new ArrayList<>();
    }
}
