package bantau.server;

import bantau.common.BaoMat;
import bantau.common.Packet;
import bantau.common.PacketType;
import bantau.common.Protocol;
import bantau.server.db.DaoRam;
import bantau.server.db.Database;
import bantau.server.db.MatchDao;
import bantau.server.db.MatchDaoMySql;
import bantau.server.db.PlayerDao;
import bantau.server.db.PlayerDaoMySql;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * SERVER - TCP + Java Serialization, co lobby nhieu phong choi.
 */
public class ServerMain {

    /** Nguoi da dang nhap: ten viet thuong -> handler. */
    private static final Map<String, ClientHandler> USERS = new ConcurrentHashMap<>();

    /** Quan ly phong. Dung chung cho toan server. */
    private static final RoomManager ROOMS = new RoomManager();

    /** Noi luu tai khoan va lich su. Chon o {@link #chonNoiLuuTru()}. */
    private static PlayerDao players;
    private static MatchDao matches;

    /**
     * Khoi dong server.
     *
     * <pre>
     *   java bantau.server.ServerMain [cong] [ssl|nossl]
     * </pre>
     *
     * <p>Tham so thu hai de demo so sanh: chay mot lan {@code nossl} roi bat
     * goi tin se doc duoc mat khau nguyen van, chay lai voi {@code ssl} thi
     * chi con byte ngau nhien.
     */
    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : Protocol.DEFAULT_PORT;
        if (args.length > 1) {
            BaoMat.batSsl(!"nossl".equalsIgnoreCase(args[1]));
        }
        ExecutorService pool = Executors.newCachedThreadPool();

        chonNoiLuuTru();
        batDauCanhGac();

        try (ServerSocket server = BaoMat.taoServerSocket(port)) {
            System.out.println("=== SERVER BAN TAU ONLINE ===");
            System.out.println("Che do duong truyen: " + BaoMat.moTaCheDo());
            System.out.println("Dang lang nghe tai cong " + port + ".");

            while (true) {
                Socket socket = server.accept();
                socket.setTcpNoDelay(true);
                try {
                    pool.execute(new ClientHandler(socket));
                } catch (IOException e) {
                    System.out.println("Khong tao duoc handler: " + e.getMessage());
                    socket.close();
                }
            }

        } catch (IOException e) {
            System.out.println("Loi server: " + e.getMessage());
        } finally {
            pool.shutdownNow();
        }
    }

    /**
     * Thu ket noi CSDL. Ket noi duoc thi luu that vao MySQL, khong duoc thi
     * luu tam trong bo nho de server van chay binh thuong.
     *
     * <p>Day la loi ich cua viec tach {@link PlayerDao} thanh interface:
     * chon cach luu tru chi nam gon trong ham nay, phan con lai cua server
     * khong he biet dang dung cach nao.
     */
    private static void chonNoiLuuTru() {
        if (Database.khoiTao()) {
            players = new PlayerDaoMySql();
            matches = new MatchDaoMySql();
            System.out.println("[CSDL] Tai khoan va lich su duoc luu vao MySQL.");
        } else {
            DaoRam ram = new DaoRam();
            players = ram;
            matches = ram;
            System.out.println("[CSDL] CHE DO KHONG CSDL: tai khoan chi luu tam trong"
                    + " bo nho, tat server la mat.");
        }
    }

    /**
     * THREAD CANH GAC - PHAT HIEN CLIENT DA CHET
     *
     * <p>Cu {@link Protocol#PING_INTERVAL_SECONDS} giay mot lan, thread nay
     * gui PING toi moi nguoi dang online. Client nhan duoc thi tra PONG ngay,
     * va {@link ClientHandler} ghi lai moc thoi gian do.
     *
     * <p>Ai im lang qua {@link Protocol#TIMEOUT_SECONDS} giay thi bi ngat ket
     * noi. Viec ngat se lam {@code readObject()} o thread cua nguoi do bat
     * duoc loi, chay vao khoi finally roi don dep: go khoi phong, xu thua
     * neu dang danh do, tra ten ve cho nguoi khac dung.
     *
     * <p><b>Vi sao khong the thieu:</b> TCP chi bao loi khi mot ben DONG
     * SOCKET dang hoang. Rut day mang hay tat nguon thi khong co goi tin nao
     * duoc gui di ca, server khong he hay biet va cu cho mai mai.
     *
     * <p>Dung thread daemon de no khong giu chuong trinh song khi server dung.
     */
    private static void batDauCanhGac() {
        Thread canhGac = new Thread(() -> {
            int giay = 0;
            while (true) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                giay++;

                // Moi giay: kiem tra ai het gio suy nghi.
                // Phai quet moi giay chu khong phai moi 10 giay, neu khong
                // dong ho tren man hinh nguoi choi se lech toi 10 giay so
                // voi luc server thuc su chuyen luot.
                for (Room r : ROOMS.tatCaPhong()) {
                    r.kiemTraHetGio();
                }

                // Moi PING_INTERVAL_SECONDS giay: hoi xem ai con song.
                if (giay % Protocol.PING_INTERVAL_SECONDS == 0) {
                    Packet ping = Packet.of(PacketType.PING);
                    for (ClientHandler c : USERS.values()) {
                        if (c.soGiayImLang() > Protocol.TIMEOUT_SECONDS) {
                            c.ngatKetNoi("khong tra loi PING trong "
                                    + Protocol.TIMEOUT_SECONDS + " giay");
                        } else {
                            c.send(ping);
                        }
                    }
                }
            }
        }, "canh-gac");
        canhGac.setDaemon(true);
        canhGac.start();
        System.out.println("[CANH GAC] Moi luot " + Protocol.TURN_SECONDS
                + " giay. PING moi " + Protocol.PING_INTERVAL_SECONDS
                + " giay, ngat sau " + Protocol.TIMEOUT_SECONDS + " giay im lang.");
    }

    public static PlayerDao players() {
        return players;
    }

    public static MatchDao matches() {
        return matches;
    }

    public static RoomManager rooms() {
        return ROOMS;
    }

    public static Collection<ClientHandler> getUsers() {
        return USERS.values();
    }

    public static boolean registerUser(String name, ClientHandler handler) {
        return USERS.putIfAbsent(name.toLowerCase(), handler) == null;
    }

    public static void unregisterUser(String name) {
        if (name != null) {
            USERS.remove(name.toLowerCase());
        }
    }

    public static int onlineCount() {
        return USERS.size();
    }

    public static String onlineNames() {
        StringBuilder sb = new StringBuilder();
        for (ClientHandler c : USERS.values()) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(c.getUsername());
        }
        return sb.toString();
    }

    /** Gui toi tat ca nguoi da dang nhap. */
    public static void broadcast(Packet packet) {
        System.out.println("[BROADCAST] " + packet);
        for (ClientHandler c : USERS.values()) {
            c.send(packet);
        }
    }
}
