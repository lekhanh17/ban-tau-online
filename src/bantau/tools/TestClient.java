package bantau.tools;

import bantau.common.Board;
import bantau.common.FireResult;
import bantau.common.Packet;
import bantau.common.PacketType;
import bantau.common.Protocol;
import bantau.common.RoomInfo;
import bantau.common.ShipType;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Random;

/**
 * CLIENT DONG LENH - choi duoc mot van hoan chinh
 *
 * <pre>
 *   LOGIN &lt;ten&gt;       dang nhap
 *   ROOMS             xem danh sach phong
 *   CREATE &lt;ten&gt;      tao phong
 *   JOIN &lt;id&gt;         vao phong
 *   LEAVE             roi phong
 *   READY             dat ngau nhien 5 tau va bao san sang
 *   FIRE C5           ban vao o C5 (hoac: FIRE 2 4)
 *   MAP               xem lai hai ban do
 *   CHAT &lt;noi dung&gt;   chat
 *   WHO               xem ai online
 *   QUIT              thoat
 * </pre>
 */
public class TestClient {

    /** Ban do cua minh - cap nhat khi doi thu ban vao. */
    private static Board myBoard = new Board();

    /**
     * Ban do doi thu theo goc nhin cua minh.
     * Khong dung Board vi minh khong biet tau doi thu o dau - chi biet
     * ket qua tung phat ban do server tra ve.
     */
    private static final char[][] enemyMarks = new char[Board.SIZE][Board.SIZE];

    private static String myName = "?";

    public static void main(String[] args) throws IOException {
        String host = args.length > 0 ? args[0] : Protocol.DEFAULT_HOST;
        int port = args.length > 1 ? Integer.parseInt(args[1]) : Protocol.DEFAULT_PORT;

        resetBanDo();

        Socket socket = new Socket(host, port);

        // Thu tu bat buoc: luong ra truoc, flush, roi luong vao.
        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

        System.out.println("Da ket noi toi " + host + ":" + port);
        System.out.println("Lenh: LOGIN <ten> | ROOMS | CREATE <ten> | JOIN <id> | LEAVE");
        System.out.println("      READY | FIRE C5 | MAP | CHAT <noi dung> | WHO | QUIT");
        System.out.println("----------------------------------------------------------");

        // THREAD PHU: cho ban tin tu server.
        Thread reader = new Thread(() -> {
            try {
                while (true) {
                    Object obj = in.readObject();
                    if (obj instanceof Packet p) {
                        hienThi(p);
                    }
                }
            } catch (EOFException e) {
                System.out.println("Server da dong ket noi.");
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("Mat ket noi: " + e.getMessage());
            }
        }, "reader");
        reader.setDaemon(true);
        reader.start();

        // THREAD CHINH: doc ban phim.
        BufferedReader keyboard = new BufferedReader(
                new InputStreamReader(System.in, StandardCharsets.UTF_8));
        String line;
        while ((line = keyboard.readLine()) != null) {
            Packet packet = phanTich(line);
            if (packet == null) {
                continue;
            }
            out.writeObject(packet);
            out.flush();
            out.reset();
            if (packet.type() == PacketType.QUIT) {
                break;
            }
        }

        socket.close();
        System.out.println("Da thoat.");
    }

    /* ------------------------------------------------------------------ */
    /* Phan tich lenh nguoi dung go                                       */
    /* ------------------------------------------------------------------ */

