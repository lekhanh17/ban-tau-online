package bantau.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * MOT BAN TIN TRAO DOI QUA MANG
 *
 * <p>Gom ba phan:
 * <ul>
 *   <li>{@code type} - loai ban tin, xem {@link PacketType}</li>
 *   <li>{@code args} - cac tham so dang chuoi (ten, ma loi, mo ta...)</li>
 *   <li>{@code payload} - DU LIEU KEM THEO dang doi tuong, co the null</li>
 * </ul>
 *
 * <p>Truong {@code payload} la phan the hien ro nhat suc manh cua Java
 * Serialization: server gui thang mot {@code ArrayList<RoomInfo>} sang client,
 * khong phai ma hoa thanh chuoi roi ben kia tu tach ra. Sau nay se dung de
 * gui ca trang thai ban co.
 *
 * <p>serialVersionUID bat buoc phai khai bao tuong minh. Neu de Java tu sinh,
 * chi can them mot truong la so nay doi, va server voi client se bao
 * InvalidClassException du code nhin y het nhau.
 */
public final class Packet implements Serializable {

    private static final long serialVersionUID = 2L;

    private final PacketType type;
    private final ArrayList<String> args;
    private final Serializable payload;

    private Packet(PacketType type, ArrayList<String> args, Serializable payload) {
        this.type = type;
        this.args = args;
        this.payload = payload;
    }

    /** Tao ban tin chi co tham so chuoi. */
    public static Packet of(PacketType type, String... args) {
        return new Packet(type, new ArrayList<>(Arrays.asList(args)), null);
    }

    /** Tao ban tin co kem doi tuong du lieu. */
    public static Packet withPayload(PacketType type, Serializable payload, String... args) {
        return new Packet(type, new ArrayList<>(Arrays.asList(args)), payload);
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

    /** Lay tham so thu i duoi dang so nguyen. Tra ve def neu khong hop le. */
    public int intArg(int i, int def) {
        try {
            return Integer.parseInt(arg(i).trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public List<String> args() {
        return Collections.unmodifiableList(args);
    }

    public Serializable payload() {
        return payload;
    }

    /**
     * Lay payload va ep kieu an toan.
     *
     * <p>Neu ben kia gui sai kieu thi tra ve null thay vi nem
     * ClassCastException - chuong trinh khong sap.
     */
    public <T> T payload(Class<T> cls) {
        return cls.isInstance(payload) ? cls.cast(payload) : null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(type.name());
        if (!args.isEmpty()) {
            sb.append(' ').append(args);
        }
        if (payload != null) {
            sb.append(" +payload(").append(payload.getClass().getSimpleName()).append(')');
        }
        return sb.toString();
    }
}