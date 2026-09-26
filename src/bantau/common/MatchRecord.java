package bantau.common;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * MOT DONG TRONG LICH SU DAU - tuong ung mot dong cua bang {@code matches}.
 *
 * <p>{@link LocalDateTime} da Serializable san nen gui thang qua mang duoc,
 * khong can doi sang chuoi roi ben kia tu tach ra.
 */
public final class MatchRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final int id;
    private final String player1;
    private final String player2;
    private final String winner;
    private final String reason;
    private final int shots;
    private final LocalDateTime startedAt;
    private final LocalDateTime endedAt;

    public MatchRecord(int id, String player1, String player2, String winner,
            String reason, int shots, LocalDateTime startedAt, LocalDateTime endedAt) {
        this.id = id;
        this.player1 = player1;
        this.player2 = player2;
        this.winner = winner;
        this.reason = reason;
        this.shots = shots;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
    }

    public int id() {
        return id;
    }

    public String player1() {
        return player1;
    }

    public String player2() {
        return player2;
    }

    public String winner() {
        return winner;
    }

    public String reason() {
        return reason;
    }

    public int shots() {
        return shots;
    }

    public LocalDateTime startedAt() {
        return startedAt;
    }

    public LocalDateTime endedAt() {
        return endedAt;
    }

    /** Ten doi thu cua nguoi dang xem lich su. */
    public String doiThuCua(String toi) {
        return toi.equals(player1) ? player2 : player1;
    }

    /** Nguoi dang xem co thang tran nay khong. */
    public boolean thangBoi(String toi) {
        return toi.equals(winner);
    }

    /** So phut van dau keo dai, lam tron len. */
    public long soPhut() {
        if (startedAt == null || endedAt == null) {
            return 0;
        }
        long giay = java.time.Duration.between(startedAt, endedAt).getSeconds();
        return Math.max(1, (giay + 59) / 60);
    }

    public String thoiGianBatDau() {
        return startedAt == null ? "-" : startedAt.format(FMT);
    }

    /** Mo ta ly do ket thuc bang tieng Viet khong dau. */
    public String moTaLyDo() {
        return Protocol.REASON_OPPONENT_LEFT.equals(reason)
                ? "doi thu roi phong" : "ban chim het tau";
    }
}
