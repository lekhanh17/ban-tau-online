package bantau.bot;

import bantau.common.FireResult;
import bantau.common.MucDoBot;
import bantau.common.ShipType;

import java.util.Random;

/**
 * CACH CHON O BAN - mot giao dien, ba cach lam.
 *
 * <p>Tach rieng viec <b>chon nuoc di</b> khoi viec <b>noi mang</b> la diem
 * dang chu y ve thiet ke o day. {@link BotClient} chi lo dang nhap, vao
 * phong, gui va nhan ban tin; no khong biet thuat toan nao dang chay. Nguoc
 * lai, cac lop thuc thi giao dien nay khong biet gi ve socket.
 *
 * <p>Nho tach nhu vay ma do duoc so lieu: {@code bantau.tools.MoPhongBot}
 * chay hang nghin van <b>khong can server</b>, dung dung nhung lop thuat
 * toan ma bot that dang dung. Neu thuat toan nam lan trong code mang thi
 * muon do phai dung server va di qua mang, cham hang tram lan va so lieu
 * cung kho tin hon.
 */
public interface ChienThuatBan {

    /** Xoa sach hieu biet de bat dau mot van moi. */
    void batDauVanMoi();

    /**
     * Chon o de ban tiep.
     *
     * @return toa do {x, y}, hoac null neu khong con o nao
     */
    int[] chonO(Random rnd);

    /**
     * Nhan ket qua phat ban vua roi de cap nhat ke hoach.
     *
     * @param tauChim ma tau vua chim, chi co nghia khi {@code kq} la
     *                {@link FireResult#SUNK}; co the null
     */
    void ghiKetQua(int x, int y, FireResult kq, ShipType tauChim);

    /** Ten ngan de in ra nhat ky. */
    String ten();

    /** Tao chien thuat tuong ung voi muc do kho. */
    static ChienThuatBan tao(MucDoBot muc) {
        return switch (muc) {
            case DE -> new BanNgauNhien();
            case THUONG -> new BanSanDiet();
            case KHO -> new BanDoXacSuat();
        };
    }
}
