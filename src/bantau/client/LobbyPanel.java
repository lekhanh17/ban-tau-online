package bantau.client;

import bantau.common.RoomInfo;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.text.Normalizer;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;

/**
 * DANH SACH PHONG: tao phong moi hoac vao phong dang cho.
 *
 * <h2>Ve phan giao dien</h2>
 *
 * <p>Dung chung bang mau va kieu ve voi {@code LoginPanel} de hai man hinh
 * nhin nhu mot bo. Ba thanh phan tu ve, deu nam trong file nay:
 * <ul>
 *   <li>{@link #paintComponent(Graphics)} - nen chuyen mau bien kem song.</li>
 *   <li>{@link The} - khung trang bo goc co do bong, chua toan bo noi dung.</li>
 *   <li>{@link Nut} - nut bo goc, phan biet nut chinh va nut phu.</li>
 * </ul>
 *
 * <p>Bang phong duoc to lai hoan toan: hang cao hon, hai mau xen ke, tieu de
 * cot co nen rieng, va cot trang thai ve thanh <b>nhan mau</b> thay vi chu
 * tron - nhin mot cai la biet phong nao con vao duoc.
 *
 * <p>Khi chua co phong nao, vung bang doi sang mot man hinh rong co huong
 * dan thay vi de mang xam trong tron - trang thai rong la thu nguoi dung
 * gap dau tien khi moi dang nhap, de trong khien chuong trinh trong nhu bi loi.
 */
public class LobbyPanel extends JPanel {

    /* ===== Bang mau, trung voi LoginPanel ===== */

    private static final Color BIEN_DAM = new Color(0x0D3B54);
    private static final Color BIEN_NHAT = new Color(0x3C8DAD);
    private static final Color CHU_CHINH = new Color(0x12405E);
    private static final Color CHU_PHU = new Color(0x6B7A86);
    private static final Color NUT_CHINH = new Color(0x1F618D);
    private static final Color NUT_PHU_VIEN = new Color(0xA8BAC6);

    private static final Color BANG_VIEN = new Color(0xE2E9EE);
    private static final Color HANG_LE = Color.WHITE;
    private static final Color HANG_CHAN = new Color(0xF7FAFC);
    private static final Color HANG_CHON = new Color(0xD6E9F5);
    private static final Color TIEU_DE_NEN = new Color(0xEDF3F7);

    /* Mau cho nhan trang thai phong */
    private static final Color CHO_NEN = new Color(0xE3F6E9);
    private static final Color CHO_CHU = new Color(0x1E8449);
    private static final Color DAT_NEN = new Color(0xFDF1DC);
    private static final Color DAT_CHU = new Color(0xB9770E);
    private static final Color DANH_NEN = new Color(0xE4EEF7);
    private static final Color DANH_CHU = new Color(0x1F618D);
    private static final Color DUNG_NEN = new Color(0xFCE9E7);
    private static final Color DUNG_CHU = new Color(0xC0392B);

    /* ===== Thanh phan ===== */

    private final ClientMain app;

    private final DefaultTableModel model = new DefaultTableModel(
            new Object[] { "ID", "Tên phòng", "Người chơi", "Trạng thái" }, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);
    private final JLabel welcomeLabel = new JLabel(" ");
    private final JLabel demPhongLabel = new JLabel(" ", SwingConstants.RIGHT);

    /** Doi qua lai giua bang phong va man hinh "chua co phong nao". */
    private final CardLayout vungBangCards = new CardLayout();
    private final JPanel vungBang = new JPanel(vungBangCards);
    private static final String THE_BANG = "bang";
    private static final String THE_RONG = "rong";

