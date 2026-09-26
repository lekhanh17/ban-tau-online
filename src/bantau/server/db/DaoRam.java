package bantau.server.db;

import bantau.common.MatchRecord;
import bantau.common.PlayerStats;
import bantau.common.Protocol;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * BAN DU PHONG: LUU TAM TRONG BO NHO, KHONG CAN CSDL
 *
 * <p>Dung khi chua bat MySQL trong XAMPP hoac thieu driver. Server van
 * chay day du chuc nang dang ky, dang nhap, bang xep hang - chi khac la
 * TAT SERVER LA MAT HET, vi du lieu chi nam trong RAM.
 *
 * <p>Muc dich: buoi demo khong bao gio chet vi loi CSDL. Neu XAMPP loi,
 * van bam Start server va choi duoc binh thuong.
 *
 * <p>Lop nay cai dat CA HAI interface nen server chi can tao mot doi tuong
 * duy nhat cho ca tai khoan lan lich su.
 *
 * <p>Dung {@link ConcurrentHashMap} vi nhieu thread ClientHandler cung
 * doc ghi mot luc - {@code HashMap} thuong se hong du lieu trong truong
 * hop do.
 */
public class DaoRam implements PlayerDao, MatchDao {

    /** Mot dong trong "bang" players. */
    private static final class Dong {
        final String hash;
        final String salt;
        int wins;
        int losses;

        Dong(String hash, String salt) {
            this.hash = hash;
            this.salt = salt;
        }
    }

    private final Map<String, Dong> players = new ConcurrentHashMap<>();
    private final List<MatchRecord> matches = new ArrayList<>();
    private final AtomicInteger idTiepTheo = new AtomicInteger(1);

    /* ------------------------------------------------------------------ */
    /* PlayerDao                                                           */
    /* ------------------------------------------------------------------ */

    @Override
    public String dangKy(String username, String matKhau) {
        String salt = MatKhau.sinhSalt();
        Dong moi = new Dong(MatKhau.bam(salt, matKhau), salt);
        // putIfAbsent tra ve khac null nghia la ten da ton tai.
        return players.putIfAbsent(username, moi) == null ? null : Protocol.E_NAME_EXISTS;
    }

    @Override
    public String kiemTraDangNhap(String username, String matKhau) {
        Dong d = players.get(username);
        if (d == null) {
            return Protocol.E_NO_ACCOUNT;
        }
        return MatKhau.khop(d.salt, d.hash, matKhau) ? null : Protocol.E_WRONG_PASSWORD;
    }

    @Override
    public void congThang(String username) {
        Dong d = players.get(username);
        if (d != null) {
            synchronized (d) {
                d.wins++;
            }
        }
    }

    @Override
    public void congThua(String username) {
        Dong d = players.get(username);
        if (d != null) {
            synchronized (d) {
                d.losses++;
            }
        }
    }

    @Override
    public List<PlayerStats> bangXepHang(int soNguoi) {
        List<PlayerStats> ds = new ArrayList<>();
        for (Map.Entry<String, Dong> e : players.entrySet()) {
            ds.add(new PlayerStats(e.getKey(), e.getValue().wins, e.getValue().losses));
        }
        ds.sort(Comparator.comparingInt(PlayerStats::wins).reversed()
                .thenComparingInt(PlayerStats::losses)
                .thenComparing(PlayerStats::username));
        return ds.size() <= soNguoi ? ds : new ArrayList<>(ds.subList(0, soNguoi));
    }

    /* ------------------------------------------------------------------ */
    /* MatchDao                                                            */
    /* ------------------------------------------------------------------ */

    @Override
    public void luuTran(String player1, String player2, String winner, String lyDo,
            int soPhatBan, LocalDateTime batDau, LocalDateTime ketThuc) {
        MatchRecord r = new MatchRecord(idTiepTheo.getAndIncrement(),
                player1, player2, winner, lyDo, soPhatBan, batDau, ketThuc);
        synchronized (matches) {
            matches.add(r);
        }
    }

    @Override
    public List<MatchRecord> lichSuCua(String username, int gioiHan) {
        List<MatchRecord> ds = new ArrayList<>();
        synchronized (matches) {
            // Duyet nguoc de tran moi nhat len dau.
            for (int i = matches.size() - 1; i >= 0 && ds.size() < gioiHan; i--) {
                MatchRecord r = matches.get(i);
                if (username.equals(r.player1()) || username.equals(r.player2())) {
                    ds.add(r);
                }
            }
        }
        return ds;
    }
}
