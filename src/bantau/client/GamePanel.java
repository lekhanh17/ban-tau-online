package bantau.client;

import bantau.common.Board;
import bantau.common.FireResult;
import bantau.common.MucDoBot;
import bantau.common.Orientation;
import bantau.common.Packet;
import bantau.common.PacketType;
import bantau.common.Protocol;
import bantau.common.RoomInfo;
import bantau.common.RoomState;
import bantau.common.ShipType;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.JToggleButton;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

/**
 * MAN HINH TRONG PHONG
 *
 * <p>Gom ba giai doan, chuyen qua lai bang {@link CardLayout} o
 * {@code dieuKhienPanel}:
 * <ul>
 *   <li>CHO - dang cho du hai nguoi vao phong.</li>
 *   <li>DAT_TAU - chon tau, xoay huong, xem truoc tren {@code boardCuaMinh}
 *       roi bam chuot de dat (Buoc 8B: {@link BoardView}).</li>
 *   <li>TRAN_DAU - bam vao {@code boardDoiThu} de ban, ket qua tu server
 *       duoc ve lai tren ca hai ban co.</li>
 * </ul>
 *
 * <p>{@code modelCuaMinh} la mot {@link Board} giu ben client, dung de:
 * kiem tra vi tri dat tau hop le truoc khi gui len server (tranh gui rac),
 * va de tu tinh lai ket qua khi doi thu ban trung minh (vi day la ban do
 * that cua minh nen tinh cuc bo se ra dung ket qua server da tinh).
 *
 * <p>Ban nay chi doi phan GIAO DIEN (mau sac, the bo goc, chip trang thai,
 * nhat ky co mau). Toan bo luong xu ly ban tin, dong ho va luat choi giu
 * nguyen nhu ban cu, nen khong can sua bat ky lop nao khac.
 */
public class GamePanel extends JPanel {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static final String THE_CHO = "cho";
    private static final String THE_DAT_TAU = "dat_tau";
    private static final String THE_TRAN_DAU = "tran_dau";

    // ---------------------- Bang mau dung chung ----------------------

    private static final Color NEN        = new Color(0xEE, 0xF3, 0xF8);
    private static final Color THE_TRANG  = new Color(0xFF, 0xFF, 0xFF);
    private static final Color VIEN       = new Color(0xDD, 0xE4, 0xEC);
    private static final Color CHU        = new Color(0x1F, 0x29, 0x37);
    private static final Color CHU_MO     = new Color(0x6B, 0x72, 0x80);
    private static final Color XANH       = new Color(0x1D, 0x4E, 0xD8);
    private static final Color XANH_DAM   = new Color(0x15, 0x3E, 0xAD);
    private static final Color XANH_NHAT  = new Color(0xE8, 0xF0, 0xFE);
    private static final Color LUC        = new Color(0x15, 0x80, 0x3D);
    private static final Color LUC_NHAT   = new Color(0xE3, 0xF6, 0xE9);
    private static final Color DO         = new Color(0xB9, 0x1C, 0x1C);
    private static final Color DO_NHAT    = new Color(0xFD, 0xE8, 0xE8);
    private static final Color CAM        = new Color(0xB4, 0x7B, 0x0B);
    private static final Color CAM_NHAT   = new Color(0xFD, 0xF3, 0xDC);
    private static final Color TIM        = new Color(0x6D, 0x28, 0xD9);

    private static final Font F_TEN_PHONG = new Font("SansSerif", Font.BOLD, 20);
    private static final Font F_NHAN      = new Font("SansSerif", Font.PLAIN, 12);
    private static final Font F_NHAN_DAM  = new Font("SansSerif", Font.BOLD, 14);
    private static final Font F_THUONG    = new Font("SansSerif", Font.PLAIN, 13);
    private static final Font F_NHO       = new Font("SansSerif", Font.PLAIN, 11);

    private final ClientMain app;

    /* ----- phan dau man hinh ----- */
    private final JLabel roomLabel = new JLabel(" ");
    private final Chip chuPhongBadge =
            new Chip("CHỦ PHÒNG", TIM, new Color(0xF1, 0xEA, 0xFD));
    private final JLabel doiThuLabel = new JLabel("-");
    private final Chip chipTrangThai = new Chip("Đang chờ", CAM, CAM_NHAT);
    private final JLabel statusLabel = new JLabel(" ");

    /* ----- nhat ky va chat ----- */
    private final JTextPane logPane = new JTextPane();
    private final JTextField chatField = new JTextField();

    private final JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
    private final CardLayout dieuKhienCards = new CardLayout();
    private final JPanel dieuKhienPanel = new JPanel(dieuKhienCards);

    private final BoardView boardCuaMinh = new BoardView();
    private final BoardView boardDoiThu = new BoardView();

    /** Hai the boc quanh ban co - dung de to sang ben dang duoc phep bam. */
    private TheBanCo theCuaMinh;
    private TheBanCo theDoiThu;

    /* ----- giai doan dat tau ----- */
    /**
     * Ban do cua minh giu ben client. KHONG final vi khi vao lai van sau khi
     * rot mang, server gui ve mot doi tuong Board moi (co san cac vet ban)
     * va ta thay ca doi tuong thay vi dung lai tung o.
     */
    private Board modelCuaMinh = new Board();
    private final Map<ShipType, JToggleButton> nutTau = new EnumMap<>(ShipType.class);
    private final JToggleButton nutXoay = new JToggleButton("Hướng: Nằm ngang");
    private final JButton nutNgauNhien = new Nut("Đặt ngẫu nhiên", THE_TRANG, XANH, true);
    private final JButton nutSanSang = new Nut("Sẵn sàng", LUC, Color.WHITE, false);
    private ShipType tauDangChon = ShipType.CARRIER;
    private Orientation huongDangChon = Orientation.HORIZONTAL;
    private int hoverX = -1;
    private int hoverY = -1;

    /** Co phai luot cua minh khong - dung de mo lai ban co khi phat ban bi tu choi. */
    private boolean luotCuaMinh;

    /* ----- dong ho dem nguoc moi luot ----- */
    private final JLabel dongHoLabel = new JLabel(" ", SwingConstants.CENTER);
    private Timer dongHo;
    private int giayConLai;

    /** Nut goi doi thu may, nam o the "dang cho". */
    private final JButton nutGoiMay = new Nut("Chơi với máy", XANH, Color.WHITE, false);

