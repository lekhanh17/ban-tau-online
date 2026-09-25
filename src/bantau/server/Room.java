package bantau.server;

import bantau.common.Board;
import bantau.common.Packet;
import bantau.common.PacketType;
import bantau.common.Protocol;
import bantau.common.RoomInfo;
import bantau.common.RoomState;

/**
 * MOT PHONG CHOI - toi da 2 nguoi, chua mot van dau.
 *
 * <p>May trang thai cua phong:
 * <pre>
 *   WAITING  --du 2 nguoi-->  PLACING  --ca hai READY-->  PLAYING
 *      ^                          |                          |
 *      +--------- mot nguoi roi phong -------------------- --+
 *                                 ^                          |
 *                                 +---- het van, choi lai ----+
 * </pre>
 *
 * <p>Moi phuong thuc thay doi trang thai deu synchronized vi cac thread
 * ClientHandler khac nhau cung truy cap vao doi tuong nay.
 */
public class Room {

    private final int id;
    private final String name;
    private final RoomManager manager;

    private ClientHandler p1;
    private ClientHandler p2;

    private RoomState state = RoomState.WAITING;
    private GameSession game;

    public Room(int id, String name, RoomManager manager) {
        this.id = id;
        this.name = name;
        this.manager = manager;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public synchronized RoomState getState() {
        return state;
    }

    public synchronized int playerCount() {
        return (p1 != null ? 1 : 0) + (p2 != null ? 1 : 0);
    }

    public synchronized boolean isEmpty() {
        return p1 == null && p2 == null;
    }

    public synchronized boolean isFull() {
        return playerCount() >= Protocol.MAX_PLAYERS_PER_ROOM;
    }

    public synchronized boolean contains(ClientHandler c) {
        return c == p1 || c == p2;
    }

    public synchronized RoomInfo info() {
        return new RoomInfo(id, name, playerCount(), state);
    }

    private String tenCua(ClientHandler c) {
        return c == null ? "-" : c.getUsername();
    }

    public synchronized void broadcast(Packet packet) {
        if (p1 != null) {
            p1.send(packet);
        }
        if (p2 != null) {
            p2.send(packet);
        }
    }

    private void guiTrangThai() {
        broadcast(Packet.withPayload(PacketType.ROOM_STATE, info(),
                tenCua(p1), tenCua(p2)));
    }

    /* ------------------------------------------------------------------ */
    /* Vao / roi phong                                                    */
    /* ------------------------------------------------------------------ */

    /** @return null neu thanh cong, nguoc lai la ma loi */
    public synchronized String join(ClientHandler c) {
        if (contains(c)) {
            return Protocol.E_ALREADY_IN_ROOM;
        }
        if (isFull()) {
            return Protocol.E_ROOM_FULL;
        }

        if (p1 == null) {
            p1 = c;
        } else {
            p2 = c;
        }
        c.setRoom(this);
        c.send(Packet.withPayload(PacketType.ROOM_JOINED, info(), c == p1 ? "1" : "0"));

        if (isFull()) {
            batDauDatTau();
        } else {
            state = RoomState.WAITING;
            guiTrangThai();
        }
        manager.broadcastRoomList();
        return null;
    }

    /** Chuyen sang giai doan dat tau. Goi khi du 2 nguoi hoac khi choi lai. */
    private void batDauDatTau() {
        state = RoomState.PLACING;
        game = new GameSession(p1, p2);
        guiTrangThai();
        broadcast(Packet.of(PacketType.PLACE_PHASE));
    }

    /** Mot nguoi roi phong - chu dong bam roi, hoac mat ket noi. */
    public synchronized void leave(ClientHandler c) {
        if (!contains(c)) {
            return;
        }
        ClientHandler conLai = (c == p1) ? p2 : p1;

        if (c == p1) {
            p1 = null;
        } else {
            p2 = null;
        }
        c.setRoom(null);
        c.send(Packet.of(PacketType.ROOM_LEFT));

        // Dang danh do ma bo di thi nguoi con lai duoc xu thang.
        if (game != null && conLai != null) {
            game.abortBecauseLeft(c);
        }
        game = null;

        if (conLai != null) {
            p1 = conLai;
            p2 = null;
            state = RoomState.WAITING;
            conLai.send(Packet.of(PacketType.SYSTEM,
                    c.getUsername() + " da roi phong. Ban tro thanh chu phong."));
            guiTrangThai();
        } else {
            manager.removeRoom(this);
        }
        manager.broadcastRoomList();
    }

    /* ------------------------------------------------------------------ */
    /* Trong van dau                                                      */
    /* ------------------------------------------------------------------ */

    /**
     * Nhan so do dat tau cua mot nguoi choi.
     *
     * <p>Day la cho server KHONG DUOC TIN client. Ban do nhan ve phai qua
     * {@link Board#kiemTraHopLe()} truoc khi chap nhan.
     *
     * @return null neu hop le, nguoc lai la ma loi
     */
    public synchronized String handleReady(ClientHandler c, Board board) {
        if (!contains(c)) {
            return Protocol.E_NOT_IN_ROOM;
        }
        if (state != RoomState.PLACING || game == null) {
            return Protocol.E_BAD_STATE;
        }
        if (board == null) {
            return Protocol.E_BAD_PLACEMENT;
        }

        String loi = board.kiemTraHopLe();
        if (loi != null) {
            System.out.println("Tu choi so do cua " + c.getUsername() + ": " + loi);
            return Protocol.E_BAD_PLACEMENT;
        }

        boolean caHaiSanSang = game.setBoard(c, board);
        c.send(Packet.of(PacketType.READY_OK));
        c.send(Packet.of(PacketType.SYSTEM, "So do hop le. Dang cho doi thu..."));

        if (caHaiSanSang) {
            state = RoomState.PLAYING;
            guiTrangThai();
            game.start();
            manager.broadcastRoomList();
        }
        return null;
    }

    /** @return null neu hop le, nguoc lai la ma loi */
    public synchronized String handleFire(ClientHandler c, int x, int y) {
        if (!contains(c)) {
            return Protocol.E_NOT_IN_ROOM;
        }
        if (state != RoomState.PLAYING || game == null) {
            return Protocol.E_BAD_STATE;
        }

        String loi = game.fire(c, x, y);
        if (loi != null) {
            return loi;
        }

        // Van dau vua ket thuc: mo luon mot van moi cho hai nguoi choi lai.
        if (game.isFinished()) {
            batDauDatTau();
            manager.broadcastRoomList();
        }
        return null;
    }
}