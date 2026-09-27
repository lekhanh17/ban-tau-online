package bantau.tools;

import bantau.common.Board;
import bantau.common.Packet;
import bantau.common.PacketType;
import bantau.common.Protocol;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Cong cu noi bo: kiem thu cac tinh huong MAT KET NOI.
 *
 * <p>Dung socket tho thay vi giao dien de kiem soat chinh xac thoi diem
 * ngat, va de mo phong duoc mot client "chet" (con ket noi nhung khong
 * tra loi PING). Khong thuoc bai nop.
 */
public final class TestMatKetNoi {

    private static int soLoi = 0;

    private static void kiemTra(String ten, boolean dieuKien) {
        System.out.println((dieuKien ? "  OK   " : "  SAI  ") + ten);
        if (!dieuKien) {
            soLoi++;
        }
    }

    /** Mot client tho: chi co socket va hai luong doi tuong. */
    private static final class Client {
        final String ten;
        final Socket socket;
        final ObjectOutputStream out;
        final ObjectInputStream in;
        final List<Packet> daNhan = new ArrayList<>();
        Thread doc;
        volatile boolean traLoiPing = true;
        volatile boolean dangChay = true;

        Client(String host, int port, String ten) throws IOException {
            this.ten = ten;
            this.socket = new Socket(host, port);
            this.socket.setTcpNoDelay(true);
            this.out = new ObjectOutputStream(socket.getOutputStream());
            this.out.flush();
            this.in = new ObjectInputStream(socket.getInputStream());
        }

        void batDauDoc() {
            doc = new Thread(() -> {
                try {
                    while (dangChay) {
                        Object o = in.readObject();
                        if (o instanceof Packet p) {
                            if (p.type() == PacketType.PING) {
                                // Client "chet" thi im lang, khong tra PONG
                                if (traLoiPing) {
                                    gui(Packet.of(PacketType.PONG));
                                }
                                continue;
                            }
                            synchronized (daNhan) {
                                daNhan.add(p);
                            }
                        }
                    }
                } catch (Exception e) {
                    dangChay = false;   // server dong ket noi hoac loi mang
                }
            }, "doc-" + ten);
            doc.setDaemon(true);
            doc.start();
        }

        synchronized void gui(Packet p) {
            try {
                out.writeObject(p);
                out.flush();
                out.reset();
            } catch (IOException ignored) {
                // ket noi da dut
            }
        }

        /** Cho toi khi nhan duoc goi tin loai nay, tra ve null neu het gio. */
        Packet cho(PacketType loai, long giayToiDa) throws InterruptedException {
            long het = System.currentTimeMillis() + giayToiDa * 1000;
            while (System.currentTimeMillis() < het) {
                synchronized (daNhan) {
                    for (Packet p : daNhan) {
                        if (p.type() == loai) {
                            return p;
                        }
                    }
                }
                Thread.sleep(100);
            }
            return null;
        }

        boolean daNhan(PacketType loai) {
            synchronized (daNhan) {
                for (Packet p : daNhan) {
                    if (p.type() == loai) {
                        return true;
                    }
                }
            }
            return false;
        }

        void xoaLichSu() {
            synchronized (daNhan) {
                daNhan.clear();
            }
        }

        /** Tat dot ngot - dong socket ma khong gui QUIT, nhu rut day mang. */
        void tatDotNgot() throws IOException {
            dangChay = false;
            socket.close();
        }
    }

    private static Client taoVaDangNhap(String host, int port, String ten)
            throws Exception {
        Client c = new Client(host, port, ten);
        c.batDauDoc();
        c.gui(Packet.of(PacketType.REGISTER, ten, "123456"));
        if (c.cho(PacketType.REGISTER_OK, 5) == null) {
            throw new IllegalStateException("Khong dang ky duoc " + ten);
        }
        c.gui(Packet.of(PacketType.LOGIN, ten, "123456"));
        if (c.cho(PacketType.LOGIN_OK, 5) == null) {
            throw new IllegalStateException("Khong dang nhap duoc " + ten);
        }
        c.xoaLichSu();
        return c;
    }

    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "127.0.0.1";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 5000;
        long moc = System.currentTimeMillis() % 100000;

