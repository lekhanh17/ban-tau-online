package bantau.server.db;

import bantau.common.MatchRecord;

import java.time.LocalDateTime;
import java.util.List;

/**
 * TRUY CAP DU LIEU LICH SU TRAN DAU
 *
 * <p>Cung mot ly do nhu {@link PlayerDao}: tach interface de doi giua luu
 * that vao MySQL va luu tam trong bo nho ma khong phai sua phan con lai.
 */
public interface MatchDao {

    /** Ghi lai mot tran vua ket thuc. */
    void luuTran(String player1, String player2, String winner, String lyDo,
            int soPhatBan, LocalDateTime batDau, LocalDateTime ketThuc);

    /** Cac tran gan nhat cua mot nguoi choi, moi nhat len dau. */
    List<MatchRecord> lichSuCua(String username, int gioiHan);
}
