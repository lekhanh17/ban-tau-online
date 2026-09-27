package bantau.tools;

import bantau.common.MatchRecord;
import bantau.common.PlayerStats;
import bantau.common.Protocol;
import bantau.server.db.DaoRam;
import bantau.server.db.MatKhau;

import java.time.LocalDateTime;
import java.util.List;

/** Cong cu noi bo: kiem thu lop bam mat khau va DAO bo nho. Khong thuoc bai nop. */
public final class TestDao {

    private static int soLoi = 0;

    private static void kiemTra(String ten, boolean dieuKien) {
        System.out.println((dieuKien ? "  OK   " : "  SAI  ") + ten);
        if (!dieuKien) {
            soLoi++;
        }
    }

    public static void main(String[] args) {
        System.out.println("=== 1. BAM MAT KHAU ===");
        String salt = MatKhau.sinhSalt();
        String hash = MatKhau.bam(salt, "matkhau123");

        kiemTra("salt dai dung 32 ky tu hex", salt.length() == 32 && salt.matches("[0-9a-f]+"));
        kiemTra("hash dai dung 64 ky tu hex", hash.length() == 64 && hash.matches("[0-9a-f]+"));
        kiemTra("dung mat khau thi khop", MatKhau.khop(salt, hash, "matkhau123"));
        kiemTra("sai mat khau thi khong khop", !MatKhau.khop(salt, hash, "matkhau124"));
        kiemTra("mat khau rong thi khong khop", !MatKhau.khop(salt, hash, ""));

        String salt2 = MatKhau.sinhSalt();
        kiemTra("hai lan sinh salt ra hai gia tri khac nhau", !salt.equals(salt2));
        kiemTra("cung mat khau nhung khac salt thi khac hash",
                !MatKhau.bam(salt2, "matkhau123").equals(hash));

        System.out.println();
        System.out.println("=== 2. DANG KY VA DANG NHAP ===");
        DaoRam dao = new DaoRam();

        kiemTra("dang ky lan dau thanh cong", dao.dangKy("khanh", "123456") == null);
        kiemTra("dang ky trung ten bi tu choi",
                Protocol.E_NAME_EXISTS.equals(dao.dangKy("khanh", "khacnhau")));
        kiemTra("dang nhap dung mat khau", dao.kiemTraDangNhap("khanh", "123456") == null);
        kiemTra("dang nhap sai mat khau bao E_WRONG_PASSWORD",
                Protocol.E_WRONG_PASSWORD.equals(dao.kiemTraDangNhap("khanh", "sai")));
        kiemTra("dang nhap tai khoan chua ton tai bao E_NO_ACCOUNT",
                Protocol.E_NO_ACCOUNT.equals(dao.kiemTraDangNhap("nguoila", "123456")));

        System.out.println();
        System.out.println("=== 3. THONG KE VA BANG XEP HANG ===");
        dao.dangKy("nam", "123456");
        dao.dangKy("linh", "123456");

        dao.congThang("khanh");
        dao.congThang("khanh");
        dao.congThua("khanh");
        dao.congThang("nam");
        dao.congThua("linh");
        dao.congThua("linh");

        List<PlayerStats> bxh = dao.bangXepHang(10);
        kiemTra("bang xep hang co du 3 nguoi", bxh.size() == 3);
        kiemTra("nguoi dan dau la khanh (2 thang)",
                bxh.get(0).username().equals("khanh") && bxh.get(0).wins() == 2);
        kiemTra("hang hai la nam (1 thang)", bxh.get(1).username().equals("nam"));
        kiemTra("hang ba la linh (0 thang, 2 thua)",
                bxh.get(2).username().equals("linh") && bxh.get(2).losses() == 2);
        kiemTra("khanh: 2 thang 1 thua = 3 tran", bxh.get(0).soTran() == 3);
        kiemTra("khanh: ty le thang 67%", Math.round(bxh.get(0).tyLeThang()) == 67);
        kiemTra("gioi han top 2 thi chi tra 2 nguoi", dao.bangXepHang(2).size() == 2);

        System.out.println();
        System.out.println("=== 4. LICH SU TRAN DAU ===");
        LocalDateTime t1 = LocalDateTime.of(2026, 9, 25, 14, 0);
        LocalDateTime t2 = LocalDateTime.of(2026, 9, 25, 14, 12);
        dao.luuTran("khanh", "nam", "khanh", Protocol.REASON_ALL_SUNK, 48, t1, t2);
        dao.luuTran("khanh", "linh", "linh", Protocol.REASON_OPPONENT_LEFT, 12,
                t2, t2.plusMinutes(5));

        List<MatchRecord> ls = dao.lichSuCua("khanh", 20);
        kiemTra("khanh co 2 tran trong lich su", ls.size() == 2);
        kiemTra("tran moi nhat len dau", ls.get(0).doiThuCua("khanh").equals("linh"));
        kiemTra("tran voi linh: khanh thua", !ls.get(0).thangBoi("khanh"));
        kiemTra("tran voi nam: khanh thang", ls.get(1).thangBoi("khanh"));
        kiemTra("tran voi nam keo dai 12 phut", ls.get(1).soPhut() == 12);
        kiemTra("tran voi nam co 48 phat ban", ls.get(1).shots() == 48);
        kiemTra("ly do doi thu roi phong hien dung chu",
                ls.get(0).moTaLyDo().equals("doi thu roi phong"));
        kiemTra("linh chi co 1 tran", dao.lichSuCua("linh", 20).size() == 1);
        kiemTra("nguoi chua danh tran nao thi lich su rong",
                dao.lichSuCua("nguoila", 20).isEmpty());

        System.out.println();
        System.out.println(soLoi == 0
                ? "=== TAT CA DEU DUNG ==="
                : "=== CO " + soLoi + " CHO SAI ===");
        System.exit(soLoi == 0 ? 0 : 1);
    }
}
