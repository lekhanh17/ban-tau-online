package bantau.common;

/**
 * Cac tham so va rang buoc dung chung cho server va client.
 *
 * <p>Ten lenh da chuyen sang enum {@link PacketType}, lop nay chi con giu
 * tham so ket noi, ma loi va quy tac dat ten.
 */
public final class Protocol {

    private Protocol() {
    }

    /* ===== Tham so ket noi ===== */

    public static final int DEFAULT_PORT = 5000;
    public static final String DEFAULT_HOST = "127.0.0.1";

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