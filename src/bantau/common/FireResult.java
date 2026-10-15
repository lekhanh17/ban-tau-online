package bantau.common;

/** Ket qua cua mot phat ban. */
public enum FireResult {

    /** Ban truot, o do khong co tau. */
    MISS("TRƯỢT"),
    /** Ban trung mot o cua tau, tau chua chim. */
    HIT("TRÚNG"),
    /** Ban trung o cuoi cung, tau chim. */
    SUNK("CHÌM"),
    /** O nay da ban roi, hoac toa do ngoai ban do - khong hop le. */
    ALREADY("ĐÃ BẮN RỒI");

    private final String moTa;

    FireResult(String moTa) {
        this.moTa = moTa;
    }

    public String moTa() {
        return moTa;
    }
}