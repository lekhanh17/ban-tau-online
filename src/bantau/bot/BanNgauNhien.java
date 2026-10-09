package bantau.bot;

import bantau.common.Board;
import bantau.common.FireResult;
import bantau.common.ShipType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * MUC DE - BAN NGAU NHIEN
 *
 * <p>Chi tranh ban lai o da ban, ngoai ra khong dung mot chut thong tin nao
 * tu ket qua cac phat truoc: ban trung cung khong thu o ke.
 *
 * <p>Lop nay ton tai vi hai ly do:
 * <ol>
 *   <li>Lam <b>muc De</b> cho nguoi moi choi.</li>
 *   <li>Quan trong hon: lam <b>moc so sanh</b>. Noi "thuat toan cua em tot"
 *       thi khong co y nghia gi neu khong co cai de so. Dem so phat ma muc
 *       nay can roi dat canh hai muc kia la do duoc thuat toan dang gia bao
 *       nhieu - xem {@code bantau.tools.MoPhongBot}.</li>
 * </ol>
 *
 * <p>Ve ly thuyet, ban ngau nhien can trung binh khoang 95 phat de ban chim
 * het 17 o tau tren ban do 100 o.
 */
public final class BanNgauNhien implements ChienThuatBan {

    private final boolean[][] daBan = new boolean[Board.SIZE][Board.SIZE];

    @Override
    public void batDauVanMoi() {
        for (boolean[] cot : daBan) {
            java.util.Arrays.fill(cot, false);
        }
    }

    @Override
    public int[] chonO(Random rnd) {
        List<int[]> conLai = new ArrayList<>();
        for (int x = 0; x < Board.SIZE; x++) {
            for (int y = 0; y < Board.SIZE; y++) {
                if (!daBan[x][y]) {
                    conLai.add(new int[] { x, y });
                }
            }
        }
        return conLai.isEmpty() ? null : conLai.get(rnd.nextInt(conLai.size()));
    }

    @Override
    public void ghiKetQua(int x, int y, FireResult kq, ShipType tauChim) {
        if (Board.inBounds(x, y)) {
            daBan[x][y] = true;
        }
    }

    @Override
    public String ten() {
        return "ngau nhien";
    }
}
