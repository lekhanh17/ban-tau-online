package bantau.tools;

import bantau.common.BaoMat;
import bantau.common.Board;
import bantau.common.MucDoBot;
import bantau.common.Packet;
import bantau.common.PacketType;
import bantau.common.Protocol;
import bantau.common.RoomInfo;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * TEST TU DONG CHO CHUC NANG CHOI VOI MAY
 *
 * <p><b>Test nay kiem tra phan MANG, khong phai suc manh thuat toan.</b> Hai
 * viec do duoc tach ra co chu dich:
 * <ul>
 *   <li>Suc manh thuat toan do bang {@link MoPhongBot} - chay 2000 van moi
 *       muc trong vai giay, khong can server. So lieu dang tin vi nhieu van.
 *   </li>
 *   <li>Test nay kiem tra <b>duong day di qua mang</b>: ban tin ADD_BOT co
 *       mang dung muc do den server khong, bot co that su la mot client TCP
 *       rieng khong, no co dang nhap - vao phong - dat tau - ban qua dung
 *       giao thuc BSP khong, va hai quy tac rieng cho bot co con dung khong.
 *   </li>
 * </ul>
 *
 * <p>Vi moi phat ban cua bot cach nhau {@code BOT_DELAY_MS}, chay tron ba
 * muc mat vai phut - do la ly do khong do thong ke o day.
 *
 * <p><b>Cach chay:</b> bat server truoc, roi
 * {@code java -cp "bin;lib/*" bantau.tools.TestBot 5100}.
 *
 * <p>Khong thuoc phan chuong trinh nop.
 */
public final class TestBot {

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

    private static final class Nguoi {
        private final Socket socket;
        private final ObjectOutputStream out;
        private final ObjectInputStream in;
        private final BlockingQueue<Packet> hopThu = new ArrayBlockingQueue<>(2000);
        private volatile boolean song = true;

