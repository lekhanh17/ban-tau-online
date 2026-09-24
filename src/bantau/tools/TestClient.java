package bantau.tools;

import bantau.common.Packet;
import bantau.common.PacketType;
import bantau.common.Protocol;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * CLIENT DONG LENH DUNG DE KIEM THU
 *
 * <p>Nguoi dung go lenh dang: LOGIN khanh / CHAT xin chao / WHO / QUIT
 * Client doi chuoi do thanh doi tuong {@link Packet} roi gui di.
 *
 * <p>Van dung HAI thread: mot thread cho ban tin tu server, mot thread cho
 * nguoi dung go phim. Ca hai ham doc deu la ham chan nen khong the gop lam mot.
 */
public class TestClient {

    public static void main(String[] args) throws IOException {
        String host = args.length > 0 ? args[0] : Protocol.DEFAULT_HOST;
        int port = args.length > 1 ? Integer.parseInt(args[1]) : Protocol.DEFAULT_PORT;

        Socket socket = new Socket(host, port);

        // THU TU QUAN TRONG: tao luong ra truoc, flush(), roi moi tao luong vao.
        // Nguoc lai se treo vi hai ben cung cho header cua nhau.
        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

        System.out.println("Da ket noi toi " + host + ":" + port);
        System.out.println("Lenh: LOGIN <ten> | CHAT <noi dung> | WHO | QUIT");
        System.out.println("--------------------------------------------");

        // THREAD PHU: cho ban tin tu server ve roi in ra.
        Thread reader = new Thread(() -> {
            try {
                while (true) {
                    Object obj = in.readObject();
                    if (obj instanceof Packet p) {
                        System.out.println("<< " + hienThi(p));
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

        // THREAD CHINH: doc ban phim, doi thanh Packet roi gui.
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

    /**
     * Doi dong nguoi dung go thanh mot Packet.
     *
     * <p>Diem khac biet lon so voi ban dung text: viec phan tich chuoi chi xay
     * ra o DAY, phia client, truoc khi gui. Tren duong truyen khong con chuoi
     * nao ca - chi co doi tuong. Server khong phai tach chuoi lan nua.
     */
    private static Packet phanTich(String line) {
        String s = line.trim();
        if (s.isEmpty()) {
            return null;
        }

        // Tach lenh va phan con lai: "CHAT xin chao ban" -> "CHAT" + "xin chao ban"
        int space = s.indexOf(' ');
        String cmd = (space < 0 ? s : s.substring(0, space)).toUpperCase();
        String rest = space < 0 ? "" : s.substring(space + 1).trim();

        return switch (cmd) {
            case "LOGIN" -> Packet.of(PacketType.LOGIN, rest);
            case "CHAT" -> Packet.of(PacketType.CHAT, rest);
            case "WHO" -> Packet.of(PacketType.WHO);
            case "QUIT" -> Packet.of(PacketType.QUIT);
            default -> {
                System.out.println("!! Lenh khong ton tai: " + cmd
                        + ". Chi co LOGIN, CHAT, WHO, QUIT");
                yield null;
            }
        };
    }

    /** Hien thi ban tin nhan duoc cho de doc. */
    private static String hienThi(Packet p) {
        return switch (p.type()) {
            case LOGIN_OK -> "Dang nhap thanh cong voi ten: " + p.arg(0);
            case ERROR -> "LOI [" + p.arg(0) + "] " + p.arg(1);
            case CHAT_MSG -> "[" + p.arg(0) + "] " + p.arg(1);
            case SYSTEM -> "* " + p.arg(0);
            case WHO_LIST -> "Dang online: " + p.arg(0);
            default -> p.toString();
        };
    }
}