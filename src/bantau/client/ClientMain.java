package bantau.client;

import bantau.common.Packet;
import bantau.common.PacketType;
import bantau.common.Protocol;
import bantau.common.RoomInfo;

import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * CUA SO CHINH CUA CLIENT
 *
 * <p>Dung {@link CardLayout} de chuyen qua lai giua ba man hinh:
 * dang nhap -> lobby -> trong phong.
 *
 * <p>Lop nay dong vai tro "tong dai": nhan moi ban tin tu server roi phan
 * phoi cho man hinh tuong ung. Cac panel khong tu goi mang, chung goi cac
 * ham doXxx() cua lop nay.
 *
 * <pre>
 *   java -cp bin bantau.client.ClientMain [host] [port]
 * </pre>
 */
public class ClientMain extends JFrame implements ServerConnection.Listener {

    private static final String CARD_LOGIN = "login";
    private static final String CARD_LOBBY = "lobby";
    private static final String CARD_GAME = "game";

    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);

    private final LoginPanel loginPanel = new LoginPanel(this);
    private final LobbyPanel lobbyPanel = new LobbyPanel(this);
    private final GamePanel gamePanel = new GamePanel(this);

    private final ServerConnection connection = new ServerConnection();
    private String username = "";

    /** Giu tam ten va mat khau giua luc bam nut va luc server tra loi. */
    private String tenTam = "";
    private String matKhauTam = "";

    public ClientMain() {
        setTitle("Ban tau online - Battleship");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(900, 620));

        root.add(loginPanel, CARD_LOGIN);
        root.add(lobbyPanel, CARD_LOBBY);
        root.add(gamePanel, CARD_GAME);
        setContentPane(root);
        cards.show(root, CARD_LOGIN);

        // Dong cua so thi bao server biet roi moi thoat.
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                connection.send(Packet.of(PacketType.QUIT));
                connection.close();
                dispose();
                System.exit(0);
            }
        });

        pack();
        setLocationRelativeTo(null);
    }

    public String getUsername() {
        return username;
    }

    public GamePanel getGamePanel() {
        return gamePanel;
    }

    /* ------------------------------------------------------------------ */
    /* Cac hanh dong do giao dien goi                                     */
    /* ------------------------------------------------------------------ */

    /**
     * Ket noi toi server roi gui LOGIN hoac REGISTER.
     *
     * @param dangKy true thi tao tai khoan moi, false thi dang nhap
     */
    public void doConnect(String host, int port, String ten, String matKhau, boolean dangKy) {
        if (ten.isEmpty()) {
            loginPanel.setStatusLoi("Hay nhap ten nguoi choi");
            return;
        }
        if (matKhau.isEmpty()) {
            loginPanel.setStatusLoi("Hay nhap mat khau");
            return;
        }

        tenTam = ten;
        matKhauTam = matKhau;

        loginPanel.setBusy(true);
        loginPanel.setStatusLoi(" ");
        loginPanel.setStatus("Dang ket noi toi " + host + ":" + port + "...");
        try {
            // Dong ket noi cu neu lan truoc bi tu choi, roi mo ket noi moi.
            connection.close();
            connection.connect(host, port, this);
            connection.send(Packet.of(
                    dangKy ? PacketType.REGISTER : PacketType.LOGIN, ten, matKhau));
        } catch (IOException e) {
            loginPanel.setBusy(false);
            loginPanel.setStatusLoi("Khong ket noi duoc: " + e.getMessage());
        }
    }

    public void doXemBangXepHang() {
        connection.send(Packet.of(PacketType.RANK_LIST));
    }

    public void doXemLichSu() {
        connection.send(Packet.of(PacketType.HISTORY_LIST));
    }

    public void doRefreshRooms() {
        connection.send(Packet.of(PacketType.ROOM_LIST));
    }

    public void doCreateRoom(String ten) {
        connection.send(Packet.of(PacketType.ROOM_CREATE, ten));
    }

    public void doJoinRoom(int roomId) {
        connection.send(Packet.of(PacketType.ROOM_JOIN, String.valueOf(roomId)));
    }

    public void doLeaveRoom() {
        connection.send(Packet.of(PacketType.ROOM_LEAVE));
    }

    public void doChat(String text) {
        connection.send(Packet.of(PacketType.CHAT, text));
    }

    public void send(Packet packet) {
        connection.send(packet);
    }

    /* ------------------------------------------------------------------ */
    /* Nhan ban tin tu server - LUON chay tren EDT                        */
    /* ------------------------------------------------------------------ */

    @Override
    @SuppressWarnings("unchecked")
    public void onPacket(Packet p) {
        switch (p.type()) {
            case LOGIN_OK -> {
                username = p.arg(0);
                matKhauTam = "";           // khong giu mat khau trong bo nho nua
                loginPanel.setBusy(false);
                loginPanel.setStatus(" ");
                lobbyPanel.setUsername(username);
                cards.show(root, CARD_LOBBY);
            }

            // Tao tai khoan xong thi dang nhap luon cho nguoi choi do phai go lai.
            case REGISTER_OK -> {
                loginPanel.setStatusOk("Tao tai khoan thanh cong, dang dang nhap...");
                connection.send(Packet.of(PacketType.LOGIN, tenTam, matKhauTam));
            }

            case RANK_DATA ->
                    ThongKeDialog.hienBangXepHang(this, p.payload(ArrayList.class));

            case HISTORY_DATA ->
                    ThongKeDialog.hienLichSu(this, username, p.payload(ArrayList.class));

            case ROOM_LIST_DATA ->
                    lobbyPanel.updateRooms(p.payload(ArrayList.class));

            case ROOM_JOINED -> {
                gamePanel.enterRoom(p.payload(RoomInfo.class), "1".equals(p.arg(0)));
                cards.show(root, CARD_GAME);
            }

            case ROOM_STATE ->
                    gamePanel.updateRoomState(p.payload(RoomInfo.class), p.arg(0), p.arg(1));

            case ROOM_LEFT -> {
                cards.show(root, CARD_LOBBY);
                doRefreshRooms();
            }

            case PLACE_PHASE -> gamePanel.batDauDatTau();

            case READY_OK -> gamePanel.batDaSanSang();

            case GAME_START -> gamePanel.batDauVanDau(p.arg(0));

            case TURN -> gamePanel.capNhatLuot(p.arg(0));

            case FIRE_RESULT -> gamePanel.ketQuaBanCuaMinh(
                    p.intArg(0, -1), p.intArg(1, -1), p.arg(2), p.arg(3));

            case INCOMING -> gamePanel.doiThuBanTrung(
                    p.intArg(0, -1), p.intArg(1, -1), p.arg(2), p.arg(3));

            case GAME_OVER -> gamePanel.ketThucVan(p.arg(0), p.arg(1));

            case CHAT_MSG -> gamePanel.onChat(p.arg(0), p.arg(1));

            case SYSTEM -> gamePanel.log(p.arg(0));

            case ERROR -> xuLyLoi(p.arg(0), p.arg(1));

            default -> gamePanel.log("Nhan: " + p);
        }
    }

    private void xuLyLoi(String code, String moTa) {
        // Chua dang nhap xong thi bao loi ngay tai man hinh dang nhap.
        if (username.isEmpty()) {
            loginPanel.setBusy(false);
            loginPanel.setStatusLoi(moTa);
            connection.close();
            return;
        }
        gamePanel.log("LOI [" + code + "] " + moTa);

        if (Protocol.E_ROOM_FULL.equals(code) || Protocol.E_ROOM_NOT_FOUND.equals(code)) {
            JOptionPane.showMessageDialog(this, moTa,
                    "Khong vao duoc phong", JOptionPane.WARNING_MESSAGE);
            doRefreshRooms();
        } else if (Protocol.E_BAD_PLACEMENT.equals(code)) {
            gamePanel.datTauBiTuChoi(moTa);
        } else if (Protocol.E_ALREADY_SHOT.equals(code)
                || Protocol.E_NOT_YOUR_TURN.equals(code)) {
            // Phat ban bi tu choi: mo lai ban co de nguoi choi ban o khac.
            gamePanel.banBiTuChoi();
        }
    }

    @Override
    public void onDisconnected(String lyDo) {
        JOptionPane.showMessageDialog(this,
                lyDo + "\nUng dung se quay ve man hinh dang nhap.",
                "Mat ket noi", JOptionPane.ERROR_MESSAGE);
        username = "";
        loginPanel.setBusy(false);
        loginPanel.setStatus(lyDo);
        cards.show(root, CARD_LOGIN);
    }

    /* ------------------------------------------------------------------ */

    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : Protocol.DEFAULT_HOST;
        String port = args.length > 1 ? args[1] : String.valueOf(Protocol.DEFAULT_PORT);

        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // dung giao dien mac dinh
            }
            ClientMain frame = new ClientMain();
            frame.loginPanel.presetServer(host, port);
            frame.setVisible(true);
            frame.loginPanel.focusName();
        });
    }
}
