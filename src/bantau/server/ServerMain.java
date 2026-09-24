package bantau.server;

import bantau.common.Protocol;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * GIAI DOAN 3 - SERVER CO DANG NHAP
 *
 * <p>Khac Giai doan 2 o hai diem:
 * <ul>
 *   <li>Moi ban tin di theo khuon dang LENH|thamso thay vi text tu do.</li>
 *   <li>Nguoi choi phai LOGIN voi ten duy nhat truoc khi lam viec khac.</li>
 * </ul>
 */
public class ServerMain {

    /**
     * Danh sach nguoi da dang nhap: ten viet thuong -> handler.
     *
     * <p>Vi sao khoa la ten VIET THUONG? De chan duoc truong hop hai nguoi
     * dung "Khanh" va "khanh" - ve mat hien thi la hai ten khac nhau nhung
     * nguoi choi se nham lan.
     *
     * <p>putIfAbsent() cua ConcurrentHashMap la thao tac NGUYEN TU: kiem tra
     * va them vao chi trong mot buoc. Neu tach thanh "if (!chua co) then them"
     * thi hai thread co the cung vuot qua buoc kiem tra roi cung them vao.
     */
    private static final Map<String, ClientHandler> USERS = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : Protocol.DEFAULT_PORT;
        ExecutorService pool = Executors.newCachedThreadPool();

        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("=== SERVER GIAI DOAN 3 ===");
            System.out.println("Dang lang nghe tai cong " + port + ".");
            System.out.println("Client phai go: LOGIN|<ten>  truoc khi lam viec khac.");

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
     * Dang ky ten dang nhap.
     *
     * @return true neu thanh cong, false neu ten da co nguoi dung
     */
    public static boolean registerUser(String name, ClientHandler handler) {
        return USERS.putIfAbsent(name.toLowerCase(), handler) == null;
    }

    /** Bo ten khoi danh sach khi nguoi choi thoat. */
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

    /** Gui mot ban tin toi TAT CA nguoi da dang nhap. */
    public static void broadcast(String msg) {
        System.out.println("[BROADCAST] " + msg);
        for (ClientHandler c : USERS.values()) {
            c.send(msg);
        }
    }
}