package bantau.server.db;

import bantau.common.MatchRecord;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * BAN CAI DAT {@link MatchDao} LUU THAT VAO MySQL.
 *
 * <p>{@link LocalDateTime} cua Java doi sang kieu DATETIME cua MySQL qua
 * {@link Timestamp#valueOf(LocalDateTime)} va nguoc lai.
 */
public class MatchDaoMySql implements MatchDao {

    @Override
    public void luuTran(String player1, String player2, String winner, String lyDo,
            int soPhatBan, LocalDateTime batDau, LocalDateTime ketThuc) {

        String sql = "INSERT INTO matches "
                + "(player1, player2, winner, reason, shots, started_at, ended_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = Database.moKetNoi();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, player1);
            ps.setString(2, player2);
            if (winner == null || winner.isBlank()) {
                ps.setNull(3, java.sql.Types.VARCHAR);
            } else {
                ps.setString(3, winner);
            }
            ps.setString(4, lyDo);
            ps.setInt(5, soPhatBan);
            ps.setTimestamp(6, Timestamp.valueOf(batDau));
            ps.setTimestamp(7, Timestamp.valueOf(ketThuc));
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("[CSDL] Loi khi luu tran: " + e.getMessage());
        }
    }

    @Override
    public List<MatchRecord> lichSuCua(String username, int gioiHan) {
        List<MatchRecord> ds = new ArrayList<>();
        String sql = "SELECT id, player1, player2, winner, reason, shots, started_at, ended_at "
                + "FROM matches WHERE player1 = ? OR player2 = ? "
                + "ORDER BY ended_at DESC LIMIT ?";
        try (Connection c = Database.moKetNoi();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, username);
            ps.setInt(3, gioiHan);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ds.add(doc(rs));
                }
            }
        } catch (SQLException e) {
            System.out.println("[CSDL] Loi khi lay lich su: " + e.getMessage());
        }
        return ds;
    }

    private MatchRecord doc(ResultSet rs) throws SQLException {
        Timestamp batDau = rs.getTimestamp("started_at");
        Timestamp ketThuc = rs.getTimestamp("ended_at");
        return new MatchRecord(
                rs.getInt("id"),
                rs.getString("player1"),
                rs.getString("player2"),
                rs.getString("winner"),
                rs.getString("reason"),
                rs.getInt("shots"),
                batDau == null ? null : batDau.toLocalDateTime(),
                ketThuc == null ? null : ketThuc.toLocalDateTime());
    }
}
