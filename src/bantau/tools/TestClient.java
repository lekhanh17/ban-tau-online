package bantau.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * CLIENT DÒNG LỆNH DÙNG ĐỂ KIỂM THỬ
 *
 * <p>Thay cho telnet: ket noi toi server, cho phep go tin nhan va hien thi
 * moi ban tin server gui ve.
 *
 * <p>Diem quan trong: chuong trinh nay dung HAI thread.
 * <ul>
 *   <li>Thread phu: dung yen cho du lieu tu socket (in.readLine()).</li>
 *   <li>Thread chinh: dung yen cho nguoi dung go phim (keyboard.readLine()).</li>
 * </ul>
 * Mot thread khong the vua cho socket vua cho ban phim, vi ca hai ham readLine()
 * deu la ham CHAN - dang dung o ham nay thi khong chay duoc ham kia.
 *
 * <p>Cach chay: bam Run phia tren ham main. Bam nhieu lan de mo nhieu client.
 */
public class TestClient {

    public static void main(String[] args) throws IOException {
        String host = args.length > 0 ? args[0] : "127.0.0.1";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 5000;

        Socket socket = new Socket(host, port);
        System.out.println("Đã kết nối tới " + host + ":" + port);
        System.out.println("Gõ QUIT để thoát");
        System.out.println("--------------------------------------------");

        BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        PrintWriter out = new PrintWriter(
                new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));

        // THREAD PHỤ: Cho tin từ server rồi in ra
        Thread reader = new Thread(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    System.out.println("<< " + line);
                }
                System.out.println("Server da dong ket noi.");
            } catch (IOException e) {
                System.out.println("Mat ket noi: " + e.getMessage());
            }
        }, "reader");

        // Thread nền: Tự kết thúc khi chương trình chính thoát
        reader.setDaemon(true);
        reader.start();

        // THREAD chính: Cho người dùng nhập rồi gửi lên server
        BufferedReader keyboard = new BufferedReader(
                new InputStreamReader(System.in, StandardCharsets.UTF_8));
        String line;
        while ((line = keyboard.readLine()) != null) {
            out.print(line + "\n");
            out.flush();
            if ("QUIT".equalsIgnoreCase(line.trim())) {
                break;
            }
        }

        socket.close();
        System.out.println("Đã thoát!");
    }
}