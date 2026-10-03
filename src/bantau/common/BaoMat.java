package bantau.common;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

/**
 * MA HOA DUONG TRUYEN BANG SSL/TLS
 *
 * <p>Lop nay tao socket cho ca server lan client, co hai che do:
 * <ul>
 *   <li><b>Khong SSL</b> - socket thuong, du lieu di qua mang o dang tho.
 *       Bat goi tin bang Wireshark se doc duoc nguyen van mat khau.</li>
 *   <li><b>Co SSL</b> - socket duoc boc mot lop ma hoa TLS. Bat goi tin chi
 *       thay byte ngau nhien.</li>
 * </ul>
 *
 * <p>Giu ca hai che do de co the <b>demo so sanh</b>: chay thu mot lan khong
 * ma hoa, mot lan co ma hoa, chup man hinh Wireshark ca hai lan dat canh nhau.
 * Day la bang chung truc quan nhat cho phan bao mat trong bao cao.
 *
 * <p><b>Ve chung chi tu ky:</b> do an dung chung chi do chinh nhom tao bang
 * cong cu keytool cua JDK, khong mua tu to chuc cap chung chi (CA) nhu
 * Let's Encrypt hay DigiCert. Hau qua: trinh duyet hay he thong khac se canh
 * bao "chung chi khong tin cay" vi khong co ben thu ba nao xac nhan. Voi do
 * an chay trong mang noi bo thi chap nhan duoc, vi client duoc cau hinh san
 * de tin dung chung chi nay. He thong that bat buoc phai dung chung chi do
 * CA cap.
 *
 * <p><b>SSL va TLS khac nhau the nao:</b> SSL la ten cu, cac phien ban SSL
 * deu da bi coi la khong an toan va bi loai bo. TLS la ten moi va la thu
 * thuc su dang chay. Java van giu ten lop la SSLSocket vi ly do tuong thich
 * nguoc, nhung ben trong no dam phan TLS.
 */
public final class BaoMat {

    /** Duong dan file kho khoa, tinh tu thu muc goc du an. */
    private static final String KEYSTORE = "certs/bantau.jks";
    private static final String MAT_KHAU_KHO = "bantau123";

    /** Co bat ma hoa khong. Doi bang {@link #batSsl(boolean)}. */
    private static boolean dungSsl = true;

    /** Dung lai sau lan tao dau tien - doc file kho khoa mot lan la du. */
    private static SSLContext boMa;

    private BaoMat() {
    }

    /**
     * DUNG BO MA TLS TU FILE KHO KHOA.
     *
     * <p><b>Vi sao khong dung {@code System.setProperty} nhu cach thong
     * thuong:</b> cach dat thuoc tinh he thong roi goi
     * {@code SSLSocketFactory.getDefault()} chi chay dung khi tien trinh do
     * CHI lam mot vai tro - hoac chi la server, hoac chi la client. Ly do la
     * {@code getDefault()} <b>ghi nho bo tao socket ngay lan dung dau tien</b>
     * va khong doc lai thuoc tinh he thong nua.
     *
     * <p>Do an nay co mot tien trinh lam ca hai vai tro: server mo cong lang
     * nghe, roi chinh no khoi dong doi thu may - mot CLIENT - trong cung tien
     * trinh. Khi do server da dung SSL truoc, bo tao socket mac dinh da bi
     * ghi nho, nen thuoc tinh {@code trustStore} dat sau khong con tac dung.
     * Bot di ket noi bang kho tin cay mac dinh cua JDK, khong he biet chung
     * chi tu ky cua nhom, va bat tay TLS that bai voi loi:
     *
     * <pre>
     *   PKIX path building failed:
     *   unable to find valid certification path to requested target
     * </pre>
     *
     * <p>Cach lam o day tranh han van de do: tu doc file kho khoa va dung
     * mot {@link SSLContext} rieng, khong phu thuoc vao trang thai toan cuc
     * cua JVM. File kho khoa dong hai vai tro:
     * <ul>
     *   <li><b>Kho khoa</b> (key store) cho phia server: chua khoa rieng,
     *       dung de chung minh "toi dung la server nay".</li>
     *   <li><b>Kho tin cay</b> (trust store) cho phia client: chi lay chung
     *       chi ra, de biet "server nao co chung chi nay thi tin".</li>
     * </ul>
     */
    private static synchronized SSLContext boMa() throws IOException {
        if (boMa != null) {
            return boMa;
        }
        try (InputStream tep = new FileInputStream(KEYSTORE)) {
            KeyStore kho = KeyStore.getInstance(KeyStore.getDefaultType());
            kho.load(tep, MAT_KHAU_KHO.toCharArray());

            KeyManagerFactory khoaRieng = KeyManagerFactory.getInstance(
                    KeyManagerFactory.getDefaultAlgorithm());
            khoaRieng.init(kho, MAT_KHAU_KHO.toCharArray());

            TrustManagerFactory tinCay = TrustManagerFactory.getInstance(
                    TrustManagerFactory.getDefaultAlgorithm());
            tinCay.init(kho);

            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(khoaRieng.getKeyManagers(), tinCay.getTrustManagers(), null);
            boMa = ctx;
            return ctx;

        } catch (GeneralSecurityException e) {
            throw new IOException("Khong doc duoc kho khoa " + KEYSTORE
                    + ": " + e.getMessage(), e);
        }
    }

