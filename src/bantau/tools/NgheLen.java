package bantau.tools;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * CONG CU NGHE LEN - CHUNG MINH TAC DUNG CUA MA HOA
 *
 * <p>Day la mot <b>proxy TCP</b> ngoi giua client va server. No nhan ket noi
 * tu client, mo mot ket noi khac toi server, roi chuyen tiep byte qua lai
 * giua hai ben. Quan trong la no <b>in ra man hinh toan bo byte di qua</b>.
 *
 * <pre>
 *   Client  ---->  NgheLen (cong 5555)  ---->  Server (cong 5000)
 *                       |
 *                  in ra man hinh
 * </pre>
 *
 * <p>Day chinh la mo hinh tan cong "nguoi dung giua" (man-in-the-middle):
 * ke tan cong chen vao giua hai ben va doc trom du lieu.
 *
 * <p><b>Cach dung de demo:</b>
 * <ol>
 *   <li>Chay server o che do KHONG ma hoa:
 *       {@code java bantau.server.ServerMain 5000 nossl}</li>
 *   <li>Chay cong cu nay: {@code java bantau.tools.NgheLen 5555 127.0.0.1 5000}</li>
 *   <li>Mo client, <b>bo tick</b> o ma hoa, doi cong thanh <b>5555</b>,
 *       dang nhap</li>
 *   <li>Nhin man hinh cong cu nay: <b>doc duoc nguyen van ten va mat khau</b></li>
 *   <li>Lam lai voi che do co ma hoa (server chay {@code ssl}, client tick o
 *       ma hoa): chi con byte ngau nhien</li>
 * </ol>
 *
 * <p>Chup man hinh hai lan nay dat canh nhau la bang chung truc quan nhat cho
 * phan bao mat trong bao cao, va khong can cai Wireshark.
 *
 * <p>Khong thuoc phan chuong trinh nop.
 */
public final class NgheLen {

    /** So byte in tren mot dong hex. */
    private static final int MOI_DONG = 16;
    /** Chi in toi da ngan nay byte moi chieu, tranh tran man hinh. */
    private static final int GIOI_HAN = 2048;

    public static void main(String[] args) throws IOException {
        int congNghe = args.length > 0 ? Integer.parseInt(args[0]) : 5555;
        String serverHost = args.length > 1 ? args[1] : "127.0.0.1";
        int serverPort = args.length > 2 ? Integer.parseInt(args[2]) : 5000;

        System.out.println("=== CONG CU NGHE LEN ===");
        System.out.println("Lang nghe tai cong " + congNghe
                + ", chuyen tiep toi " + serverHost + ":" + serverPort);
        System.out.println("Hay cho client ket noi vao cong " + congNghe
                + " thay vi " + serverPort + ".");
        System.out.println();

        try (ServerSocket cho = new ServerSocket(congNghe)) {
            int soKetNoi = 0;
            while (true) {
                Socket tuClient = cho.accept();
                soKetNoi++;
                final int id = soKetNoi;
                System.out.println("--- Ket noi #" + id + " tu "
                        + tuClient.getRemoteSocketAddress() + " ---");

                Socket toiServer = new Socket(serverHost, serverPort);

                // Hai chieu, moi chieu mot thread.
                chuyenTiep(tuClient, toiServer, "CLIENT -> SERVER", id);
                chuyenTiep(toiServer, tuClient, "SERVER -> CLIENT", id);
            }
        }
    }

    /** Doc tu nguon, in ra man hinh, roi ghi sang dich. */
    private static void chuyenTiep(Socket nguon, Socket dich,
            String nhan, int id) {
        Thread t = new Thread(() -> {
            byte[] dem = new byte[4096];
            int daIn = 0;
            try (InputStream in = nguon.getInputStream();
                    OutputStream out = dich.getOutputStream()) {
                int n;
                while ((n = in.read(dem)) != -1) {
                    if (daIn < GIOI_HAN) {
                        inGoi(nhan, id, dem, n);
                        daIn += n;
                        if (daIn >= GIOI_HAN) {
                            System.out.println("  [" + nhan
                                    + "] ... (da du " + GIOI_HAN
                                    + " byte, thoi khong in nua)");
                        }
                    }
                    out.write(dem, 0, n);
                    out.flush();
                }
            } catch (IOException e) {
                // mot ben dong ket noi, binh thuong
            }
        }, "nghelen-" + id + "-" + nhan);
        t.setDaemon(true);
        t.start();
    }

    /** In mot goi du lieu duoi dang hex kem cot chu de doc. */
    private static synchronized void inGoi(String nhan, int id,
            byte[] dem, int soByte) {
        System.out.println();
        System.out.println("[#" + id + "] " + nhan + "  (" + soByte + " byte)");

        for (int i = 0; i < soByte; i += MOI_DONG) {
            StringBuilder hex = new StringBuilder();
            StringBuilder chu = new StringBuilder();

            for (int j = 0; j < MOI_DONG; j++) {
                if (i + j < soByte) {
                    int b = dem[i + j] & 0xFF;
                    hex.append(String.format("%02X ", b));
                    // Ky tu doc duoc thi in ra, con lai thay bang dau cham.
                    chu.append(b >= 32 && b < 127 ? (char) b : '.');
                } else {
                    hex.append("   ");
                }
            }
            System.out.printf("  %04X  %s |%s|%n", i, hex, chu);
        }
    }
}
