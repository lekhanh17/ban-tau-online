package bantau.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * GIAI DOAN 3 - MOT BAN TIN CUA GIAO THUC
 *
 * <p>Chiu trach nhiem chuyen doi qua lai giua:
 * <ul>
 *   <li>Chuoi text di tren mang:  "CHAT|xin chao"</li>
 *   <li>Doi tuong trong chuong trinh:  lenh = "CHAT", tham so[0] = "xin chao"</li>
 * </ul>
 *
 * <p>Ca server va client deu dung chung lop nay, nho vay hai ben chac chan
 * hieu nhau - khong ai tu phan tich chuoi theo cach rieng.
 */
public final class Message {

    private final String command;
    private final List<String> args;

    private Message(String command, List<String> args) {
        this.command = command;
        this.args = args;
    }

    /**
     * GIAI MA mot dong nhan duoc tu socket.
     *
     * <p>Vi du: "CHAT|xin chao|ban" -> lenh "CHAT", tham so ["xin chao", "ban"]
     *
     * @return null neu dong rong
     */
    public static Message parse(String line) {
        if (line == null) {
            return null;
        }
        String trimmed = line.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        // Tham so -1 bao split giu lai ca cac truong rong o cuoi.
        // Khong co -1 thi "LOGIN|" se bi cat thanh mang 1 phan tu, mat truong rong.
        String[] parts = trimmed.split(Protocol.SEP_REGEX, -1);

        List<String> args = new ArrayList<>(
                Arrays.asList(parts).subList(1, parts.length));

        // Ten lenh luon viet HOA de nguoi dung go "login" hay "LOGIN" deu duoc.
        return new Message(parts[0].toUpperCase(), args);
    }

    /**
     * MA HOA mot ban tin de gui di.
     *
     * <p>Vi du: build("CHAT", "khanh", "xin chao") -> "CHAT|khanh|xin chao"
     */
    public static String build(String command, Object... args) {
        StringBuilder sb = new StringBuilder(command);
        for (Object a : args) {
            sb.append(Protocol.SEP).append(sanitize(String.valueOf(a)));
        }
        return sb.toString();
    }

    /**
     * LAM SACH du lieu do nguoi dung nhap vao.
     *
     * <p>Rat quan trong: neu nguoi choi go ten la "kha|nh" thi ban tin
     * "LOGIN|kha|nh" se bi tach thanh 3 truong, pha vo khuon dang.
     * Tuong tu, ky tu xuong dong se bi hieu nham la ket thuc ban tin.
     */
    public static String sanitize(String s) {
        if (s == null) {
            return "";
        }
        return s.replace('|', '/')
                .replace('\n', ' ')
                .replace('\r', ' ')
                .trim();
    }

    public String command() {
        return command;
    }

    public int argCount() {
        return args.size();
    }

    /** Lay tham so thu i. Tra ve chuoi rong neu khong co - tranh loi mang. */
    public String arg(int i) {
        return i >= 0 && i < args.size() ? args.get(i) : "";
    }

    /**
     * Ghep cac tham so tu vi tri i tro di lai thanh mot chuoi.
     *
     * <p>Dung cho lenh CHAT: neu nguoi dung go tin nhan co chua ky tu |
     * (da bi doi thanh /) thi van lay duoc du noi dung.
     */
    public String tail(int i) {
        if (i >= args.size()) {
            return "";
        }
        return String.join(Protocol.SEP, args.subList(i, args.size()));
    }

    @Override
    public String toString() {
        return args.isEmpty()
                ? command
                : command + Protocol.SEP + String.join(Protocol.SEP, args);
    }
}