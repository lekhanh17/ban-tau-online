package bantau.server.db;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * QUAN LY KET NOI TOI CSDL MySQL / MariaDB
 *
 * <p>Doc thong so ket noi tu file {@code db.properties} o thu muc goc du an,
 * roi thu ket noi mot lan luc khoi dong server.
 *
 * <p><b>Khong co CSDL thi server van chay.</b> Neu thieu file cau hinh,
 * thieu driver, hoac XAMPP chua bat MySQL, {@link #khaDung()} tra ve false
 * va server tu chuyen sang luu tai khoan tam trong bo nho. Buoi demo nho
 * vay khong bao gio chet vi loi CSDL.
 *
 * <p><b>Ve cach quan ly ket noi:</b> moi thao tac CSDL mo mot
 * {@link Connection} moi roi dong ngay bang try-with-resources. Cach nay
 * don gian va an toan khi nhieu thread ClientHandler cung truy cap, vi
 * moi thread co ket noi rieng, khong dung chung. Nhuoc diem la moi lan mo
 * ket noi deu ton thoi gian bat tay voi MySQL. He thong that se dung be
 * ket noi (connection pool) nhu HikariCP de tai su dung ket noi co san -
 * day la huong phat trien nen neu trong bao cao.
 */
public final class Database {

    private static final String FILE_CAU_HINH = "db.properties";

    private static String url;
    private static String user;
    private static String password;
    private static boolean khaDung;

    private Database() {
    }

    /**
     * Doc cau hinh va thu ket noi mot lan. Goi khi server khoi dong.
     *
     * @return true neu ket noi duoc CSDL
     */
    public static boolean khoiTao() {
        Properties p = new Properties();
        try (InputStream in = new FileInputStream(FILE_CAU_HINH)) {
            p.load(in);
        } catch (IOException e) {
            System.out.println("[CSDL] Khong doc duoc " + FILE_CAU_HINH
                    + " (" + e.getMessage() + ")");
            khaDung = false;
            return false;
        }

        url = p.getProperty("db.url", "");
        user = p.getProperty("db.user", "root");
        password = p.getProperty("db.password", "");

        if (url.isBlank()) {
            System.out.println("[CSDL] Thieu db.url trong " + FILE_CAU_HINH);
            khaDung = false;
            return false;
        }

        // Thu ket noi that mot lan de biet chac CSDL dung duoc.
        try (Connection c = DriverManager.getConnection(url, user, password)) {
            khaDung = c != null && !c.isClosed();
            System.out.println("[CSDL] Ket noi thanh cong toi " + rutGonUrl());
            return khaDung;
        } catch (SQLException e) {
            System.out.println("[CSDL] Khong ket noi duoc: " + e.getMessage());
            System.out.println("[CSDL] Kiem tra: da bat MySQL trong XAMPP chua,"
                    + " da chay file sql/bantau_mysql.sql chua,"
                    + " da co mysql-connector-j.jar trong lib chua.");
            khaDung = false;
            return false;
        }
    }

    /** Co dung duoc CSDL khong. Server hoi truoc khi chon DAO nao. */
    public static boolean khaDung() {
        return khaDung;
    }

    /**
     * Mo mot ket noi moi. Nguoi goi BAT BUOC dong lai,
     * tot nhat la dung try-with-resources.
     */
    public static Connection moKetNoi() throws SQLException {
        if (!khaDung) {
            throw new SQLException("CSDL khong kha dung");
        }
        return DriverManager.getConnection(url, user, password);
    }

    /** Cat bo phan tham so phia sau dau ? cho de doc khi in ra man hinh. */
    private static String rutGonUrl() {
        int i = url.indexOf('?');
        return i < 0 ? url : url.substring(0, i);
    }
}
