package bantau.server;

import bantau.common.Packet;
import bantau.common.Protocol;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * SERVER - trao doi bang doi tuong Java (Java Serialization) tren nen TCP.
 *
 * <p>Tang giao van: TCP, cong 5000 - giao thuc chuan co san.
 * Tang ung dung: gui/nhan doi tuong {@link Packet} bang co che tuan tu hoa
 * cua Java, khong tu dinh nghia khuon dang chuoi.
 */
public class ServerMain {

    /**
     * Danh sach nguoi da dang nhap: ten viet thuong -> handler.
     * putIfAbsent() la thao tac nguyen tu, chan duoc hai nguoi cung dang ky
     * mot ten trong cung mot khoanh khac.
     */
    private static final Map<String, ClientHandler> USERS = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : Protocol.DEFAULT_PORT;
        ExecutorService pool = Executors.newCachedThreadPool();

        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("=== SERVER (Java Serialization) ===");
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

    /** Danh sach ten nguoi dang online, ngan cach bang dau phay. */
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

    /** Gui mot ban tin toi tat ca nguoi da dang nhap. */
    public static void broadcast(Packet packet) {
        System.out.println("[BROADCAST] " + packet);
        for (ClientHandler c : USERS.values()) {
            c.send(packet);
        }
    }
}