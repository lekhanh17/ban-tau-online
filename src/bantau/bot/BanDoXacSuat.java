package bantau.bot;

import bantau.common.Board;
import bantau.common.FireResult;
import bantau.common.Orientation;
import bantau.common.ShipType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;

/**
 * THUAT TOAN XAC SUAT - CHON O BAN BANG CACH DEM CAC CACH DAT TAU
 *
 * <h2>Y tuong</h2>
 *
 * <p>Bot khong biet tau doi thu o dau. Nhung no biet ba thu:
 * <ul>
 *   <li>Nhung o nao da ban va ket qua ra sao (truot / trung / chim).</li>
 *   <li>Nhung tau nao van con noi - vi khi mot tau chim, server gui kem ma
 *       tau trong {@code FIRE_RESULT}.</li>
 *   <li>Luat dat tau: tau dai lien nhau, khong chong nhau, khong tran vien.</li>
 * </ul>
 *
 * <p>Tu ba thu do suy ra duoc rat nhieu. Voi moi o, hay dem <b>co bao nhieu
 * cach dat cac tau con lai di qua o do ma khong mau thuan voi nhung gi da
 * biet</b>. O nao co nhieu cach dat di qua nhat la o co kha nang chua tau
 * cao nhat - ban vao do.
 *
 * <h2>Vi sao cach nay manh hon san - diet</h2>
 *
 * <p>San - diet chi suy luan <b>cuc bo</b>: ban trung thi thu bon o ke.
 * Thuat toan nay suy luan tren <b>toan ban do</b>, nen tu lam duoc nhung
 * viec ma san - diet khong lam duoc, va quan trong la <b>khong phai viet
 * code rieng cho tung viec</b> - tat ca deu la he qua cua phep dem:
 *
 * <ul>
 *   <li><b>Tu bo qua vung trong qua hep.</b> Mot khoang trong rong 1 o nam
 *       giua hai o da truot thi khong tau nao nhet vao duoc, nen so cach dat
 *       di qua no bang 0 - bot khong bao gio ban vao do. San - diet van ban.
 *   </li>
 *   <li><b>Tu biet tau dai con nam duoc o dau.</b> Khi ban do da bi ban
 *       nhieu, tau 5 o chi con vai cho nhet vao; nhung cho do tu dong duoc
 *       diem cao.</li>
 *   <li><b>Tu chuyen sang che do diet.</b> Khi co o dang trung ma tau chua
 *       chim, cac cach dat phai di qua o trung do (xem
 *       {@link #congDiem(boolean)}), nen diem tu dong don vao cac o quanh
 *       vet trung. Khong can hang doi o can thu nhu san - diet.</li>
 *   <li><b>Tu dung thong tin tau nao da chim.</b> Tau da chim bi loai khoi
 *       phep dem, nen ban do duoc danh gia lai theo dung nhung tau con
 *       lai.</li>
 * </ul>
 *
 * <h2>Day KHONG phai hoc may</h2>
 *
 * <p>Khong co mang no-ron, khong huan luyen, khong du lieu hoc. Day la
 * thuat toan <b>dem va suy luan rang buoc</b> - chay xong trong vai
 * mili giay va cho ket qua giong nhau moi lan voi cung mot trang thai ban
 * do. Trong thuat ngu game thi goi la AI (tri tue doi thu may) hoan toan
 * dung, nhung no thuoc nhanh tim kiem co heuristic, khong phai nhanh hoc
 * may.
 *
 * <p>Lop nay <b>khong he biet den mang</b>: no chi nhan ket qua cac phat ban
 * va tra ve o nen ban tiep. Nho vay chay mo phong duoc hang nghin van ma
 * khong can server - xem {@code bantau.tools.MoPhongBot}.
 */
public final class BanDoXacSuat implements ChienThuatBan {

    /** Hieu biet cua bot ve mot o tren ban do doi thu. */
    public enum O {
        /** Chua ban - chua biet gi. */
        CHUA_BAN,
        /** Da ban, khong co tau. */
        TRUOT,
        /** Da ban, trung mot tau nhung tau do chua chim. */
        TRUNG,
        /** Da ban, va con tau o o nay da chim han. */
        CHIM
    }

