package bantau.common;

import java.io.Serializable;

/**
 * THANH TICH CUA MOT NGUOI CHOI - dung cho bang xep hang.
 *
 * <p>Doi tuong BAT BIEN, server doc tu bang {@code players} roi gui nguyen
 * ca danh sach sang client, khong phai noi chuoi rieng le.
 */
public final class PlayerStats implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String username;
    private final int wins;
    private final int losses;

    public PlayerStats(String username, int wins, int losses) {
        this.username = username;
        this.wins = wins;
        this.losses = losses;
    }

    public String username() {
        return username;
    }

    public int wins() {
        return wins;
    }

    public int losses() {
        return losses;
    }

    public int soTran() {
        return wins + losses;
    }

    /** Ty le thang tinh bang phan tram, 0 neu chua danh tran nao. */
    public double tyLeThang() {
        return soTran() == 0 ? 0.0 : (wins * 100.0) / soTran();
    }

    @Override
    public String toString() {
        return String.format("%-16s %3d thang / %3d thua (%.0f%%)",
                username, wins, losses, tyLeThang());
    }
}
