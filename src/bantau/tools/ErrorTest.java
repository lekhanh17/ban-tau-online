package bantau.tools;

import bantau.common.Board;
import bantau.common.Orientation;
import bantau.common.Packet;
import bantau.common.PacketType;
import bantau.common.Protocol;
import bantau.common.ShipType;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * KIEM THU CAC TINH HUONG LOI CUA VAN DAU
 *
 * <p>Mo hai ket noi that toi server roi lan luot thu cac truong hop gian lan
 * va sai thu tu. Day la phan chung minh server KHONG TIN client.
 *
 * <p>Cach chay: bat server truoc, roi Run file nay.
 */
public class ErrorTest {

    private static int dat = 0;
    private static int truot = 0;

    /** Mot ket noi thu nghiem. */
    private static final class Conn {
        final String ten;
        final Socket socket;
        final ObjectOutputStream out;
        final ObjectInputStream in;

        Conn(String ten, String host, int port) throws IOException {
            this.ten = ten;
            this.socket = new Socket(host, port);
            this.out = new ObjectOutputStream(socket.getOutputStream());
            this.out.flush();
            this.in = new ObjectInputStream(socket.getInputStream());
        }

        void send(Packet p) throws IOException {
            out.writeObject(p);
            out.flush();
            out.reset();
        }

        /** Doc toi khi gap dung loai ban tin mong doi, bo qua cac ban tin khac. */
        Packet cho(PacketType loai, int soBanTinToiDa) throws Exception {
            for (int i = 0; i < soBanTinToiDa; i++) {
                socket.setSoTimeout(3000);
                Object o = in.readObject();
                if (o instanceof Packet p && p.type() == loai) {
                    return p;
                }
            }
            return null;
        }

        void close() throws IOException {
            socket.close();
        }
    }

    private static void kiemTra(String nhan, boolean dieuKien, String chiTiet) {
        if (dieuKien) {
            dat++;
            System.out.println("  [OK]   " + nhan);
        } else {
            truot++;
            System.out.println("  [SAI]  " + nhan + "   -> " + chiTiet);
        }
    }

    /** Ban do co dinh de biet truoc vi tri tau. */
    private static Board banDoCoDinh() {
        Board b = new Board();
        b.place(ShipType.CARRIER, 0, 0, Orientation.HORIZONTAL);
        b.place(ShipType.BATTLESHIP, 0, 1, Orientation.HORIZONTAL);
        b.place(ShipType.CRUISER, 0, 2, Orientation.HORIZONTAL);
        b.place(ShipType.SUBMARINE, 0, 3, Orientation.HORIZONTAL);
        b.place(ShipType.DESTROYER, 0, 4, Orientation.HORIZONTAL);
        return b;
    }

    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : Protocol.DEFAULT_HOST;
        int port = args.length > 1 ? Integer.parseInt(args[1]) : Protocol.DEFAULT_PORT;

        System.out.println("=== KIEM THU TINH HUONG LOI CUA VAN DAU ===\n");

        Conn a = new Conn("khanh", host, port);
        Conn b = new Conn("nam", host, port);

        // 1. Dang nhap
        a.send(Packet.of(PacketType.LOGIN, "khanh"));
        kiemTra("Dang nhap A", a.cho(PacketType.LOGIN_OK, 5) != null, "khong nhan duoc LOGIN_OK");

        b.send(Packet.of(PacketType.LOGIN, "nam"));
        kiemTra("Dang nhap B", b.cho(PacketType.LOGIN_OK, 5) != null, "khong nhan duoc LOGIN_OK");

        // 2. Tao va vao phong
        a.send(Packet.of(PacketType.ROOM_CREATE, "Phong kiem thu loi"));
        Packet joined = a.cho(PacketType.ROOM_JOINED, 5);
        kiemTra("A tao phong", joined != null, "khong vao duoc phong");

        b.send(Packet.of(PacketType.ROOM_LIST));
        b.send(Packet.of(PacketType.ROOM_JOIN, "1"));
        kiemTra("B vao phong", b.cho(PacketType.ROOM_JOINED, 8) != null, "khong vao duoc");

        kiemTra("Ca hai nhan PLACE_PHASE",
                a.cho(PacketType.PLACE_PHASE, 8) != null
                        && b.cho(PacketType.PLACE_PHASE, 8) != null,
                "thieu PLACE_PHASE");

        // 3. Gui ban do RONG - phai bi tu choi
        a.send(Packet.withPayload(PacketType.READY, new Board()));
        Packet e1 = a.cho(PacketType.ERROR, 5);
        kiemTra("Chan ban do rong (thieu tau)",
                e1 != null && Protocol.E_BAD_PLACEMENT.equals(e1.arg(0)),
                e1 == null ? "khong co loi tra ve" : e1.toString());

