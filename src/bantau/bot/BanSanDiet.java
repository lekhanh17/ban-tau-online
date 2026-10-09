package bantau.bot;

import bantau.common.Board;
import bantau.common.FireResult;
import bantau.common.ShipType;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Random;

/**
 * MUC THUONG - SAN VA DIET (hunt and target)
 *
 * <p>Hai che do luan phien:
 *
 * <p><b>DIET:</b> ban trung mot o thi day bon o ke vao hang doi
 * {@link #oCanThu} va thu tiep - vi tau nam lien nhau nen phan con lai chac
 * chan o mot trong bon huong. Ban CHIM thi xoa hang doi: con tau do xong
 * roi, giu lai cac o ke chi lam phi dan.
 *
 * <p><b>SAN:</b> het o can thu thi ban mo moi, nhung khong ban bua - chi ban
 * vao cac o co {@code (x + y)} chan, kieu ban co:
 *
 * <pre>
 *   X . X . X . X . X .
 *   . X . X . X . X . X
 *   X . X . X . X . X .
 * </pre>
 *
 * <p>Ly do: con tau ngan nhat dai 2 o, nen <b>moi con tau deu chac chan che
 * it nhat mot o thuoc luoi ban co</b>. Chi can quet nua ban do la tim duoc
 * het tau, thay vi quet ca 100 o. Het o ban co moi ban sang nhung o con lai.
 *
 * <p><b>Han che:</b> thuat toan nay chi suy luan cuc bo quanh vet trung. No
 * khong biet bo qua mot khoang trong rong 1 o ma khong tau nao nhet vao
 * duoc, va khong dung thong tin ve kich thuoc cac tau con lai. Do la nhung
 * viec {@link BanDoXacSuat} lam duoc.
 */
public final class BanSanDiet implements ChienThuatBan {

    private final boolean[][] daBan = new boolean[Board.SIZE][Board.SIZE];
    private final Deque<int[]> oCanThu = new ArrayDeque<>();

    @Override
    public void batDauVanMoi() {
        for (boolean[] cot : daBan) {
            Arrays.fill(cot, false);
        }
        oCanThu.clear();
    }

    @Override
    public int[] chonO(Random rnd) {
        // --- Che do DIET ---
        while (!oCanThu.isEmpty()) {
            int[] o = oCanThu.peek();
            if (chuaBan(o[0], o[1])) {
                return o;
            }
            oCanThu.poll();     // o nay da ban roi, bo di
        }

        // --- Che do SAN ---
        List<int[]> banCo = new ArrayList<>();
        List<int[]> conLai = new ArrayList<>();
        for (int x = 0; x < Board.SIZE; x++) {
            for (int y = 0; y < Board.SIZE; y++) {
                if (!chuaBan(x, y)) {
                    continue;
                }
                if ((x + y) % 2 == 0) {
                    banCo.add(new int[] { x, y });
                } else {
                    conLai.add(new int[] { x, y });
                }
            }
        }
        List<int[]> nguon = banCo.isEmpty() ? conLai : banCo;
        if (nguon.isEmpty()) {
            return null;
        }
        Collections.shuffle(nguon, rnd);
        return nguon.get(0);
    }

    @Override
    public void ghiKetQua(int x, int y, FireResult kq, ShipType tauChim) {
        if (!Board.inBounds(x, y) || kq == null) {
            return;
        }
        daBan[x][y] = true;

        if (kq == FireResult.SUNK) {
            oCanThu.clear();
            return;
        }
        if (kq != FireResult.HIT) {
            return;
        }
        int[][] ke = { { x + 1, y }, { x - 1, y }, { x, y + 1 }, { x, y - 1 } };
        for (int[] o : ke) {
            if (chuaBan(o[0], o[1])) {
                oCanThu.add(o);
            }
        }
    }

    private boolean chuaBan(int x, int y) {
        return Board.inBounds(x, y) && !daBan[x][y];
    }

    @Override
    public String ten() {
        return "san - diet";
    }
}
