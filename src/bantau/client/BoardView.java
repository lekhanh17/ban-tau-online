package bantau.client;

import bantau.common.Board;
import bantau.common.ShipType;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.List;

import javax.swing.JPanel;

/**
 * BAN CO 10x10 - THANH PHAN GIAO DIEN TU VE, KHONG BIET GI VE MANG
 *
 * <p>Day la lop "thuan giao dien": no chi giu MOT BAN SAO trang thai de ve
 * (tau o dau, o nao trung/truot/chim, dang xem truoc o nao) va tu ve lai bang
 * {@link #paintComponent(Graphics)} bang {@code Graphics2D}. No khong tu ket
 * noi mang, khong tu gui goi tin - {@link GamePanel} la noi "dieu khien" no:
 * doc du lieu tu {@link Board} hoac tu goi FIRE_RESULT/INCOMING nhan duoc roi
 * goi cac ham cong khai o day de cap nhat hinh ve.
 *
 * <p>Dung chung cho ca hai ban co trong tran dau:
 * <ul>
 *   <li>Ban co CUA MINH: hien tau that, dung o giai doan dat tau.</li>
 *   <li>Ban co DOI THU: khong bao gio biet vi tri tau, chi hien dau
 *       TRUOT / TRUNG / CHIM sau khi ban.</li>
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

    private static final Color MAU_BIEN = new Color(0xAED6F1);
    private static final Color MAU_BIEN_VIEN = new Color(0x5DADE2);
    private static final Color MAU_TAU = new Color(0x566573);
    private static final Color MAU_TAU_VIEN = new Color(0x2C3E50);
    private static final Color MAU_TRUOT = new Color(0xFFFFFF);
    private static final Color MAU_TRUNG = new Color(0xE67E22);
    private static final Color MAU_CHIM = new Color(0xC0392B);
    private static final Color MAU_XEM_TRUOC_OK = new Color(0x2ECC71, false);
    private static final Color MAU_XEM_TRUOC_LOI = new Color(0xE74C3C, false);

    /** grid[x][y]: tau nao dang o o do, null neu trong. Chi dung khi hienTau = true. */
    private final ShipType[][] tau = new ShipType[SIZE][SIZE];
    /** danhDau[x][y]: ket qua ban, mac dinh NONE. */
    private final Mark[][] danhDau = new Mark[SIZE][SIZE];

    /** Co hien vi tri tau khong - true cho ban co cua minh, false cho ban co doi thu. */
    private boolean hienTau = true;
    /** Co cho phep bam chuot khong - tat khi chua den luot, hoac van da ket thuc. */
    private boolean choPhepClick = true;

    /** Cac o dang xem truoc (khi ruoc chuot lua vi tri dat tau), null neu khong xem truoc. */
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
        oXemTruoc = null;
        repaint();
    }

    /** Sao chep toan bo vi tri tau tu mot {@link Board} that (dung cho ban co cua minh). */
    public void napTuBanDo(Board board) {
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

    /**
     * Khi mot tau chim, ve lai het nhung o cua no thanh mau CHIM - kem theo
     * cac o tau (dung cho ban co doi thu, noi minh moi chi biet duoc hinh
     * dang tau sau khi no chim, khong biet truoc).
     */
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

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        veNhan(g);
        veLuoiVaO(g);
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

    private void veLuoiVaO(Graphics2D g) {
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                int px = LE_TRAI + x * O;
                int py = LE_TREN + y * O;

                g.setColor(MAU_BIEN);
                g.fillRect(px, py, O, O);

                if (hienTau && tau[x][y] != null) {
                    g.setColor(MAU_TAU);
                    g.fillRect(px + 3, py + 3, O - 6, O - 6);
                    g.setColor(MAU_TAU_VIEN);
                    g.setStroke(new BasicStroke(1.5f));
                    g.drawRect(px + 3, py + 3, O - 6, O - 6);
                }

                veDanhDau(g, px, py, danhDau[x][y]);

                g.setColor(MAU_BIEN_VIEN);
                g.drawRect(px, py, O, O);
            }
        }
        // Vien ngoai day net cho ro rang.
        g.setColor(new Color(0x2E4053));
        g.setStroke(new BasicStroke(2f));
        g.drawRect(LE_TRAI, LE_TREN, SIZE * O, SIZE * O);
    }

    private void veDanhDau(Graphics2D g, int px, int py, Mark m) {
        int giua = O / 2;
        switch (m) {
            case MISS -> {
                g.setColor(MAU_TRUOT);
                g.fillOval(px + giua - 4, py + giua - 4, 8, 8);
                g.setColor(MAU_BIEN_VIEN);
                g.drawOval(px + giua - 4, py + giua - 4, 8, 8);
            }
            case HIT -> veChuThapX(g, px, py, MAU_TRUNG);
            case SUNK -> {
                g.setColor(MAU_CHIM);
                g.fillRect(px + 2, py + 2, O - 4, O - 4);
                veChuThapX(g, px, py, Color.WHITE);
            }
            case NONE -> {
                // khong ve gi them
            }
        }
    }

    private void veChuThapX(Graphics2D g, int px, int py, Color mau) {
        g.setColor(mau);
        g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int d = 8;
        g.drawLine(px + d, py + d, px + O - d, py + O - d);
        g.drawLine(px + O - d, py + d, px + d, py + O - d);
    }

    private void veXemTruoc(Graphics2D g) {
        if (oXemTruoc == null) {
            return;
        }
        Color mau = xemTruocHopLe ? MAU_XEM_TRUOC_OK : MAU_XEM_TRUOC_LOI;
        g.setColor(new Color(mau.getRed(), mau.getGreen(), mau.getBlue(), 130));
        for (int[] o : oXemTruoc) {
            if (!Board.inBounds(o[0], o[1])) {
                continue;
            }
            int px = LE_TRAI + o[0] * O;
            int py = LE_TREN + o[1] * O;
            g.fillRect(px + 2, py + 2, O - 4, O - 4);
        }
    }
}
