package bantau.common;

/**
 * Cac loai ban tin trao doi giua client va server.
 */
public enum PacketType {

    /* ===== Client gui len Server ===== */

    /** args[0] = ten nguoi choi, args[1] = mat khau. */
    LOGIN,
    /** args[0] = ten nguoi choi, args[1] = mat khau. Tao tai khoan moi. */
    REGISTER,
    /** Khong tham so. Xin bang xep hang. */
    RANK_LIST,
    /** Khong tham so. Xin lich su dau cua chinh minh. */
    HISTORY_LIST,
    /** args[0] = noi dung tin nhan. */
    CHAT,
    /** Khong tham so. */
    WHO,
    /** Khong tham so. */
    QUIT,

    /** Khong tham so. */
    ROOM_LIST,
    /** args[0] = ten phong. */
    ROOM_CREATE,
    /** args[0] = id phong. */
    ROOM_JOIN,
    /** Khong tham so. */
    ROOM_LEAVE,

    /** payload = Board da dat du 5 tau. Bao san sang vao tran. */
    READY,
    /** args[0] = x, args[1] = y. Ban vao ban do doi thu. */
    FIRE,

    /* ===== Server gui xuong Client ===== */

    /** args[0] = ten. */
    LOGIN_OK,
    /** args[0] = ten. Tao tai khoan thanh cong, moi dang nhap. */
    REGISTER_OK,
    /** payload = ArrayList&lt;PlayerStats&gt;. Bang xep hang. */
    RANK_DATA,
    /** payload = ArrayList&lt;MatchRecord&gt;. Lich su dau cua nguoi xin. */
    HISTORY_DATA,
    /** args[0] = ma loi, args[1] = mo ta. */
    ERROR,
    /** args[0] = nguoi gui, args[1] = noi dung. */
    CHAT_MSG,
    /** args[0] = thong bao. */
    SYSTEM,
    /** args[0] = danh sach ten. */
    WHO_LIST,

    /** payload = ArrayList&lt;RoomInfo&gt;. */
    ROOM_LIST_DATA,
    /** payload = RoomInfo, args[0] = "1" neu la chu phong. */
    ROOM_JOINED,
    /** payload = RoomInfo, args[0] = ten nguoi 1, args[1] = ten nguoi 2. */
    ROOM_STATE,
    /** Khong tham so. */
    ROOM_LEFT,

    /** Khong tham so. Du 2 nguoi, bat dau dat tau. */
    PLACE_PHASE,
    /** Khong tham so. So do dat tau hop le, dang cho doi thu. */
    READY_OK,
    /** args[0] = ten nguoi di truoc. */
    GAME_START,
    /** args[0] = ten nguoi dang danh luot. */
    TURN,
    /** args[0]=x, args[1]=y, args[2]=ket qua, args[3]=ma tau neu CHIM. Ket qua phat ban CUA MINH. */
    FIRE_RESULT,
    /** Nhu tren nhung la phat ban cua DOI THU vao minh. */
    INCOMING,
    /** args[0] = WIN hoac LOSE, args[1] = ly do. */
    GAME_OVER
}
