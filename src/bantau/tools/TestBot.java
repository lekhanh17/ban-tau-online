package bantau.tools;

import bantau.common.BaoMat;
import bantau.common.Board;
import bantau.common.Packet;
import bantau.common.PacketType;
import bantau.common.Protocol;
import bantau.common.RoomInfo;

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
 * TEST TU DONG CHO CHUC NANG CHOI VOI MAY
 *
 * <p>Kiem tra ba nhom:
 * <ol>
 *   <li><b>Ket noi:</b> bot co that su la mot client TCP rieng khong - no co
 *       dang nhap, vao phong, dat tau va ban qua dung giao thuc BSP khong.
 *   </li>
 *   <li><b>Thuat toan:</b> so phat bot can de ban chim het 17 o tau phai IT
 *       HON RO RET so voi ban bua. Ban bua trung binh can khoang 95 phat;
 *       san - diet phai duoi 85 phat moi coi la co tac dung.</li>
 *   <li><b>Luat:</b> tran voi may khong duoc ghi vao thanh tich, va nguoi
 *       that khong dang ky duoc ten bat dau bang {@code May_}.</li>
 * </ol>
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

        System.out.println("=== TEST CHOI VOI MAY ===");
        System.out.println();

        kiemTraTenDanhRieng(port);
        System.out.println();
        List<Integer> soPhat = new ArrayList<>();
        // Choi nhieu van roi lay trung binh: moi van ban do dat ngau nhien
        // nen so phat can thiet dao dong khong it.
        for (int van = 1; van <= 3; van++) {
            System.out.println("--- Van " + van + " ---");
            Integer n = choiMotVan(port);
            if (n != null) {
                soPhat.add(n);
            }
            System.out.println();
        }

        System.out.println("--- Thuat toan ---");
        kiemTra("Do duoc it nhat mot van may thang", !soPhat.isEmpty());
        if (!soPhat.isEmpty()) {
            int tong = 0;
            for (int n : soPhat) {
                tong += n;
            }
            int tb = tong / soPhat.size();

            System.out.println("  So phat may can de chim het 17 o tau: " + soPhat
                    + ", trung binh " + tb);
            kiemTra("Thuat toan san-diet tot hon ban bua"
                            + " (trung binh " + tb + " phat, ban bua ~95)",
                    tb < 85);
        }

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
     * Choi tron mot van voi may va dem so phat may can de thang.
     *
     * <p>Nguoi test co y danh cham va deu: moi luot ban dung mot o theo thu
     * tu. Nho vay may gan nhu chac chan thang truoc, va ta do duoc so phat
     * may thuc su can.
     *
     * @return so phat may da ban, null neu van khong ket thuc duoc
     */
    private static Integer choiMotVan(int port) throws Exception {
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
            return null;
        }

        // ----- Goi doi thu may -----
        toi.gui(Packet.of(PacketType.ADD_BOT));
        Packet datTau = toi.cho(PacketType.PLACE_PHASE, 15);
        kiemTra("May vao phong va chuyen sang giai doan dat tau",
                datTau != null);
        if (datTau == null) {
            toi.dong();
            return null;
        }

        Board banDo = new Board();
        banDo.randomPlace(new Random());
        toi.gui(Packet.withPayload(PacketType.READY, banDo));

        Packet batDau = toi.cho(PacketType.GAME_START, 20);
        kiemTra("Van dau bat dau - may da tu dat tau xong", batDau != null);
        if (batDau == null) {
            toi.dong();
            return null;
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
                        // Danh cham va deu: moi luot dung mot o theo thu tu.
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
        System.out.println("  (may ban " + mayDaBan + " phat, nguoi ban "
                + oKeTiep + " phat)");
        return Protocol.RESULT_LOSE.equals(ketQua) ? mayDaBan : null;
    }
}