        Nguoi(String ten, int port) throws IOException {
            this.socket = new Socket("127.0.0.1", port);
            this.socket.setTcpNoDelay(true);
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

        Packet cho(PacketType loai, int giay) throws InterruptedException {
            long hetHan = System.currentTimeMillis() + giay * 1000L;
            while (System.currentTimeMillis() < hetHan) {
                Packet p = hopThu.poll(300, TimeUnit.MILLISECONDS);
                if (p != null && p.type() == loai) {
                    return p;
                }
            }
            return null;
        }

        /** Lay goi tin bat ky, null neu het gio. */
        Packet lay(int ms) throws InterruptedException {
            return hopThu.poll(ms, TimeUnit.MILLISECONDS);
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

        System.out.println("=== TEST CHOI VOI MAY (phan mang) ===");
        System.out.println();

        kiemTraTenDanhRieng(port);
        System.out.println();

        for (MucDoBot muc : MucDoBot.values()) {
            System.out.println("--- Muc " + muc.ten() + " ---");
            choiMotVan(port, muc);
            System.out.println();
        }

        System.out.println("Ghi chu: so lieu suc manh thuat toan do bang"
                + " MoPhongBot (2000 van moi muc), khong do o day.");
        System.out.println();
        System.out.println("=== KET QUA: " + soDung + " DUNG, " + soSai + " SAI ===");
        if (soSai > 0) {
            System.exit(1);
        }
    }

    /* ------------------------------------------------------------------ */

    private static void kiemTraTenDanhRieng(int port) throws Exception {
        System.out.println("--- Ten danh rieng cho may ---");
        Nguoi n = new Nguoi("reserved", port);
        n.gui(Packet.of(PacketType.REGISTER, "May_gianlan", "matkhau123"));
        Packet loi = n.cho(PacketType.ERROR, 5);
        kiemTra("Nguoi that KHONG dang ky duoc ten bat dau bang \""
                        + Protocol.BOT_PREFIX + "\"",
                loi != null && Protocol.E_NAME_RESERVED.equals(loi.arg(0)));
        n.dong();
    }

    /**
     * Doc ten tai khoan bot tu ban tin SYSTEM ma server gui ve sau ADD_BOT.
     *
     * <p>Ban tin co dang {@code "Dang goi doi thu may (May_K1_abcd, muc
     * Kho) vao phong..."}. Ham nay lay ra cai ten bat dau bang
     * {@link Protocol#BOT_PREFIX}.
     *
     * @return ten bot, hoac chuoi rong neu khong thay
     */
    private static String choTenBot(Nguoi toi, int giay) throws InterruptedException {
        long hetHan = System.currentTimeMillis() + giay * 1000L;
        while (System.currentTimeMillis() < hetHan) {
            Packet p = toi.cho(PacketType.SYSTEM, 2);
            if (p == null) {
                continue;
            }
            for (String tu : p.arg(0).split("[\\s(),]+")) {
                if (tu.startsWith(Protocol.BOT_PREFIX)) {
                    return tu;
                }
            }
        }
        return "";
    }

    /**
     * Choi tron mot van voi may o mot muc do, va kiem tra ca duong day mang.
     *
     * <p>Nguoi test ban theo thu tu o, moi luot mot o - khong can thang hay
     * thua, chi can van dau chay tron ven.
     */
    private static void choiMotVan(int port, MucDoBot muc) throws Exception {
        String ten = "tb" + (System.currentTimeMillis() % 1000000);
        Nguoi toi = new Nguoi(ten, port);

        toi.gui(Packet.of(PacketType.REGISTER, ten, "matkhau123"));
        kiemTra("Tao duoc tai khoan nguoi that",
                toi.cho(PacketType.REGISTER_OK, 5) != null);
        toi.gui(Packet.of(PacketType.LOGIN, ten, "matkhau123"));
        kiemTra("Dang nhap duoc", toi.cho(PacketType.LOGIN_OK, 5) != null);

        toi.gui(Packet.of(PacketType.ROOM_CREATE, "test-bot"));
        Packet joined = toi.cho(PacketType.ROOM_JOINED, 5);
        kiemTra("Tao duoc phong", joined != null);
        if (joined == null) {
            toi.dong();
            return;
        }

        // ----- Goi doi thu may o muc do nay -----
        toi.gui(Packet.of(PacketType.ADD_BOT, muc.name()));

        // Server tra ve mot ban tin SYSTEM co kem ten tai khoan bot vua tao.
        // Ten do mang chu dau cua muc do (May_D.. / May_T.. / May_K..), nen
        // doc no la kiem tra duoc tham so muc do DA DI QUA MANG den dung noi.
        String tenBot = choTenBot(toi, 15);
        String tienToMong = Protocol.BOT_PREFIX + muc.name().charAt(0);
        kiemTra("ADD_BOT mang dung muc do qua mang (ten bot \"" + tenBot
                        + "\" bat dau bang \"" + tienToMong + "\")",
                tenBot.startsWith(tienToMong));

        Packet datTau = toi.cho(PacketType.PLACE_PHASE, 15);
        kiemTra("May vao phong va chuyen sang giai doan dat tau",
                datTau != null);
        if (datTau == null) {
            toi.dong();
            return;
        }

        Board banDo = new Board();
        banDo.randomPlace(new Random());
        toi.gui(Packet.withPayload(PacketType.READY, banDo));

        Packet batDau = toi.cho(PacketType.GAME_START, 20);
        kiemTra("Van dau bat dau - may da tu dat tau xong", batDau != null);
        if (batDau == null) {
            toi.dong();
            return;
        }

        // ----- Choi -----
        int mayDaBan = 0;
        int oKeTiep = 0;
        String ketQua = null;
        String lyDo = null;
        long hetHan = System.currentTimeMillis() + 240_000L;

        while (System.currentTimeMillis() < hetHan) {
            Packet p = toi.lay(1000);
            if (p == null) {
                continue;
            }
            switch (p.type()) {
                case INCOMING -> mayDaBan++;
                case TURN -> {
                    if (ten.equals(p.arg(0)) && oKeTiep < 100) {
                        toi.gui(Packet.of(PacketType.FIRE,
                                String.valueOf(oKeTiep % 10),
                                String.valueOf(oKeTiep / 10)));
                        oKeTiep++;
                    }
                }
                case GAME_OVER -> {
                    ketQua = p.arg(0);
                    lyDo = p.arg(1);
                }
                default -> {
                    // bo qua
                }
            }
            if (ketQua != null) {
                break;
            }
        }

        kiemTra("Van dau ket thuc duoc", ketQua != null);
        kiemTra("Ket thuc vi ban chim het tau, khong phai do bo cuoc",
                Protocol.REASON_ALL_SUNK.equals(lyDo));

        // ----- Tran voi may khong tinh thanh tich -----
        toi.gui(Packet.of(PacketType.HISTORY_LIST));
        Packet lichSu = toi.cho(PacketType.HISTORY_DATA, 5);
        kiemTra("Xin duoc lich su dau", lichSu != null);
        if (lichSu != null) {
            ArrayList<?> ds = lichSu.payload(ArrayList.class);
            kiemTra("TRAN VOI MAY KHONG duoc ghi vao lich su"
                            + " (lich su co " + (ds == null ? 0 : ds.size())
                            + " tran)",
                    ds == null || ds.isEmpty());
        }

        // ----- May tu thoat khi nguoi choi roi phong -----
        toi.gui(Packet.of(PacketType.ROOM_LEAVE));
        kiemTra("Roi phong duoc", toi.cho(PacketType.ROOM_LEFT, 5) != null);

        // May phai nhan ROOM_STATE, gui QUIT, roi server moi xoa phong -
        // chuoi viec nay di qua mang nen khong xong ngay. Hoi lai vai lan.
        boolean conPhongBot = true;
        for (int lan = 0; lan < 10 && conPhongBot; lan++) {
            Thread.sleep(500);
            toi.gui(Packet.of(PacketType.ROOM_LIST));
            Packet ds = toi.cho(PacketType.ROOM_LIST_DATA, 5);
            conPhongBot = false;
            if (ds != null) {
                ArrayList<?> ds2 = ds.payload(ArrayList.class);
                if (ds2 != null) {
                    for (Object o : ds2) {
                        if (o instanceof RoomInfo ri && "test-bot".equals(ri.name())) {
                            conPhongBot = true;
                        }
                    }
                }
            }
        }
        kiemTra("May tu thoat khi nguoi choi roi phong, phong duoc don sach",
                !conPhongBot);

        toi.dong();
        System.out.println("  (muc " + muc.ten() + ": may ban " + mayDaBan
                + " phat, nguoi ban " + oKeTiep + " phat, ket qua cua nguoi: "
                + ketQua + ")");
    }
}
