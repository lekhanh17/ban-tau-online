package bantau.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * GIAI DOAN 2 - Mot thread phuc vu MOT client.
 *
 * <p>Mo hinh "thread per connection": moi ket noi TCP duoc giao cho mot thread
 * rieng. Nho vay client nay doc du lieu cham cung khong lam ket client khac.
 */
public class ClientHandler implements Runnable {

    /**
     * Bo sinh so thu tu cho client.
     * Dung AtomicInteger chu khong dung "int dem++" vi nhieu thread co the
     * cung tang mot luc - phep ++ khong phai la thao tac nguyen tu.
     */
    private static final AtomicInteger ID_GEN = new AtomicInteger(1);

    private final Socket socket;
    private final String name;
    private final BufferedReader in;
    private final PrintWriter out;

    /**
     * Tao luong doc/ghi ngay trong constructor.
     * Lam vay de khi handler duoc them vao danh sach thi no da san sang nhan
     * ban tin broadcast - neu tao trong run() se co khoang thoi gian ngan
     * bi mat ban tin.
     */
    public ClientHandler(Socket socket) throws IOException {
        this.socket = socket;
        this.name = "Client-" + ID_GEN.getAndIncrement();
        this.in = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.out = new PrintWriter(
                new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
    }

    public String getName() {
        return name;
    }

    /**
     * Gui mot ban tin xuong client nay.
     *
     * <p>Phai dong bo: nhieu thread khac (thread cua cac client khac) cung goi
     * ham nay de broadcast. Neu hai thread ghi xen ke nhau thi hai ban tin se
     * bi tron lan, client nhan duoc rac.
     */
    public synchronized void send(String msg) {
        out.print(msg + "\n");
        out.flush();
    }

    @Override
    public void run() {
        System.out.println(name + " ket noi tu " + socket.getRemoteSocketAddress());
        try {
            send("Chao " + name + "! Dang co " + ServerMain.onlineCount() + " nguoi online.");
            send("Go 'QUIT' de thoat.");
            ServerMain.broadcast("*** " + name + " da vao phong ***");

            String line;
            while ((line = in.readLine()) != null) {
                if ("QUIT".equalsIgnoreCase(line.trim())) {
                    break;
                }
                // Gui ban tin cua nguoi nay toi TAT CA moi nguoi dang online.
                ServerMain.broadcast("[" + name + "] " + line);
            }

        } catch (IOException e) {
            // Client tat dot ngot (dong cua so, rut mang) se roi vao day.
            System.out.println(name + " mat ket noi: " + e.getMessage());

        } finally {
            // Khoi finally LUON chay, du thoat binh thuong hay do loi.
            // Day la cho don dep duy nhat dang tin cay.
            ServerMain.removeClient(this);
            ServerMain.broadcast("*** " + name + " da roi di ***");
            try {
                socket.close();
            } catch (IOException ignored) {
                // khong con gi de lam
            }
            System.out.println(name + " da ngat. Con lai " + ServerMain.onlineCount() + " nguoi.");
        }
    }
}