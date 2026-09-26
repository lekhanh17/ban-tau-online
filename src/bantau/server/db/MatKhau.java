package bantau.server.db;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * BAM MAT KHAU - SHA-256 KEM SALT
 *
 * <p>CSDL khong bao gio luu mat khau tho. Moi tai khoan co mot chuoi
 * {@code salt} ngau nhien rieng, va thu duoc luu la
 * {@code SHA-256(salt + mat khau)}.
 *
 * <p><b>Vi sao phai co salt:</b> neu chi bam mat khau khong, thi hai nguoi
 * dat cung mat khau "123456" se ra cung mot chuoi bam. Ke tan cong lay duoc
 * bang players chi viec tra bang co san (rainbow table) la ra mat khau.
 * Them salt rieng cho tung nguoi thi moi tai khoan ra mot chuoi bam khac
 * nhau, bang tra san vo dung.
 *
 * <p><b>Han che can noi ro khi bao ve:</b> SHA-256 chay rat nhanh nen neu
 * lo CSDL, may manh van co the do mat khau ngan bang vet can. He thong
 * that nen dung ham bam cham chuyen dung cho mat khau nhu bcrypt, scrypt
 * hoac PBKDF2. Do an nay dung SHA-256 + salt vi chi can thu vien san co
 * trong JDK, khong phai them thu vien ngoai.
 */
public final class MatKhau {

    private static final SecureRandom RND = new SecureRandom();

    private MatKhau() {
    }

    /** Sinh salt ngau nhien 16 byte, tra ve duoi dang 32 ky tu hex. */
    public static String sinhSalt() {
        byte[] b = new byte[16];
        RND.nextBytes(b);
        return sangHex(b);
    }

    /** Tinh SHA-256(salt + mat khau), tra ve 64 ky tu hex. */
    public static String bam(String salt, String matKhau) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt.getBytes(StandardCharsets.UTF_8));
            md.update(matKhau.getBytes(StandardCharsets.UTF_8));
            return sangHex(md.digest());
        } catch (NoSuchAlgorithmException e) {
            // Moi ban JDK deu co SHA-256, nhanh nay khong bao gio chay.
            throw new IllegalStateException("JDK khong co SHA-256", e);
        }
    }

    /** Mat khau nguoi dung vua nhap co khop voi thu da luu trong CSDL khong. */
    public static boolean khop(String salt, String hashDaLuu, String matKhauNhap) {
        if (salt == null || hashDaLuu == null || matKhauNhap == null) {
            return false;
        }
        return soSanhAnToan(hashDaLuu, bam(salt, matKhauNhap));
    }

    /**
     * So sanh hai chuoi trong thoi gian khong doi.
     *
     * <p>Phep so sanh {@code equals()} thong thuong dung ngay khi gap ky tu
     * dau tien khac nhau. Ke tan cong do thoi gian phan hoi co the doan dan
     * tung ky tu cua chuoi bam. Ham nay luon duyet het ca hai chuoi nen
     * thoi gian chay khong tiet lo gi.
     */
    private static boolean soSanhAnToan(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int khac = 0;
        for (int i = 0; i < a.length(); i++) {
            khac |= a.charAt(i) ^ b.charAt(i);
        }
        return khac == 0;
    }

    private static String sangHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }
}
