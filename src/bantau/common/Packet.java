package bantau.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * MOT BAN TIN TRAO DOI QUA MANG
 *
 * <p>Lop nay cai dat {@link Serializable} nen Java tu dong biet cach chuyen
 * doi tuong thanh dai byte de gui di va dung lai o dau kia. Khong con phai
 * tu noi chuoi va tu tach chuoi nhu truoc.
 *
 * <p><b>Loi ich so voi ban tin dang text:</b> khong con van de ky tu dac biet.
 * Nguoi choi go tin nhan chua dau gach dung hay ky tu xuong dong deu binh
 * thuong, vi moi truong la mot doi tuong String rieng biet chu khong phai
 * mot chuoi dai bi cat theo ky tu phan tach.
 *
 * <p><b>serialVersionUID</b> la so hieu phien ban cua lop. Bat buoc phai khai
 * bao tuong minh: neu khong, Java tu sinh ra tu cau truc lop, va chi can them
 * mot truong la so nay doi - luc do server va client se bao
 * InvalidClassException du code y het nhau.
 */
public final class Packet implements Serializable {

    private static final long serialVersionUID = 1L;

    private final PacketType type;
    private final List<String> args;

    private Packet(PacketType type, List<String> args) {
        this.type = type;
        this.args = args;
    }

    /** Tao mot ban tin. Vi du: Packet.of(PacketType.LOGIN, "khanh") */
    public static Packet of(PacketType type, String... args) {
        List<String> list = new ArrayList<>(Arrays.asList(args));
        return new Packet(type, Collections.unmodifiableList(list));
    }

    public PacketType type() {
        return type;
    }

    public int argCount() {
        return args.size();
    }

    /** Lay tham so thu i. Tra ve chuoi rong neu khong co - tranh loi mang. */
    public String arg(int i) {
        return i >= 0 && i < args.size() ? args.get(i) : "";
    }

    @Override
    public String toString() {
        return args.isEmpty() ? type.name() : type + " " + args;
    }
}