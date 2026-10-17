package bantau.client;

import bantau.common.MatchRecord;
import bantau.common.PlayerStats;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;

/**
 * HAI HOP THOAI DOC DU LIEU TU CSDL: bang xep hang va lich su dau.
 *
 * <p>Lop nay chi co ham tinh, khong tao doi tuong. No nhan danh sach doi
 * tuong server gui sang roi do vao {@link JTable} de hien thi.
 *
 * <p>Ban nay chi thay doi phan VE (renderer, mau sac, bo cuc). Toan bo cach
 * doc du lieu tu {@link PlayerStats} va {@link MatchRecord} giu nguyen nhu
 * ban cu, nen khong can sua bat ky lop nao khac.
 */
public final class ThongKeDialog {

    // ---------- Bang mau dung chung voi LoginPanel / LobbyPanel ----------

    private static final Color NEN_THE    = new Color(0xFF, 0xFF, 0xFF);
    private static final Color NEN_XEN    = new Color(0xF7, 0xFA, 0xFC); // hang xen ke
    private static final Color VIEN       = new Color(0xDD, 0xE4, 0xEC);
    private static final Color CHU        = new Color(0x1F, 0x29, 0x37);
    private static final Color CHU_MO     = new Color(0x6B, 0x72, 0x80);
    private static final Color XANH       = new Color(0x1D, 0x4E, 0xD8);
    private static final Color XANH_NHAT  = new Color(0xE8, 0xF0, 0xFE);
    private static final Color LUC        = new Color(0x15, 0x80, 0x3D);
    private static final Color LUC_NHAT   = new Color(0xE3, 0xF6, 0xE9);
    private static final Color DO         = new Color(0xB9, 0x1C, 0x1C);
    private static final Color DO_NHAT    = new Color(0xFD, 0xE8, 0xE8);
    private static final Color VANG       = new Color(0xD9, 0x9E, 0x0B); // hang 1
    private static final Color BAC        = new Color(0x94, 0xA3, 0xB8); // hang 2
    private static final Color DONG       = new Color(0xB4, 0x7B, 0x3F); // hang 3

    private static final Font F_TIEU_DE  = new Font("SansSerif", Font.BOLD, 19);
    private static final Font F_HEADER   = new Font("SansSerif", Font.BOLD, 13);
    private static final Font F_O        = new Font("SansSerif", Font.PLAIN, 14);
    private static final Font F_O_DAM    = new Font("SansSerif", Font.BOLD, 14);
    private static final Font F_NHO      = new Font("SansSerif", Font.PLAIN, 11);
    private static final Font F_GHI_CHU  = new Font("SansSerif", Font.ITALIC, 12);

    private ThongKeDialog() {
    }

    // =====================================================================
    //  1. CAC THANH PHAN VE DUNG CHUNG
    // =====================================================================

    /** Nut bo goc, phang, dung cho nut OK. */
    private static final class Nut extends JButton {
        private final Color nen;
        private final Color chu;

        Nut(String text, Color nen, Color chu) {
            super(text);
            this.nen = nen;
            this.chu = chu;
            setFont(new Font("SansSerif", Font.BOLD, 14));
            setForeground(chu);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setBorder(BorderFactory.createEmptyBorder(10, 30, 10, 30));
            setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        }

