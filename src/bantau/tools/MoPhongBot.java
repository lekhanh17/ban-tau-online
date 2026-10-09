package bantau.tools;

import bantau.bot.ChienThuatBan;
import bantau.common.Board;
import bantau.common.FireResult;
import bantau.common.MucDoBot;
import bantau.common.ShipType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * DO SUC MANH BA MUC DO CUA DOI THU MAY - KHONG CAN SERVER
 *
 * <p>Cong cu nay <b>khong noi mang</b>. No dung dung nhung lop thuat toan ma
 * bot that dang dung ({@code bantau.bot.ChienThuatBan}), nhung thay server
 * bang mot {@link Board} dat ngau nhien ngay trong bo nho. Nho vay chay duoc
 * hang nghin van trong vai giay, thay vi phai cho tung phat ban di qua mang
 * voi do tre {@code BOT_DELAY_MS} moi luot.
 *
 * <p>Day la ly do thuat toan duoc tach khoi {@code BotClient}: neu no nam lan
 * trong code mang thi muon do phai dung ca server, cham hang tram lan va so
 * lieu it van hon vi khong chay duoc nhieu van.
 *
 * <p><b>Thuoc do:</b> so phat can de ban chim het 17 o tau. Cang it cang
 * manh. Con so nay la thuoc do dung dan vi no khong phu thuoc vao doi thu -
 * chi phu thuoc vao thuat toan.
 *
 * <p><b>Cach chay:</b> {@code java -cp bin bantau.tools.MoPhongBot [so van]}
 *
 * <p>Khong thuoc phan chuong trinh nop.
 */
public final class MoPhongBot {

    /** Tong so o ma 5 tau chiem - dieu kien ket thuc. */
    private static final int TONG_O_TAU = ShipType.totalCells();

    public static void main(String[] args) {
        int soVan = args.length > 0 ? Integer.parseInt(args[0]) : 2000;

        System.out.println("=== DO SUC MANH BA MUC DO DOI THU MAY ===");
        System.out.println("So van moi muc: " + soVan);
        System.out.println("Thuoc do: so phat can de ban chim het "
                + TONG_O_TAU + " o tau (cang it cang manh)");
        System.out.println();

        List<KetQua> bang = new ArrayList<>();
        for (MucDoBot muc : MucDoBot.values()) {
            KetQua kq = do1Muc(muc.ten(), () -> ChienThuatBan.tao(muc), soVan);
            bang.add(kq);
            System.out.println(kq);
        }

        System.out.println();
        inBangSoSanh(bang);

        System.out.println();
        System.out.println("--- Thu nghiem rieng: loc theo luoi co dang gia khong? ---");
        System.out.println(do1Muc("Kho, CO luoi",
                () -> new bantau.bot.BanDoXacSuat(true), soVan));
        System.out.println(do1Muc("Kho, KHONG luoi",
                () -> new bantau.bot.BanDoXacSuat(false), soVan));
    }

    /* ------------------------------------------------------------------ */

    private interface TaoChienThuat {
        ChienThuatBan tao();
    }

    private record KetQua(String ten, String tenThuatToan, double trungBinh,
            int itNhat, int nhieuNhat, int trungVi) {

        @Override
        public String toString() {
            return String.format(
                    "  %-16s %-16s trung binh %5.1f phat"
                            + "   (it nhat %d, nhieu nhat %d, trung vi %d)",
                    ten, "[" + tenThuatToan + "]", trungBinh,
                    itNhat, nhieuNhat, trungVi);
        }
    }

    private static KetQua do1Muc(String ten, TaoChienThuat tao, int soVan) {
        // Hat giong co dinh cho MOI muc: ca ba muc gap dung cung mot day ban
        // do dat tau. So sanh nhu vay moi cong bang - khong muc nao duoc
        // huong loi vi gap toan ban do de.
        Random rndBanDo = new Random(20261009L);
        Random rndBot = new Random(777L);

        ChienThuatBan ct = tao.tao();
        List<Integer> soPhat = new ArrayList<>(soVan);

        for (int van = 0; van < soVan; van++) {
            Board b = new Board();
            b.randomPlace(rndBanDo);
            soPhat.add(choiMotVan(b, ct, rndBot));
        }

        Collections.sort(soPhat);
        int tong = 0;
        for (int n : soPhat) {
            tong += n;
        }
        return new KetQua(ten, ct.ten(), (double) tong / soPhat.size(),
                soPhat.get(0), soPhat.get(soPhat.size() - 1),
                soPhat.get(soPhat.size() / 2));
    }

    /** @return so phat da ban de chim het tau */
    private static int choiMotVan(Board b, ChienThuatBan ct, Random rnd) {
        ct.batDauVanMoi();
        int phat = 0;
        int lapLai = 0;

        while (!b.allSunk() && phat < Board.SIZE * Board.SIZE + 10) {
            int[] o = ct.chonO(rnd);
            if (o == null) {
                break;
            }
            FireResult kq = b.fire(o[0], o[1]);
            if (kq == FireResult.ALREADY) {
                // Thuat toan chon lai o da ban - khong duoc phep xay ra.
                // Dung lai sau vai lan de khong treo vong lap.
                if (++lapLai > 5) {
                    throw new IllegalStateException("Thuat toan " + ct.ten()
                            + " chon lai o da ban: (" + o[0] + "," + o[1] + ")");
                }
                continue;
            }
            lapLai = 0;
            phat++;

            ShipType tauChim = kq == FireResult.SUNK ? b.shipAt(o[0], o[1]) : null;
            ct.ghiKetQua(o[0], o[1], kq, tauChim);
        }
        return phat;
    }

    /**
     * Bang so sanh: moi muc tiet kiem bao nhieu phan tram so voi muc De
     * (ban ngau nhien).
     */
    private static void inBangSoSanh(List<KetQua> bang) {
        if (bang.isEmpty()) {
            return;
        }
        double moc = bang.get(0).trungBinh();
        System.out.println("--- So voi muc De (ban ngau nhien) ---");
        for (KetQua kq : bang) {
            double tietKiem = (moc - kq.trungBinh()) * 100.0 / moc;
            System.out.printf("  %-16s %5.1f phat   tiet kiem %5.1f%%%n",
                    kq.ten(), kq.trungBinh(), tietKiem);
        }
    }
}
