package bantau.server;

import bantau.common.Board;
import bantau.common.FireResult;
import bantau.common.Packet;
import bantau.common.PacketType;
import bantau.common.Protocol;
import bantau.common.ShipType;

/**
 * MOT VAN DAU GIUA DUNG HAI NGUOI CHOI
 *
 * <p>Toan bo trang thai ban do nam o day, phia server. Client chi nhan duoc
 * ket qua tung phat ban nen khong bao gio biet vi tri tau cua doi thu.
 *
 * <p>Lop nay khong tu dong bo. No luon duoc goi tu ben trong khoi
 * {@code synchronized} cua {@link Room}, nen tai mot thoi diem chi co mot
 * thread chay trong day.
 */
public class GameSession {

    private final ClientHandler playerA;
    private final ClientHandler playerB;

    /** Ban do THAT cua tung nguoi, do server giu. */
    private Board boardA;
    private Board boardB;

    private ClientHandler turn;
    private boolean started;
    private boolean finished;

    public GameSession(ClientHandler playerA, ClientHandler playerB) {
        this.playerA = playerA;
        this.playerB = playerB;
    }

    public boolean isStarted() {
        return started;
    }

    public boolean isFinished() {
        return finished;
    }

    private ClientHandler doiThuCua(ClientHandler c) {
        return c == playerA ? playerB : playerA;
    }

    private Board banDoCua(ClientHandler c) {
        return c == playerA ? boardA : boardB;
    }

    public boolean hasBoard(ClientHandler c) {
        return banDoCua(c) != null;
    }

    /**
     * Nhan ban do da duoc kiem tra hop le cua mot nguoi choi.
     *
     * @return true neu ca hai ben deu da san sang
     */
    public boolean setBoard(ClientHandler c, Board board) {
        if (c == playerA) {
            boardA = board;
        } else {
            boardB = board;
        }
        return boardA != null && boardB != null;
    }

    /** Bat dau van dau, chon ngau nhien nguoi di truoc. */
    public void start() {
        if (started) {
            return;
        }
        started = true;
        turn = Math.random() < 0.5 ? playerA : playerB;

        String nguoiDiTruoc = turn.getUsername();
        playerA.send(Packet.of(PacketType.GAME_START, nguoiDiTruoc));
        playerB.send(Packet.of(PacketType.GAME_START, nguoiDiTruoc));
        baoLuot();
    }

    private void baoLuot() {
        String ten = turn.getUsername();
        playerA.send(Packet.of(PacketType.TURN, ten));
        playerB.send(Packet.of(PacketType.TURN, ten));
    }

    /**
     * Xu ly mot phat ban.
     *
     * <p>BA TANG KIEM TRA truoc khi chap nhan - day la phan chong gian lan:
     * <ol>
     *   <li>Van dau da bat dau va chua ket thuc chua?</li>
     *   <li>Nguoi gui co dung la nguoi dang danh luot khong?</li>
     *   <li>Toa do co trong ban do va chua ban chua?</li>
     * </ol>
     *
     * @return null neu hop le, nguoc lai la ma loi
     */
    public String fire(ClientHandler shooter, int x, int y) {
        if (!started || finished) {
            return Protocol.E_BAD_STATE;
        }
        if (shooter != turn) {
            return Protocol.E_NOT_YOUR_TURN;
        }
        if (!Board.inBounds(x, y)) {
            return Protocol.E_ALREADY_SHOT;
        }

        ClientHandler target = doiThuCua(shooter);
        Board banDoDich = banDoCua(target);

        FireResult ketQua = banDoDich.fire(x, y);
        if (ketQua == FireResult.ALREADY) {
            return Protocol.E_ALREADY_SHOT;
        }

        // Neu chim thi bao them la tau gi
        String maTau = "";
        if (ketQua == FireResult.SUNK) {
            ShipType tau = banDoDich.shipAt(x, y);
            maTau = (tau == null) ? "" : String.valueOf(tau.code());
        }

        // Nguoi ban biet ket qua phat ban cua minh
        shooter.send(Packet.of(PacketType.FIRE_RESULT,
                String.valueOf(x), String.valueOf(y), ketQua.name(), maTau));
        // Nguoi bi ban biet minh vua bi ban vao dau
        target.send(Packet.of(PacketType.INCOMING,
                String.valueOf(x), String.valueOf(y), ketQua.name(), maTau));

        // Kiem tra dieu kien thang
        if (banDoDich.allSunk()) {
            finished = true;
            shooter.send(Packet.of(PacketType.GAME_OVER,
                    Protocol.RESULT_WIN, Protocol.REASON_ALL_SUNK));
            target.send(Packet.of(PacketType.GAME_OVER,
                    Protocol.RESULT_LOSE, Protocol.REASON_ALL_SUNK));
            System.out.println("Van dau ket thuc: " + shooter.getUsername()
                    + " thang " + target.getUsername());
            return null;
        }

        // Ban trung thi duoc ban tiep, ban truot thi mat luot
        boolean giuLuot = Protocol.HIT_GRANTS_EXTRA_TURN
                && (ketQua == FireResult.HIT || ketQua == FireResult.SUNK);
        if (!giuLuot) {
            turn = target;
        }
        baoLuot();
        return null;
    }

    /** Mot nguoi thoat giua van - nguoi con lai thang. */
    public void abortBecauseLeft(ClientHandler nguoiThoat) {
        if (finished) {
            return;
        }
        finished = true;
        ClientHandler conLai = doiThuCua(nguoiThoat);
        if (started && conLai != null) {
            conLai.send(Packet.of(PacketType.GAME_OVER,
                    Protocol.RESULT_WIN, Protocol.REASON_OPPONENT_LEFT));
        }
    }
}