    private static Packet phanTich(String line) {
        String s = line.trim();
        if (s.isEmpty()) {
            return null;
        }
        int space = s.indexOf(' ');
        String cmd = (space < 0 ? s : s.substring(0, space)).toUpperCase();
        String rest = space < 0 ? "" : s.substring(space + 1).trim();

        switch (cmd) {
            case "LOGIN": return Packet.of(PacketType.LOGIN, rest);
            case "CHAT": return Packet.of(PacketType.CHAT, rest);
            case "WHO": return Packet.of(PacketType.WHO);
            case "ROOMS": return Packet.of(PacketType.ROOM_LIST);
            case "CREATE": return Packet.of(PacketType.ROOM_CREATE, rest);
            case "JOIN": return Packet.of(PacketType.ROOM_JOIN, rest);
            case "LEAVE": return Packet.of(PacketType.ROOM_LEAVE);
            case "QUIT": return Packet.of(PacketType.QUIT);

            case "MAP":
                veCaHaiBanDo();
                return null;

            case "READY": {
                // Dat ngau nhien roi gui NGUYEN DOI TUONG Board len server.
                // Khong phai ma hoa thanh chuoi - day la loi ich cua Serialization.
                myBoard = new Board();
                myBoard.randomPlace(new Random());
                System.out.println("Da dat ngau nhien 5 tau:");
                System.out.print(myBoard.veBanDo(true));
                return Packet.withPayload(PacketType.READY, myBoard);
            }

            case "FIRE": {
                int[] toaDo = doiToaDo(rest);
                if (toaDo == null) {
                    System.out.println("!! Sai cu phap. Vi du: FIRE C5  hoac  FIRE 2 4");
                    return null;
                }
                return Packet.of(PacketType.FIRE,
                        String.valueOf(toaDo[0]), String.valueOf(toaDo[1]));
            }

            default:
                System.out.println("!! Lenh khong ton tai: " + cmd);
                return null;
        }
    }

