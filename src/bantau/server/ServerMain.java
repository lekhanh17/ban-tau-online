package bantau.server;

import bantau.common.Packet;
import bantau.common.Protocol;

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

    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : Protocol.DEFAULT_PORT;
        ExecutorService pool = Executors.newCachedThreadPool();

        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("=== SERVER GIAI DOAN 4 - LOBBY NHIEU PHONG ===");
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