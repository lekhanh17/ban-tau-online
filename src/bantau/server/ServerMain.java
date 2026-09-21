package bantau.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * GIAI DOAN 2 - SERVER DA LUONG
 *
 * <p>Khac Giai doan 1 o ba diem:
 * <ul>
 *   <li>accept() nam trong vong lap while(true) nen nhan duoc nhieu client.</li>
 *   <li>Moi client duoc giao cho mot thread rieng lay tu thread pool.</li>
 *   <li>Co danh sach client dang online de gui ban tin cho tat ca (broadcast).</li>
 * </ul>
 *
 * <p>Cach kiem thu: chay file nay, sau do mo 3 cua so CMD cung go
 * "telnet 127.0.0.1 5000". Go chu o cua so nay, ca 3 cua so deu thay.
 */
public class ServerMain {

    public static final int PORT = 5000;

    /**
     * Danh sach client dang ket noi.
     *
     * <p>Day la du lieu CHIA SE giua nhieu thread: thread accept them vao,
     * thread cua tung client xoa ra, va moi lan broadcast lai duyet qua no.
     * Dung ArrayList thuong se gap ConcurrentModificationException hoac mat
     * du lieu. ConcurrentHashMap.newKeySet() cho phep vua duyet vua sua an toan.
     */
    private static final Set<ClientHandler> CLIENTS = ConcurrentHashMap.newKeySet();

    public static void main(String[] args) {

        // Thread pool tu quan ly viec tao va tai su dung thread.
        // newCachedThreadPool: tao thread moi khi can, thu hoi thread ranh sau 60 giay.
        ExecutorService pool = Executors.newCachedThreadPool();

        try (ServerSocket server = new ServerSocket(PORT)) {
            System.out.println("=== SERVER GIAI DOAN 2 ===");
            System.out.println("Dang lang nghe tai cong " + PORT + ". Nhan Ctrl+C de dung.");
            System.out.println("Mo nhieu cua so CMD va go: telnet 127.0.0.1 " + PORT);

            while (true) {
                // Vong lap nay chi lam mot viec: don khach roi giao cho thread khac.
                // Nho vay no quay lai accept() ngay, san sang don client tiep theo.
                Socket socket = server.accept();
                socket.setTcpNoDelay(true);

                try {
                    ClientHandler handler = new ClientHandler(socket);
                    CLIENTS.add(handler);
                    pool.execute(handler);
                } catch (IOException e) {
                    // Mot client loi thi bo qua client do, server van chay tiep.
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

    /** So nguoi dang online. */
    public static int onlineCount() {
        return CLIENTS.size();
    }

    /** Bo mot client khoi danh sach khi ho ngat ket noi. */
    public static void removeClient(ClientHandler handler) {
        CLIENTS.remove(handler);
    }

    /** Gui mot ban tin toi TAT CA client dang online. */
    public static void broadcast(String msg) {
        System.out.println("[BROADCAST] " + msg);
        for (ClientHandler c : CLIENTS) {
            c.send(msg);
        }
    }
}