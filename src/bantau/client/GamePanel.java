package bantau.client;

import bantau.common.Board;
import bantau.common.FireResult;
import bantau.common.Orientation;
import bantau.common.Packet;
import bantau.common.PacketType;
import bantau.common.Protocol;
import bantau.common.RoomInfo;
import bantau.common.RoomState;
import bantau.common.ShipType;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.Timer;

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
 */
public class GamePanel extends JPanel {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static final String THE_CHO = "cho";
    private static final String THE_DAT_TAU = "dat_tau";
    private static final String THE_TRAN_DAU = "tran_dau";

    private final ClientMain app;

    private final JLabel roomLabel = new JLabel(" ");
    private final JLabel statusLabel = new JLabel(" ");

    private final JTextArea logArea = new JTextArea();
    private final JTextField chatField = new JTextField();

    private final JPanel centerPanel = new JPanel(new BorderLayout(8, 8));
    private final CardLayout dieuKhienCards = new CardLayout();
    private final JPanel dieuKhienPanel = new JPanel(dieuKhienCards);

    private final BoardView boardCuaMinh = new BoardView();
    private final BoardView boardDoiThu = new BoardView();

    /* ----- giai doan dat tau ----- */
    /**
     * Ban do cua minh giu ben client. KHONG final vi khi vao lai van sau khi
     * rot mang, server gui ve mot doi tuong Board moi (co san cac vet ban)
     * va ta thay ca doi tuong thay vi dung lai tung o.
     */
    private Board modelCuaMinh = new Board();
    private final Map<ShipType, JToggleButton> nutTau = new EnumMap<>(ShipType.class);
    private final JToggleButton nutXoay = new JToggleButton("Huong: Nam ngang");
    private final JButton nutNgauNhien = new JButton("Dat ngau nhien");
    private final JButton nutSanSang = new JButton("San sang");
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
    private final JButton nutGoiMay = new JButton("Choi voi may");

    private String tenPhong = "";
    private String doiThu = "-";

    public GamePanel(ClientMain app) {
        this.app = app;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(buildHeader(), BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(buildChat(), BorderLayout.EAST);
        add(buildControls(), BorderLayout.SOUTH);

        centerPanel.add(buildBoardsRow(), BorderLayout.CENTER);
        centerPanel.add(dieuKhienPanel, BorderLayout.SOUTH);

        dieuKhienPanel.add(buildTheCho(), THE_CHO);
        dieuKhienPanel.add(buildTheDatTau(), THE_DAT_TAU);
        dieuKhienPanel.add(buildTheTranDau(), THE_TRAN_DAU);
        dieuKhienCards.show(dieuKhienPanel, THE_CHO);

        noiSuKienBanCo();
    }

    private JPanel buildHeader() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        roomLabel.setFont(new Font("SansSerif", Font.BOLD, 17));
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        statusLabel.setForeground(new java.awt.Color(0x1F618D));
        p.add(roomLabel);
        p.add(statusLabel);
        return p;
    }

    private JPanel buildChat() {
        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

        JScrollPane scroll = new JScrollPane(logArea);
        scroll.setPreferredSize(new java.awt.Dimension(300, 200));

        JPanel p = new JPanel(new BorderLayout(4, 4));
        p.setBorder(BorderFactory.createTitledBorder("Nhat ky va chat"));
        p.add(scroll, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(4, 0));
        JButton sendButton = new JButton("Gui");
        sendButton.addActionListener(e -> guiChat());
        chatField.addActionListener(e -> guiChat());
        bottom.add(chatField, BorderLayout.CENTER);
        bottom.add(sendButton, BorderLayout.EAST);
        p.add(bottom, BorderLayout.SOUTH);
        return p;
    }

