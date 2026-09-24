package bantau.common;

/**
 * Cac loai ban tin trao doi giua client va server.
 *
 * <p>Dung enum thay cho chuoi co hai cai loi lon:
 * <ul>
 *   <li>Go sai ten la bao loi ngay luc bien dich.</li>
 *   <li>Khong the tao ra mot loai ban tin khong ton tai.</li>
 * </ul>
 */
public enum PacketType {

    /* ===== Client gui len Server ===== */

    /** args[0] = ten nguoi choi. */
    LOGIN,
    /** args[0] = noi dung tin nhan. */
    CHAT,
    /** Khong tham so. Hoi danh sach nguoi online. */
    WHO,
    /** Khong tham so. Thoat lich su. */
    QUIT,

    /** Khong tham so. Hoi danh sach phong. */
    ROOM_LIST,
    /** args[0] = ten phong muon tao. */
    ROOM_CREATE,
    /** args[0] = id phong muon vao. */
    ROOM_JOIN,
    /** Khong tham so. Roi phong hien tai. */
    ROOM_LEAVE,

    /* ===== Server gui xuong Client ===== */

    /** args[0] = ten da duoc chap nhan. */
    LOGIN_OK,
    /** args[0] = ma loi, args[1] = mo ta. */
    ERROR,
    /** args[0] = nguoi gui, args[1] = noi dung. */
    CHAT_MSG,
    /** args[0] = thong bao he thong. */
    SYSTEM,
    /** args[0] = danh sach ten, ngan cach bang dau phay. */
    WHO_LIST,

    /** payload = ArrayList&lt;RoomInfo&gt;. Danh sach phong hien co. */
    ROOM_LIST_DATA,
    /** payload = RoomInfo cua phong vua vao, args[0] = "1" neu la chu phong. */
    ROOM_JOINED,
    /** payload = RoomInfo, args[0] = ten nguoi 1, args[1] = ten nguoi 2. */
    ROOM_STATE,
    /** Khong tham so. Xac nhan da roi phong. */
    ROOM_LEFT
}