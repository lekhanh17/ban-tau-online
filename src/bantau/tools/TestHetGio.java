package bantau.tools;

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

/**
 * Cong cu noi bo: kiem thu dong ho dem nguoc moi luot.
 *
 * <p>Kiem chung 3 dieu: khong ban thi mat luot sau dung 30 giay, ban kip
 * thi giu duoc luot, va dong ho duoc dat lai moi khi sang luot moi.
 * Khong thuoc bai nop.
 */
public final class TestHetGio {

    private static int soLoi = 0;

    private static void kiemTra(String ten, boolean dieuKien) {
        System.out.println((dieuKien ? "  OK   " : "  SAI  ") + ten);
        if (!dieuKien) {
            soLoi++;
        }
    }

    private static final class Client {
        final String ten;
        final Socket socket;
        final ObjectOutputStream out;
        final ObjectInputStream in;
        final List<Packet> daNhan = new ArrayList<>();
        volatile boolean dangChay = true;

        Client(String host, int port, String ten) throws IOException {
            this.ten = ten;
            this.socket = new Socket(host, port);
            this.socket.setTcpNoDelay(true);
            this.out = new ObjectOutputStream(socket.getOutputStream());
            this.out.flush();
            this.in = new ObjectInputStream(socket.getInputStream());
            Thread t = new Thread(() -> {
                try {
                    while (dangChay) {
                        Object o = in.readObject();
                        if (o instanceof Packet p) {
                            if (p.type() == PacketType.PING) {
                                gui(Packet.of(PacketType.PONG));
                                continue;
                            }
                            synchronized (daNhan) {
                                daNhan.add(p);
                            }
                        }
                    }
                } catch (Exception e) {
                    dangChay = false;
                }
            }, "doc-" + ten);
            t.setDaemon(true);
            t.start();
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
                Thread.sleep(50);
            }
            return null;
        }

        /** Goi TURN moi nhat da nhan, null neu chua co. */
        Packet turnMoiNhat() {
            synchronized (daNhan) {
                for (int i = daNhan.size() - 1; i >= 0; i--) {
                    if (daNhan.get(i).type() == PacketType.TURN) {
                        return daNhan.get(i);
                    }
                }
            }
            return null;
        }

        boolean coSystemChua(String chuaChu) {
            synchronized (daNhan) {
                for (Packet p : daNhan) {
                    if (p.type() == PacketType.SYSTEM && p.arg(0).contains(chuaChu)) {
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

        void dong() throws IOException {
            dangChay = false;
            socket.close();
        }
    }

    private static Client vaoGame(String host, int port, String ten) throws Exception {
        Client c = new Client(host, port, ten);
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
        int hanGio = Protocol.TURN_SECONDS;

        Client a = vaoGame(host, port, "hg1" + moc);
        Client b = vaoGame(host, port, "hg2" + moc);

        a.gui(Packet.of(PacketType.ROOM_CREATE, "Phong test het gio"));
        Packet joined = a.cho(PacketType.ROOM_JOINED, 5);
        int roomId = joined == null ? 1 : ((RoomInfo) joined.payload()).id();
        b.gui(Packet.of(PacketType.ROOM_JOIN, String.valueOf(roomId)));
        b.cho(PacketType.PLACE_PHASE, 5);

        Board bdA = new Board();
        Board bdB = new Board();
        bdA.randomPlace(new Random(3));
        bdB.randomPlace(new Random(4));
        a.gui(Packet.withPayload(PacketType.READY, bdA));
        b.gui(Packet.withPayload(PacketType.READY, bdB));

        Packet start = a.cho(PacketType.GAME_START, 5);
        kiemTra("van dau bat dau", start != null);

        Packet turn1 = a.cho(PacketType.TURN, 5);
        kiemTra("goi TURN co kem so giay suy nghi",
                turn1 != null && turn1.intArg(1, -1) == hanGio);

        String nguoiDauTien = turn1 == null ? "" : turn1.arg(0);
        System.out.println("  Nguoi di truoc: " + nguoiDauTien);

        /* ============================================================== */
        System.out.println();
        System.out.println("=== 1. KHONG BAN GI TRONG " + hanGio + " GIAY ===");
        System.out.println("    Cho " + (hanGio + 6) + " giay...");

        a.xoaLichSu();
        b.xoaLichSu();
        long batDau = System.currentTimeMillis();

        // Ca hai ben ngoi im, khong ban phat nao
        Packet turn2 = a.cho(PacketType.TURN, hanGio + 6);
        long troi = (System.currentTimeMillis() - batDau) / 1000;

        kiemTra("server tu chuyen luot khi het gio", turn2 != null);
        kiemTra("chuyen luot sau khoang " + hanGio + " giay (thuc te " + troi + "s)",
                turn2 != null && troi >= hanGio - 2 && troi <= hanGio + 5);
        kiemTra("luot da sang nguoi khac",
                turn2 != null && !turn2.arg(0).equals(nguoiDauTien));
        kiemTra("ca hai ben deu duoc bao la co nguoi het gio",
                a.coSystemChua("het") && b.coSystemChua("het"));
        kiemTra("dong ho duoc dat lai ve " + hanGio + " giay cho luot moi",
                turn2 != null && turn2.intArg(1, -1) == hanGio);

        /* ============================================================== */
        System.out.println();
        System.out.println("=== 2. BAN KIP GIO THI KHONG MAT LUOT OAN ===");

        // Ai dang den luot thi ban ngay mot phat
        String dangDanh = turn2 == null ? "" : turn2.arg(0);
        Client nguoiDanh = dangDanh.equals(a.ten) ? a : b;
        a.xoaLichSu();
        b.xoaLichSu();

        nguoiDanh.gui(Packet.of(PacketType.FIRE, "0", "0"));
        Packet ketQua = nguoiDanh.cho(PacketType.FIRE_RESULT, 5);
        kiemTra("ban duoc trong thoi han", ketQua != null);

        Packet turn3 = a.cho(PacketType.TURN, 5);
        kiemTra("co goi TURN moi ngay sau phat ban", turn3 != null);
        kiemTra("dong ho dat lai ve " + hanGio + " giay",
                turn3 != null && turn3.intArg(1, -1) == hanGio);
        kiemTra("khong ai bi bao het gio oan",
                !a.coSystemChua("het " + hanGio));

        a.dong();
        b.dong();

        System.out.println();
        System.out.println(soLoi == 0
                ? "=== TAT CA DEU DUNG ==="
                : "=== CO " + soLoi + " CHO SAI ===");
        System.exit(soLoi == 0 ? 0 : 1);
    }
}
