package bantau.client;

import bantau.common.Protocol;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;

/**
 * MAN HINH DAU TIEN: dia chi server, ten nguoi choi va mat khau.
 *
 * <p>Hai nut: "Dang nhap" cho tai khoan da co, "Dang ky" tao tai khoan moi.
 *
 * <p>Dung {@link JPasswordField} chu khong phai {@link JTextField} de mat
 * khau hien thanh dau cham. Lay noi dung bang {@code getPassword()} tra ve
 * mang char chu khong phai String - vi String nam lai trong bo nho Java
 * cho toi khi bo thu gom rac don, con mang char thi xoa trang duoc ngay.
 *
 * <h2>Ve phan giao dien</h2>
 *
 * <p>Toan bo phan ve nam trong lop nay va hai lop con o cuoi file, khong
 * dung thu vien ngoai. Ba thanh phan tu ve:
 * <ul>
 *   <li>{@link #paintComponent(Graphics)} - nen chuyen mau bien kem vai
 *       duong song mo, dong tong voi ban co trong {@code BoardView}.</li>
 *   <li>{@link The} - khung trang bo goc co do bong, dung lam the dang
 *       nhap dat giua man hinh.</li>
 *   <li>{@link Nut} - nut bo goc, co mau dam cho nut chinh va vien nhat
 *       cho nut phu, de nguoi dung biet nen bam cai nao truoc.</li>
 * </ul>
 */
public class LoginPanel extends JPanel {

    /* ===== Bang mau dung chung ===== */

    private static final Color BIEN_DAM = new Color(0x0D3B54);
    private static final Color BIEN_NHAT = new Color(0x3C8DAD);
    private static final Color CHU_CHINH = new Color(0x12405E);
    private static final Color CHU_PHU = new Color(0x6B7A86);
    private static final Color VIEN_O = new Color(0xCBD6DE);
    private static final Color NUT_CHINH = new Color(0x1F618D);
    private static final Color NUT_PHU_VIEN = new Color(0xA8BAC6);
    private static final Color LOI_NEN = new Color(0xFCEDEC);
    private static final Color LOI_CHU = new Color(0xC0392B);
    private static final Color OK_NEN = new Color(0xEAF7EF);
    private static final Color OK_CHU = new Color(0x1E8449);
    private static final Color TIN_NEN = new Color(0xEDF4F9);
    private static final Color TIN_CHU = new Color(0x34607D);

    /* ===== Cac o nhap =====
     *
     * Thu tu khai bao VA thu tu them vao khung phai giu nguyen
     * (host, cong, ten, mat khau) vi may cong cu chup man hinh noi bo
     * tim o nhap theo thu tu xuat hien.
     */

    private final JTextField hostField = new JTextField(Protocol.DEFAULT_HOST, 14);
    private final JTextField portField = new JTextField(String.valueOf(Protocol.DEFAULT_PORT), 6);
    private final JTextField nameField = new JTextField(12);
    private final JPasswordField passField = new JPasswordField(12);
    private final JCheckBox sslBox =
            new JCheckBox("Mã hóa đường truyền (TLS)", true);

    private final Nut loginButton = new Nut("Đăng nhập", NUT_CHINH, Color.WHITE, null);
    private final Nut registerButton = new Nut("Đăng ký", Color.WHITE, CHU_CHINH, NUT_PHU_VIEN);

    private final JLabel statusLabel = new JLabel(" ", SwingConstants.CENTER);
    /** Khung bao quanh dong trang thai - an di khi khong co gi de bao. */
    private final JPanel statusHolder = new JPanel(new BorderLayout());

    public LoginPanel(ClientMain app) {
        setLayout(new GridBagLayout());        // chi de can giua cai the
        setOpaque(true);

        add(buildThe(app), new GridBagConstraints());

        loginButton.addActionListener(e -> gui(app, false));
        registerButton.addActionListener(e -> gui(app, true));
        // Go Enter o o mat khau la dang nhap luon cho nhanh.
        passField.addActionListener(e -> gui(app, false));
        nameField.addActionListener(e -> passField.requestFocusInWindow());
    }

    /* ------------------------------------------------------------------ */
    /* Dung giao dien                                                      */
    /* ------------------------------------------------------------------ */

    private The buildThe(ClientMain app) {
        The the = new The(new GridBagLayout());
        the.setBorder(BorderFactory.createEmptyBorder(
                The.BONG + 26, The.BONG + 38, The.BONG + 24, The.BONG + 38));

        GridBagConstraints gc = new GridBagConstraints();
        gc.anchor = GridBagConstraints.WEST;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.gridx = 0;
        gc.gridwidth = 2;
        gc.weightx = 1;

        /* ----- Tieu de ----- */

        JLabel title = new JLabel("BẮN TÀU ONLINE", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 30));
        title.setForeground(CHU_CHINH);
        gc.gridy = 0;
        gc.insets = new Insets(0, 0, 2, 0);
        the.add(title, gc);

