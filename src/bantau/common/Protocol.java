package bantau.common;

/**
 * GIAI DOAN 3 - HANG SO CUA GIAO THUC
 *
 * <p>Toan bo ten lenh va ma loi duoc khai bao tap trung tai day.
 *
 * <p>Vi sao khong go thang chuoi "LOGIN" vao code? Vi go sai mot ky tu
 * (vi du "LOGIIN") thi chuong trinh van bien dich binh thuong, chi khi chay
 * moi phat hien - va rat kho tim. Dung hang so thi go sai la bao loi ngay.
 *
 * <p>Khuon dang ban tin:  LENH|thamso1|thamso2|...\n
 */
public final class Protocol {

    /** Lop chi chua hang so, khong cho tao doi tuong. */
    private Protocol() {
    }

    /* ===== Tham so chung ===== */

    public static final int DEFAULT_PORT = 5000;
    public static final String DEFAULT_HOST = "127.0.0.1";

    /** Ky tu ngan cach cac truong trong mot ban tin. */
    public static final String SEP = "|";
    /** Dang bieu thuc chinh quy cua SEP. Ky tu | co y nghia dac biet nen phai co \\ */
    public static final String SEP_REGEX = "\\|";

    /* ===== Lenh Client gui len Server ===== */

    /** LOGIN|<ten> - dang nhap voi ten tu chon. */
    public static final String C_LOGIN = "LOGIN";
    /** CHAT|<noi dung> - gui tin nhan cho tat ca. */
    public static final String C_CHAT = "CHAT";
    /** WHO - hoi danh sach nguoi dang online. */
    public static final String C_WHO = "WHO";
    /** QUIT - thoat mot cach lich su. */
    public static final String C_QUIT = "QUIT";

    /* ===== Lenh Server gui xuong Client ===== */

    /** LOGIN_OK|<ten> - dang nhap thanh cong. */
    public static final String S_LOGIN_OK = "LOGIN_OK";
    /** ERROR|<ma loi>|<mo ta> - tu choi yeu cau. */
    public static final String S_ERROR = "ERROR";
    /** CHAT|<nguoi gui>|<noi dung> - tin nhan tu mot nguoi choi. */
    public static final String S_CHAT = "CHAT";
    /** SYSTEM|<noi dung> - thong bao cua he thong. */
    public static final String S_SYSTEM = "SYSTEM";
    /** WHO|<ten1,ten2,...> - danh sach nguoi online. */
    public static final String S_WHO = "WHO";

    /* ===== Ma loi ===== */

    public static final String E_NAME_TAKEN = "E_NAME_TAKEN";
    public static final String E_NAME_INVALID = "E_NAME_INVALID";
    public static final String E_NOT_LOGGED_IN = "E_NOT_LOGGED_IN";
    public static final String E_BAD_STATE = "E_BAD_STATE";
    public static final String E_UNKNOWN_CMD = "E_UNKNOWN_CMD";

    /* ===== Rang buoc ten nguoi choi ===== */

    public static final int NAME_MIN = 3;
    public static final int NAME_MAX = 16;
    /** Chi cho phep chu cai, chu so va dau gach duoi. */
    public static final String NAME_PATTERN = "^[A-Za-z0-9_]{" + NAME_MIN + "," + NAME_MAX + "}$";
}