        private Color pha(Color c, Color voi, float ti) {
            return new Color(
                    (int) (c.getRed()   * (1 - ti) + voi.getRed()   * ti),
                    (int) (c.getGreen() * (1 - ti) + voi.getGreen() * ti),
                    (int) (c.getBlue()  * (1 - ti) + voi.getBlue()  * ti));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Color c = nen;
            if (getModel().isPressed()) {
                c = pha(nen, Color.BLACK, 0.18f);
            } else if (getModel().isRollover()) {
                c = pha(nen, Color.WHITE, 0.14f);
            }
            g2.setColor(c);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Tieu de cot: nen xam nhat, chu dam, chi co mot duong ke duoi. */
    private static final class VeHeader extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel,
                                                       boolean focus, int r, int c) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, false, false, r, c);
            l.setFont(F_HEADER);
            l.setForeground(CHU_MO);
            l.setBackground(new Color(0xF1, 0xF5, 0xF9));
            l.setOpaque(true);
            l.setHorizontalAlignment(SwingConstants.CENTER);
            l.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 2, 0, VIEN),
                    BorderFactory.createEmptyBorder(9, 8, 9, 8)));
            return l;
        }
    }

    /** Mau nen cua mot hang: hang xen ke, hang cua chinh minh, hang dang chon. */
    private static Color nenHang(JTable t, int row, boolean sel, boolean noiBat) {
        if (sel) {
            return XANH_NHAT;
        }
        if (noiBat) {
            return new Color(0xEF, 0xF6, 0xFF);
        }
        return (row % 2 == 0) ? NEN_THE : NEN_XEN;
    }

    /** O chu thuong. */
    private static final class VeChu extends DefaultTableCellRenderer {
        private final int canLe;
        private final boolean dam;
        private final java.util.function.IntPredicate noiBat;

        VeChu(int canLe, boolean dam, java.util.function.IntPredicate noiBat) {
            this.canLe = canLe;
            this.dam = dam;
            this.noiBat = noiBat;
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel,
                                                       boolean focus, int r, int c) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, sel, false, r, c);
            l.setFont(dam ? F_O_DAM : F_O);
            l.setForeground(CHU);
            l.setHorizontalAlignment(canLe);
            l.setBackground(nenHang(t, r, sel, noiBat != null && noiBat.test(r)));
            l.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
            return l;
        }
    }

    /** O so co to mau (thang = luc, thua = do). */
    private static final class VeSo extends DefaultTableCellRenderer {
        private final Color mau;
        private final java.util.function.IntPredicate noiBat;

        VeSo(Color mau, java.util.function.IntPredicate noiBat) {
            this.mau = mau;
            this.noiBat = noiBat;
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel,
                                                       boolean focus, int r, int c) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, sel, false, r, c);
            l.setFont(F_O_DAM);
            int so = 0;
            try {
                so = Integer.parseInt(String.valueOf(v));
            } catch (NumberFormatException ignore) {
                // khong phai so thi de mau mac dinh
            }
            l.setForeground(so == 0 ? CHU_MO : mau);
            l.setHorizontalAlignment(SwingConstants.CENTER);
            l.setBackground(nenHang(t, r, sel, noiBat != null && noiBat.test(r)));
            l.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
            return l;
        }
    }

    /**
     * Cot "Hang": ba hang dau duoc ve thanh huy hieu tron (vang / bac / dong),
     * tu hang 4 tro di chi la so thuong.
     */
    private static final class VeHuyHieu extends JPanel implements TableCellRenderer {
        private int hang;
        private Color nen;

        VeHuyHieu() {
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel,
                                                       boolean focus, int r, int c) {
            try {
                hang = Integer.parseInt(String.valueOf(v));
            } catch (NumberFormatException e) {
                hang = r + 1;
            }
            nen = nenHang(t, r, sel, false);
            setBackground(nen);
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            String s = String.valueOf(hang);
            int cx = getWidth() / 2;
            int cy = getHeight() / 2;

            if (hang >= 1 && hang <= 3) {
                Color m = (hang == 1) ? VANG : (hang == 2) ? BAC : DONG;
                int d = Math.min(getHeight() - 10, 26);
                g2.setColor(m);
                g2.fillOval(cx - d / 2, cy - d / 2, d, d);
                g2.setFont(new Font("SansSerif", Font.BOLD, 13));
                g2.setColor(Color.WHITE);
                int w = g2.getFontMetrics().stringWidth(s);
                int h = g2.getFontMetrics().getAscent();
                g2.drawString(s, cx - w / 2, cy + h / 2 - 1);
            } else {
                g2.setFont(F_O);
                g2.setColor(CHU_MO);
                int w = g2.getFontMetrics().stringWidth(s);
                int h = g2.getFontMetrics().getAscent();
                g2.drawString(s, cx - w / 2, cy + h / 2 - 1);
            }
            g2.dispose();
        }
    }

    /** Cot "Ty le thang": ve thanh mot thanh tien trinh kem so phan tram. */
    private static final class VeTyLe extends JPanel implements TableCellRenderer {
        private int phanTram;
        private boolean coTran;

        VeTyLe() {
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel,
                                                       boolean focus, int r, int c) {
            String s = String.valueOf(v).replace("%", "").trim();
            try {
                phanTram = (int) Math.round(Double.parseDouble(s));
            } catch (NumberFormatException e) {
                phanTram = 0;
            }
            // Cot 4 la tong tran; neu chua danh tran nao thi khong ve thanh.
            Object tong = t.getValueAt(r, 4);
            coTran = !"0".equals(String.valueOf(tong));
            setBackground(nenHang(t, r, sel, false));
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int lePhai = 62;              // cho chu phan tram
            int x = 12;
            int w = Math.max(10, getWidth() - x - lePhai);
            int h = 8;
            int y = (getHeight() - h) / 2;

            if (coTran) {
                g2.setColor(new Color(0xE2, 0xE8, 0xF0));
                g2.fill(new RoundRectangle2D.Float(x, y, w, h, h, h));

                int wd = Math.round(w * Math.min(100, Math.max(0, phanTram)) / 100f);
                if (wd > 0) {
                    Color m = (phanTram >= 60) ? LUC : (phanTram >= 40) ? XANH : DO;
                    g2.setColor(m);
                    g2.fill(new RoundRectangle2D.Float(x, y, Math.max(h, wd), h, h, h));
                }
            }

            g2.setFont(F_O_DAM);
            g2.setColor(coTran ? CHU : CHU_MO);
            String s = coTran ? phanTram + "%" : "--";
            int sw = g2.getFontMetrics().stringWidth(s);
            int sh = g2.getFontMetrics().getAscent();
            g2.drawString(s, getWidth() - 14 - sw, getHeight() / 2 + sh / 2 - 1);
            g2.dispose();
        }
    }

    /** Cot "Ket qua" trong lich su: ve thanh mot chip bo goc co mau. */
    private static final class VeChip extends JPanel implements TableCellRenderer {
        private String chu = "";
        private boolean thang;

        VeChip() {
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel,
                                                       boolean focus, int r, int c) {
            chu = String.valueOf(v);
            thang = "THẮNG".equals(chu);
            setBackground(nenHang(t, r, sel, false));
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setFont(new Font("SansSerif", Font.BOLD, 12));
            int tw = g2.getFontMetrics().stringWidth(chu);
            int w = tw + 26;
            int h = 24;
            int x = (getWidth() - w) / 2;
            int y = (getHeight() - h) / 2;

            g2.setColor(thang ? LUC_NHAT : DO_NHAT);
            g2.fill(new RoundRectangle2D.Float(x, y, w, h, h, h));
            g2.setColor(thang ? LUC : DO);
            g2.setStroke(new BasicStroke(1.2f));
            g2.draw(new RoundRectangle2D.Float(x, y, w - 1, h - 1, h, h));

            int sh = g2.getFontMetrics().getAscent();
            g2.drawString(chu, x + 13, y + h / 2 + sh / 2 - 1);
            g2.dispose();
        }
    }

    /**
     * Cot "Thoi gian": tach thanh hai dong - ngay o tren (nho, mo), gio o duoi
     * (to, ro). Neu chuoi khong co dau cach thi ve nguyen mot dong.
     */
    private static final class VeThoiGian extends JPanel implements TableCellRenderer {
        private String ngay = "";
        private String gio = "";

        VeThoiGian() {
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel,
                                                       boolean focus, int r, int c) {
            String s = String.valueOf(v).trim();
            int k = s.indexOf(' ');
            if (k < 0) {
                k = s.indexOf('T');   // phong khi la dang ISO 2026-10-15T17:47
            }
            if (k > 0 && k < s.length() - 1) {
                ngay = s.substring(0, k);
                gio = s.substring(k + 1);
            } else {
                ngay = "";
                gio = s;
            }
            setBackground(nenHang(t, r, sel, false));
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int x = 14;
            if (ngay.isEmpty()) {
                g2.setFont(F_O);
                g2.setColor(CHU);
                g2.drawString(gio, x, getHeight() / 2 + 5);
            } else {
                g2.setFont(F_NHO);
                g2.setColor(CHU_MO);
                g2.drawString(ngay, x, getHeight() / 2 - 3);

                g2.setFont(F_O_DAM);
                g2.setColor(CHU);
                g2.drawString(gio, x, getHeight() / 2 + 14);
            }
            g2.dispose();
        }
    }

    // =====================================================================
    //  2. KHUNG CHUNG CHO CA HAI HOP THOAI
    // =====================================================================

    /** Bang chi de xem, khong sua duoc o nao. */
    private static JTable taoBang(DefaultTableModel model) {
        JTable t = new JTable(model) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        t.setRowHeight(38);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setFillsViewportHeight(true);
        t.setBackground(NEN_THE);
        t.setFont(F_O);
        t.setForeground(CHU);

        JTableHeader h = t.getTableHeader();
        h.setFont(F_HEADER);
        h.setReorderingAllowed(false);
        h.setResizingAllowed(false);
        h.setPreferredSize(new Dimension(10, 36));
        h.setDefaultRenderer(new VeHeader());
        return t;
    }

    /** Man hinh hien khi bang khong co dong nao. */
    private static JPanel manHinhTrong(String chinh, String phu) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(NEN_THE);

        JPanel hop = new JPanel();
        hop.setOpaque(false);
        hop.setLayout(new BoxLayout(hop, BoxLayout.Y_AXIS));

        JLabel l1 = new JLabel(chinh, SwingConstants.CENTER);
        l1.setFont(new Font("SansSerif", Font.BOLD, 15));
        l1.setForeground(CHU_MO);
        l1.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel l2 = new JLabel(phu, SwingConstants.CENTER);
        l2.setFont(F_GHI_CHU);
        l2.setForeground(new Color(0x9C, 0xA3, 0xAF));
        l2.setAlignmentX(Component.CENTER_ALIGNMENT);

        hop.add(l1);
        hop.add(Box.createVerticalStrut(6));
        hop.add(l2);
        p.add(hop, new GridBagConstraints());
        return p;
    }

    /**
     * Dung phan noi dung: tieu de - bang (hoac man hinh trong) - ghi chu.
     * Tra ve panel da bo cuc xong, chua co nut OK.
     */
    private static JPanel khung(String tieuDe, String phuDe, JTable bang,
                                String ghiChu, String trongChinh, String trongPhu,
                                int rong, int cao) {
        JPanel goc = new JPanel(new BorderLayout(0, 0));
        goc.setBackground(NEN_THE);
        goc.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        // --- phan dau ---
        JPanel dau = new JPanel();
        dau.setOpaque(false);
        dau.setLayout(new BoxLayout(dau, BoxLayout.Y_AXIS));
        dau.setBorder(BorderFactory.createEmptyBorder(6, 4, 16, 4));

        JLabel nhan = new JLabel(tieuDe, SwingConstants.CENTER);
        nhan.setFont(F_TIEU_DE);
        nhan.setForeground(CHU);
        nhan.setAlignmentX(Component.CENTER_ALIGNMENT);
        dau.add(nhan);

        if (phuDe != null && !phuDe.isEmpty()) {
            JLabel p = new JLabel(phuDe, SwingConstants.CENTER);
            p.setFont(new Font("SansSerif", Font.PLAIN, 12));
            p.setForeground(CHU_MO);
            p.setAlignmentX(Component.CENTER_ALIGNMENT);
            dau.add(Box.createVerticalStrut(4));
            dau.add(p);
        }
        goc.add(dau, BorderLayout.NORTH);

        // --- phan giua: bang hoac man hinh trong ---
        // Chieu cao co gian theo so dong: it tran thi khong de lai mang trang.
        int caoThat = Math.min(cao, Math.max(140, bang.getRowCount() * bang.getRowHeight() + 42));

        JScrollPane scroll = new JScrollPane(bang);
        scroll.setPreferredSize(new Dimension(rong, caoThat));
        scroll.setBorder(BorderFactory.createLineBorder(VIEN, 1, true));
        scroll.getViewport().setBackground(NEN_THE);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        JPanel giua = new JPanel(new CardLayout());
        giua.setOpaque(false);
        giua.setPreferredSize(new Dimension(rong, caoThat));
        giua.add(scroll, "bang");

        JPanel trong = manHinhTrong(trongChinh, trongPhu);
        trong.setBorder(BorderFactory.createLineBorder(VIEN, 1, true));
        giua.add(trong, "trong");

        ((CardLayout) giua.getLayout()).show(giua,
                bang.getRowCount() == 0 ? "trong" : "bang");
        goc.add(giua, BorderLayout.CENTER);

        // --- phan chan: ghi chu ---
        if (ghiChu != null && !ghiChu.isEmpty()) {
            JLabel gc = new JLabel(ghiChu, SwingConstants.CENTER);
            gc.setFont(F_GHI_CHU);
            gc.setForeground(CHU_MO);
            gc.setBorder(BorderFactory.createEmptyBorder(12, 4, 2, 4));
            goc.add(gc, BorderLayout.SOUTH);
        }
        return goc;
    }

    /**
     * Hien hop thoai voi mot nut OK tu ve. Dung {@link JOptionPane} de giu
     * dung hanh vi modal / canh giua cha nhu ban cu, nhung bo day nut mac
     * dinh de thay bang nut rieng.
     */
    private static void hien(Component cha, JPanel noiDung, String tieuDeCuaSo) {
        Nut ok = new Nut("OK", XANH, Color.WHITE);

        JPanel hangNut = new JPanel();
        hangNut.setOpaque(false);
        hangNut.setLayout(new BoxLayout(hangNut, BoxLayout.X_AXIS));
        hangNut.setBorder(BorderFactory.createEmptyBorder(14, 0, 2, 0));
        hangNut.add(Box.createHorizontalGlue());
        hangNut.add(ok);
        hangNut.add(Box.createHorizontalGlue());

        JPanel boc = new JPanel(new BorderLayout());
        boc.setBackground(NEN_THE);
        boc.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
        boc.add(noiDung, BorderLayout.CENTER);
        boc.add(hangNut, BorderLayout.SOUTH);

        JOptionPane pane = new JOptionPane(boc, JOptionPane.PLAIN_MESSAGE,
                JOptionPane.DEFAULT_OPTION, null, new Object[] {}, null);
        pane.setBackground(NEN_THE);
        pane.setBorder(BorderFactory.createEmptyBorder());

        JDialog d = pane.createDialog(cha, tieuDeCuaSo);
        ok.addActionListener(e -> d.dispose());
        d.getRootPane().setDefaultButton(ok);
        d.setResizable(false);
        d.setVisible(true);
        d.dispose();
    }

    // =====================================================================
    //  3. BANG XEP HANG
    // =====================================================================

    /** Hien bang xep hang: hang, ten, thang, thua, tong tran, ty le thang. */
    public static void hienBangXepHang(Component cha, List<PlayerStats> ds) {
        hienBangXepHang(cha, ds, null);
    }

    /**
     * Nhu tren, nhung to sang hang cua nguoi dang dang nhap.
     *
     * @param toi ten nguoi dang dang nhap, co the null neu khong can to sang
     */
    public static void hienBangXepHang(Component cha, List<PlayerStats> ds, String toi) {
        DefaultTableModel model = new DefaultTableModel(
                new Object[] { "Hạng", "Người chơi", "Thắng", "Thua", "Tổng trận", "Tỷ lệ thắng" }, 0);

        int hangCuaToi = -1;
        if (ds != null) {
            int hang = 1;
            for (PlayerStats p : ds) {
                if (toi != null && toi.equalsIgnoreCase(p.username())) {
                    hangCuaToi = hang - 1;
                }
                model.addRow(new Object[] {
                        hang++,
                        p.username(),
                        p.wins(),
                        p.losses(),
                        p.soTran(),
                        String.format("%.0f%%", p.tyLeThang())
                });
            }
        }
        final int rowToi = hangCuaToi;
        java.util.function.IntPredicate laToi = r -> r == rowToi;

        JTable bang = taoBang(model);
        bang.getColumnModel().getColumn(0).setMaxWidth(66);
        bang.getColumnModel().getColumn(0).setPreferredWidth(66);
        bang.getColumnModel().getColumn(2).setMaxWidth(80);
        bang.getColumnModel().getColumn(3).setMaxWidth(80);
        bang.getColumnModel().getColumn(4).setMinWidth(100);
        bang.getColumnModel().getColumn(4).setMaxWidth(110);
        bang.getColumnModel().getColumn(1).setMinWidth(150);
        bang.getColumnModel().getColumn(1).setPreferredWidth(175);
        bang.getColumnModel().getColumn(5).setMinWidth(165);
        bang.getColumnModel().getColumn(5).setPreferredWidth(175);

        bang.getColumnModel().getColumn(0).setCellRenderer(new VeHuyHieu());
        bang.getColumnModel().getColumn(1).setCellRenderer(
                new VeChu(SwingConstants.LEFT, true, laToi));
        bang.getColumnModel().getColumn(2).setCellRenderer(new VeSo(LUC, laToi));
        bang.getColumnModel().getColumn(3).setCellRenderer(new VeSo(DO, laToi));
        bang.getColumnModel().getColumn(4).setCellRenderer(
                new VeChu(SwingConstants.CENTER, false, laToi));
        bang.getColumnModel().getColumn(5).setCellRenderer(new VeTyLe());

        // Khi bang rong thi man hinh trong da noi roi, khong lap lai o ghi chu.
        String ghiChu = model.getRowCount() == 0
                ? null
                : "Sắp theo số trận thắng giảm dần."
                  + (rowToi >= 0 ? "   •   Bạn đang đứng hạng " + (rowToi + 1) + "." : "");

        JPanel noiDung = khung(
                "BẢNG XẾP HẠNG",
                model.getRowCount() == 0 ? null
                        : model.getRowCount() + " người chơi đã có trong hệ thống",
                bang, ghiChu,
                "Chưa có ai đánh trận nào",
                "Hãy tạo phòng và chơi ván đầu tiên.",
                700, 342);

        hien(cha, noiDung, "Bảng xếp hạng");
    }

    // =====================================================================
    //  4. LICH SU DAU
    // =====================================================================

    /** Hien lich su dau cua chinh nguoi dang dang nhap. */
    public static void hienLichSu(Component cha, String toi, List<MatchRecord> ds) {
        DefaultTableModel model = new DefaultTableModel(
                new Object[] { "Thời gian", "Đối thủ", "Kết quả", "Lý do", "Lượt bắn", "Thời lượng" }, 0);

        int thang = 0;
        if (ds != null) {
            for (MatchRecord m : ds) {
                boolean win = m.thangBoi(toi);
                if (win) {
                    thang++;
                }
                model.addRow(new Object[] {
                        m.thoiGianBatDau(),
                        m.doiThuCua(toi),
                        win ? "THẮNG" : "THUA",
                        m.moTaLyDo(),
                        m.shots(),
                        m.soPhut() + " phút"
                });
            }
        }

        JTable bang = taoBang(model);
        // Dat be rong cho tung cot de chu khong bi cat.
        int[] rongCot = { 140, 120, 100, 170, 90, 100 };
        for (int i = 0; i < rongCot.length; i++) {
            bang.getColumnModel().getColumn(i).setPreferredWidth(rongCot[i]);
        }

        bang.getColumnModel().getColumn(0).setCellRenderer(new VeThoiGian());
        bang.getColumnModel().getColumn(1).setCellRenderer(
                new VeChu(SwingConstants.LEFT, true, null));
        bang.getColumnModel().getColumn(2).setCellRenderer(new VeChip());
        bang.getColumnModel().getColumn(3).setCellRenderer(
                new VeChu(SwingConstants.LEFT, false, null));
        bang.getColumnModel().getColumn(4).setCellRenderer(
                new VeChu(SwingConstants.CENTER, false, null));
        bang.getColumnModel().getColumn(5).setCellRenderer(
                new VeChu(SwingConstants.CENTER, false, null));

        int tong = model.getRowCount();
        String ghiChu = tong == 0
                ? null
                : tong + " trận gần nhất   •   thắng " + thang + "   •   thua " + (tong - thang);

        JPanel noiDung = khung(
                "LỊCH SỬ ĐẤU CỦA " + toi.toUpperCase(),
                tong == 0 ? null : "Trận mới nhất ở trên cùng",
                bang, ghiChu,
                "Bạn chưa đánh trận nào",
                "Thắng hay thua đều được ghi lại ở đây.",
                720, 330);

        hien(cha, noiDung, "Lịch sử đấu");
    }
}