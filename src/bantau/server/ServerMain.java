package bantau.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * GIAI DOAN 1 - ECHO SERVER
 *
 * <p>Server nhan mot dong text tu client, in ra man hinh, roi gui tra lai
 * dong do kem tien to "ECHO: ". Chi phuc vu MOT client duy nhat.
 *
 * <p>Muc dich: nam vung 4 lop co ban cua lap trinh socket truoc khi lam game.
 *
 * <p>Cach chay: chuot phai file nay -> Run File (Shift+F6),
 * sau do mo CMD va go: telnet 127.0.0.1 5000
 */
public class ServerMain {

    /** Cong ma server lang nghe. */
    public static final int PORT = 5000;

    public static void main(String[] args) {

        // TODO 1: ServerSocket "chiem" cong 5000 cua may nay.
        // Dat trong try-with-resources de Java tu dong dong lai khi ket thuc,
        // neu khong cong 5000 se bi giu lai va lan chay sau bao "Address already in use".
        try (ServerSocket server = new ServerSocket(PORT)) {

            // TODO 2: Bao cho nguoi dung biet server da san sang.
            System.out.println("Server dang lang nghe tai cong " + PORT + "...");
            System.out.println("Mo mot cua so CMD khac va go: telnet 127.0.0.1 " + PORT);

            // TODO 3: accept() la ham CHAN (blocking) - chuong trinh dung lai o dong nay
            // cho toi khi co client ket noi vao. Khi do no tra ve mot Socket dai dien
            // cho duong ong hai chieu toi dung client vua ket noi.
            try (Socket socket = server.accept();

                 // TODO 4: Boc luong byte tho thanh luong ky tu doc duoc theo tung dong.
                 //   getInputStream()   -> byte tho tu client gui len
                 //   InputStreamReader  -> doi byte thanh ky tu (phai chi ro UTF-8)
                 //   BufferedReader     -> them bo dem va cho phep goi readLine()
                 BufferedReader in = new BufferedReader(
                         new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

                 PrintWriter out = new PrintWriter(
                         new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {

                System.out.println("Client da ket noi tu " + socket.getRemoteSocketAddress());

                // TODO 5: Vong lap doc tung dong cho toi khi client ngat ket noi.
                String line;
                while ((line = in.readLine()) != null) {

                    System.out.println("Nhan duoc: " + line);

                    // Dung print(... + "\n") chu KHONG dung println().
                    // Tren Windows, println() gui "\r\n" - ky tu \r thua se gay loi
                    // khi ta tu phan tich ban tin o cac giai doan sau.
                    out.print("ECHO: " + line + "\n");

                    // Bat buoc phai flush(). PrintWriter co bo dem, neu khong flush
                    // thi du lieu con nam trong bo nho, chua thuc su di ra socket
                    // va client se khong nhan duoc gi.
                    out.flush();
                }

                // readLine() tra ve null nghia la client da dong ket noi.
                // TODO 6:
                System.out.println("Client da ngat ket noi.");
            }

        } catch (IOException e) {
            // TODO 7: Loi thuong gap nhat la cong 5000 dang bi chuong trinh khac chiem.
            System.out.println("Loi: " + e.getMessage());
        }

        System.out.println("Server da dung.");
    }
}