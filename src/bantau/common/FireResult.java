package bantau.common;

/** Ket qua cua mot phat ban. */
public enum FireResult {

    /** Ban truot, o do khong co tau. */
    MISS("TRUOT"),
    /** Ban trung mot o cua tau, tau chua chim. */
    HIT("TRUNG"),
    /** Ban trung o cuoi cung, tau chim. */
    SUNK("CHIM"),
    /** O nay da ban roi, hoac toa do ngoai ban do - khong hop le. */
    ALREADY("DA BAN ROI");

    private final String moTa;

    FireResult(String moTa) {
        this.moTa = moTa;
    }

    public String moTa() {
        return moTa;
    }
}