    /**
     * Co ap dung loc theo luoi khi dang SAN khong.
     *
     * <p>Y tuong: tau ngan nhat con noi dai {@code S} o, nen moi con tau
     * chac chan che it nhat mot o thoa {@code (x + y) % S == 0}. Chi ban vao
     * nhung o do thi quet nhanh hon ma khong bo sot tau nao. Day la meo lam
     * muc {@link bantau.common.MucDoBot#THUONG} nhanh hon han ban bua.
     *
     * <p><b>Nhung do thuc nghiem cho thay o day no KHONG co tac dung.</b>
     * Chay 2000 van cho moi phuong an ({@code bantau.tools.MoPhongBot}):
     *
     * <pre>
     *   Kho, CO luoi    : trung binh 48.1 phat
     *   Kho, KHONG luoi : trung binh 48.0 phat
     * </pre>
     *
     * <p>Chenh lech 0.1 phat la trong khoang nhieu thong ke, tuc la bang
     * nhau. Ly do hop ly: phep dem cac cach dat <b>da bao ham san y tuong
     * gian cach</b> roi. Mot o nam sat o da truot thi it cach dat di qua
     * hon, nen diem thap hon va tu dong bi tranh - khong can ai bao no phai
     * giu khoang cach.
     *
     * <p>Vi vay mac dinh de {@code false}: khong them mot co che khong mang
     * lai gi. Co che van duoc giu lai de co the chay lai thu nghiem, va de
     * ghi vao bao cao nhu mot phuong an <b>da thu va da loai bo co can cu</b>
     * - mot ket qua am cung la ket qua.
     */
    private final boolean dungLuoi;

    private final O[][] trangThai = new O[Board.SIZE][Board.SIZE];
    private final EnumSet<ShipType> tauConNoi = EnumSet.allOf(ShipType.class);
    /** Bo dem diem, dung lai moi luot de khong cap phat mang lien tuc. */
    private final int[][] diem = new int[Board.SIZE][Board.SIZE];

    public BanDoXacSuat() {
        this(false);
    }

    public BanDoXacSuat(boolean dungLuoi) {
        this.dungLuoi = dungLuoi;
        batDauVanMoi();
    }

    @Override
    public String ten() {
        return "xac suat" + (dungLuoi ? " + luoi" : "");
    }

    /** Xoa sach hieu biet - goi khi bat dau van moi. */
    @Override
    public void batDauVanMoi() {
        for (O[] cot : trangThai) {
            Arrays.fill(cot, O.CHUA_BAN);
        }
        tauConNoi.clear();
        tauConNoi.addAll(EnumSet.allOf(ShipType.class));
    }

    public O trangThaiCua(int x, int y) {
        return Board.inBounds(x, y) ? trangThai[x][y] : O.TRUOT;
    }

    public boolean daBan(int x, int y) {
        return trangThaiCua(x, y) != O.CHUA_BAN;
    }

    /** Diem cua mot o o luot vua tinh - chi dung de kiem thu va go loi. */
    public int diemCua(int x, int y) {
        return Board.inBounds(x, y) ? diem[x][y] : 0;
    }

    /* ------------------------------------------------------------------ */
    /* Ghi nhan ket qua                                                    */
    /* ------------------------------------------------------------------ */

    /**
     * Cap nhat hieu biet sau mot phat ban cua chinh bot.
     *
     * @param tauChim ma tau vua chim, chi co nghia khi {@code kq} la
     *                {@link FireResult#SUNK}; co the null neu server khong
     *                gui kem
     */
    @Override
    public void ghiKetQua(int x, int y, FireResult kq, ShipType tauChim) {
        if (!Board.inBounds(x, y) || kq == null) {
            return;
        }
        switch (kq) {
            case MISS -> trangThai[x][y] = O.TRUOT;
            case HIT -> trangThai[x][y] = O.TRUNG;
            case SUNK -> ghiChim(x, y, tauChim);
            case ALREADY -> {
                // Phat ban khong duoc tinh, hieu biet khong doi.
            }
            default -> {
                // khong co nhanh nao khac
            }
        }
    }