        // 4. Gui ban do THIEU 1 TAU - phai bi tu choi
        Board thieu = new Board();
        thieu.place(ShipType.CARRIER, 0, 0, Orientation.HORIZONTAL);
        thieu.place(ShipType.BATTLESHIP, 0, 1, Orientation.HORIZONTAL);
        a.send(Packet.withPayload(PacketType.READY, thieu));
        Packet e2 = a.cho(PacketType.ERROR, 5);
        kiemTra("Chan ban do thieu tau",
                e2 != null && Protocol.E_BAD_PLACEMENT.equals(e2.arg(0)),
                e2 == null ? "khong co loi" : e2.toString());

        // 5. Ban khi van chua bat dau
        a.send(Packet.of(PacketType.FIRE, "0", "0"));
        Packet e3 = a.cho(PacketType.ERROR, 5);
        kiemTra("Chan ban khi chua bat dau van",
                e3 != null && Protocol.E_BAD_STATE.equals(e3.arg(0)),
                e3 == null ? "khong co loi" : e3.toString());

        // 6. Ca hai gui ban do hop le
        a.send(Packet.withPayload(PacketType.READY, banDoCoDinh()));
        kiemTra("Chap nhan ban do hop le cua A",
                a.cho(PacketType.READY_OK, 5) != null, "khong co READY_OK");

        b.send(Packet.withPayload(PacketType.READY, banDoCoDinh()));
        kiemTra("Chap nhan ban do hop le cua B",
                b.cho(PacketType.READY_OK, 5) != null, "khong co READY_OK");

        Packet start = a.cho(PacketType.GAME_START, 8);
        kiemTra("Van dau bat dau", start != null, "khong co GAME_START");
        b.cho(PacketType.GAME_START, 8);

        String nguoiDiTruoc = start == null ? "" : start.arg(0);
        Packet turnA = a.cho(PacketType.TURN, 5);
        b.cho(PacketType.TURN, 5);
        kiemTra("Co bao luot di", turnA != null, "khong co TURN");

        Conn nguoiBan = "khanh".equals(nguoiDiTruoc) ? a : b;
        Conn nguoiCho = (nguoiBan == a) ? b : a;

        // 7. Nguoi chua den luot ma ban
        nguoiCho.send(Packet.of(PacketType.FIRE, "9", "9"));
        Packet e4 = nguoiCho.cho(PacketType.ERROR, 5);
        kiemTra("Chan ban khi chua den luot",
                e4 != null && Protocol.E_NOT_YOUR_TURN.equals(e4.arg(0)),
                e4 == null ? "khong co loi" : e4.toString());

        // 8. Ban trung tau tai (0,0) - phai duoc giu luot
        nguoiBan.send(Packet.of(PacketType.FIRE, "0", "0"));
        Packet kq = nguoiBan.cho(PacketType.FIRE_RESULT, 5);
        kiemTra("Ban trung tau tai A1",
                kq != null && "HIT".equals(kq.arg(2)),
                kq == null ? "khong co ket qua" : kq.toString());

        Packet turn2 = nguoiBan.cho(PacketType.TURN, 5);
        kiemTra("Ban trung thi giu luot",
                turn2 != null && nguoiDiTruoc.equals(turn2.arg(0)),
                turn2 == null ? "khong co TURN" : turn2.toString());

        // 9. Ban lai o vua ban
        nguoiBan.send(Packet.of(PacketType.FIRE, "0", "0"));
        Packet e5 = nguoiBan.cho(PacketType.ERROR, 5);
        kiemTra("Chan ban lai o da ban",
                e5 != null && Protocol.E_ALREADY_SHOT.equals(e5.arg(0)),
                e5 == null ? "khong co loi" : e5.toString());

        // 10. Ban ra ngoai ban do
        nguoiBan.send(Packet.of(PacketType.FIRE, "99", "99"));
        Packet e6 = nguoiBan.cho(PacketType.ERROR, 5);
        kiemTra("Chan ban ra ngoai ban do",
                e6 != null && Protocol.E_ALREADY_SHOT.equals(e6.arg(0)),
                e6 == null ? "khong co loi" : e6.toString());

        // 11. Doi thu thoat giua van -> nguoi con lai thang
        nguoiCho.send(Packet.of(PacketType.ROOM_LEAVE));
        Packet over = nguoiBan.cho(PacketType.GAME_OVER, 8);
        kiemTra("Doi thu thoat giua van thi minh thang",
                over != null && Protocol.RESULT_WIN.equals(over.arg(0))
                        && Protocol.REASON_OPPONENT_LEFT.equals(over.arg(1)),
                over == null ? "khong co GAME_OVER" : over.toString());

        a.close();
        b.close();

        System.out.println("\n===============================");
        System.out.println("KET QUA: " + dat + " dat / " + truot + " truot");
        System.out.println("===============================");
    }
}