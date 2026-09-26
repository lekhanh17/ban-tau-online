package bantau.server.db;

import bantau.common.PlayerStats;
import bantau.common.Protocol;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * BAN CAI DAT {@link PlayerDao} LUU THAT VAO MySQL
 *
 * <p><b>Moi cau lenh deu dung {@link PreparedStatement}.</b> Day la diem
 * quan trong ve bao mat: cau lenh duoc gui len MySQL truoc, du lieu nguoi
 * dung gui sau va luon duoc coi la GIA TRI, khong bao gio duoc coi la lenh.
 * Nho vay khong the chen SQL (SQL Injection). Neu noi chuoi kieu
 * {@code "SELECT ... WHERE username = '" + ten + "'"} thi chi can nguoi
 * dung go ten la {@code ' OR '1'='1} la doc duoc ca bang.
 */
public class PlayerDaoMySql implements PlayerDao {

    @Override
    public String dangKy(String username, String matKhau) {
        String salt = MatKhau.sinhSalt();
        String hash = MatKhau.bam(salt, matKhau);

        String sql = "INSERT INTO players (username, password_hash, salt) VALUES (?, ?, ?)";
        try (Connection c = Database.moKetNoi();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, hash);
            ps.setString(3, salt);
            ps.executeUpdate();
            return null;
        } catch (SQLException e) {
            // 1062 = trung khoa UNIQUE, tuc la ten da co nguoi dung.
            if (e.getErrorCode() == 1062) {
                return Protocol.E_NAME_EXISTS;
            }
            System.out.println("[CSDL] Loi khi dang ky: " + e.getMessage());
            return Protocol.E_DB_ERROR;
        }
    }

    @Override
    public String kiemTraDangNhap(String username, String matKhau) {
        String sql = "SELECT password_hash, salt FROM players WHERE username = ?";
        try (Connection c = Database.moKetNoi();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Protocol.E_NO_ACCOUNT;
                }
                String hash = rs.getString("password_hash");
                String salt = rs.getString("salt");
                return MatKhau.khop(salt, hash, matKhau) ? null : Protocol.E_WRONG_PASSWORD;
            }
        } catch (SQLException e) {
            System.out.println("[CSDL] Loi khi dang nhap: " + e.getMessage());
            return Protocol.E_DB_ERROR;
        }
    }

    @Override
    public void congThang(String username) {
        congMot("wins", username);
    }

    @Override
    public void congThua(String username) {
        congMot("losses", username);
    }

    /**
     * Cong mot vao cot thang hoac thua.
     *
     * <p>Ten cot duoc ghep thang vao chuoi SQL, nhung an toan vi no chi
     * nhan dung hai gia tri co dinh do chinh lop nay truyen vao, khong he
     * lay tu nguoi dung. Ten nguoi choi thi van di qua tham so ?.
     */
    private void congMot(String cot, String username) {
        String sql = "UPDATE players SET " + cot + " = " + cot + " + 1 WHERE username = ?";
        try (Connection c = Database.moKetNoi();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("[CSDL] Loi khi cap nhat " + cot + ": " + e.getMessage());
        }
    }

    @Override
    public List<PlayerStats> bangXepHang(int soNguoi) {
        List<PlayerStats> ds = new ArrayList<>();
        String sql = "SELECT username, wins, losses FROM players "
                + "ORDER BY wins DESC, losses ASC, username ASC LIMIT ?";
        try (Connection c = Database.moKetNoi();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, soNguoi);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ds.add(new PlayerStats(
                            rs.getString("username"),
                            rs.getInt("wins"),
                            rs.getInt("losses")));
                }
            }
        } catch (SQLException e) {
            System.out.println("[CSDL] Loi khi lay bang xep hang: " + e.getMessage());
        }
        return ds;
    }
}