    public LobbyPanel(ClientMain app) {
        this.app = app;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        setOpaque(true);

        The the = new The(new BorderLayout(0, 14));
        the.setBorder(BorderFactory.createEmptyBorder(
                The.BONG + 18, The.BONG + 22, The.BONG + 18, The.BONG + 22));

        the.add(buildHeader(), BorderLayout.NORTH);
        the.add(buildVungBang(), BorderLayout.CENTER);
        the.add(buildNut(), BorderLayout.SOUTH);

        add(the, BorderLayout.CENTER);

        // Nhay dup chuot de vao phong cho nhanh.
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    vaoPhongDangChon();
                }
            }
        });
    }

    /* ------------------------------------------------------------------ */
    /* Dung giao dien                                                      */
    /* ------------------------------------------------------------------ */

    private JPanel buildHeader() {
        welcomeLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        welcomeLabel.setForeground(CHU_CHINH);

        JLabel ghiChu = new JLabel("Nhấp đúp vào một phòng để vào nhanh");
        ghiChu.setFont(new Font("SansSerif", Font.PLAIN, 12));
        ghiChu.setForeground(CHU_PHU);

        JPanel trai = new JPanel(new BorderLayout(0, 2));
        trai.setOpaque(false);
        trai.add(welcomeLabel, BorderLayout.NORTH);
        trai.add(ghiChu, BorderLayout.SOUTH);

        demPhongLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        demPhongLabel.setForeground(CHU_PHU);

        JPanel p = new JPanel(new BorderLayout(10, 0));
        p.setOpaque(false);
        p.add(trai, BorderLayout.CENTER);
        p.add(demPhongLabel, BorderLayout.EAST);
        return p;
    }

    private JPanel buildVungBang() {
        dinhKieuBang();

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(new LineBorder(BANG_VIEN, 1, true));
        scroll.getViewport().setBackground(Color.WHITE);

        vungBang.setOpaque(false);
        vungBang.add(scroll, THE_BANG);
        vungBang.add(buildManHinhRong(), THE_RONG);
        vungBangCards.show(vungBang, THE_RONG);
        return vungBang;
    }

    /**
     * Man hinh hien khi danh sach phong rong.
     *
     * <p>Co ve thua nhung day la thu nguoi choi nhin thay dau tien sau khi
     * dang nhap. Mot o xam trong tron rat de bi hieu nham la chuong trinh
     * dang hong hoac dang tai do dang.
     */
    private JPanel buildManHinhRong() {
        JLabel chinh = new JLabel("Chưa có phòng nào", SwingConstants.CENTER);
        chinh.setFont(new Font("SansSerif", Font.BOLD, 16));
        chinh.setForeground(CHU_PHU);

        JLabel phu = new JLabel(
                "Bấm \"Tạo phòng mới\" để mở một ván, hoặc \"Làm mới\" để xem lại danh sách.",
                SwingConstants.CENTER);
        phu.setFont(new Font("SansSerif", Font.PLAIN, 13));
        phu.setForeground(CHU_PHU);

        JPanel giua = new JPanel(new BorderLayout(0, 6));
        giua.setOpaque(false);
        giua.add(chinh, BorderLayout.NORTH);
        giua.add(phu, BorderLayout.CENTER);

        JPanel p = new JPanel(new java.awt.GridBagLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(new LineBorder(BANG_VIEN, 1, true));
        p.add(giua, new java.awt.GridBagConstraints());
        return p;
    }

    private JPanel buildNut() {
        Nut rankButton = new Nut("Bảng xếp hạng", Color.WHITE, CHU_CHINH, NUT_PHU_VIEN);
        Nut historyButton = new Nut("Lịch sử đấu", Color.WHITE, CHU_CHINH, NUT_PHU_VIEN);
        Nut refreshButton = new Nut("Làm mới", Color.WHITE, CHU_CHINH, NUT_PHU_VIEN);
        Nut createButton = new Nut("Tạo phòng mới", Color.WHITE, CHU_CHINH, NUT_PHU_VIEN);
        Nut joinButton = new Nut("Vào phòng", NUT_CHINH, Color.WHITE, null);

        rankButton.setPreferredSize(new Dimension(138, 36));
        historyButton.setPreferredSize(new Dimension(120, 36));
        refreshButton.setPreferredSize(new Dimension(104, 36));
        createButton.setPreferredSize(new Dimension(142, 36));
        joinButton.setPreferredSize(new Dimension(126, 36));

        rankButton.addActionListener(e -> app.doXemBangXepHang());
        historyButton.addActionListener(e -> app.doXemLichSu());
        refreshButton.addActionListener(e -> app.doRefreshRooms());
        createButton.addActionListener(e -> taoPhong());
        joinButton.addActionListener(e -> vaoPhongDangChon());

        // Ben trai la 2 nut xem thong ke tu CSDL, ben phai la nut thao tac phong.
        JPanel thongKe = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        thongKe.setOpaque(false);
        thongKe.add(rankButton);
        thongKe.add(historyButton);

        JPanel thaoTac = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        thaoTac.setOpaque(false);
        thaoTac.add(refreshButton);
        thaoTac.add(createButton);
        thaoTac.add(joinButton);

        JPanel buttons = new JPanel(new BorderLayout());
        buttons.setOpaque(false);
        buttons.add(thongKe, BorderLayout.WEST);
        buttons.add(thaoTac, BorderLayout.EAST);
        return buttons;
    }

    /* ------------------------------------------------------------------ */
    /* To lai bang phong                                                   */
    /* ------------------------------------------------------------------ */

    private void dinhKieuBang() {
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(36);
        table.setFont(new Font("SansSerif", Font.PLAIN, 14));
        table.setForeground(CHU_CHINH);
        table.setBackground(Color.WHITE);
        table.setSelectionBackground(HANG_CHON);
        table.setSelectionForeground(CHU_CHINH);
        table.setGridColor(BANG_VIEN);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setFillsViewportHeight(true);
        table.setFocusable(false);

        // ----- Tieu de cot -----
        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setPreferredSize(new Dimension(0, 34));
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v,
                    boolean chon, boolean focus, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(
                        t, v, chon, focus, r, c);
                l.setFont(new Font("SansSerif", Font.BOLD, 13));
                l.setForeground(CHU_CHINH);
                l.setBackground(TIEU_DE_NEN);
                l.setOpaque(true);
                l.setHorizontalAlignment(c == 1 ? SwingConstants.LEFT : SwingConstants.CENTER);
                l.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, BANG_VIEN),
                        BorderFactory.createEmptyBorder(0, 12, 0, 12)));
                return l;
            }
        });

        // ----- Ba cot thuong -----
        DefaultTableCellRenderer o = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v,
                    boolean chon, boolean focus, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(
                        t, v, chon, focus, r, c);
                l.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                l.setHorizontalAlignment(c == 1 ? SwingConstants.LEFT : SwingConstants.CENTER);
                if (!chon) {
                    // Hai mau xen ke giup mat lan theo dung hang khi bang dai.
                    l.setBackground(r % 2 == 0 ? HANG_LE : HANG_CHAN);
                }
                return l;
            }
        };
        table.getColumnModel().getColumn(0).setCellRenderer(o);
        table.getColumnModel().getColumn(1).setCellRenderer(o);
        table.getColumnModel().getColumn(2).setCellRenderer(o);

        // ----- Cot trang thai ve thanh nhan mau -----
        table.getColumnModel().getColumn(3).setCellRenderer(new NhanTrangThai());

        table.getColumnModel().getColumn(0).setMaxWidth(70);
        table.getColumnModel().getColumn(0).setPreferredWidth(70);
        table.getColumnModel().getColumn(2).setMaxWidth(120);
        table.getColumnModel().getColumn(2).setPreferredWidth(120);
        table.getColumnModel().getColumn(3).setMinWidth(190);
        table.getColumnModel().getColumn(3).setPreferredWidth(230);
    }

    /* ------------------------------------------------------------------ */
    /* Hanh dong                                                           */
    /* ------------------------------------------------------------------ */

    private void taoPhong() {
        String ten = JOptionPane.showInputDialog(this,
                "Tên phòng:", "Tạo phòng", JOptionPane.PLAIN_MESSAGE);
        if (ten != null) {
            app.doCreateRoom(ten.trim());
        }
    }

    private void vaoPhongDangChon() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this,
                    "Hãy chọn một phòng trong danh sách.",
                    "Chưa chọn phòng", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int id = Integer.parseInt(String.valueOf(model.getValueAt(row, 0)));
        app.doJoinRoom(id);
    }

    public void setUsername(String username) {
        welcomeLabel.setText("Xin chào " + username);
    }

    /**
     * Cap nhat bang tu danh sach doi tuong RoomInfo.
     *
     * <p>Ko phai tach chuoi gi ca vi server gui thang doi tuong sang.
     */
    public void updateRooms(List<RoomInfo> danhSach) {
        // Nho lai phong dang chon de chon lai sau khi lam moi.
        int idDangChon = -1;
        int row = table.getSelectedRow();
        if (row >= 0) {
            idDangChon = Integer.parseInt(String.valueOf(model.getValueAt(row, 0)));
        }

        model.setRowCount(0);
        if (danhSach != null) {
            for (RoomInfo r : danhSach) {
                model.addRow(new Object[] {
                        r.id(), r.name(), r.playerCount() + "/2", r.state().moTa() });
            }
        }

        if (idDangChon >= 0) {
            for (int i = 0; i < model.getRowCount(); i++) {
                if (String.valueOf(idDangChon).equals(String.valueOf(model.getValueAt(i, 0)))) {
                    table.setRowSelectionInterval(i, i);
                    break;
                }
            }
        }

        int soPhong = model.getRowCount();
        demPhongLabel.setText(soPhong == 0 ? " " : soPhong + " phòng đang mở");
        vungBangCards.show(vungBang, soPhong == 0 ? THE_RONG : THE_BANG);
    }

    /* ------------------------------------------------------------------ */
    /* Nen man hinh                                                        */
    /* ------------------------------------------------------------------ */

    /** Ve nen chuyen mau bien kem vai duong song mo, giong man hinh dang nhap. */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        g2.setPaint(new GradientPaint(0, 0, BIEN_NHAT, 0, h, BIEN_DAM));
        g2.fillRect(0, 0, w, h);

        g2.setColor(new Color(255, 255, 255, 22));
        g2.setStroke(new BasicStroke(2f));
        for (int hang = 0; hang < 3; hang++) {
            int y = h - 110 + hang * 40;
            Path2D.Float song = new Path2D.Float();
            song.moveTo(-30, y);
            for (int x = -30; x <= w + 60; x += 60) {
                song.quadTo(x + 15, y - 9, x + 30, y);
                song.quadTo(x + 45, y + 9, x + 60, y);
            }
            g2.draw(song);
        }
        g2.dispose();
    }

    /* ================================================================== */
    /* Cac lop phu tro tu ve                                              */
    /* ================================================================== */

    /**
     * NHAN MAU CHO COT TRANG THAI.
     *
     * <p>Ve mot hinh bo goc co mau nen rieng cho tung trang thai phong, thay
     * vi in chu den tron. Nho vay luot mat qua la biet phong nao con vao
     * duoc ma khong phai doc tung dong.
     *
     * <p><b>Vi sao so khop bang chuoi KHONG DAU:</b> van ban trang thai lay
     * tu {@code RoomState.moTa()} ben goi common. Neu mai sau ai do sua
     * chuoi do - them dau tieng Viet, doi hoa thuong, viet lai cau - thi
     * doi chieu nguyen van se sai ngay. O day chuan hoa ve khong dau, chu
     * thuong, roi tim tu khoa, nen chiu duoc moi cach viet.
     */
    private static final class NhanTrangThai extends JLabel implements TableCellRenderer {

        private Color nen = DANH_NEN;
        private Color vienMau = DANH_CHU;
        private Color nenHang = Color.WHITE;

        NhanTrangThai() {
            setOpaque(false);
            setFont(new Font("SansSerif", Font.BOLD, 12));
            setHorizontalAlignment(SwingConstants.CENTER);
            setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v,
                boolean chon, boolean focus, int r, int c) {

            String chu = v == null ? "" : String.valueOf(v);
            setText(chu);

            String k = khongDau(chu);
            if (k.contains("tam dung")) {
                nen = DUNG_NEN;
                vienMau = DUNG_CHU;
            } else if (k.contains("dat tau")) {
                nen = DAT_NEN;
                vienMau = DAT_CHU;
            } else if (k.contains("dang danh")) {
                nen = DANH_NEN;
                vienMau = DANH_CHU;
            } else if (k.contains("cho")) {
                nen = CHO_NEN;
                vienMau = CHO_CHU;
            } else {
                nen = TIEU_DE_NEN;
                vienMau = CHU_PHU;
            }

            setForeground(vienMau);
            nenHang = chon ? HANG_CHON : (r % 2 == 0 ? HANG_LE : HANG_CHAN);
            return this;
        }

        /** Bo dau tieng Viet va chuyen chu thuong de so khop cho chac. */
        private static String khongDau(String s) {
            String n = Normalizer.normalize(s, Normalizer.Form.NFD);
            return n.replaceAll("\\p{M}", "")
                    .replace('đ', 'd').replace('Đ', 'D')
                    .toLowerCase();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            // Nen cua ca o, de hai mau hang xen ke khong bi dut doan.
            g2.setColor(nenHang);
            g2.fillRect(0, 0, getWidth(), getHeight());

            // Nhan bo goc nam giua o, chua vua chieu rong chu.
            int cao = Math.min(24, getHeight() - 8);
            int rong = Math.min(getWidth() - 16,
                    getFontMetrics(getFont()).stringWidth(getText()) + 24);
            int x = (getWidth() - rong) / 2;
            int y = (getHeight() - cao) / 2;

            g2.setColor(nen);
            g2.fillRoundRect(x, y, rong, cao, cao, cao);
            g2.setColor(vienMau);
            g2.drawRoundRect(x, y, rong, cao, cao, cao);

            g2.dispose();
            super.paintComponent(g);
        }
    }

    /**
     * THE TRANG BO GOC CO DO BONG.
     *
     * <p>Swing khong co san panel bo goc, nen phai tu ve. Cach lam: ve nhieu
     * hinh chu nhat bo goc mau toi mo chong len nhau, moi lop thu nho lai
     * mot chut, tao thanh vien bong mem; roi ve hinh trang len tren.
     *
     * <p>Vi do bong nam o ria nen phan rong {@link #BONG} diem anh moi canh
     * khong dung duoc de dat noi dung - duong vien rong cua the phai cong
     * them chung ay.
     */
    private static final class The extends JPanel {

        /** Be rong vien bong, tinh bang diem anh. */
        static final int BONG = 9;

        The(LayoutManager lm) {
            super(lm);
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            for (int i = 0; i < BONG; i++) {
                g2.setColor(new Color(6, 30, 46, 7));
                g2.fillRoundRect(i, i + 2, w - 2 * i - 1, h - 2 * i - 3, 24, 24);
            }
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(BONG, BONG, w - 2 * BONG - 1, h - 2 * BONG - 1, 16, 16);

            g2.dispose();
            super.paintComponent(g);
        }
    }

    /**
     * NUT BO GOC TU VE.
     *
     * <p>Hai kieu: nut chinh nen dam chu trang, nut phu nen trang co vien.
     * Nho vay nguoi dung nhin la biet nen bam cai nao truoc.
     *
     * <p>Phai tat {@code setContentAreaFilled} va {@code setBorderPainted},
     * neu khong Swing van ve nen va vien mac dinh cua he dieu hanh de len
     * phan minh tu ve.
     */
    private static final class Nut extends JButton {

        private final Color nen;
        private final Color vien;

        Nut(String chu, Color nen, Color mauChu, Color vien) {
            super(chu);
            this.nen = nen;
            this.vien = vien;
            setForeground(mauChu);
            setFont(new Font("SansSerif", Font.BOLD, 13));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            Color ve = nen;
            if (!isEnabled()) {
                ve = pha(nen, Color.WHITE, 0.55f);
            } else if (getModel().isPressed()) {
                ve = pha(nen, Color.BLACK, 0.15f);
            } else if (getModel().isRollover()) {
                ve = pha(nen, nen.equals(Color.WHITE) ? NUT_CHINH : Color.WHITE, 0.14f);
            }

            g2.setColor(ve);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
            if (vien != null) {
                g2.setColor(vien);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
            }

            g2.dispose();
            super.paintComponent(g);     // de lop cha ve chu len tren
        }

        /** Tron hai mau theo ty le, dung de lam sang hoac toi mot mau. */
        private static Color pha(Color a, Color b, float tyLe) {
            return new Color(
                    Math.round(a.getRed() + (b.getRed() - a.getRed()) * tyLe),
                    Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * tyLe),
                    Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * tyLe));
        }
    }
}