        /* ============================================================== */
        System.out.println("=== 1. DOI THU TAT DOT NGOT KHI DANG DANH ===");
        {
            Client a = taoVaDangNhap(host, port, "a" + moc);
            Client b = taoVaDangNhap(host, port, "b" + moc);

            a.gui(Packet.of(PacketType.ROOM_CREATE, "Phong test 1"));
            Packet joined = a.cho(PacketType.ROOM_JOINED, 5);
            kiemTra("nguoi A tao va vao duoc phong", joined != null);

            int roomId = joined == null ? 1
                    : ((bantau.common.RoomInfo) joined.payload()).id();
            b.gui(Packet.of(PacketType.ROOM_JOIN, String.valueOf(roomId)));
            kiemTra("nguoi B vao duoc phong", b.cho(PacketType.ROOM_JOINED, 5) != null);
            kiemTra("ca hai nhan PLACE_PHASE", a.cho(PacketType.PLACE_PHASE, 5) != null);

            Board bdA = new Board();
            Board bdB = new Board();
            bdA.randomPlace(new Random(1));
            bdB.randomPlace(new Random(2));
            a.gui(Packet.withPayload(PacketType.READY, bdA));
            b.gui(Packet.withPayload(PacketType.READY, bdB));
            kiemTra("van dau bat dau", b.cho(PacketType.GAME_START, 5) != null);

            b.xoaLichSu();
            a.tatDotNgot();                      // A rut day mang giua van

            Packet over = b.cho(PacketType.GAME_OVER, 8);
            kiemTra("nguoi B nhan duoc GAME_OVER", over != null);
            kiemTra("nguoi B duoc xu THANG",
                    over != null && Protocol.RESULT_WIN.equals(over.arg(0)));
            kiemTra("ly do dung la OPPONENT_LEFT",
                    over != null && Protocol.REASON_OPPONENT_LEFT.equals(over.arg(1)));
            b.tatDotNgot();
        }

        /* ============================================================== */
        System.out.println();
        System.out.println("=== 2. DOI THU TAT KHI DANG DAT TAU (van chua bat dau) ===");
        {
            Client a = taoVaDangNhap(host, port, "c" + moc);
            Client b = taoVaDangNhap(host, port, "d" + moc);

            a.gui(Packet.of(PacketType.ROOM_CREATE, "Phong test 2"));
            Packet joined = a.cho(PacketType.ROOM_JOINED, 5);
            int roomId = joined == null ? 1
                    : ((bantau.common.RoomInfo) joined.payload()).id();
            b.gui(Packet.of(PacketType.ROOM_JOIN, String.valueOf(roomId)));
            b.cho(PacketType.PLACE_PHASE, 5);

            b.xoaLichSu();
            a.tatDotNgot();                      // A thoat luc dang dat tau

            Packet state = b.cho(PacketType.ROOM_STATE, 8);
            kiemTra("nguoi B nhan ROOM_STATE moi", state != null);
            kiemTra("phong quay ve trang thai WAITING",
                    state != null && ((bantau.common.RoomInfo) state.payload())
                            .state() == bantau.common.RoomState.WAITING);
            kiemTra("nguoi B duoc bao bang tin nhan he thong",
                    b.daNhan(PacketType.SYSTEM));
            kiemTra("KHONG xu thang thua vi van chua bat dau",
                    !b.daNhan(PacketType.GAME_OVER));
            b.tatDotNgot();
        }

        /* ============================================================== */
        System.out.println();
        System.out.println("=== 3. CLIENT CHET NHUNG SOCKET VAN MO (PING/PONG) ===");
        System.out.println("    Cho toi da " + (Protocol.TIMEOUT_SECONDS + 15)
                + " giay...");
        {
            Client chet = taoVaDangNhap(host, port, "e" + moc);
            chet.traLoiPing = false;             // treo may: khong tra PONG nua

            long batDau = System.currentTimeMillis();
            boolean biNgat = false;
            while (System.currentTimeMillis() - batDau
                    < (Protocol.TIMEOUT_SECONDS + 15) * 1000L) {
                if (!chet.dangChay) {
                    biNgat = true;
                    break;
                }
                Thread.sleep(500);
            }
            long giay = (System.currentTimeMillis() - batDau) / 1000;

            kiemTra("server tu ngat client khong tra loi PING", biNgat);
            kiemTra("ngat sau khoang " + Protocol.TIMEOUT_SECONDS
                    + " giay (thuc te " + giay + "s)",
                    biNgat && giay >= Protocol.TIMEOUT_SECONDS
                            && giay <= Protocol.TIMEOUT_SECONDS + 15);
        }

        /* ============================================================== */
        System.out.println();
        System.out.println("=== 4. CLIENT BINH THUONG KHONG BI NGAT OAN ===");
        {
            Client song = taoVaDangNhap(host, port, "f" + moc);
            // Ngoi im nhung VAN tra loi PING - phai song sot qua timeout
            Thread.sleep((Protocol.TIMEOUT_SECONDS + 8) * 1000L);
            kiemTra("client tra loi PING day du thi khong bi ngat", song.dangChay);
            song.tatDotNgot();
        }

        System.out.println();
        System.out.println(soLoi == 0
                ? "=== TAT CA DEU DUNG ==="
                : "=== CO " + soLoi + " CHO SAI ===");
        System.exit(soLoi == 0 ? 0 : 1);
    }
}
