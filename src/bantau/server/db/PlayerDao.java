package bantau.server.db;

import bantau.common.PlayerStats;

import java.util.List;

/**
 * TRUY CAP DU LIEU TAI KHOAN NGUOI CHOI
 *
 * <p>Day la mot interface chu khong phai lop cu the, vi du an co hai cach
 * luu tru:
 * <ul>
 *   <li>{@link PlayerDaoMySql} - luu that vao CSDL MySQL.</li>
 *   <li>{@link DaoRam} - luu tam trong bo nho, dung khi chua bat XAMPP.</li>
 * </ul>
 *
 * <p>Phan con lai cua server chi lam viec qua interface nay nen khong he
 * biet dang dung cach nao. Doi cach luu tru khong phai sua mot dong nao
 * trong {@code ClientHandler}.
 */
public interface PlayerDao {

    /**
     * Tao tai khoan moi.
     *
     * @return null neu thanh cong, nguoc lai la ma loi trong {@code Protocol}
     */
    String dangKy(String username, String matKhau);

    /**
     * Kiem tra ten va mat khau khi dang nhap.
     *
     * @return null neu dung, nguoc lai la ma loi trong {@code Protocol}
     */
    String kiemTraDangNhap(String username, String matKhau);

    /** Cong mot tran thang cho nguoi choi. */
    void congThang(String username);

    /** Cong mot tran thua cho nguoi choi. */
    void congThua(String username);

    /** Lay bang xep hang: sap theo so tran thang giam dan. */
    List<PlayerStats> bangXepHang(int soNguoi);
}
