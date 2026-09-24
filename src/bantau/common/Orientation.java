package bantau.common;

/** Huong dat tau tren ban do. */
public enum Orientation {

    /** Nam ngang - tau keo dai theo truc x (sang phai). */
    HORIZONTAL('H'),
    /** Nam doc - tau keo dai theo truc y (xuong duoi). */
    VERTICAL('V');

    private final char code;

    Orientation(char code) {
        this.code = code;
    }

    public char code() {
        return code;
    }

    /** Doi huong - dung cho nut "Xoay tau" o giao dien. */
    public Orientation flip() {
        return this == HORIZONTAL ? VERTICAL : HORIZONTAL;
    }

    public static Orientation fromCode(char c) {
        return Character.toUpperCase(c) == 'V' ? VERTICAL : HORIZONTAL;
    }
}