        JLabel phu = new JLabel("Đăng nhập để vào sảnh chờ", SwingConstants.CENTER);
        phu.setFont(new Font("SansSerif", Font.PLAIN, 13));
        phu.setForeground(CHU_PHU);
        gc.gridy = 1;
        gc.insets = new Insets(0, 0, 18, 0);
        the.add(phu, gc);

        /* ----- Hang 1: dia chi server va cong nam chung mot hang -----
         *
         * Gop hai o nay lai vi o cong chi can rat ngan. De rieng hai hang
         * nhu truoc lam bieu mau dai ra ma khong them thong tin gi.
         */

        gc.gridwidth = 1;
        gc.insets = new Insets(0, 0, 3, 8);
        gc.gridx = 0;
        gc.gridy = 2;
        gc.weightx = 1;
        the.add(nhan("Địa chỉ server"), gc);

        gc.gridx = 1;
        gc.weightx = 0;
        gc.insets = new Insets(0, 0, 3, 0);
        the.add(nhan("Cổng"), gc);

        gc.gridx = 0;
        gc.gridy = 3;
        gc.weightx = 1;
        gc.insets = new Insets(0, 0, 14, 8);
        the.add(oNhap(hostField), gc);

        gc.gridx = 1;
        gc.weightx = 0;
        gc.insets = new Insets(0, 0, 14, 0);
        portField.setHorizontalAlignment(SwingConstants.CENTER);
        the.add(oNhap(portField), gc);

        /* ----- Ten nguoi choi ----- */

        gc.gridx = 0;
        gc.gridwidth = 2;
        gc.weightx = 1;
        gc.gridy = 4;
        gc.insets = new Insets(0, 0, 3, 0);
        the.add(nhan("Tên người chơi"), gc);

        gc.gridy = 5;
        gc.insets = new Insets(0, 0, 14, 0);
        the.add(oNhap(nameField), gc);

        /* ----- Mat khau ----- */

        gc.gridy = 6;
        gc.insets = new Insets(0, 0, 3, 0);
        the.add(nhan("Mật khẩu"), gc);

        gc.gridy = 7;
        gc.insets = new Insets(0, 0, 12, 0);
        the.add(oNhap(passField), gc);

        /* ----- O tick ma hoa -----
         *
         * De demo so sanh co ma hoa va khong ma hoa. Phai khop voi che do
         * server dang chay, neu khong se khong ket noi duoc.
         */

        gc.gridy = 8;
        gc.insets = new Insets(0, 0, 18, 0);
        sslBox.setFont(new Font("SansSerif", Font.PLAIN, 13));
        sslBox.setForeground(CHU_CHINH);
        sslBox.setOpaque(false);
        sslBox.setFocusPainted(false);
        sslBox.setToolTipText("Phải khớp với chế độ server đang chạy");
        the.add(sslBox, gc);

        /* ----- Hai nut ----- */

        JPanel nutRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        nutRow.setOpaque(false);
        loginButton.setPreferredSize(new Dimension(142, 40));
        registerButton.setPreferredSize(new Dimension(142, 40));
        nutRow.add(loginButton);
        nutRow.add(registerButton);

        gc.gridy = 9;
        gc.insets = new Insets(0, 0, 14, 0);
        the.add(nutRow, gc);

        /* ----- Ghi chu ----- */

        JLabel ghiChu = new JLabel(
                "<html><div style='text-align:center'><i>"
                        + "Chưa có tài khoản? Nhập tên và mật khẩu rồi bấm Đăng ký.<br>"
                        + "Mật khẩu dài " + Protocol.PASS_MIN + "–" + Protocol.PASS_MAX
                        + " ký tự.</i></div></html>", SwingConstants.CENTER);
        ghiChu.setFont(new Font("SansSerif", Font.PLAIN, 11));
        ghiChu.setForeground(CHU_PHU);
        gc.gridy = 10;
        gc.insets = new Insets(0, 0, 0, 0);
        the.add(ghiChu, gc);

        /* ----- Dong trang thai -----
         *
         * Nam trong mot khung co nen mau, va duoc AN HAN khi khong co gi de
         * bao. Truoc day no luon chiem cho bang mot dong trang, lam bo cuc
         * nhay len nhay xuong moi khi co thong bao.
         */

        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        statusHolder.setOpaque(true);
        statusHolder.add(statusLabel, BorderLayout.CENTER);
        statusHolder.setVisible(false);
        datKieuTrangThai(TIN_NEN, TIN_CHU);

        gc.gridy = 11;
        gc.insets = new Insets(14, 0, 0, 0);
        the.add(statusHolder, gc);

