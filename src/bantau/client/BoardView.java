package bantau.client;

import bantau.common.Board;
import bantau.common.Orientation;
import bantau.common.ShipType;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import javax.swing.JPanel;

/**
 * BAN CO 10x10 - THANH PHAN GIAO DIEN TU VE, KHONG BIET GI VE MANG
 *
 * <p>Day la lop "thuan giao dien": no chi giu MOT BAN SAO trang thai de ve
 * (tau o dau, o nao trung/truot/chim, dang xem truoc o nao) va tu ve lai bang
 * {@link #paintComponent(Graphics)} bang {@code Graphics2D}. No khong tu ket
 * noi mang, khong tu gui goi tin - {@link GamePanel} la noi "dieu khien" no.
 *
 * <p><b>Ve tau lien mach, khong phai to tung o.</b> Moi con tau duoc ve mot
 * lan duy nhat thanh mot THAN TAU keo dai qua het cac o no chiem: mui nhon o
 * dau, duoi tron o cuoi, co boong, thap chi huy va cua so tron. Muon lam duoc
 * vay thi phai biet GOC va HUONG cua tung con tau chu khong chi biet "o nay
 * co tau hay khong" - do la ly do co {@link #tauDat}.
 *
 * <p>Dung chung cho ca hai ban co trong tran dau:
 * <ul>
 *   <li>Ban co CUA MINH: hien tau that, dung o giai doan dat tau.</li>
 *   <li>Ban co DOI THU: khong bao gio biet vi tri tau. Chi khi mot tau da
 *       CHIM han thi hinh dang no moi lo ra, va luc do ta ve xac tau - xem
 *       {@link #veXacTauDoiThu(Graphics2D)}.</li>
 * </ul>
 */
public class BoardView extends JPanel {

    private static final long serialVersionUID = 1L;

    /** Danh dau ket qua tren mot o - doc lap voi FireResult cua goi tin. */
    public enum Mark { NONE, MISS, HIT, SUNK }

    /** Nguoi nghe su kien click vao mot o. */
    public interface ClickListener {
        void onClick(int x, int y);
    }

    /** Nguoi nghe su kien di chuot qua cac o (dung xem truoc khi dat tau). */
    public interface HoverListener {
        /** x = -1, y = -1 khi chuot ra khoi ban co. */
        void onHover(int x, int y);
    }

    private static final int SIZE = Board.SIZE;
    private static final int O = 34;                 // kich thuoc mot o, tinh bang pixel
    private static final int LE_TRAI = 26;            // le trai danh cho nhan hang A..J
    private static final int LE_TREN = 26;            // le tren danh cho nhan cot 1..10

    /* ----- mau bien ----- */
    private static final Color BIEN_NONG = new Color(0xC7E6F7);
    private static final Color BIEN_SAU = new Color(0x9BCBE6);
    private static final Color LUOI = new Color(0x6FB3D6);
    private static final Color VIEN_NGOAI = new Color(0x2E4053);
    private static final Color SONG = new Color(0xFFFFFF);

    /* ----- mau than tau ----- */
    private static final Color VO_SANG = new Color(0x8A9BA8);
    private static final Color VO_TOI = new Color(0x3E4C57);
    private static final Color VO_VIEN = new Color(0x222E36);

    /* ----- mau danh dau ----- */
    private static final Color MAU_TRUOT = new Color(0xFFFFFF);
    private static final Color MAU_TRUNG = new Color(0xE67E22);
    private static final Color MAU_CHIM = new Color(0xC0392B);
    private static final Color MAU_XEM_TRUOC_OK = new Color(0x2ECC71);
    private static final Color MAU_XEM_TRUOC_LOI = new Color(0xE74C3C);

    /** grid[x][y]: tau nao dang o o do, null neu trong. Dung cho logic, khong de ve. */
    private final ShipType[][] tau = new ShipType[SIZE][SIZE];
    /** danhDau[x][y]: ket qua ban, mac dinh NONE. */
    private final Mark[][] danhDau = new Mark[SIZE][SIZE];

    /**
     * GOC VA HUONG cua tung con tau: {x, y, ma huong}.
     *
     * <p>Chi co o ban co cua minh. Khong co thong tin nay thi khong ve duoc
     * than tau lien mach, chi to duoc tung o roi rac.
     */
    private final Map<ShipType, int[]> tauDat = new EnumMap<>(ShipType.class);