    private JPanel buildControls() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        JButton leaveButton = new JButton("Roi phong");
        leaveButton.addActionListener(e -> app.doLeaveRoom());
        p.add(leaveButton);
        return p;
    }

    /** Hai ban co dat canh nhau: ben trai cua minh, ben phai cua doi thu. */
    private JPanel buildBoardsRow() {
        JPanel row = new JPanel(new java.awt.GridLayout(1, 2, 12, 0));
        row.add(boardSlot("Ban co cua ban", boardCuaMinh));
        row.add(boardSlot("Ban co doi thu", boardDoiThu));
        return row;
    }

    private JPanel boardSlot(String tieuDe, BoardView view) {
        JPanel khung = new JPanel(new BorderLayout());
        khung.setBorder(BorderFactory.createTitledBorder(tieuDe));
        JPanel giua = new JPanel(new FlowLayout(FlowLayout.CENTER));
        giua.add(view);
        khung.add(giua, BorderLayout.CENTER);
        return khung;
    }

    /**
     * The "dang cho": mot dong chu va nut goi doi thu may.
     *
     * <p>Nut nay chi gui ban tin ADD_BOT. Viec khoi dong bot do server lam,
     * va bot ket noi nguoc lai nhu mot client binh thuong - client Swing
     * khong he biet gi them ve no.
     */
    private JPanel buildTheCho() {
        JLabel nhan = new JLabel(
                "Dang cho du hai nguoi choi vao phong...", SwingConstants.CENTER);
        nhan.setFont(new Font("SansSerif", Font.PLAIN, 13));

        nutGoiMay.setPreferredSize(new java.awt.Dimension(190, 32));
        nutGoiMay.setToolTipText(
                "Khong doi nguoi nua, goi mot doi thu may vao choi ngay");
        nutGoiMay.addActionListener(e -> goiDoiThuMay());

        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        nhan.setAlignmentX(CENTER_ALIGNMENT);

        JPanel hangNut = new JPanel(new FlowLayout(FlowLayout.CENTER));
        hangNut.add(nutGoiMay);
        hangNut.setAlignmentX(CENTER_ALIGNMENT);

        p.add(nhan);
        p.add(hangNut);
        return p;
    }

    /** Gui ADD_BOT va khoa nut lai de khong goi hai con bot vao mot phong. */
    private void goiDoiThuMay() {
        nutGoiMay.setEnabled(false);
        nutGoiMay.setText("Dang goi doi thu may...");
        app.send(Packet.of(PacketType.ADD_BOT));
        log("Da goi mot doi thu may vao phong.");
    }

    /** Mo lai nut khi quay ve trang thai cho - vi du bot vua thoat. */
    private void moLaiNutGoiMay() {
        nutGoiMay.setEnabled(true);
        nutGoiMay.setText("Choi voi may");
    }

    private JPanel buildTheDatTau() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JPanel hangTau = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        ButtonGroup nhom = new ButtonGroup();
        for (ShipType t : ShipType.values()) {
            JToggleButton nut = new JToggleButton(nhanNut(t, false));
            nut.addActionListener(e -> chonTau(t));
            nhom.add(nut);
            hangTau.add(nut);
            nutTau.put(t, nut);
        }
        nutTau.get(tauDangChon).setSelected(true);

        JPanel hangNut = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        nutXoay.addActionListener(e -> xoayHuong());
        nutNgauNhien.addActionListener(e -> datNgauNhien());
        nutSanSang.addActionListener(e -> guiSanSang());
        nutSanSang.setEnabled(false);
        hangNut.add(nutXoay);
        hangNut.add(nutNgauNhien);
        hangNut.add(nutSanSang);

        JLabel huongDan = new JLabel(
                "Chon tau, ruoc chuot vao ban co ben trai de xem truoc (xanh = hop le,"
                        + " do = khong), roi bam chuot de dat.");
        huongDan.setFont(new Font("SansSerif", Font.ITALIC, 12));

        p.add(hangTau);
        p.add(hangNut);
        p.add(huongDan);
        return p;
    }

    private JPanel buildTheTranDau() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        dongHoLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        dongHoLabel.setAlignmentX(CENTER_ALIGNMENT);

        JLabel nhan = new JLabel(
                "Den luot ban thi bam vao ban co doi thu (ben phai) de ban.",
                SwingConstants.CENTER);
        nhan.setFont(new Font("SansSerif", Font.PLAIN, 13));
        nhan.setAlignmentX(CENTER_ALIGNMENT);

        p.add(dongHoLabel);
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
            dongHoLabel.setText("Het gio!");
            dongHoLabel.setForeground(new java.awt.Color(0xC0392B));
            return;
        }
        dongHoLabel.setText((luotCuaMinh ? "Luot cua ban: " : "Doi thu con: ")
                + giayConLai + " giay");
        // Duoi 10 giay thi chuyen do cho de thay.
        dongHoLabel.setForeground(giayConLai <= 10
                ? new java.awt.Color(0xC0392B)
                : new java.awt.Color(0x1E8449));
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
        return t.label() + " (" + t.size() + " o)" + (daDat ? " - da dat" : "");
    }

    private void chonTau(ShipType t) {
        tauDangChon = t;
        capNhatXemTruoc();
    }

    private void xoayHuong() {
        huongDangChon = huongDangChon.flip();
        nutXoay.setText("Huong: "
                + (huongDangChon == Orientation.HORIZONTAL ? "Nam ngang" : "Nam doc"));
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
        setStatus("Da gui so do, dang cho server xac nhan...");
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
        setStatus("So do bi tu choi: " + moTa);
        JOptionPane.showMessageDialog(this, moTa,
                "So do khong hop le", JOptionPane.WARNING_MESSAGE);
    }

    /* ------------------------------------------------------------------ */
    /* Giai doan tran dau                                                  */
    /* ------------------------------------------------------------------ */

    private void banVaoO(int x, int y) {
        // Chan ngay tai client: o da ban roi thi khong gui goi tin lam gi,
        // server chac chan tu choi bang loi E_ALREADY_SHOT.
        if (boardDoiThu.daBan(x, y)) {
            log("O " + tenO(x, y) + " da ban roi, hay chon o khac.");
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
        tenPhong = (info == null ? "?" : "#" + info.id() + " - " + info.name())
                + (laChuPhong ? "  (ban la chu phong)" : "");
        doiThu = "-";
        capNhatTieuDe();
        logArea.setText("");
        log("Da vao phong.");

        moLaiNutGoiMay();
        dieuKhienCards.show(dieuKhienPanel, THE_CHO);
        modelCuaMinh.clear();
        boardCuaMinh.xoaHet();
        boardDoiThu.xoaHet();
    }

    public void updateRoomState(RoomInfo info, String nguoi1, String nguoi2) {
        String me = app.getUsername();
        doiThu = me.equals(nguoi1) ? nguoi2 : nguoi1;
        if (doiThu == null || doiThu.isBlank()) {
            doiThu = "-";
        }
        capNhatTieuDe();
        statusLabel.setText("Trang thai: " + (info == null ? "?" : info.state().moTa()));

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
        moLaiNutGoiMay();
        dieuKhienCards.show(dieuKhienPanel, THE_CHO);
    }

    private void capNhatTieuDe() {
        roomLabel.setText("Phong " + tenPhong + "   |   Doi thu: " + doiThu);
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
        nutXoay.setText("Huong: Nam ngang");
        for (ShipType t : ShipType.values()) {
            nutTau.get(t).setText(nhanNut(t, false));
        }
        nutTau.get(tauDangChon).setSelected(true);
        setDatTauChoPhep(true);

        dieuKhienCards.show(dieuKhienPanel, THE_DAT_TAU);
        setStatus("Hay dat du 5 tau roi bam \"San sang\".");
        log("Bat dau giai doan dat tau.");
    }

    /** READY_OK: so do cua minh hop le, dang cho doi thu dat xong. */
    public void batDaSanSang() {
        setStatus("So do hop le. Dang cho doi thu dat tau xong...");
        log("So do da duoc chap nhan.");
    }

    /** GAME_START: ca hai da san sang, van dau bat dau. */
    public void batDauVanDau(String nguoiDiTruoc) {
        dieuKhienCards.show(dieuKhienPanel, THE_TRAN_DAU);
        boardCuaMinh.setChoPhepClick(false);
        log("Van dau bat dau. Nguoi di truoc: " + nguoiDiTruoc);
    }

    /** TURN: server bao den luot ai. */
    public void capNhatLuot(String tenNguoiDangDanh, int giaySuyNghi) {
        luotCuaMinh = app.getUsername().equals(tenNguoiDangDanh);
        boardDoiThu.setChoPhepClick(luotCuaMinh);
        setStatus(luotCuaMinh
                ? "Den luot ban - bam vao ban co doi thu de ban."
                : "Doi thu (" + tenNguoiDangDanh + ") dang danh...");
        batDauDemNguoc(giaySuyNghi);
    }

    /** FIRE_RESULT: ket qua phat ban CUA MINH vao ban co doi thu. */
    public void ketQuaBanCuaMinh(int x, int y, String ketQua, String maTau) {
        boardDoiThu.danhDauO(x, y, doiMark(ketQua));
        String toa = tenO(x, y);
        if (FireResult.SUNK.name().equals(ketQua)) {
            log("Ban " + toa + ": CHIM tau " + tenTau(maTau) + " cua doi thu!");
        } else {
            log("Ban " + toa + ": " + FireResult.valueOf(ketQua).moTa());
        }
    }

    /** INCOMING: doi thu vua ban vao ban co cua minh. */
    public void doiThuBanTrung(int x, int y, String ketQua, String maTau) {
        modelCuaMinh.fire(x, y);
        boardCuaMinh.napTuBanDo(modelCuaMinh);
        String toa = tenO(x, y);
        if (FireResult.SUNK.name().equals(ketQua)) {
            log("Doi thu ban " + toa + ": CHIM tau " + tenTau(maTau) + " cua minh!");
        } else {
            log("Doi thu ban " + toa + ": " + FireResult.valueOf(ketQua).moTa());
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

        log(tenDoiThu + " mat ket noi. Van dau tam dung, cho ho vao lai trong "
                + giayAnHan + " giay.");
        setStatus("Tam dung - cho " + tenDoiThu + " vao lai...");
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
        dongHoLabel.setForeground(new java.awt.Color(0xB9770E));
        dongHoLabel.setText(giayConLai > 0
                ? "Cho " + tenDoiThu + " vao lai: " + giayConLai + " giay"
                : "Het thoi gian cho");
    }

    /** OPPONENT_BACK: doi thu vao lai duoc. Server se gui TURN ngay sau. */
    public void doiThuVaoLai(String tenDoiThu) {
        dungDongHo();
        log(tenDoiThu + " da vao lai. Van dau tiep tuc.");
        setStatus("Van dau tiep tuc.");
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
            log("Server gui ban do rong, khong khoi phuc duoc.");
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
        log("Da khoi phuc van dau dang do. Tau con lai: "
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
        boolean thang = Protocol.RESULT_WIN.equals(ketQua);
        boolean doiThuBoDi = Protocol.REASON_OPPONENT_LEFT.equals(lyDo);
        boolean doiThuRotMang = Protocol.REASON_OPPONENT_LOST.equals(lyDo);

        // Doi thu rot mang va khong vao lai kip: phong duoc giu lai o trang
        // thai cho nguoi choi moi, nen chi thong bao roi o lai, khong ve lobby.
        if (doiThuRotMang) {
            String tb = "BAN THANG! (doi thu mat ket noi va khong vao lai duoc)";
            log(tb);
            setStatus(tb);
            JOptionPane.showMessageDialog(this,
                    tb + "\nPhong van con, ban co the cho nguoi choi moi.",
                    "Ket thuc van dau", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String moTaLyDo;
        if (doiThuBoDi) {
            moTaLyDo = "doi thu da roi phong";
        } else {
            moTaLyDo = thang ? "ban da ban chim het tau doi thu"
                    : "tau cua ban da bi chim het";
        }
        String thongBao = (thang ? "BAN THANG! " : "BAN THUA! ") + "(" + moTaLyDo + ")";
        log(thongBao);
        setStatus(thongBao);

        // Doi thu bo di thi khong con ai de choi lai, chi ve lobby duoc.
        if (doiThuBoDi) {
            JOptionPane.showMessageDialog(this,
                    thongBao + "\nBan se quay ve danh sach phong.",
                    "Ket thuc van dau", JOptionPane.INFORMATION_MESSAGE);
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
                thongBao + "\n\nBan muon lam gi tiep?",
                "Ket thuc van dau",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                new String[] { "Choi lai mot van", "Ve danh sach phong" },
                "Choi lai mot van");

        if (chon == JOptionPane.NO_OPTION) {
            app.doLeaveRoom();
        } else {
            log("Ban chon choi lai. Hay dat tau cho van moi.");
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
        log("[" + nguoiGui + "] " + noiDung);
    }

    public void setStatus(String text) {
        statusLabel.setText(text);
    }

    public void log(String text) {
        logArea.append("[" + LocalTime.now().format(TIME_FMT) + "] " + text + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }
}