    /**
     * Doi chuoi toa do thanh cap so.
     *
     * <p>Chap nhan hai dang: "C5" (kieu ban co) hoac "2 4" (kieu chi so).
     */
    private static int[] doiToaDo(String s) {
        String t = s.trim().toUpperCase();
        if (t.isEmpty()) {
            return null;
        }

        // Dang "C5"
        if (t.matches("^[A-J]([1-9]|10)$")) {
            int x = t.charAt(0) - 'A';
            int y = Integer.parseInt(t.substring(1)) - 1;
            return new int[] { x, y };
        }

        // Dang "2 4"
        String[] p = t.split("\\s+");
        if (p.length == 2) {
            try {
                return new int[] { Integer.parseInt(p[0]), Integer.parseInt(p[1]) };
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /* ------------------------------------------------------------------ */
    /* Hien thi                                                           */
    /* ------------------------------------------------------------------ */

    @SuppressWarnings("unchecked")
    private static void hienThi(Packet p) {
        switch (p.type()) {
            case LOGIN_OK -> {
                myName = p.arg(0);
                System.out.println("<< Dang nhap thanh cong: " + myName);
            }

            case ERROR -> System.out.println("<< LOI [" + p.arg(0) + "] " + p.arg(1));

            case CHAT_MSG -> System.out.println("<< [" + p.arg(0) + "] " + p.arg(1));

            case SYSTEM -> System.out.println("<< * " + p.arg(0));

            case WHO_LIST -> System.out.println("<< Dang online: " + p.arg(0));

            case ROOM_LIST_DATA -> {
                ArrayList<RoomInfo> ds = p.payload(ArrayList.class);
                System.out.println("<< ===== DANH SACH PHONG =====");
                if (ds == null || ds.isEmpty()) {
                    System.out.println("   (chua co phong nao)");
                } else {
                    for (RoomInfo r : ds) {
                        System.out.println("   " + r);
                    }
                }
                System.out.println("<< ===========================");
            }

            case ROOM_JOINED -> {
                RoomInfo r = p.payload(RoomInfo.class);
                System.out.println("<< Da vao phong #" + (r == null ? "?" : r.id())
                        + " - " + (r == null ? "?" : r.name())
                        + ("1".equals(p.arg(0)) ? "  (ban la chu phong)" : ""));
            }

            case ROOM_STATE -> {
                RoomInfo r = p.payload(RoomInfo.class);
                System.out.println("<< Trang thai: " + (r == null ? "?" : r.state().moTa())
                        + " | " + p.arg(0) + " vs " + p.arg(1));
            }

            case ROOM_LEFT -> System.out.println("<< Ban da roi phong, quay ve lobby.");

            case PLACE_PHASE -> {
                resetBanDo();
                System.out.println("<< ==========================================");
                System.out.println("<< DU HAI NGUOI. Go READY de dat tau va vao tran.");
                System.out.println("<< ==========================================");
            }

            case READY_OK -> System.out.println("<< So do da duoc chap nhan.");

            case GAME_START -> {
                System.out.println("<< ############ VAN DAU BAT DAU ############");
                System.out.println("<< Nguoi di truoc: " + p.arg(0));
            }

            case TURN -> {
                boolean luotCuaToi = myName.equals(p.arg(0));
                System.out.println(luotCuaToi
                        ? "<< >>> DEN LUOT BAN. Go: FIRE C5"
                        : "<< Dang cho " + p.arg(0) + " ban...");
            }

            case FIRE_RESULT -> {
                int x = p.intArg(0, 0);
                int y = p.intArg(1, 0);
                FireResult r = layKetQua(p.arg(2));
                danhDauBanDoDich(x, y, r);
                System.out.println("<< Ban vao " + tenO(x, y) + ": " + moTaKetQua(r, p.arg(3)));
                veCaHaiBanDo();
            }

            case INCOMING -> {
                int x = p.intArg(0, 0);
                int y = p.intArg(1, 0);
                FireResult r = layKetQua(p.arg(2));
                // Ap ket qua len ban do cua minh de hien thi cho dung.
                myBoard.fire(x, y);
                System.out.println("<< Doi thu ban vao " + tenO(x, y) + ": "
                        + moTaKetQua(r, p.arg(3)));
                veCaHaiBanDo();
            }

            case GAME_OVER -> {
                boolean thang = Protocol.RESULT_WIN.equals(p.arg(0));
                String lyDo = Protocol.REASON_OPPONENT_LEFT.equals(p.arg(1))
                        ? "Doi thu da roi phong." : "Da ban chim toan bo ham doi.";
                System.out.println("<< ##########################################");
                System.out.println("<< " + (thang ? "BAN THANG!" : "BAN THUA!") + " " + lyDo);
                System.out.println("<< ##########################################");
            }

            default -> System.out.println("<< " + p);
        }
    }

    private static FireResult layKetQua(String s) {
        try {
            return FireResult.valueOf(s.trim().toUpperCase());
        } catch (Exception e) {
            return FireResult.MISS;
        }
    }

    private static String moTaKetQua(FireResult r, String maTau) {
        if (r == FireResult.SUNK) {
            ShipType t = (maTau == null || maTau.isEmpty())
                    ? null : ShipType.fromCode(maTau.charAt(0));
            return "CHIM" + (t != null ? " " + t.label() + "!" : "!");
        }
        return r.moTa();
    }

    private static String tenO(int x, int y) {
        return "" + (char) ('A' + x) + (y + 1);
    }

    /* ------------------------------------------------------------------ */
    /* Ban do doi thu                                                     */
    /* ------------------------------------------------------------------ */

    private static void resetBanDo() {
        myBoard = new Board();
        for (int x = 0; x < Board.SIZE; x++) {
            for (int y = 0; y < Board.SIZE; y++) {
                enemyMarks[x][y] = '.';
            }
        }
    }

    private static void danhDauBanDoDich(int x, int y, FireResult r) {
        if (!Board.inBounds(x, y)) {
            return;
        }
        enemyMarks[x][y] = switch (r) {
            case MISS -> 'o';
            case HIT -> 'X';
            case SUNK -> '#';
            default -> enemyMarks[x][y];
        };
    }

    private static String veBanDoDich() {
        StringBuilder sb = new StringBuilder();
        sb.append("    A B C D E F G H I J\n");
        for (int y = 0; y < Board.SIZE; y++) {
            sb.append(String.format("%2d  ", y + 1));
            for (int x = 0; x < Board.SIZE; x++) {
                sb.append(enemyMarks[x][y]).append(' ');
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    /** In hai ban do canh nhau cho de theo doi. */
    private static void veCaHaiBanDo() {
        String[] a = myBoard.veBanDo(true).split("\n");
        String[] b = veBanDoDich().split("\n");
        System.out.println();
        System.out.println("   BAN DO CUA BAN              BAN DO DOI THU");
        int n = Math.max(a.length, b.length);
        for (int i = 0; i < n; i++) {
            String l = i < a.length ? a[i] : "";
            String r = i < b.length ? b[i] : "";
            System.out.printf("%-28s%s%n", l, r);
        }
        System.out.println(Board.chuGiai());
        System.out.println();
    }
}