package bantau.server;

import bantau.common.Packet;
import bantau.common.PacketType;
import bantau.common.RoomInfo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Quan ly danh sach phong.
 */
public class RoomManager {

    private final AtomicInteger idGen = new AtomicInteger(1);
    private final Map<Integer, Room> rooms = new ConcurrentHashMap<>();

    /**
     * DAY DANH SACH PHONG TREN MOT THREAD RIENG - de tranh DEADLOCK.
     *
     * <p>Van de: broadcastRoomList() duoc goi tu BEN TRONG khoi synchronized
     * cua Room (trong join() va leave()). No lai phai duyet qua TAT CA cac
     * phong de lay thong tin - moi lan lay lai can khoa cua phong do.
     *
     * <p>Tinh huong ket cung: thread A dang giu khoa phong 1 va doi khoa phong 2,
     * cung luc thread B giu khoa phong 2 va doi khoa phong 1. Ca hai cho nhau
     * vinh vien, server treo hoan toan.
     *
     * <p>Cach xu ly: khong lam ngay, ma giao cho mot thread rieng lam sau.
     * Thread do khong giu khoa nao khi bat dau, va no lay khoa tung phong mot
     * roi tra lai ngay - khong bao gio giu hai khoa cung luc.
     */
    private final ExecutorService lobbyNotifier = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "lobby-notifier");
        t.setDaemon(true);
        return t;
    });

    public Room createRoom(String name) {
        int id = idGen.getAndIncrement();
        Room room = new Room(id, name, this);
        rooms.put(id, room);
        System.out.println("Tao phong #" + id + " \"" + name + "\"");
        return room;
    }

    public Room getRoom(int id) {
        return rooms.get(id);
    }

    public void removeRoom(Room room) {
        rooms.remove(room.getId());
        System.out.println("Xoa phong #" + room.getId() + " (khong con nguoi choi)");
    }

    public int roomCount() {
        return rooms.size();
    }

    /**
     * Chup mot ban sao danh sach phong tai thoi diem goi.
     *
     * <p>Tra ve ArrayList chu khong phai List vi ArrayList moi chac chan
     * Serializable - no se duoc gui thang qua mang.
     */
    public ArrayList<RoomInfo> snapshot() {
        List<Room> ds = new ArrayList<>(rooms.values());
        ds.sort(Comparator.comparingInt(Room::getId));

        ArrayList<RoomInfo> ketQua = new ArrayList<>();
        for (Room r : ds) {
            ketQua.add(r.info());
        }
        return ketQua;
    }

    /** Gui danh sach phong cho mot nguoi. */
    public void sendRoomList(ClientHandler c) {
        c.send(Packet.withPayload(PacketType.ROOM_LIST_DATA, snapshot()));
    }

    /** Day danh sach phong moi nhat xuong tat ca nguoi dang o lobby. */
    public void broadcastRoomList() {
        lobbyNotifier.execute(() -> {
            Packet packet = Packet.withPayload(PacketType.ROOM_LIST_DATA, snapshot());
            for (ClientHandler c : ServerMain.getUsers()) {
                // Chi gui cho nguoi dang o lobby, nguoi trong phong khong can.
                if (c.getRoom() == null) {
                    c.send(packet);
                }
            }
        });
    }
}