    /**
     * Mot tau vua chim: danh dau o vua ban, loai tau do khoi phep dem, va
     * co gang gan ca nhung o TRUNG lien ke vao dung con tau nay.
     *
     * <p><b>Vi sao phai gan:</b> neu de cac o do o trang thai TRUNG thi bot
     * se tuong con mot con tau chua chim o day va cu tim cach keo dai no mai.
     *
     * <p><b>Vi sao doi khi khong gan duoc:</b> khi hai tau nam sat nhau tren
     * cung mot duong thang, mot chuoi o trung lien tuc co the thuoc hai tau
     * khac nhau. Luc do khong the biet chac o nao thuoc tau nao, nen chi danh
     * dau dung o vua ban va de nhung o con lai cho cac phat sau lam ro. Day
     * la han che da biet cua cach lam nay, nhung no chi gay ton vai phat
     * trong truong hop it gap, va {@link #tinhDiem()} co buoc du phong de
     * khong bi tac han.
     */
    private void ghiChim(int x, int y, ShipType tau) {
        trangThai[x][y] = O.CHIM;
        if (tau == null) {
            return;
        }
        tauConNoi.remove(tau);

        int dai = tau.size();
        List<int[]> ngang = chuoiDaTrung(x, y, 1, 0);
        List<int[]> doc = chuoiDaTrung(x, y, 0, 1);

        List<int[]> cuaTauNay = null;
        if (ngang.size() == dai && doc.size() != dai) {
            cuaTauNay = ngang;
        } else if (doc.size() == dai && ngang.size() != dai) {
            cuaTauNay = doc;
        }
        if (cuaTauNay != null) {
            for (int[] c : cuaTauNay) {
                trangThai[c[0]][c[1]] = O.CHIM;
            }
        }
    }

    /**
     * Chuoi o lien tuc da ban trung (TRUNG hoac CHIM) di qua (x, y) theo mot
     * truc, tinh ca hai chieu.
     */
    private List<int[]> chuoiDaTrung(int x, int y, int dx, int dy) {
        List<int[]> ds = new ArrayList<>();
        ds.add(new int[] { x, y });
        for (int huong = -1; huong <= 1; huong += 2) {
            int cx = x + dx * huong;
            int cy = y + dy * huong;
            while (Board.inBounds(cx, cy) && daTrung(cx, cy)) {
                ds.add(new int[] { cx, cy });
                cx += dx * huong;
                cy += dy * huong;
            }
        }
        return ds;
    }

    private boolean daTrung(int x, int y) {
        O t = trangThai[x][y];
        return t == O.TRUNG || t == O.CHIM;
    }

    /* ------------------------------------------------------------------ */
    /* Tinh diem                                                           */
    /* ------------------------------------------------------------------ */

    /**
     * Tinh diem cho tung o.
     *
     * @return true neu dang o che do DIET (co o trung chua ro thuoc tau nao)
     */
    private boolean tinhDiem() {
        xoaDiem();

        if (coOTrungChuaGiai()) {
            // Che do DIET: chi dem nhung cach dat DI QUA o dang trung. Nho
            // the diem tu don vao vung quanh vet trung.
            if (congDiem(true) > 0) {
                return true;
            }
            // Du phong: khong con cach dat nao giai thich duoc cac o trung
            // (xay ra khi khong gan duoc o trung vao tau da chim - xem
            // ghiChim). Khi do bo qua chung va san binh thuong, thay vi
            // dung lai khong ban duoc gi.
            xoaDiem();
        }

        congDiem(false);
        return false;
    }

    private void xoaDiem() {
        for (int[] cot : diem) {
            Arrays.fill(cot, 0);
        }
    }

