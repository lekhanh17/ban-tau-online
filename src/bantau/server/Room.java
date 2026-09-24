package bantau.server;

import bantau.common.Packet;
import bantau.common.PacketType;
import bantau.common.Protocol;
import bantau.common.RoomInfo;
import bantau.common.RoomState;

/**
 * MOT PHONG CHOI - toi da 2 nguoi.
 *
 * <p>Day la lop quan trong nhat ve mat DONG BO DU LIEU trong ca do an.
 *
 * <p>Tinh huong nguy hiem: phong dang co 1 nguoi, hai nguoi khac cung bam
 * "Vao phong" trong cung mot phan nghin giay. Neu khong dong bo, ca hai cung
 * doc thay "con 1 cho trong" roi ca hai cung vao - phong co 3 nguoi.
 *
 * <p>Cach xu ly: moi phuong thuc thay doi trang thai deu {@code synchronized}.
 * Khoa nam tren chinh doi tuong Room, nen hai thread khong bao gio cung chay
 * ben trong join() cua cung mot phong.
 */
public class Room {

    private final int id;
    private final String name;
    private final RoomManager manager;

    /** Nguoi thu nhat - la chu phong. */
    private ClientHandler p1;
    /** Nguoi thu hai. */
    private ClientHandler p2;

    private RoomState state = RoomState.WAITING;

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

    /** Ban sao thong tin phong de gui cho client. */
    public synchronized RoomInfo info() {
        return new RoomInfo(id, name, playerCount(), state);
    }

    private String tenCua(ClientHandler c) {
        return c == null ? "-" : c.getUsername();
    }

    /** Gui mot ban tin toi moi nguoi trong phong. */
    public synchronized void broadcast(Packet packet) {
        if (p1 != null) {
            p1.send(packet);
        }
        if (p2 != null) {
            p2.send(packet);
        }
    }

    /** Bao cho ca phong biet trang thai hien tai. */
    private void guiTrangThai() {
        broadcast(Packet.withPayload(PacketType.ROOM_STATE, info(),
                tenCua(p1), tenCua(p2)));
    }

    /* ------------------------------------------------------------------ */
    /* Vao / roi phong                                                    */
    /* ------------------------------------------------------------------ */

    /**
     * Cho mot nguoi vao phong.
     *
     * @return null neu thanh cong, nguoc lai la ma loi
     */
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

        // Nguoi vao dau tien la chu phong.
        c.send(Packet.withPayload(PacketType.ROOM_JOINED, info(), c == p1 ? "1" : "0"));

        // Du 2 nguoi thi chuyen sang giai doan dat tau (Giai doan 6 se dung).
        state = isFull() ? RoomState.PLACING : RoomState.WAITING;
        guiTrangThai();

        manager.broadcastRoomList();
        return null;
    }

    /**
     * Mot nguoi roi phong - chu dong bam roi, hoac mat ket noi.
     *
     * <p>Ba viec phai lam dung thu tu:
     * <ol>
     *   <li>Go nguoi do ra khoi phong.</li>
     *   <li>Neu con nguoi o lai: don len lam chu phong, quay ve trang thai cho.</li>
     *   <li>Neu phong rong: xoa phong khoi danh sach, tranh de lai rac.</li>
     * </ol>
     */
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

        if (conLai != null) {
            // Don nguoi con lai len lam chu phong.
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
}