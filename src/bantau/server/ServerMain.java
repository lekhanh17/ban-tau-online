package bantau.server;

import bantau.common.Packet;
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

    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : Protocol.DEFAULT_PORT;
        ExecutorService pool = Executors.newCachedThreadPool();

        chonNoiLuuTru();

        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("=== SERVER BAN TAU ONLINE ===");
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
