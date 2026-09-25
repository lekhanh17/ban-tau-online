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

    /* ===== Gioi han phong ===== */

    public static final int MAX_PLAYERS_PER_ROOM = 2;
    public static final int ROOM_NAME_MAX = 30;

    /* ===== Luat choi ===== */

    /**
     * Ban trung thi duoc ban tiep.
     * Doi thanh false neu muon luan phien tuyet doi moi phat mot luot.
     */
    public static final boolean HIT_GRANTS_EXTRA_TURN = true;

    /* ===== Ma loi ===== */

    public static final String E_NAME_TAKEN = "E_NAME_TAKEN";
    public static final String E_NAME_INVALID = "E_NAME_INVALID";
    public static final String E_NOT_LOGGED_IN = "E_NOT_LOGGED_IN";
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

    /* ===== Rang buoc ten nguoi choi ===== */

    public static final int NAME_MIN = 3;
    public static final int NAME_MAX = 16;
    public static final String NAME_PATTERN = "^[A-Za-z0-9_]{" + NAME_MIN + "," + NAME_MAX + "}$";
}