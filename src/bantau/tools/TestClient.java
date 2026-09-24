package bantau.tools;

import bantau.common.Packet;
import bantau.common.PacketType;
import bantau.common.Protocol;
import bantau.common.RoomInfo;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

/**
 * CLIENT DONG LENH DUNG DE KIEM THU
 *
 * <p>Lenh ho tro:
 * <pre>
 *   LOGIN &lt;ten&gt;        dang nhap
 *   ROOMS              xem danh sach phong
 *   CREATE &lt;ten&gt;       tao phong moi
 *   JOIN &lt;id&gt;          vao phong
 *   LEAVE              roi phong
 *   CHAT &lt;noi dung&gt;    chat (trong phong hoac o lobby)
 *   WHO                xem ai dang online
 *   QUIT               thoat
 * </pre>
 */
public class TestClient {

    public static void main(String[] args) throws IOException {
        String host = args.length > 0 ? args[0] : Protocol.DEFAULT_HOST;
        int port = args.length > 1 ? Integer.parseInt(args[1]) : Protocol.DEFAULT_PORT;

        Socket socket = new Socket(host, port);

        // Thu tu bat buoc: luong ra truoc, flush, roi luong vao.
        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

        System.out.println("Da ket noi toi " + host + ":" + port);
        System.out.println("Lenh: LOGIN <ten> | ROOMS | CREATE <ten> | JOIN <id> | LEAVE");
        System.out.println("      CHAT <noi dung> | WHO | QUIT");
        System.out.println("--------------------------------------------");

        // THREAD PHU: cho ban tin tu server.
        Thread reader = new Thread(() -> {
            try {
                while (true) {
                    Object obj = in.readObject();
                    if (obj instanceof Packet p) {
                        hienThi(p);
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

        // THREAD CHINH: doc ban phim.
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

    /** Doi dong nguoi dung go thanh mot Packet. */
    private static Packet phanTich(String line) {
        String s = line.trim();
        if (s.isEmpty()) {
            return null;
        }
        int space = s.indexOf(' ');
        String cmd = (space < 0 ? s : s.substring(0, space)).toUpperCase();
        String rest = space < 0 ? "" : s.substring(space + 1).trim();

        return switch (cmd) {
            case "LOGIN" -> Packet.of(PacketType.LOGIN, rest);
            case "CHAT" -> Packet.of(PacketType.CHAT, rest);
            case "WHO" -> Packet.of(PacketType.WHO);
            case "ROOMS" -> Packet.of(PacketType.ROOM_LIST);
            case "CREATE" -> Packet.of(PacketType.ROOM_CREATE, rest);
            case "JOIN" -> Packet.of(PacketType.ROOM_JOIN, rest);
            case "LEAVE" -> Packet.of(PacketType.ROOM_LEAVE);
            case "QUIT" -> Packet.of(PacketType.QUIT);
            default -> {
                System.out.println("!! Lenh khong ton tai: " + cmd);
                yield null;
            }
        };
    }

    /** Hien thi ban tin nhan duoc cho de doc. */
    @SuppressWarnings("unchecked")
    private static void hienThi(Packet p) {
        switch (p.type()) {
            case LOGIN_OK -> System.out.println("<< Dang nhap thanh cong: " + p.arg(0));

            case ERROR -> System.out.println("<< LOI [" + p.arg(0) + "] " + p.arg(1));

            case CHAT_MSG -> System.out.println("<< [" + p.arg(0) + "] " + p.arg(1));

            case SYSTEM -> System.out.println("<< * " + p.arg(0));

            case WHO_LIST -> System.out.println("<< Dang online: " + p.arg(0));

            case ROOM_LIST_DATA -> {
                // Day la cho the hien ro loi ich cua Serialization:
                // nhan thang mot danh sach doi tuong, khong phai tach chuoi.
                ArrayList<RoomInfo> ds = p.payload(ArrayList.class);
                System.out.println("<< ===== DANH SACH PHONG =====");
                if (ds == null || ds.isEmpty()) {
                    System.out.println("   (chua co phong nao)");
                } else {
                    for (RoomInfo r : ds) {
                        System.out.println("   " + r);
                    }
                }
                System.out.println("<< ===========================");
            }

            case ROOM_JOINED -> {
                RoomInfo r = p.payload(RoomInfo.class);
                boolean laChuPhong = "1".equals(p.arg(0));
                System.out.println("<< Da vao phong #" + (r == null ? "?" : r.id())
                        + " - " + (r == null ? "?" : r.name())
                        + (laChuPhong ? "  (ban la chu phong)" : ""));
            }

            case ROOM_STATE -> {
                RoomInfo r = p.payload(RoomInfo.class);
                System.out.println("<< Trang thai phong: "
                        + (r == null ? "?" : r.state().moTa())
                        + " | Nguoi choi: " + p.arg(0) + " vs " + p.arg(1));
            }

            case ROOM_LEFT -> System.out.println("<< Ban da roi phong, quay ve lobby.");

            default -> System.out.println("<< " + p);
        }
    }
}