    /** Chon muc do kho cho doi thu may. */
    private final JComboBox<MucDoBot> chonMucDo =
            new JComboBox<>(MucDoBot.values());

    /** Thong tin phong, giu rieng tung manh de dung lai phan dau man hinh. */
    private String maPhong = "";
    private String tenPhongGoc = "";
    private boolean laChuPhong;
    private String doiThu = "-";

    public GamePanel(ClientMain app) {
        this.app = app;
        setLayout(new BorderLayout(12, 12));
        setBackground(NEN);
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        centerPanel.setOpaque(false);
        dieuKhienPanel.setOpaque(false);

        add(buildHeader(), BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(buildChat(), BorderLayout.EAST);
        add(buildControls(), BorderLayout.SOUTH);

        centerPanel.add(buildBoardsRow(), BorderLayout.CENTER);
        centerPanel.add(bocThe(dieuKhienPanel), BorderLayout.SOUTH);

        dieuKhienPanel.add(buildTheCho(), THE_CHO);
        dieuKhienPanel.add(buildTheDatTau(), THE_DAT_TAU);
        dieuKhienPanel.add(buildTheTranDau(), THE_TRAN_DAU);
        dieuKhienCards.show(dieuKhienPanel, THE_CHO);

        noiSuKienBanCo();
    }

    // =====================================================================
    //  CAC THANH PHAN VE DUNG CHUNG
    // =====================================================================

    /** Nut bo goc phang, co hieu ung ro chuot. */
    private static class Nut extends JButton {
        private final Color nen;
        private final boolean coVien;

        Nut(String text, Color nen, Color chu, boolean coVien) {
            super(text);
            this.nen = nen;
            this.coVien = coVien;
            setFont(F_NHAN_DAM);
            setForeground(chu);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setBorder(BorderFactory.createEmptyBorder(9, 20, 9, 20));
            setCursor(new Cursor(Cursor.HAND_CURSOR));
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
            if (!isEnabled()) {
                c = pha(nen, NEN, 0.62f);
            } else if (getModel().isPressed()) {
                c = pha(nen, Color.BLACK, 0.18f);
            } else if (getModel().isRollover()) {
                c = pha(nen, coVien ? XANH_NHAT : Color.WHITE, 0.16f);
            }
            g2.setColor(c);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 11, 11));

            if (coVien) {
                g2.setColor(isEnabled() ? XANH : VIEN);
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1f, getHeight() - 1f, 11, 11));
            }
            g2.dispose();

            Color chuGoc = getForeground();
            if (!isEnabled()) {
                setForeground(CHU_MO);
            }
            super.paintComponent(g);
            setForeground(chuGoc);
        }
    }

    /** Nut chon tau: bat/tat, khi bat thi to mau xanh. */
    private static final class NutTau extends JToggleButton {
        NutTau(String text) {
            super(text);
            setFont(new Font("SansSerif", Font.PLAIN, 12));
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setBorder(BorderFactory.createEmptyBorder(7, 13, 7, 13));
            setCursor(new Cursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            boolean chon = isSelected();
            boolean daDat = getText().contains("đã đặt");

            Color nen = chon ? XANH : (daDat ? LUC_NHAT : THE_TRANG);
            if (!isEnabled()) {
                nen = NEN;
            }
            g2.setColor(nen);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 9, 9));

            g2.setColor(chon ? XANH_DAM : (daDat ? LUC : VIEN));
            g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1f, getHeight() - 1f, 9, 9));
            g2.dispose();

            setForeground(!isEnabled() ? CHU_MO
                    : chon ? Color.WHITE : (daDat ? LUC : CHU));
            super.paintComponent(g);
        }
    }

    /** Chip bo tron hien mot trang thai ngan, co mau nen rieng. */
    private static final class Chip extends JLabel {
        private Color vien;
        private Color nen;

        Chip(String text, Color vien, Color nen) {
            super(text);
            this.vien = vien;
            this.nen = nen;
            setFont(new Font("SansSerif", Font.BOLD, 12));
            setBorder(BorderFactory.createEmptyBorder(5, 13, 5, 13));
            setForeground(vien);
        }

        void doiMau(String text, Color vien, Color nen) {
            setText(text);
            this.vien = vien;
            this.nen = nen;
            setForeground(vien);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(nen);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), getHeight(), getHeight()));
            g2.setColor(vien);
            g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1f, getHeight() - 1f,
                    getHeight(), getHeight()));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** The trang bo goc, dung thay cho TitledBorder kieu Swing cu. */
    private static class The extends JPanel {
        The() {
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(0, 0, 0, 10));
            g2.fill(new RoundRectangle2D.Float(1, 2, getWidth() - 2f, getHeight() - 2f, 14, 14));
            g2.setColor(THE_TRANG);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 2f, getHeight() - 3f, 14, 14));
            g2.setColor(VIEN);
            g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 3f, getHeight() - 4f, 14, 14));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /**
     * The boc quanh mot ban co. Khi {@code sangLen} bat thi vien doi mau
     * nhan - dung de bao "ban co nay dang bam duoc".
     */
    private static final class TheBanCo extends JPanel {
        private final Color mauNhan;
        private boolean sangLen;

        TheBanCo(Color mauNhan) {
            this.mauNhan = mauNhan;
            setOpaque(false);
            setLayout(new BorderLayout(0, 10));
            setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        }

        void setSangLen(boolean b) {
            if (sangLen != b) {
                sangLen = b;
                repaint();
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            float w = getWidth() - 2f;
            float h = getHeight() - 3f;

            if (sangLen) {
                g2.setColor(new Color(mauNhan.getRed(), mauNhan.getGreen(), mauNhan.getBlue(), 38));
                g2.fill(new RoundRectangle2D.Float(0, 0, w + 2, h + 2, 16, 16));
            }
            g2.setColor(new Color(0, 0, 0, 10));
            g2.fill(new RoundRectangle2D.Float(1, 2, w, h, 14, 14));
            g2.setColor(THE_TRANG);
            g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 14, 14));

            g2.setColor(sangLen ? mauNhan : VIEN);
            g2.setStroke(new java.awt.BasicStroke(sangLen ? 2f : 1f));
            g2.draw(new RoundRectangle2D.Float(1f, 1f, w - 2f, h - 2f, 14, 14));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Boc mot panel bat ky vao trong mot the trang. */
    private static JPanel bocThe(JPanel ruot) {
        The t = new The();
        t.setLayout(new BorderLayout());
        ruot.setOpaque(false);
        t.add(ruot, BorderLayout.CENTER);
        return t;
    }

    // =====================================================================
    //  PHAN DAU MAN HINH
    // =====================================================================

    private JPanel buildHeader() {
        The the = new The();
        the.setLayout(new BorderLayout(12, 0));
        the.setBorder(BorderFactory.createEmptyBorder(14, 18, 16, 18));

        // --- ben trai: ten phong + huy hieu chu phong + trang thai ---
        JPanel trai = new JPanel();
        trai.setOpaque(false);
        trai.setLayout(new BoxLayout(trai, BoxLayout.Y_AXIS));

        JPanel hangTen = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        hangTen.setOpaque(false);
        roomLabel.setFont(F_TEN_PHONG);
        roomLabel.setForeground(CHU);

        chuPhongBadge.setFont(new Font("SansSerif", Font.BOLD, 10));
        chuPhongBadge.setBorder(BorderFactory.createEmptyBorder(4, 11, 4, 11));
        chuPhongBadge.setVisible(false);

        hangTen.add(roomLabel);
        hangTen.add(chuPhongBadge);
        hangTen.add(chipTrangThai);
        hangTen.setAlignmentX(LEFT_ALIGNMENT);

        statusLabel.setFont(F_THUONG);
        statusLabel.setForeground(CHU_MO);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 4, 0, 0));
        statusLabel.setAlignmentX(LEFT_ALIGNMENT);

        trai.add(hangTen);
        trai.add(statusLabel);

        // --- ben phai: khoi doi thu ---
        JPanel phai = new JPanel();
        phai.setOpaque(false);
        phai.setLayout(new BoxLayout(phai, BoxLayout.Y_AXIS));

        JLabel nhanDoiThu = new JLabel("ĐỐI THỦ");
        nhanDoiThu.setFont(new Font("SansSerif", Font.BOLD, 10));
        nhanDoiThu.setForeground(CHU_MO);
        nhanDoiThu.setAlignmentX(RIGHT_ALIGNMENT);

        doiThuLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        doiThuLabel.setForeground(CHU);
        doiThuLabel.setAlignmentX(RIGHT_ALIGNMENT);

        phai.add(nhanDoiThu);
        phai.add(Box.createVerticalStrut(3));
        phai.add(doiThuLabel);

        the.add(trai, BorderLayout.CENTER);
        the.add(phai, BorderLayout.EAST);
        return the;
    }

    /** Doi mau chip theo trang thai phong. */
    private void capNhatChip(RoomState tt) {
        if (tt == null) {
            chipTrangThai.doiMau("Đang chờ", CAM, CAM_NHAT);
            return;
        }
        switch (tt) {
            case PLAYING -> chipTrangThai.doiMau("Đang thi đấu", LUC, LUC_NHAT);
            case PLACING -> chipTrangThai.doiMau("Đang đặt tàu", XANH, XANH_NHAT);
            case PAUSED  -> chipTrangThai.doiMau("Tạm dừng", DO, DO_NHAT);
            default      -> chipTrangThai.doiMau("Đang chờ", CAM, CAM_NHAT);
        }
    }

    // =====================================================================
    //  NHAT KY VA CHAT
    // =====================================================================

    private JPanel buildChat() {
        The the = new The();
        the.setLayout(new BorderLayout(0, 10));
        the.setPreferredSize(new Dimension(330, 200));

        JLabel tieuDe = new JLabel("NHẬT KÝ & CHAT");
        tieuDe.setFont(new Font("SansSerif", Font.BOLD, 11));
        tieuDe.setForeground(CHU_MO);

        logPane.setEditable(false);
        logPane.setBackground(THE_TRANG);
        logPane.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        logPane.setFont(F_THUONG);

        JScrollPane scroll = new JScrollPane(logPane);
        scroll.setBorder(BorderFactory.createLineBorder(VIEN, 1, true));
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getViewport().setBackground(THE_TRANG);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        // --- o nhap + nut gui ---
        chatField.setFont(F_THUONG);
        chatField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(VIEN, 1, true),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));

        JButton sendButton = new Nut("Gửi", XANH, Color.WHITE, false);
        sendButton.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        sendButton.addActionListener(e -> guiChat());
        chatField.addActionListener(e -> guiChat());

        JPanel bottom = new JPanel(new BorderLayout(8, 0));
        bottom.setOpaque(false);
        bottom.add(chatField, BorderLayout.CENTER);
        bottom.add(sendButton, BorderLayout.EAST);

        the.add(tieuDe, BorderLayout.NORTH);
        the.add(scroll, BorderLayout.CENTER);
        the.add(bottom, BorderLayout.SOUTH);
        return the;
    }

    /**
     * Them mot dong vao nhat ky. Gio duoc to xam nho, noi dung to dam hon,
     * tin chat cua nguoi choi mang mau khac tin he thong.
     */
    private void themDong(String gio, String ten, String noiDung, Color mauChu) {
        StyledDocument d = logPane.getStyledDocument();

        SimpleAttributeSet sGio = new SimpleAttributeSet();
        StyleConstants.setForeground(sGio, new Color(0x9C, 0xA3, 0xAF));
        StyleConstants.setFontSize(sGio, 11);
        StyleConstants.setFontFamily(sGio, "SansSerif");

        SimpleAttributeSet sTen = new SimpleAttributeSet();
        StyleConstants.setForeground(sTen, XANH);
        StyleConstants.setBold(sTen, true);
        StyleConstants.setFontSize(sTen, 13);
        StyleConstants.setFontFamily(sTen, "SansSerif");

        SimpleAttributeSet sChu = new SimpleAttributeSet();
        StyleConstants.setForeground(sChu, mauChu);
        StyleConstants.setFontSize(sChu, 13);
        StyleConstants.setFontFamily(sChu, "SansSerif");

        try {
            d.insertString(d.getLength(), gio + "  ", sGio);
            if (ten != null && !ten.isEmpty()) {
                d.insertString(d.getLength(), ten + ": ", sTen);
            }
            d.insertString(d.getLength(), noiDung + "\n", sChu);
        } catch (BadLocationException ignore) {
            // Chi la nhat ky hien thi, loi chen chu khong anh huong van dau.
        }
        logPane.setCaretPosition(d.getLength());
    }

    // =====================================================================
    //  THANH DUOI VA HAI BAN CO
    // =====================================================================

    private JPanel buildControls() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        p.setOpaque(false);
        JButton leaveButton = new Nut("Rời phòng", DO_NHAT, DO, false);
        leaveButton.addActionListener(e -> app.doLeaveRoom());
        p.add(leaveButton);
        return p;
    }

    /** Hai ban co dat canh nhau: ben trai cua minh, ben phai cua doi thu. */
    private JPanel buildBoardsRow() {
        JPanel row = new JPanel(new GridLayout(1, 2, 14, 0));
        row.setOpaque(false);
        theCuaMinh = boardSlot("BÀN CỜ CỦA BẠN", "Tàu của bạn nằm ở đây", boardCuaMinh, LUC);
        theDoiThu = boardSlot("BÀN CỜ CỦA ĐỐI THỦ", "Bấm vào đây để bắn", boardDoiThu, XANH);
        row.add(theCuaMinh);
        row.add(theDoiThu);
        return row;
    }

    private TheBanCo boardSlot(String tieuDe, String phuDe, BoardView view, Color mauNhan) {
        TheBanCo khung = new TheBanCo(mauNhan);

        JPanel dau = new JPanel(new BorderLayout());
        dau.setOpaque(false);

        JLabel t = new JLabel(tieuDe);
        t.setFont(new Font("SansSerif", Font.BOLD, 12));
        t.setForeground(mauNhan);

        JLabel p = new JLabel(phuDe, SwingConstants.RIGHT);
        p.setFont(F_NHO);
        p.setForeground(CHU_MO);

        dau.add(t, BorderLayout.WEST);
        dau.add(p, BorderLayout.EAST);

        JPanel giua = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        giua.setOpaque(false);
        giua.add(view);

        khung.add(dau, BorderLayout.NORTH);
        khung.add(giua, BorderLayout.CENTER);
        return khung;
    }

    // =====================================================================
    //  BA THE DIEU KHIEN
    // =====================================================================

    /**
     * The "dang cho": mot dong chu, o chon muc do kho va nut goi doi thu may.
     *
     * <p>Nut nay chi gui ban tin ADD_BOT kem muc do. Viec khoi dong bot do
     * server lam, va bot ket noi nguoc lai nhu mot client binh thuong -
     * client Swing khong he biet gi them ve no, cung khong biet thuat toan
     * nao dang chay ben trong bot.
     */
    private JPanel buildTheCho() {
        JLabel nhan = new JLabel("Đang chờ đối thủ vào phòng...", SwingConstants.CENTER);
        nhan.setFont(new Font("SansSerif", Font.BOLD, 14));
        nhan.setForeground(CHU);
        nhan.setAlignmentX(CENTER_ALIGNMENT);

        JLabel phu = new JLabel("Không muốn đợi? Gọi một đối thủ máy vào chơi ngay.",
                SwingConstants.CENTER);
        phu.setFont(F_NHAN);
        phu.setForeground(CHU_MO);
        phu.setAlignmentX(CENTER_ALIGNMENT);

        // O chon muc do: dat be rong du de khong bi cat chu "Thường".
        chonMucDo.setSelectedItem(MucDoBot.THUONG);
        chonMucDo.setFont(F_THUONG);
        chonMucDo.setPreferredSize(new Dimension(150, 36));
        chonMucDo.setMaximumSize(new Dimension(150, 36));
        chonMucDo.setBackground(THE_TRANG);
        chonMucDo.setFocusable(false);
        chonMucDo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> l, Object v, int i,
                                                          boolean sel, boolean focus) {
                super.getListCellRendererComponent(l, v, i, sel, focus);
                if (v instanceof MucDoBot m) {
                    setText(m.ten());
                }
                setBorder(BorderFactory.createEmptyBorder(5, 9, 5, 9));
                return this;
            }
        });
        capNhatChuGiaiMucDo();
        chonMucDo.addActionListener(e -> capNhatChuGiaiMucDo());

        nutGoiMay.setPreferredSize(new Dimension(200, 36));
        nutGoiMay.setToolTipText("Không đợi đối thủ nữa, gọi máy vào chơi ngay");
        nutGoiMay.addActionListener(e -> goiDoiThuMay());

        JLabel nhanMuc = new JLabel("Mức độ:");
        nhanMuc.setFont(F_THUONG);
        nhanMuc.setForeground(CHU_MO);

        JPanel hangNut = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        hangNut.setOpaque(false);
        hangNut.add(nhanMuc);
        hangNut.add(chonMucDo);
        hangNut.add(nutGoiMay);
        hangNut.setAlignmentX(CENTER_ALIGNMENT);

        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.add(nhan);
        p.add(Box.createVerticalStrut(4));
        p.add(phu);
        p.add(Box.createVerticalStrut(12));
        p.add(hangNut);
        return p;
    }

    /** Hien mo ta cua muc do dang chon lam tooltip cua o chon. */
    private void capNhatChuGiaiMucDo() {
        MucDoBot m = mucDoDangChon();
        chonMucDo.setToolTipText(m.ten() + ": " + m.moTa());
    }

    private MucDoBot mucDoDangChon() {
        Object o = chonMucDo.getSelectedItem();
        return o instanceof MucDoBot m ? m : MucDoBot.THUONG;
    }

    /** Gui ADD_BOT va khoa nut lai de khong goi hai con bot vao mot phong. */
    private void goiDoiThuMay() {
        MucDoBot m = mucDoDangChon();
        nutGoiMay.setEnabled(false);
        chonMucDo.setEnabled(false);
        nutGoiMay.setText("Đang gọi đối thủ máy...");
        app.send(Packet.of(PacketType.ADD_BOT, m.name()));
        log("Đã gọi một đối thủ máy mức " + m.ten() + " vào phòng.");
    }

    /** Mo lai nut khi quay ve trang thai cho - vi du bot vua thoat. */
    private void moLaiNutGoiMay() {
        nutGoiMay.setEnabled(true);
        chonMucDo.setEnabled(true);
        nutGoiMay.setText("Chơi với máy");
    }

    private JPanel buildTheDatTau() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JPanel hangTau = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        hangTau.setOpaque(false);
        ButtonGroup nhom = new ButtonGroup();
        for (ShipType t : ShipType.values()) {
            JToggleButton nut = new NutTau(nhanNut(t, false));
            nut.addActionListener(e -> chonTau(t));
            nhom.add(nut);
            hangTau.add(nut);
            nutTau.put(t, nut);
        }
        nutTau.get(tauDangChon).setSelected(true);
        hangTau.setAlignmentX(LEFT_ALIGNMENT);

        JPanel hangNut = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        hangNut.setOpaque(false);
        nutXoay.setFont(F_THUONG);
        nutXoay.setFocusPainted(false);
        nutXoay.setBackground(THE_TRANG);
        nutXoay.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(VIEN, 1, true),
                BorderFactory.createEmptyBorder(8, 14, 8, 14)));
        nutXoay.setCursor(new Cursor(Cursor.HAND_CURSOR));
        nutXoay.addActionListener(e -> xoayHuong());
        nutNgauNhien.addActionListener(e -> datNgauNhien());
        nutSanSang.addActionListener(e -> guiSanSang());
        nutSanSang.setEnabled(false);
        hangNut.add(nutXoay);
        hangNut.add(nutNgauNhien);
        hangNut.add(nutSanSang);
        hangNut.setAlignmentX(LEFT_ALIGNMENT);

        JLabel huongDan = new JLabel(
                "Chọn tàu, rê chuột vào bàn cờ bên trái để xem trước (xanh = hợp lệ,"
                        + " đỏ = không hợp lệ), click chuột trái để đặt.");
        huongDan.setFont(F_NHAN);
        huongDan.setForeground(CHU_MO);
        huongDan.setBorder(BorderFactory.createEmptyBorder(6, 4, 0, 0));
        huongDan.setAlignmentX(LEFT_ALIGNMENT);

        p.add(hangTau);
        p.add(Box.createVerticalStrut(6));
        p.add(hangNut);
        p.add(huongDan);
        return p;
    }

    private JPanel buildTheTranDau() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        dongHoLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        dongHoLabel.setAlignmentX(CENTER_ALIGNMENT);

        JLabel nhan = new JLabel(
                "Đến lượt bạn thì bấm vào bàn cờ của đối thủ (bên phải) để bắn.",
                SwingConstants.CENTER);
        nhan.setFont(F_NHAN);
        nhan.setForeground(CHU_MO);
        nhan.setAlignmentX(CENTER_ALIGNMENT);

        p.add(dongHoLabel);
        p.add(Box.createVerticalStrut(4));
        p.add(nhan);
        return p;
    }

    /* ------------------------------------------------------------------ */
    /* Dong ho dem nguoc moi luot                                          */
    /* ------------------------------------------------------------------ */

    /**
     * Bat dau dem nguoc cho luot moi.
     *
     * <p>Dung {@link javax.swing.Timer} chu KHONG dung {@code Thread.sleep}.
     * Timer cua Swing goi ham xu ly ngay tren EDT nen sua giao dien an toan,
     * con Thread.sleep tren EDT se lam treo ca cua so.
     *
     * <p>Dong ho nay chi de NHIN. Server moi la noi quyet dinh ai mat luot,
     * nen dong ho client co lech vai phan tram giay cung khong sao.
     */
    private void batDauDemNguoc(int giay) {
        dungDongHo();
        giayConLai = giay;
        veDongHo();

        dongHo = new Timer(1000, e -> {
            giayConLai--;
            veDongHo();
            if (giayConLai <= 0) {
                // Het gio roi thi dung dem, cho server bao chuyen luot.
                dungDongHo();
            }
        });
        dongHo.start();
    }

    private void dungDongHo() {
        if (dongHo != null) {
            dongHo.stop();
            dongHo = null;
        }
    }

    private void veDongHo() {
        if (giayConLai <= 0) {
            dongHoLabel.setText("Hết giờ!");
            dongHoLabel.setForeground(DO);
            return;
        }
        dongHoLabel.setText((luotCuaMinh ? "Lượt của bạn: " : "Đối thủ còn: ")
                + giayConLai + " giây");
        // Duoi 10 giay thi chuyen do cho de thay.
        dongHoLabel.setForeground(giayConLai <= 10 ? DO : (luotCuaMinh ? LUC : CHU_MO));
    }

    private void noiSuKienBanCo() {
        boardCuaMinh.setHienTau(true);
        boardCuaMinh.setClickListener(this::datTauTaiO);
        boardCuaMinh.setHoverListener(this::xemTruocTaiO);
        boardCuaMinh.setChoPhepClick(false);

        boardDoiThu.setHienTau(false);
        boardDoiThu.setClickListener(this::banVaoO);
        boardDoiThu.setChoPhepClick(false);
    }

    private void guiChat() {
        String text = chatField.getText().trim();
        if (!text.isEmpty()) {
            app.doChat(text);
            chatField.setText("");
        }
    }

    /* ------------------------------------------------------------------ */
    /* Giai doan dat tau                                                   */
    /* ------------------------------------------------------------------ */

    private String nhanNut(ShipType t, boolean daDat) {
        return t.label() + " (" + t.size() + " ô)" + (daDat ? " - đã đặt" : "");
    }

    private void chonTau(ShipType t) {
        tauDangChon = t;
        capNhatXemTruoc();
    }

    private void xoayHuong() {
        huongDangChon = huongDangChon.flip();
        nutXoay.setText("Hướng: "
                + (huongDangChon == Orientation.HORIZONTAL ? "Nằm ngang" : "Nằm dọc"));
        capNhatXemTruoc();
    }

    private void xemTruocTaiO(int x, int y) {
        hoverX = x;
        hoverY = y;
        capNhatXemTruoc();
    }

    private void capNhatXemTruoc() {
        if (hoverX < 0 || hoverY < 0) {
            boardCuaMinh.boXemTruoc();
            return;
        }
        List<int[]> oCells = Board.cellsOf(tauDangChon, hoverX, hoverY, huongDangChon);
        boolean hopLe = modelCuaMinh.canPlace(tauDangChon, hoverX, hoverY, huongDangChon);
        boardCuaMinh.xemTruoc(oCells, hopLe);
    }

    private void datTauTaiO(int x, int y) {
        if (!modelCuaMinh.place(tauDangChon, x, y, huongDangChon)) {
            return;
        }
        boardCuaMinh.napTuBanDo(modelCuaMinh);
        nutTau.get(tauDangChon).setText(nhanNut(tauDangChon, true));
        chonTauKeTiepChuaDat();
        capNhatXemTruoc();
        nutSanSang.setEnabled(modelCuaMinh.isComplete());
    }

    private void chonTauKeTiepChuaDat() {
        for (ShipType t : ShipType.values()) {
            if (modelCuaMinh.originOf(t) == null) {
                tauDangChon = t;
                nutTau.get(t).setSelected(true);
                return;
            }
        }
    }

    private void datNgauNhien() {
        modelCuaMinh.randomPlace(new Random());
        boardCuaMinh.napTuBanDo(modelCuaMinh);
        for (ShipType t : ShipType.values()) {
            nutTau.get(t).setText(nhanNut(t, true));
        }
        nutSanSang.setEnabled(true);
    }

    private void guiSanSang() {
        if (!modelCuaMinh.isComplete()) {
            return;
        }
        app.send(Packet.withPayload(PacketType.READY, modelCuaMinh));
        setDatTauChoPhep(false);
        setStatus("Đã gửi sơ đồ, đang chờ server nhập lệnh...");
    }

    private void setDatTauChoPhep(boolean choPhep) {
        boardCuaMinh.setChoPhepClick(choPhep);
        for (JToggleButton nut : nutTau.values()) {
            nut.setEnabled(choPhep);
        }
        nutXoay.setEnabled(choPhep);
        nutNgauNhien.setEnabled(choPhep);
        nutSanSang.setEnabled(choPhep && modelCuaMinh.isComplete());
    }

    /** Server tu choi so do (E_BAD_PLACEMENT) - mo lai giao dien dat tau. */
    public void datTauBiTuChoi(String moTa) {
        setDatTauChoPhep(true);
        setStatus("Sơ đồ bị từ chối: " + moTa);
        JOptionPane.showMessageDialog(this, moTa,
                "Sơ đồ không hợp lệ", JOptionPane.WARNING_MESSAGE);
    }

    /* ------------------------------------------------------------------ */
    /* Giai doan tran dau                                                  */
    /* ------------------------------------------------------------------ */

    private void banVaoO(int x, int y) {
        // Chan ngay tai client: o da ban roi thi khong gui goi tin lam gi,
        // server chac chan tu choi bang loi E_ALREADY_SHOT.
        if (boardDoiThu.daBan(x, y)) {
            log("Ô " + tenO(x, y) + " đã bắn rồi, hãy chọn ô khác.");
            return;
        }
        app.send(Packet.of(PacketType.FIRE, String.valueOf(x), String.valueOf(y)));
        // Tat chuot trong luc cho ket qua de tranh bam lien tuc nhieu phat.
        boardDoiThu.setChoPhepClick(false);
    }

    /**
     * Server tu choi phat ban (sai luot hoac o da ban).
     *
     * <p>Phai mo lai ban co, vi luc gui di ta da tat chuot de cho ket qua
     * ma server thi khong gui goi TURN moi trong truong hop nay - khong mo
     * lai thi nguoi choi ngoi nhin, khong bam duoc nua.
     */
    public void banBiTuChoi() {
        if (luotCuaMinh) {
            boardDoiThu.setChoPhepClick(true);
        }
    }

    private String tenO(int x, int y) {
        return "" + (char) ('A' + x) + (y + 1);
    }

    /* ------------------------------------------------------------------ */
    /* Cac su kien tu server (ClientMain goi xuong)                        */
    /* ------------------------------------------------------------------ */

    public void enterRoom(RoomInfo info, boolean laChuPhong) {
        this.maPhong = (info == null) ? "?" : "#" + info.id();
        this.tenPhongGoc = (info == null) ? "" : info.name();
        this.laChuPhong = laChuPhong;
        doiThu = "-";
        capNhatTieuDe();
        capNhatChip(info == null ? null : info.state());
        logPane.setText("");
        log("Đã vào phòng");

        moLaiNutGoiMay();
        dieuKhienCards.show(dieuKhienPanel, THE_CHO);
        modelCuaMinh.clear();
        boardCuaMinh.xoaHet();
        boardDoiThu.xoaHet();
        theCuaMinh.setSangLen(false);
        theDoiThu.setSangLen(false);
    }

    public void updateRoomState(RoomInfo info, String nguoi1, String nguoi2) {
        String me = app.getUsername();
        doiThu = me.equals(nguoi1) ? nguoi2 : nguoi1;
        if (doiThu == null || doiThu.isBlank()) {
            doiThu = "-";
        }
        capNhatTieuDe();
        capNhatChip(info == null ? null : info.state());
        statusLabel.setText("Trạng thái: " + (info == null ? "?" : info.state().moTa()));

        // Phong quay ve WAITING giua chung nghia la doi thu vua roi di.
        // Phai dua giao dien ve trang thai cho, neu khong nguoi con lai se
        // ngoi nhin man hinh dat tau hoac tran dau da chet.
        if (info != null && info.state() == RoomState.WAITING) {
            veTrangThaiCho();
        }
    }

    /** Dua man hinh ve trang thai cho nguoi choi moi vao phong. */
    private void veTrangThaiCho() {
        luotCuaMinh = false;
        dungDongHo();
        dongHoLabel.setText(" ");
        modelCuaMinh.clear();
        boardCuaMinh.xoaHet();
        boardDoiThu.xoaHet();
        boardCuaMinh.setChoPhepClick(false);
        boardDoiThu.setChoPhepClick(false);
        theCuaMinh.setSangLen(false);
        theDoiThu.setSangLen(false);
        moLaiNutGoiMay();
        dieuKhienCards.show(dieuKhienPanel, THE_CHO);
    }

    private void capNhatTieuDe() {
        String ten = tenPhongGoc == null || tenPhongGoc.isBlank()
                ? "Phòng " + maPhong
                : "Phòng " + maPhong + " · " + tenPhongGoc;
        roomLabel.setText(ten);
        chuPhongBadge.setVisible(laChuPhong);
        doiThuLabel.setText(doiThu);
        doiThuLabel.setForeground("-".equals(doiThu) ? CHU_MO : CHU);
    }

    /** PLACE_PHASE: du hai nguoi, bat dau (hoac choi lai) giai doan dat tau. */
    public void batDauDatTau() {
        modelCuaMinh.clear();
        boardCuaMinh.xoaHet();
        boardDoiThu.xoaHet();
        boardDoiThu.setChoPhepClick(false);
        luotCuaMinh = false;
        dungDongHo();
        dongHoLabel.setText(" ");
        hoverX = -1;
        hoverY = -1;
        tauDangChon = ShipType.CARRIER;
        huongDangChon = Orientation.HORIZONTAL;
        nutXoay.setText("Hướng: Nằm ngang");
        for (ShipType t : ShipType.values()) {
            nutTau.get(t).setText(nhanNut(t, false));
        }
        nutTau.get(tauDangChon).setSelected(true);
        setDatTauChoPhep(true);

        theCuaMinh.setSangLen(true);
        theDoiThu.setSangLen(false);
        capNhatChip(RoomState.PLACING);

        dieuKhienCards.show(dieuKhienPanel, THE_DAT_TAU);
        setStatus("Hãy đặt đủ 5 tàu rồi bấm \"Sẵn sàng\".");
        log("Bắt đầu giai đoạn đặt tàu.");
    }

    /** READY_OK: so do cua minh hop le, dang cho doi thu dat xong. */
    public void batDaSanSang() {
        theCuaMinh.setSangLen(false);
        setStatus("Sơ đồ hợp lệ. Đang chờ đối thủ đặt tàu xong...");
        log("Sơ đồ đã được chấp nhận.");
    }

    /** GAME_START: ca hai da san sang, van dau bat dau. */
    public void batDauVanDau(String nguoiDiTruoc) {
        dieuKhienCards.show(dieuKhienPanel, THE_TRAN_DAU);
        boardCuaMinh.setChoPhepClick(false);
        capNhatChip(RoomState.PLAYING);
        log("Ván đấu bắt đầu. Người đi trước: " + nguoiDiTruoc);
    }

    /** TURN: server bao den luot ai. */
    public void capNhatLuot(String tenNguoiDangDanh, int giaySuyNghi) {
        luotCuaMinh = app.getUsername().equals(tenNguoiDangDanh);
        boardDoiThu.setChoPhepClick(luotCuaMinh);
        theDoiThu.setSangLen(luotCuaMinh);
        theCuaMinh.setSangLen(!luotCuaMinh);
        capNhatChip(RoomState.PLAYING);
        setStatus(luotCuaMinh
                ? "Đến lượt bạn - bấm vào bàn cờ đối thủ để bắn."
                : "Đối thủ (" + tenNguoiDangDanh + ") đang bắn...");
        batDauDemNguoc(giaySuyNghi);
    }

    /** FIRE_RESULT: ket qua phat ban CUA MINH vao ban co doi thu. */
    public void ketQuaBanCuaMinh(int x, int y, String ketQua, String maTau) {
        boardDoiThu.danhDauO(x, y, doiMark(ketQua));
        String toa = tenO(x, y);
        if (FireResult.SUNK.name().equals(ketQua)) {
            log("Bắn " + toa + ": chìm tàu " + tenTau(maTau) + " của đối thủ!");
        } else {
            log("Bắn " + toa + ": " + FireResult.valueOf(ketQua).moTa());
        }
    }

    /** INCOMING: doi thu vua ban vao ban co cua minh. */
    public void doiThuBanTrung(int x, int y, String ketQua, String maTau) {
        modelCuaMinh.fire(x, y);
        boardCuaMinh.napTuBanDo(modelCuaMinh);
        String toa = tenO(x, y);
        if (FireResult.SUNK.name().equals(ketQua)) {
            log("Đối thủ bắn " + toa + ": chìm tàu " + tenTau(maTau) + " của bạn!");
        } else {
            log("Đối thủ bắn " + toa + ": " + FireResult.valueOf(ketQua).moTa());
        }
    }

    /* ------------------------------------------------------------------ */
    /* Rot mang va vao lai van                                             */
    /* ------------------------------------------------------------------ */

    /**
     * OPPONENT_LOST: doi thu vua rot mang, van dau tam dung.
     *
     * <p>Phai tat ban co ngay. Server cung chan phat ban trong trang thai
     * PAUSED, nhung de nguoi choi bam duoc roi nhan loi thi rat kho hieu.
     */
    public void doiThuMatKetNoi(String tenDoiThu, int giayAnHan) {
        dungDongHo();
        luotCuaMinh = false;
        boardDoiThu.setChoPhepClick(false);
        theDoiThu.setSangLen(false);
        theCuaMinh.setSangLen(false);
        capNhatChip(RoomState.PAUSED);

        log(tenDoiThu + " mất kết nối. Ván đấu tạm dừng, chờ họ vào lại trong "
                + giayAnHan + " giây.");
        setStatus("Tạm dừng - chờ " + tenDoiThu + " vào lại...");
        batDauDemAnHan(tenDoiThu, giayAnHan);
    }

    /**
     * Dem nguoc thoi gian an han tren dong ho, de nguoi choi con lai biet
     * phai cho bao lau nua.
     *
     * <p>Dong ho nay cung chi de NHIN nhu dong ho luot: server moi la noi
     * quyet dinh khi nao het han va xu thang.
     */
    private void batDauDemAnHan(String tenDoiThu, int giay) {
        dungDongHo();
        giayConLai = giay;
        veDongHoAnHan(tenDoiThu);

        dongHo = new Timer(1000, e -> {
            giayConLai--;
            veDongHoAnHan(tenDoiThu);
            if (giayConLai <= 0) {
                dungDongHo();
            }
        });
        dongHo.start();
    }

    private void veDongHoAnHan(String tenDoiThu) {
        dongHoLabel.setForeground(CAM);
        dongHoLabel.setText(giayConLai > 0
                ? "Chờ " + tenDoiThu + " vào lại: " + giayConLai + " giây"
                : "Hết thời gian chờ");
    }

    /** OPPONENT_BACK: doi thu vao lai duoc. Server se gui TURN ngay sau. */
    public void doiThuVaoLai(String tenDoiThu) {
        dungDongHo();
        capNhatChip(RoomState.PLAYING);
        log(tenDoiThu + " đã vào lại. Ván đấu tiếp tục.");
        setStatus("Ván đấu tiếp tục.");
    }

    /**
     * RESUME_DATA: CHINH MINH vua vao lai van dang do - ve lai toan bo.
     *
     * <p>Client nay vua khoi dong lai nen trong khong: khong biet tau cua
     * minh o dau, khong biet da ban nhung o nao. Tat ca du lieu o day den
     * tu server, la ban sao duy nhat co gia tri.
     *
     * @param cuaMinh ban do that cua minh, ke ca cac o da bi ban
     * @param maDoiThu chuoi 100 ky tu: nhung gi minh DA BIET ve ban do doi thu
     * @param tenDoiThu ten doi thu
     * @param tenDangDanh ten nguoi dang giu luot
     * @param giayConLai so giay con lai cua luot do
     */
    public void vaoLaiVan(Board cuaMinh, String maDoiThu, String tenDoiThu,
            String tenDangDanh, int giayConLai) {
        if (cuaMinh == null) {
            log("Server gửi bản đồ rỗng, không thể khôi phục.");
            return;
        }
        modelCuaMinh = cuaMinh;
        boardCuaMinh.napTuBanDo(modelCuaMinh);
        boardCuaMinh.setChoPhepClick(false);

        napBanDoDoiThu(maDoiThu);

        doiThu = (tenDoiThu == null || tenDoiThu.isBlank()) ? "-" : tenDoiThu;
        capNhatTieuDe();

        // Bo qua giai doan dat tau, vao thang man hinh tran dau.
        dieuKhienCards.show(dieuKhienPanel, THE_TRAN_DAU);
        log("Đã khôi phục ván đấu đang diễn ra. Tàu còn lại: "
                + modelCuaMinh.remainingShips() + "/5.");

        capNhatLuot(tenDangDanh, giayConLai);
    }

    /**
     * Doc chuoi 100 ky tu tu server ve lai ban co doi thu.
     *
     * <p>Chuoi duoc {@link Board#maDoiThuThay()} sinh ra, doc theo hang tu
     * tren xuong duoi. Dau cham la o chua ban - ta khong biet gi ve o do, va
     * server cung khong gui.
     */
    private void napBanDoDoiThu(String ma) {
        boardDoiThu.xoaHet();
        if (ma == null || ma.length() < Board.SIZE * Board.SIZE) {
            return;
        }
        for (int y = 0; y < Board.SIZE; y++) {
            for (int x = 0; x < Board.SIZE; x++) {
                char k = ma.charAt(y * Board.SIZE + x);
                BoardView.Mark m = switch (k) {
                    case 'o' -> BoardView.Mark.MISS;
                    case 'X' -> BoardView.Mark.HIT;
                    case '#' -> BoardView.Mark.SUNK;
                    default -> BoardView.Mark.NONE;
                };
                if (m != BoardView.Mark.NONE) {
                    boardDoiThu.danhDauO(x, y, m);
                }
            }
        }
    }

    /** GAME_OVER: van dau ket thuc. */
    public void ketThucVan(String ketQua, String lyDo) {
        luotCuaMinh = false;
        dungDongHo();
        dongHoLabel.setText(" ");
        boardDoiThu.setChoPhepClick(false);
        theCuaMinh.setSangLen(false);
        theDoiThu.setSangLen(false);
        boolean thang = Protocol.RESULT_WIN.equals(ketQua);
        boolean doiThuBoDi = Protocol.REASON_OPPONENT_LEFT.equals(lyDo);
        boolean doiThuRotMang = Protocol.REASON_OPPONENT_LOST.equals(lyDo);

        // Doi thu rot mang va khong vao lai kip: phong duoc giu lai o trang
        // thai cho nguoi choi moi, nen chi thong bao roi o lai, khong ve lobby.
        if (doiThuRotMang) {
            String tb = "BẠN THẮNG! (đối thủ mất kết nối và không vào lại được)";
            log(tb);
            setStatus(tb);
            JOptionPane.showMessageDialog(this,
                    tb + "\nPhòng ván còn, bạn có thể cho người chơi mới.",
                    "Kết thúc ván đấu", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String moTaLyDo;
        if (doiThuBoDi) {
            moTaLyDo = "đối thủ đã rời phòng";
        } else {
            moTaLyDo = thang ? "bạn đã bắn chìm hết tàu đối thủ"
                    : "tàu của bạn đã bị bắn chìm hết";
        }
        String thongBao = (thang ? "BẠN THẮNG! " : "BẠN THUA! ") + "(" + moTaLyDo + ")";
        log(thongBao);
        setStatus(thongBao);

        // Doi thu bo di thi khong con ai de choi lai, chi ve lobby duoc.
        if (doiThuBoDi) {
            JOptionPane.showMessageDialog(this,
                    thongBao + "\nBạn sẽ quay về danh sách phòng.",
                    "Kết thúc ván đấu", JOptionPane.INFORMATION_MESSAGE);
            app.doLeaveRoom();
            return;
        }

        hoiChoiLai(thongBao);
    }

    /**
     * Hoi nguoi choi muon danh tiep hay ve lobby.
     *
     * <p>Server da tu mo mot van moi (gui PLACE_PHASE) ngay sau khi van cu
     * ket thuc, nen chon "Choi lai" chi don gian la o lai va dat tau tiep.
     * Chon "Ve lobby" thi roi phong.
     */
    private void hoiChoiLai(String thongBao) {
        int chon = JOptionPane.showOptionDialog(this,
                thongBao + "\n\nBạn muốn làm gì tiếp?",
                "Kết thúc ván đấu",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                new String[] { "Chơi lại", "Về danh sách phòng" },
                "Chơi lại");

        if (chon == JOptionPane.NO_OPTION) {
            app.doLeaveRoom();
        } else {
            log("Bạn đã chọn chơi lại. Hãy đặt tàu mới và bấm \"Sẵn sàng\".");
        }
    }

    private BoardView.Mark doiMark(String ketQua) {
        if (FireResult.MISS.name().equals(ketQua)) {
            return BoardView.Mark.MISS;
        }
        if (FireResult.SUNK.name().equals(ketQua)) {
            return BoardView.Mark.SUNK;
        }
        return BoardView.Mark.HIT;
    }

    private String tenTau(String maTau) {
        if (maTau == null || maTau.isEmpty()) {
            return "?";
        }
        ShipType t = ShipType.fromCode(maTau.charAt(0));
        return t != null ? t.label() : "?";
    }

    public void onChat(String nguoiGui, String noiDung) {
        themDong(LocalTime.now().format(TIME_FMT), nguoiGui, noiDung, CHU);
    }

    public void setStatus(String text) {
        statusLabel.setText(text);
    }

    public void log(String text) {
        themDong(LocalTime.now().format(TIME_FMT), null, text, new Color(0x37, 0x41, 0x51));
    }
}