package bantau.common;

/**
 * Trang thai cua mot phong choi.
 *
 * <p>Enum cua Java mac dinh da Serializable nen gui qua mang duoc ngay.
 */
public enum RoomState {

    /** Dang cho du 2 nguoi. */
    WAITING,
    /** Du 2 nguoi, hai ben dang dat tau. Giai doan 6 se dung den. */
    PLACING,
    /** Dang danh. Giai doan 6 se dung den. */
    PLAYING,
    /**
     * Dang danh nhung MOT NGUOI VUA MAT KET NOI.
     *
     * <p>Ban do cua ca hai ben van duoc giu nguyen, dong ho luot bi tam
     * dung. Phong cho nguoi do dang nhap lai trong
     * {@link Protocol#RECONNECT_SECONDS} giay. Het han thi nguoi con lai
     * duoc xu thang va phong tro ve {@link #WAITING}.
     */
    PAUSED;

    /** Mo ta bang tieng Viet de hien thi cho nguoi choi. */
    public String moTa() {
        return switch (this) {
            case WAITING -> "Dang cho nguoi choi";
            case PLACING -> "Dang dat tau";
            case PLAYING -> "Dang danh";
            case PAUSED -> "Tam dung - cho doi thu vao lai";
        };
    }
}