    private boolean coOTrungChuaGiai() {
        for (int x = 0; x < Board.SIZE; x++) {
            for (int y = 0; y < Board.SIZE; y++) {
                if (trangThai[x][y] == O.TRUNG) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * PHEP DEM CHINH: duyet moi cach dat cua moi tau con noi, va voi moi
     * cach dat hop le thi cong diem cho cac o chua ban ma no di qua.
     *
     * <p>Mot cach dat la hop le khi moi o cua no nam trong ban do va khong
     * phai o TRUOT hay o CHIM - vi tau con noi khong the nam tren o da ban
     * truot, cung khong the nam tren o thuoc mot tau da chim.
     *
     * @param phaiPhuOTrung true thi chi tinh cac cach dat di qua it nhat mot
     *                      o dang TRUNG (che do diet)
     * @return so cach dat da dem duoc
     */
    private int congDiem(boolean phaiPhuOTrung) {
        int soCach = 0;

        for (ShipType tau : tauConNoi) {
            for (Orientation huong : Orientation.values()) {
                for (int x = 0; x < Board.SIZE; x++) {
                    for (int y = 0; y < Board.SIZE; y++) {

                        List<int[]> o = Board.cellsOf(tau, x, y, huong);
                        int soTrung = 0;
                        boolean hopLe = true;

                        for (int[] c : o) {
                            if (!Board.inBounds(c[0], c[1])) {
                                hopLe = false;
                                break;
                            }
                            O t = trangThai[c[0]][c[1]];
                            if (t == O.TRUOT || t == O.CHIM) {
                                hopLe = false;
                                break;
                            }
                            if (t == O.TRUNG) {
                                soTrung++;
                            }
                        }

                        if (!hopLe || (phaiPhuOTrung && soTrung == 0)) {
                            continue;
                        }
                        soCach++;

                        // O che do diet, cach dat giai thich duoc NHIEU o
                        // trung thi dang tin hon, nen duoc tinh nang hon.
                        int trongSo = phaiPhuOTrung ? soTrung : 1;
                        for (int[] c : o) {
                            if (trangThai[c[0]][c[1]] == O.CHUA_BAN) {
                                diem[c[0]][c[1]] += trongSo;
                            }
                        }
                    }
                }
            }
        }
        return soCach;
    }

    /* ------------------------------------------------------------------ */
    /* Chon o ban                                                          */
    /* ------------------------------------------------------------------ */

    /**
     * Chon o nen ban tiep.
     *
     * <p>Khi nhieu o cung diem cao nhat thi chon ngau nhien trong so do -
     * neu luon lay o dau tien, bot se danh y het nhau moi van va nguoi choi
     * hoc thuoc ngay.
     *
     * @return toa do {x, y}, hoac null neu khong con o nao de ban
     */
    @Override
    public int[] chonO(Random rnd) {
        boolean cheDoDiet = tinhDiem();

        // Luoi chi ap dung khi dang SAN. Dang diet thi phai ban dung o ke
        // vet trung, khong duoc bo qua vi no lech luoi.
        if (!cheDoDiet && dungLuoi) {
            int[] chon = oDiemCaoNhat(rnd, kichThuocNhoNhatConNoi());
            if (chon != null) {
                return chon;
            }
        }

        int[] chon = oDiemCaoNhat(rnd, 0);
        return chon != null ? chon : oNgauNhienChuaBan(rnd);
    }

    /**
     * O co diem cao nhat.
     *
     * @param buocLuoi lon hon 1 thi chi xet cac o thoa
     *                 {@code (x + y) % buocLuoi == 0}; 0 hoac 1 la xet het
     * @return null neu khong o nao thoa dieu kien va co diem duong
     */
    private int[] oDiemCaoNhat(Random rnd, int buocLuoi) {
        int cao = 0;
        List<int[]> tot = new ArrayList<>();

        for (int x = 0; x < Board.SIZE; x++) {
            for (int y = 0; y < Board.SIZE; y++) {
                if (trangThai[x][y] != O.CHUA_BAN || diem[x][y] <= 0) {
                    continue;
                }
                if (buocLuoi > 1 && (x + y) % buocLuoi != 0) {
                    continue;
                }
                if (diem[x][y] > cao) {
                    cao = diem[x][y];
                    tot.clear();
                    tot.add(new int[] { x, y });
                } else if (diem[x][y] == cao) {
                    tot.add(new int[] { x, y });
                }
            }
        }
        return tot.isEmpty() ? null : tot.get(rnd.nextInt(tot.size()));
    }

    /** Tau ngan nhat trong so cac tau con noi - buoc cua luoi khi san. */
    private int kichThuocNhoNhatConNoi() {
        int nhoNhat = Board.SIZE;
        for (ShipType t : tauConNoi) {
            nhoNhat = Math.min(nhoNhat, t.size());
        }
        return nhoNhat;
    }

    /** Luoi an toan cuoi cung - gan nhu khong bao gio dung den. */
    private int[] oNgauNhienChuaBan(Random rnd) {
        List<int[]> conLai = new ArrayList<>();
        for (int x = 0; x < Board.SIZE; x++) {
            for (int y = 0; y < Board.SIZE; y++) {
                if (trangThai[x][y] == O.CHUA_BAN) {
                    conLai.add(new int[] { x, y });
                }
            }
        }
        return conLai.isEmpty() ? null : conLai.get(rnd.nextInt(conLai.size()));
    }
}