    public static void batSsl(boolean bat) {
        dungSsl = bat;
    }

    public static boolean dangDungSsl() {
        return dungSsl;
    }

    /** Ten che do de in ra man hinh cho de theo doi. */
    public static String moTaCheDo() {
        return dungSsl ? "CO MA HOA (TLS)" : "KHONG MA HOA";
    }

    /* ------------------------------------------------------------------ */
    /* Phia server                                                         */
    /* ------------------------------------------------------------------ */

    /**
     * Tao socket lang nghe cho server.
     *
     * <p>Che do SSL doc khoa rieng tu file keystore. Khoa nay chung minh
     * server dung la server, va duoc dung de thoa thuan khoa phien voi client.
     */
    public static ServerSocket taoServerSocket(int port) throws IOException {
        if (!dungSsl) {
            return new ServerSocket(port);
        }

        return boMa().getServerSocketFactory().createServerSocket(port);
    }

    /* ------------------------------------------------------------------ */
    /* Phia client                                                         */
    /* ------------------------------------------------------------------ */

    /**
     * Tao socket ket noi toi server, co dat thoi gian cho.
     *
     * <p>Che do SSL lam hai buoc: mo socket thuong truoc de dung duoc thoi
     * gian cho ket noi, roi boc no bang mot lop SSL. Neu goi thang
     * {@code SSLSocketFactory.createSocket(host, port)} thi khong dat duoc
     * thoi gian cho, may khong co server se treo rat lau.
     *
     * <p>Client doc file keystore nhu mot <b>kho tin cay</b> (truststore):
     * no khong lay khoa rieng ra, chi lay chung chi de biet "server nao co
     * chung chi nay thi tin".
     */
    public static Socket taoSocket(String host, int port, int thoiGianChoMs)
            throws IOException {

        Socket socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), thoiGianChoMs);

        if (!dungSsl) {
            return socket;
        }

        SSLSocketFactory factory = boMa().getSocketFactory();
        // Tham so cuoi: dong luon socket goc khi dong socket SSL.
        SSLSocket ssl = (SSLSocket) factory.createSocket(socket, host, port, true);

        // Bat tay TLS ngay bay gio de loi chung chi lo ra o day, chu khong
        // phai lo ra giua chung khi dang doc du lieu.
        ssl.startHandshake();
        return ssl;
    }

    /** Ten bo ma hoa dang dung, de in ra cho biet TLS that su hoat dong. */
    public static String moTaKetNoi(Socket socket) {
        if (socket instanceof SSLSocket ssl) {
            return ssl.getSession().getProtocol() + " / "
                    + ssl.getSession().getCipherSuite();
        }
        return "TCP thuong, khong ma hoa";
    }
}