    /** Co hien vi tri tau khong - true cho ban co cua minh, false cho doi thu. */
    private boolean hienTau = true;
    /** Co cho phep bam chuot khong - tat khi chua den luot, hoac van da ket thuc. */
    private boolean choPhepClick = true;

    /** Cac o dang xem truoc, null neu khong xem truoc. */
    private List<int[]> oXemTruoc;
    private boolean xemTruocHopLe;

    private ClickListener clickListener;
    private HoverListener hoverListener;

    public BoardView() {
        xoaHet();

        int rong = LE_TRAI + SIZE * O + 2;
        int cao = LE_TREN + SIZE * O + 2;
        setPreferredSize(new Dimension(rong, cao));
        setBackground(Color.WHITE);

        MouseAdapter chuot = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!choPhepClick) {
                    return;
                }
                int[] o = pixelSangO(e.getX(), e.getY());
                if (o != null && clickListener != null) {
                    clickListener.onClick(o[0], o[1]);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (hoverListener != null) {
                    hoverListener.onHover(-1, -1);
                }
            }
        };
        addMouseListener(chuot);

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (hoverListener == null) {
                    return;
                }
                int[] o = pixelSangO(e.getX(), e.getY());
                hoverListener.onHover(o == null ? -1 : o[0], o == null ? -1 : o[1]);
            }
        });
    }

    /** Doi toa do pixel tren man hinh thanh toa do o (x, y), null neu ngoai ban co. */
    private int[] pixelSangO(int px, int py) {
        int x = (px - LE_TRAI) / O;
        int y = (py - LE_TREN) / O;
        return Board.inBounds(x, y) ? new int[] { x, y } : null;
    }

    /* ------------------------------------------------------------------ */
    /* API dieu khien tu GamePanel                                        */
    /* ------------------------------------------------------------------ */

    public void setHienTau(boolean hienTau) {
        this.hienTau = hienTau;
        repaint();
    }

    public void setChoPhepClick(boolean choPhep) {
        this.choPhepClick = choPhep;
        setCursor(java.awt.Cursor.getPredefinedCursor(
                choPhep ? java.awt.Cursor.HAND_CURSOR : java.awt.Cursor.DEFAULT_CURSOR));
    }

    public void setClickListener(ClickListener l) {
        this.clickListener = l;
    }

    public void setHoverListener(HoverListener l) {
        this.hoverListener = l;
    }

    /** Xoa toan bo tau va dau ban, dung khi vao van moi. */
    public void xoaHet() {
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                tau[x][y] = null;
                danhDau[x][y] = Mark.NONE;
            }
        }
        tauDat.clear();
        oXemTruoc = null;
        repaint();
    }

    /**
     * Sao chep vi tri tau tu mot {@link Board} that (dung cho ban co cua minh).
     *
     * <p>Lay ca {@link Board#originOf(ShipType)} chu khong chi lay luoi o:
     * goc va huong moi du de ve than tau lien mach.
     */
    public void napTuBanDo(Board board) {
        tauDat.clear();
        for (ShipType t : ShipType.values()) {
            int[] goc = board.originOf(t);
            if (goc != null) {
                tauDat.put(t, goc);
            }
        }
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                tau[x][y] = board.shipAt(x, y);
                if (board.isShot(x, y)) {
                    ShipType t = board.shipAt(x, y);
                    danhDau[x][y] = (t != null && board.isSunk(t)) ? Mark.SUNK
                            : (t != null ? Mark.HIT : Mark.MISS);
                } else {
                    danhDau[x][y] = Mark.NONE;
                }
            }
        }
        repaint();
    }

    /**
     * O nay da ban roi chua.
     *
     * <p>Dung de chan tu phia client: bam vao o da ban thi khong gui goi tin
     * len server nua, do la phat ban chac chan bi tu choi.
     */
    public boolean daBan(int x, int y) {
        return Board.inBounds(x, y) && danhDau[x][y] != Mark.NONE;
    }

    /** Danh dau ket qua mot phat ban (TRUOT / TRUNG / CHIM) len o (x, y). */
    public void danhDauO(int x, int y, Mark m) {
        if (Board.inBounds(x, y)) {
            danhDau[x][y] = m;
            repaint();
        }
    }

    /** Khi mot tau chim, ve lai het nhung o cua no thanh mau CHIM. */
    public void danhDauTauChim(List<int[]> oCuaTau) {
        for (int[] o : oCuaTau) {
            danhDauO(o[0], o[1], Mark.SUNK);
        }
    }

    /** Hien xem truoc vi tri se dat tau khi ruoc chuot: xanh la hop le, do la khong. */
    public void xemTruoc(List<int[]> oCells, boolean hopLe) {
        this.oXemTruoc = oCells;
        this.xemTruocHopLe = hopLe;
        repaint();
    }

    public void boXemTruoc() {
        this.oXemTruoc = null;
        repaint();
    }

    /* ------------------------------------------------------------------ */
    /* Ve                                                                  */
    /* ------------------------------------------------------------------ */

    /**
     * Thu tu ve rat quan trong, lop sau de len lop truoc:
     * bien -> luoi -> than tau -> xac tau doi thu -> dau ban -> xem truoc.
     */
    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                RenderingHints.VALUE_STROKE_PURE);

        veNhan(g);
        veBien(g);
        veLuoi(g);

        if (hienTau) {
            veTauCuaMinh(g);
        } else {
            veXacTauDoiThu(g);
        }

        veTatCaDanhDau(g);
        veXemTruoc(g);
    }

    private void veNhan(Graphics2D g) {
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.setColor(new Color(0x34495E));
        FontMetrics fm = g.getFontMetrics();

        for (int x = 0; x < SIZE; x++) {
            String chu = String.valueOf((char) ('A' + x));
            int cx = LE_TRAI + x * O + (O - fm.stringWidth(chu)) / 2;
            g.drawString(chu, cx, LE_TREN - 8);
        }
        for (int y = 0; y < SIZE; y++) {
            String chu = String.valueOf(y + 1);
            int cy = LE_TREN + y * O + (O + fm.getAscent()) / 2 - 2;
            g.drawString(chu, LE_TRAI - fm.stringWidth(chu) - 6, cy);
        }
    }

    /**
     * Mat bien: do mau dam dan tu tren xuong cho co chieu sau, them vai net
     * song mo. Khong to phang mot mau nhu truoc.
     */
    private void veBien(Graphics2D g) {
        int w = SIZE * O;
        int h = SIZE * O;
        g.setPaint(new GradientPaint(LE_TRAI, LE_TREN, BIEN_NONG,
                LE_TRAI, LE_TREN + h, BIEN_SAU));
        g.fillRect(LE_TRAI, LE_TREN, w, h);

        // Net song: nhat va thua, chi de mat bien do phang, khong gay roi mat.
        g.setColor(new Color(SONG.getRed(), SONG.getGreen(), SONG.getBlue(), 38));
        g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int hang = 0; hang < SIZE; hang++) {
            int py = LE_TREN + hang * O + O * 2 / 3;
            for (int cot = (hang % 2); cot < SIZE; cot += 2) {
                int px = LE_TRAI + cot * O + 6;
                Path2D.Float song = new Path2D.Float();
                song.moveTo(px, py);
                song.quadTo(px + 5, py - 4, px + 10, py);
                song.quadTo(px + 15, py + 4, px + 20, py);
                g.draw(song);
            }
        }
    }

    private void veLuoi(Graphics2D g) {
        g.setColor(LUOI);
        g.setStroke(new BasicStroke(1f));
        for (int i = 0; i <= SIZE; i++) {
            int p = LE_TRAI + i * O;
            g.drawLine(p, LE_TREN, p, LE_TREN + SIZE * O);
            int q = LE_TREN + i * O;
            g.drawLine(LE_TRAI, q, LE_TRAI + SIZE * O, q);
        }
        g.setColor(VIEN_NGOAI);
        g.setStroke(new BasicStroke(2f));
        g.drawRect(LE_TRAI, LE_TREN, SIZE * O, SIZE * O);
    }

    /* ------------------------------------------------------------------ */
    /* Ve than tau                                                         */
    /* ------------------------------------------------------------------ */

    private void veTauCuaMinh(Graphics2D g) {
        for (Map.Entry<ShipType, int[]> e : tauDat.entrySet()) {
            ShipType t = e.getKey();
            int[] goc = e.getValue();
            Orientation huong = Orientation.fromCode((char) goc[2]);
            veMotTau(g, goc[0], goc[1], t.size(), huong, daChim(t));
        }
    }

    /** Ca con tau nay da chim han chua - de ve bang mau xac tau. */
    private boolean daChim(ShipType t) {
        int[] goc = tauDat.get(t);
        if (goc == null) {
            return false;
        }
        Orientation huong = Orientation.fromCode((char) goc[2]);
        for (int[] o : Board.cellsOf(t, goc[0], goc[1], huong)) {
            if (!Board.inBounds(o[0], o[1]) || danhDau[o[0]][o[1]] != Mark.SUNK) {
                return false;
            }
        }
        return true;
    }

    /**
     * BAN CO DOI THU: chi ve nhung con tau DA CHIM HAN.
     *
     * <p>Minh khong biet tau doi thu nam o dau cho den khi ban chim no. Luc
     * do cac o SUNK nam lien nhau chinh la hinh dang con tau, nen ta gom
     * chung lai thanh tung day roi ve xac tau - vua dep vua cho nguoi choi
     * thay ro minh da diet duoc gi.
     *
     * <p>Khong co ro ri thong tin nao o day: toan bo du lieu dung de ve deu
     * la ket qua nhung phat minh da ban ra.
     */
    private void veXacTauDoiThu(Graphics2D g) {
        boolean[][] daVe = new boolean[SIZE][SIZE];
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                if (danhDau[x][y] != Mark.SUNK || daVe[x][y]) {
                    continue;
                }
                // Dem chieu dai day o SUNK lien nhau theo chieu ngang va doc.
                int ngang = 0;
                while (x + ngang < SIZE && danhDau[x + ngang][y] == Mark.SUNK
                        && !daVe[x + ngang][y]) {
                    ngang++;
                }
                int doc = 0;
                while (y + doc < SIZE && danhDau[x][y + doc] == Mark.SUNK
                        && !daVe[x][y + doc]) {
                    doc++;
                }

                Orientation huong = (ngang >= doc)
                        ? Orientation.HORIZONTAL : Orientation.VERTICAL;
                int dai = Math.max(ngang, doc);
                for (int i = 0; i < dai; i++) {
                    if (huong == Orientation.HORIZONTAL) {
                        daVe[x + i][y] = true;
                    } else {
                        daVe[x][y + i] = true;
                    }
                }
                veMotTau(g, x, y, dai, huong, true);
            }
        }
    }

    /**
     * Ve mot con tau dai {@code dai} o, bat dau tu o (x, y).
     *
     * <p><b>Ky thuat:</b> than tau luon duoc ve o tu the NAM NGANG trong he
     * toa do cuc bo. Tau nam doc thi khong ve lai hinh khac, ma XOAY he toa
     * do 90 do roi ve y het. Nho vay chi can viet mot ham ve duy nhat.
     *
     * <p>Dung {@code g.create()} de lay mot ban sao Graphics rieng: moi phep
     * xoay, doi goc chi anh huong ban sao do. Khong lam vay thi phep xoay con
     * lai se lam lech moi thu ve sau no.
     */
    private void veMotTau(Graphics2D g, int x, int y, int dai,
            Orientation huong, boolean chim) {
        Graphics2D gg = (Graphics2D) g.create();
        gg.translate(LE_TRAI + x * O, LE_TREN + y * O);

        if (huong == Orientation.VERTICAL) {
            // Xoay 90 do roi day nguoc lai mot o: sau phep bien doi nay, truc
            // "chieu dai" cua he toa do cuc bo chay tu tren xuong duoi.
            gg.rotate(Math.PI / 2);
            gg.translate(0, -O);
        }

        veThanTauNgang(gg, dai * O, O, dai, chim);
        gg.dispose();
    }

    /**
     * VE THAN TAU trong he toa do cuc bo: dai L, rong W, mui huong sang phai.
     *
     * <p>Cac phan: vo tau (mui nhon, duoi tron) - mat boong - thap chi huy -
     * cua so tron. Tau cang dai thi cang nhieu chi tiet, de con tau 5 o khac
     * han con tau 2 o khi nhin thoang qua.
     */
    private void veThanTauNgang(Graphics2D g, int L, int W, int soO, boolean chim) {
        float le = 3f;
        float caoVo = W - 2 * le;
        float muiDai = Math.min(16f, L * 0.22f);

        // --- Vo tau ---
        Path2D.Float vo = new Path2D.Float();
        vo.moveTo(le + 7, le);
        vo.lineTo(L - le - muiDai, le);
        vo.quadTo(L - le, W / 2f, L - le - muiDai, W - le);   // mui nhon
        vo.lineTo(le + 7, W - le);
        vo.quadTo(le - 1, W / 2f, le + 7, le);                // duoi tron
        vo.closePath();

        // Bong do duoi than tau, de con tau trong nhu dang noi tren mat nuoc
        // chu khong phai dan bet vao luoi o.
        Graphics2D bong = (Graphics2D) g.create();
        bong.translate(1.5, 2.5);
        bong.setColor(new Color(0x1B3A4B, false));
        bong.setComposite(java.awt.AlphaComposite.getInstance(
                java.awt.AlphaComposite.SRC_OVER, 0.22f));
        bong.fill(vo);
        bong.dispose();

        Color sang = chim ? new Color(0x9E5B52) : VO_SANG;
        Color toi = chim ? new Color(0x5B2A24) : VO_TOI;
        g.setPaint(new GradientPaint(0, le, sang, 0, W - le, toi));
        g.fill(vo);

        g.setColor(chim ? new Color(0x3A1713) : VO_VIEN);
        g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(vo);

        // --- Mat boong: mot vet sang chay doc giua than tau ---
        float boongCao = Math.max(5f, caoVo * 0.34f);
        float boongY = (W - boongCao) / 2f;
        g.setColor(chim ? new Color(0x7A3B34) : new Color(0x6E8597));
        g.fill(new java.awt.geom.RoundRectangle2D.Float(
                le + 9, boongY, L - 2 * le - muiDai - 9, boongCao, 5f, 5f));

        // --- Thap chi huy: chi ve voi tau tu 3 o tro len ---
        if (soO >= 3) {
            float thapRong = Math.min(14f, O * 0.4f);
            float thapX = L * 0.42f;
            g.setColor(chim ? new Color(0x8C4A41) : new Color(0x9DB0BD));
            g.fill(new java.awt.geom.RoundRectangle2D.Float(
                    thapX, le + 2, thapRong, caoVo - 4, 4f, 4f));
            g.setColor(chim ? new Color(0x3A1713) : VO_VIEN);
            g.setStroke(new BasicStroke(1.1f));
            g.draw(new java.awt.geom.RoundRectangle2D.Float(
                    thapX, le + 2, thapRong, caoVo - 4, 4f, 4f));

            // Cot an-ten tren thap
            g.drawLine((int) (thapX + thapRong / 2), (int) (le + 2),
                    (int) (thapX + thapRong / 2), (int) (le - 3));
        }

        // --- Cua so tron doc than tau ---
        if (!chim) {
            g.setColor(new Color(0xD6E4EC));
            int soCua = Math.max(1, soO - 1);
            float buoc = (L - 2 * le - muiDai - 14) / soCua;
            for (int i = 0; i < soCua; i++) {
                float cx = le + 14 + buoc * i + buoc / 2f;
                g.fillOval((int) cx - 2, (int) (W / 2f) - 2, 4, 4);
            }
        }

        // --- Phao o mui, chi voi tau tu 4 o tro len ---
        if (soO >= 4 && !chim) {
            g.setColor(new Color(0x37444E));
            g.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine((int) (L - le - muiDai - 6), (int) (W / 2f),
                    (int) (L - le - muiDai + 4), (int) (W / 2f));
        }
    }

    /* ------------------------------------------------------------------ */
    /* Ve dau ban                                                          */
    /* ------------------------------------------------------------------ */

    private void veTatCaDanhDau(Graphics2D g) {
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                if (danhDau[x][y] != Mark.NONE) {
                    veDanhDau(g, LE_TRAI + x * O, LE_TREN + y * O, danhDau[x][y]);
                }
            }
        }
    }

    private void veDanhDau(Graphics2D g, int px, int py, Mark m) {
        int giua = O / 2;
        switch (m) {
            case MISS -> veSongNuoc(g, px, py);
            case HIT -> veLuaChay(g, px, py);
            case SUNK -> {
                // Xac tau da duoc ve o lop duoi, o day chi them dau cheo.
                g.setColor(new Color(255, 255, 255, 210));
                g.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int d = 10;
                g.drawLine(px + d, py + d, px + O - d, py + O - d);
                g.drawLine(px + O - d, py + d, px + d, py + O - d);
            }
            case NONE -> {
                // khong ve gi
            }
        }
    }

    /**
     * BAN TRUOT: mot vong tron nuoc ban ra, giong vet dan roi xuong bien.
     * De trang va nho de khong lan at hinh con tau.
     */
    private void veSongNuoc(Graphics2D g, int px, int py) {
        int giua = O / 2;
        g.setColor(new Color(255, 255, 255, 150));
        g.setStroke(new BasicStroke(1.4f));
        g.drawOval(px + giua - 9, py + giua - 9, 18, 18);
        g.drawOval(px + giua - 5, py + giua - 5, 10, 10);
        g.setColor(MAU_TRUOT);
        g.fillOval(px + giua - 3, py + giua - 3, 6, 6);
    }

    /**
     * BAN TRUNG: dom lua tren than tau. Ve ba vong tron long nhau tu vang
     * den do cam, nhin ra ngay la "cho nay dang chay".
     */
    private void veLuaChay(Graphics2D g, int px, int py) {
        int giua = O / 2;
        g.setColor(new Color(0x8E2B0E));
        g.fillOval(px + giua - 10, py + giua - 10, 20, 20);
        g.setColor(MAU_TRUNG);
        g.fillOval(px + giua - 7, py + giua - 7, 14, 14);
        g.setColor(new Color(0xF4D03F));
        g.fillOval(px + giua - 3, py + giua - 3, 6, 6);
    }

    /**
     * XEM TRUOC khi dat tau: ve luon hinh THAN TAU mo chu khong phai o vuong,
     * de nguoi choi thay truoc con tau se trong nhu the nao.
     *
     * <p>Huong duoc suy ra tu hai o dau: cung hang la nam ngang, khac hang
     * la nam doc. Khong can truyen them tham so.
     */
    private void veXemTruoc(Graphics2D g) {
        if (oXemTruoc == null || oXemTruoc.isEmpty()) {
            return;
        }
        // O nao tran ra ngoai ban co thi chi to bang o vuong bao loi.
        List<int[]> trong = new ArrayList<>();
        for (int[] o : oXemTruoc) {
            if (Board.inBounds(o[0], o[1])) {
                trong.add(o);
            }
        }

        Color mau = xemTruocHopLe ? MAU_XEM_TRUOC_OK : MAU_XEM_TRUOC_LOI;

        if (xemTruocHopLe && trong.size() == oXemTruoc.size()) {
            int[] dau = oXemTruoc.get(0);
            Orientation huong = (oXemTruoc.size() > 1
                    && oXemTruoc.get(1)[1] != dau[1])
                    ? Orientation.VERTICAL : Orientation.HORIZONTAL;

            Graphics2D gg = (Graphics2D) g.create();
            gg.setComposite(java.awt.AlphaComposite.getInstance(
                    java.awt.AlphaComposite.SRC_OVER, 0.55f));
            veMotTau(gg, dau[0], dau[1], oXemTruoc.size(), huong, false);
            gg.dispose();

            // Vien xanh bao hieu dat duoc
            g.setColor(new Color(mau.getRed(), mau.getGreen(), mau.getBlue(), 170));
            g.setStroke(new BasicStroke(2f));
            for (int[] o : trong) {
                g.drawRect(LE_TRAI + o[0] * O + 1, LE_TREN + o[1] * O + 1, O - 2, O - 2);
            }
            return;
        }

        // Khong dat duoc: to do cac o de bao loi.
        g.setColor(new Color(mau.getRed(), mau.getGreen(), mau.getBlue(), 130));
        for (int[] o : trong) {
            g.fillRect(LE_TRAI + o[0] * O + 2, LE_TREN + o[1] * O + 2, O - 4, O - 4);
        }
    }
}