        return the;
    }

    /** Nhan nho nam tren moi o nhap. */
    private static JLabel nhan(String chu) {
        JLabel l = new JLabel(chu);
        l.setFont(new Font("SansSerif", Font.PLAIN, 12));
        l.setForeground(CHU_PHU);
        return l;
    }

    /** Dat vien va khoang dem cho mot o nhap, roi tra lai chinh no. */
    private static JTextField oNhap(JTextField f) {
        f.setFont(new Font("SansSerif", Font.PLAIN, 14));
        f.setForeground(CHU_CHINH);
        f.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(VIEN_O, 1, true),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        return f;
    }

    /* ------------------------------------------------------------------ */
    /* Nen man hinh                                                        */
    /* ------------------------------------------------------------------ */

    /**
     * Ve nen chuyen mau bien kem vai duong song mo.
     *
     * <p>Cung bang mau voi {@code BoardView} de ca chuong trinh nhin lien
     * mach, thay vi mau xam mac dinh cua Swing.
     */
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

        // Vai duong song mo o nua duoi cho do trong trai.
        g2.setColor(new Color(255, 255, 255, 26));
        g2.setStroke(new BasicStroke(2f));
        for (int hang = 0; hang < 4; hang++) {
            int y = h - 150 + hang * 38;
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

    /* ------------------------------------------------------------------ */
    /* Gui du lieu di                                                      */
    /* ------------------------------------------------------------------ */

    /** Doc o nhap roi goi ClientMain. {@code dangKy} = true thi tao tai khoan moi. */
    private void gui(ClientMain app, boolean dangKy) {
        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException e) {
            setStatusLoi("Cổng phải là số");
            portField.requestFocusInWindow();
            return;
        }

        char[] mk = passField.getPassword();
        String matKhau = new String(mk);
        java.util.Arrays.fill(mk, '\0');   // xoa dau vet trong bo nho

        // Dat che do ma hoa truoc khi mo ket noi.
        bantau.common.BaoMat.batSsl(sslBox.isSelected());

        app.doConnect(hostField.getText().trim(), port,
                nameField.getText().trim(), matKhau, dangKy);
    }

    /* ------------------------------------------------------------------ */
    /* Bao trang thai                                                      */
    /* ------------------------------------------------------------------ */

    /** Thong bao thuong, mau xanh nhat - vi du "Dang ket noi...". */
    public void setStatus(String text) {
        datKieuTrangThai(TIN_NEN, TIN_CHU);
        hienTrangThai(text);
    }

    /** Doi mau chu trang thai: xanh la bao tin vui, do la bao loi. */
    public void setStatusOk(String text) {
        datKieuTrangThai(OK_NEN, OK_CHU);
        hienTrangThai(text);
    }

    public void setStatusLoi(String text) {
        datKieuTrangThai(LOI_NEN, LOI_CHU);
        hienTrangThai(text);
    }

    private void datKieuTrangThai(Color nen, Color chu) {
        statusHolder.setBackground(nen);
        statusHolder.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(chu.brighter(), 1, true),
                BorderFactory.createEmptyBorder(7, 10, 7, 10)));
        statusLabel.setForeground(chu);
    }

    /** Hien dong trang thai, hoac an han neu khong co chu nao. */
    private void hienTrangThai(String text) {
        String t = text == null ? "" : text.trim();
        statusLabel.setText(t.isEmpty() ? " " : t);
        statusHolder.setVisible(!t.isEmpty());
        revalidate();
        repaint();
    }

    /* ------------------------------------------------------------------ */
    /* Cac ham ClientMain goi toi                                          */
    /* ------------------------------------------------------------------ */

    /**
     * Khoa hai nut trong luc dang cho server tra loi.
     *
     * <p>Doi luon chu tren nut chinh de nguoi choi biet chuong trinh dang
     * lam viec chu khong phai bi treo.
     */
    public void setBusy(boolean busy) {
        loginButton.setEnabled(!busy);
        registerButton.setEnabled(!busy);
        loginButton.setText(busy ? "Đang kết nối..." : "Đăng nhập");
    }

    public void focusName() {
        nameField.requestFocusInWindow();
    }

    /**
     * Dien san ten nguoi choi va dua con tro vao o mat khau.
     *
     * <p>Dung khi vua mat ket noi: nguoi choi chi phai go lai mat khau roi
     * bam Dang nhap la vao lai duoc van dang do.
     */
    public void presetTen(String ten) {
        nameField.setText(ten == null ? "" : ten);
        passField.setText("");
        passField.requestFocusInWindow();
    }

    /** Dien san dia chi server truyen qua tham so dong lenh. */
    public void presetServer(String host, String port) {
        hostField.setText(host);
        portField.setText(port);
    }

    /* ================================================================== */
    /* Hai lop phu tro tu ve                                              */
    /* ================================================================== */

    /**
     * THE TRANG BO GOC CO DO BONG.
     *
     * <p>Swing khong co san panel bo goc, nen phai tu ve. Cach lam: ve
     * nhieu hinh chu nhat bo goc mau toi mo chong len nhau, moi lop thu
     * nho lai mot chut, tao thanh vien bong mem; roi ve hinh trang len tren.
     *
     * <p>Vi do bong nam o ria nen phan rong {@link #BONG} diem anh moi
     * canh khong dung duoc de dat noi dung - duong vien rong cua the phai
     * cong them chung ay.
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
     * Nho vay nguoi dung nhin la biet nen bam cai nao truoc - hai nut giong
     * het nhau thi khong truyen dat duoc dieu do.
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
            setFont(new Font("SansSerif", Font.BOLD, 14));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
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
                ve = pha(nen, Color.WHITE, 0.14f);
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