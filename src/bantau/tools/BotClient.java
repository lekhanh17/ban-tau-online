package bantau.tools;

import bantau.common.Board;
import bantau.common.Packet;
import bantau.common.PacketType;
import bantau.common.Protocol;
import bantau.common.RoomInfo;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * BOT TU DONG CHOI - dung de kiem thu toan bo mot van dau ma khong can go tay.
 *
 * <pre>
 *   java -cp out bantau.tools.BotClient &lt;host&gt; &lt;port&gt; &lt;ten&gt; create &lt;tenPhong&gt;
 *   java -cp out bantau.tools.BotClient &lt;host&gt; &lt;port&gt; &lt;ten&gt; join
 * </pre>
 */
public class BotClient {

    private final String host;
    private final int port;
    private final String name;
    private final String mode;
    private final String roomName;

    private ObjectOutputStream out;
    private ObjectInputStream in;

    private final Random rnd = new Random();
    private final List<int[]> muctieu = new ArrayList<>();
    private boolean daVaoPhong;
    private boolean xong;

    public BotClient(String host, int port, String name, String mode, String roomName) {
        this.host = host;
        this.port = port;
        this.name = name;
        this.mode = mode;
        this.roomName = roomName;
    }

    private void log(String s) {
        System.out.println("[" + name + "] " + s);
    }

    private void send(Packet p) throws IOException {
        out.writeObject(p);
        out.flush();
        out.reset();
        log("--> " + p);
    }

    public void run() throws IOException, ClassNotFoundException, InterruptedException {
        Socket socket = new Socket(host, port);
        out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        in = new ObjectInputStream(socket.getInputStream());

        resetMucTieu();
        send(Packet.of(PacketType.LOGIN, name));

        try {
            while (!xong) {
                Object obj = in.readObject();
                if (obj instanceof Packet p) {
                    if (p.type() != PacketType.ROOM_LIST_DATA) {
                        log("<-- " + p);
                    }
                    handle(p);
                }
            }
        } catch (EOFException e) {
            log("Server dong ket noi.");
        }
        socket.close();
        log("Ket thuc.");
    }

    private void resetMucTieu() {
        muctieu.clear();
        for (int x = 0; x < Board.SIZE; x++) {
            for (int y = 0; y < Board.SIZE; y++) {
                muctieu.add(new int[] { x, y });
            }
        }
        Collections.shuffle(muctieu, rnd);
    }

    @SuppressWarnings("unchecked")
    private void handle(Packet p) throws IOException, InterruptedException {
        switch (p.type()) {
            case LOGIN_OK -> {
                if ("create".equalsIgnoreCase(mode)) {
                    send(Packet.of(PacketType.ROOM_CREATE, roomName));
                } else {
                    send(Packet.of(PacketType.ROOM_LIST));
                }
            }

            case ROOM_LIST_DATA -> {
                if (daVaoPhong || "create".equalsIgnoreCase(mode)) {
                    return;
                }
                ArrayList<RoomInfo> ds = p.payload(ArrayList.class);
                if (ds != null) {
                    for (RoomInfo r : ds) {
                        if (r.playerCount() < Protocol.MAX_PLAYERS_PER_ROOM) {
                            send(Packet.of(PacketType.ROOM_JOIN, String.valueOf(r.id())));
                            return;
                        }
                    }
                }
                Thread.sleep(200);
                send(Packet.of(PacketType.ROOM_LIST));
            }

            case ROOM_JOINED -> daVaoPhong = true;

            case PLACE_PHASE -> {
                if (xong) {
                    return;
                }
                Board b = new Board();
                b.randomPlace(rnd);
                resetMucTieu();
                send(Packet.withPayload(PacketType.READY, b));
            }

            case TURN -> {
                if (name.equals(p.arg(0))) {
                    banTiep();
                }
            }

            case GAME_OVER -> {
                log("KET QUA: " + p.arg(0) + " (" + p.arg(1) + ")");
                xong = true;
                send(Packet.of(PacketType.QUIT));
            }

            case ERROR -> {
                log("LOI TU SERVER: " + p.arg(0) + " - " + p.arg(1));
                if (Protocol.E_NAME_TAKEN.equals(p.arg(0))
                        || Protocol.E_NAME_INVALID.equals(p.arg(0))) {
                    xong = true;
                }
            }

            default -> {
                // khong can xu ly
            }
        }
    }

    private void banTiep() throws IOException {
        if (muctieu.isEmpty()) {
            return;
        }
        int[] t = muctieu.remove(0);
        send(Packet.of(PacketType.FIRE, String.valueOf(t[0]), String.valueOf(t[1])));
    }

    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : Protocol.DEFAULT_HOST;
        int port = args.length > 1 ? Integer.parseInt(args[1]) : Protocol.DEFAULT_PORT;
        String name = args.length > 2 ? args[2] : "bot" + new Random().nextInt(100);
        String mode = args.length > 3 ? args[3] : "join";
        String room = args.length > 4 ? args[4] : "Phong kiem thu";
        new BotClient(host, port, name, mode, room).run();
    }
}