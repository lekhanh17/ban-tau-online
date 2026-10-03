package bantau.common;

/**
 * Cac tham so va rang buoc dung chung cho server va client.
 */
public final class Protocol {

    private Protocol() {
    }

    /* ===== Tham so ket noi ===== */

    public static final int DEFAULT_PORT = 5000;
    public static final String DEFAULT_HOST = "127.0.0.1";

    /* ===== Phat hien client chet (PING/PONG) ===== */

    /**
     * Bao nhieu giay server gui PING mot lan.
     *
     * <p><b>Vi sao can PING:</b> TCP khong bao ngay khi ben kia bien mat.
     * Nguoi choi dong chuong trinh dang hoang thi socket duoc dong tu te va
     * server biet lien. Nhung neu ho RUT DAY MANG, tat Wi-Fi hay may sap
     * nguon thi khong ai gui goi tin dong ket noi ca - server cu ngoi cho
     * mai, phong ket lai voi mot nguoi da chet.
     *
     * <p>Cach xu ly: server dinh ky hoi "con song khong", client nao khong
     * tra loi trong {@link #TIMEOUT_SECONDS} giay thi coi nhu da chet.
     */
    public static final int PING_INTERVAL_SECONDS = 10;

    /** Khong nghe thay gi tu client qua ngan nay giay thi coi nhu da chet. */
    public static final int TIMEOUT_SECONDS = 30;

    /**
     * THOI GIAN AN HAN DE VAO LAI VAN DANG DO.
     *
     * <p>Khi mot nguoi dang danh bi mat ket noi, server KHONG xu thua ngay.
     * No giu nguyen phong va toan bo ban do trong ngan nay giay, cho nguoi
     * do dang nhap lai. Qua han ma khong thay thi nguoi con lai moi duoc
     * xu thang.
     *
     * <p><b>Vi sao can:</b> Wi-Fi chap chon, rut day mang, may ngu dong hay
     * chuong trinh bi tat dot ngot la chuyen thuong. Xu thua ngay lap tuc
     * vi mot su co mang vai giay la trai nghiem rat te, va cung khong dung
     * tinh than cua mot he thong mang chiu loi.
     *
     * <p>Thoi gian nay phai LON HON {@link #TIMEOUT_SECONDS}, vi server chi
     * biet client da chet sau khi het thoi gian cho PONG.
     */
    public static final int RECONNECT_SECONDS = 60;

    /* ===== Gioi han phong ===== */

    public static final int MAX_PLAYERS_PER_ROOM = 2;
    public static final int ROOM_NAME_MAX = 30;

    /* ===== Luat choi ===== */

    /**
     * Ban trung thi duoc ban tiep.
     * Doi thanh false neu muon luan phien tuyet doi moi phat mot luot.
     */
    public static final boolean HIT_GRANTS_EXTRA_TURN = true;

    /**
     * So giay suy nghi cho moi luot. Qua ngan nay ma chua ban thi mat luot.
     *
     * <p><b>Dem gio o dau moi dung:</b> dong ho phai chay o SERVER. Neu de
     * client tu dem roi tu bao "toi het gio" thi nguoi choi chi can sua
     * client la ngoi bao lau cung duoc. Client chi HIEN THI dong ho cho
     * nguoi choi nhin, con quyet dinh mat luot hay khong la cua server.
     */
    public static final int TURN_SECONDS = 30;

    /* ===== Doi thu may ===== */

    /**
     * Tien to ten tai khoan danh rieng cho bot.
     *
     * <p>Nguoi that KHONG duoc dang ky ten bat dau bang chuoi nay. Nho vay
     * nhin ten la biet ngay day co phai doi thu may khong, va he thong co
     * cho de doi xu khac di - cu the la khong ghi tran dau voi may vao thanh
     * tich.
     */
    public static final String BOT_PREFIX = "May_";

    /**
     * Bot cho bao nhieu mili giay truoc moi phat ban.
     *
     * <p>Khong cho thi bot ban het ban do trong tich tac, nguoi choi khong
     * kip nhin thay gi. Cung khong nen cho qua lau vi con dong ho
     * {@link #TURN_SECONDS} giay moi luot.
     */
    public static final int BOT_DELAY_MS = 900;

    /* ===== Ma loi ===== */

    public static final String E_NAME_TAKEN = "E_NAME_TAKEN";
    public static final String E_NAME_INVALID = "E_NAME_INVALID";
    public static final String E_NAME_EXISTS = "E_NAME_EXISTS";
    public static final String E_WRONG_PASSWORD = "E_WRONG_PASSWORD";
    public static final String E_PASS_INVALID = "E_PASS_INVALID";
    public static final String E_NO_ACCOUNT = "E_NO_ACCOUNT";
    public static final String E_DB_ERROR = "E_DB_ERROR";
    public static final String E_NOT_LOGGED_IN = "E_NOT_LOGGED_IN";
    /** Ten bi he thong giu rieng, nguoi that khong duoc dung. */
    public static final String E_NAME_RESERVED = "E_NAME_RESERVED";
    /** Khong khoi dong duoc doi thu may. */
    public static final String E_BOT_FAILED = "E_BOT_FAILED";
    public static final String E_BAD_STATE = "E_BAD_STATE";
    public static final String E_UNKNOWN_CMD = "E_UNKNOWN_CMD";

    public static final String E_ROOM_NOT_FOUND = "E_ROOM_NOT_FOUND";
    public static final String E_ROOM_FULL = "E_ROOM_FULL";
    public static final String E_ALREADY_IN_ROOM = "E_ALREADY_IN_ROOM";
    public static final String E_NOT_IN_ROOM = "E_NOT_IN_ROOM";

    public static final String E_BAD_PLACEMENT = "E_BAD_PLACEMENT";
    public static final String E_NOT_YOUR_TURN = "E_NOT_YOUR_TURN";
    public static final String E_ALREADY_SHOT = "E_ALREADY_SHOT";

    /* ===== Ket qua va ly do ket thuc van ===== */

    public static final String RESULT_WIN = "WIN";
    public static final String RESULT_LOSE = "LOSE";
    public static final String REASON_ALL_SUNK = "ALL_SUNK";
    public static final String REASON_OPPONENT_LEFT = "OPPONENT_LEFT";
    /** Doi thu mat ket noi va khong vao lai kip trong thoi gian an han. */
    public static final String REASON_OPPONENT_LOST = "OPPONENT_LOST";

    /* ===== Rang buoc ten nguoi choi ===== */

    public static final int NAME_MIN = 3;
    public static final int NAME_MAX = 16;
    public static final String NAME_PATTERN = "^[A-Za-z0-9_]{" + NAME_MIN + "," + NAME_MAX + "}$";

    /* ===== Rang buoc mat khau ===== */

    public static final int PASS_MIN = 4;
    public static final int PASS_MAX = 32;

    /** So nguoi hien trong bang xep hang. */
    public static final int RANK_TOP = 10;
    /** So tran gan nhat hien trong lich su dau. */
    public static final int HISTORY_LIMIT = 20;
}
