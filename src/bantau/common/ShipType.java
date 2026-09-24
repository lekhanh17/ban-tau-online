package bantau.common;

/**
 * Nam loai tau theo luat co dien cua game ban tau.
 *
 * <p>Tong cong 5 tau chiem 17 o tren ban do 10x10.
 */
public enum ShipType {

    CARRIER("Tau san bay", 5, 'C'),
    BATTLESHIP("Thiet giap ham", 4, 'B'),
    CRUISER("Tuan duong ham", 3, 'R'),
    SUBMARINE("Tau ngam", 3, 'S'),
    DESTROYER("Tau khu truc", 2, 'D');

    private final String label;
    private final int size;
    private final char code;

    ShipType(String label, int size, char code) {
        this.label = label;
        this.size = size;
        this.code = code;
    }

    public String label() {
        return label;
    }

    /** So o ma tau chiem. */
    public int size() {
        return size;
    }

    /** Ky tu dai dien khi ve ban do bang chu. */
    public char code() {
        return code;
    }

    public static ShipType fromCode(char c) {
        for (ShipType t : values()) {
            if (t.code == Character.toUpperCase(c)) {
                return t;
            }
        }
        return null;
    }

    /** Tong so o ma ca 5 tau chiem = 17. Dung de kiem tra dieu kien thang. */
    public static int totalCells() {
        int sum = 0;
        for (ShipType t : values()) {
            sum += t.size;
        }
        return sum;
    }
}