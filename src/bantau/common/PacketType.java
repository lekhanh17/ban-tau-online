package bantau.common;

/**
 * Cac loai ban tin trao doi giua client va server.
 *
 * <p>Dung enum thay cho chuoi "LOGIN", "CHAT" co hai cai loi lon:
 * <ul>
 *   <li>Go sai ten la bao loi ngay luc bien dich, khong phai doi toi luc chay.</li>
 *   <li>Khong the tao ra mot loai ban tin khong ton tai - Java khong cho.</li>
 * </ul>
 *
 * <p>Enum cua Java mac dinh da Serializable nen gui qua mang duoc ngay.
 */
public enum PacketType {

    /* ===== Client gui len Server ===== */

    /** LOGIN - args[0] = ten nguoi choi. */
    LOGIN,
    /** CHAT - args[0] = noi dung tin nhan. */
    CHAT,
    /** WHO - khong co tham so, hoi danh sach nguoi online. */
    WHO,
    /** QUIT - khong co tham so, thoat lich su. */
    QUIT,

    /* ===== Server gui xuong Client ===== */

    /** LOGIN_OK - args[0] = ten da duoc chap nhan. */
    LOGIN_OK,
    /** ERROR - args[0] = ma loi, args[1] = mo ta. */
    ERROR,
    /** CHAT_MSG - args[0] = nguoi gui, args[1] = noi dung. */
    CHAT_MSG,
    /** SYSTEM - args[0] = thong bao cua he thong. */
    SYSTEM,
    /** WHO_LIST - args[0] = danh sach ten, ngan cach bang dau phay. */
    WHO_LIST
}