package bantau.server;

import bantau.common.Board;
import bantau.common.FireResult;
import bantau.common.Packet;
import bantau.common.PacketType;
import bantau.common.Protocol;
import bantau.common.ShipType;

import java.time.LocalDateTime;

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

    /**
     * Hai nguoi choi. KHONG phai final vi khi mot nguoi rot mang roi vao
     * lai, ta phai thay tham chieu cu bang doi tuong {@link ClientHandler}
     * moi - socket cu da chet, socket moi la mot doi tuong khac hoan toan.
     * Toan bo ban do thi giu nguyen.
     */
    private ClientHandler playerA;
    private ClientHandler playerB;

    /** Ban do THAT cua tung nguoi, do server giu. */
    private Board boardA;
    private Board boardB;

    private ClientHandler turn;
    private boolean started;
    private boolean finished;

    /** Dem de ghi vao CSDL: tong so phat ban hop le cua ca hai ben. */
    private int soPhatBan;
    private LocalDateTime batDau;

    /** Moc thoi gian bat dau luot hien tai, de tinh het gio suy nghi. */
    private long luotBatDauMs;

    /** Dang tam dung vi mot ben rot mang. Dong ho luot khong chay. */
    private boolean tamDung;
    /** So giay con lai cua luot tai thoi diem tam dung. */
    private int giayConLaiKhiTamDung;

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
        batDau = LocalDateTime.now();
        turn = Math.random() < 0.5 ? playerA : playerB;

        String nguoiDiTruoc = turn.getUsername();
        playerA.send(Packet.of(PacketType.GAME_START, nguoiDiTruoc));
        playerB.send(Packet.of(PacketType.GAME_START, nguoiDiTruoc));
        baoLuot();
    }

    private void baoLuot() {
        // Moc bat dau tinh gio cho luot moi.
        luotBatDauMs = System.currentTimeMillis();

        String ten = turn.getUsername();
        String giay = String.valueOf(Protocol.TURN_SECONDS);
        playerA.send(Packet.of(PacketType.TURN, ten, giay));
        playerB.send(Packet.of(PacketType.TURN, ten, giay));
    }

    /**
     * Kiem tra nguoi dang danh co qua gio suy nghi chua.
     *
     * <p>Thread canh gac ben {@link ServerMain} goi ham nay moi giay. Qua
     * {@link Protocol#TURN_SECONDS} giay ma chua ban thi bi mat luot, du
     * dang giu luot nho ban trung di nua.
     *
     * @return true neu vua chuyen luot vi het gio
     */
    public boolean kiemTraHetGio() {
        if (!started || finished || turn == null || tamDung) {
            return false;
        }
        long daTroi = (System.currentTimeMillis() - luotBatDauMs) / 1000;
        if (daTroi < Protocol.TURN_SECONDS) {
            return false;
        }

        ClientHandler heHan = turn;
        turn = doiThuCua(heHan);

        String tb = heHan.getUsername() + " het " + Protocol.TURN_SECONDS
                + " giay suy nghi, mat luot.";
        playerA.send(Packet.of(PacketType.SYSTEM, tb));
        playerB.send(Packet.of(PacketType.SYSTEM, tb));
        System.out.println(tb);

        baoLuot();
        return true;
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
        soPhatBan++;

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
            ghiKetQua(shooter.getUsername(), Protocol.REASON_ALL_SUNK);
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

    /* ------------------------------------------------------------------ */
    /* Rot mang va vao lai van                                            */
    /* ------------------------------------------------------------------ */

    /** Ten nguoi dang danh luot, de gui cho nguoi vua vao lai. */
    public String tenNguoiDangDanh() {
        return turn == null ? "" : turn.getUsername();
    }

    /** Doi tuong handler cua nguoi dang o trong van, tim theo ten. */
    public ClientHandler timTheoTen(String ten) {
        if (ten == null) {
            return null;
        }
        if (ten.equalsIgnoreCase(playerA.getUsername())) {
            return playerA;
        }
        if (ten.equalsIgnoreCase(playerB.getUsername())) {
            return playerB;
        }
        return null;
    }

    /**
     * TAM DUNG VAN DAU vi mot ben mat ket noi.
     *
     * <p>Viec duy nhat can lam o day la DUNG DONG HO. Ban do khong bi dong
     * den, luot khong bi chuyen. Phai nho lai so giay con lai, neu khong thi
     * nguoi vao lai se duoc tron ven mot luot moi - thanh ra rot mang lai co
     * loi, nguoi choi se co dong co tu rut day mang khi bi dua vao the kho.
     */
    public void tamDung() {
        if (tamDung || !started || finished) {
            return;
        }
        giayConLaiKhiTamDung = giayConLaiCuaLuot();
        tamDung = true;
    }

    /**
     * CHAY LAI DONG HO sau khi nguoi rot mang da vao lai.
     *
     * <p>Doi moc {@code luotBatDauMs} ve qua khu dung bang phan thoi gian da
     * dung het truoc khi tam dung, nho vay so giay con lai tinh ra dung bang
     * luc tam dung.
     */
    public void tiepTuc() {
        if (!tamDung) {
            return;
        }
        int daDung = Protocol.TURN_SECONDS - giayConLaiKhiTamDung;
        luotBatDauMs = System.currentTimeMillis() - (long) daDung * 1000L;
        tamDung = false;
        baoLuot();
    }

    public boolean dangTamDung() {
        return tamDung;
    }

    /** So giay con lai cua luot hien tai, khong bao gio am. */
    public int giayConLaiCuaLuot() {
        if (tamDung) {
            return giayConLaiKhiTamDung;
        }
        long daTroi = (System.currentTimeMillis() - luotBatDauMs) / 1000;
        long conLai = Protocol.TURN_SECONDS - daTroi;
        return (int) Math.max(0, Math.min(Protocol.TURN_SECONDS, conLai));
    }

    /**
     * THAY MOT NGUOI CHOI bang ket noi moi cua chinh ho.
     *
     * <p>Van dau nhan dang nguoi choi bang THAM CHIEU doi tuong
     * {@link ClientHandler} (so sanh bang {@code ==}), khong phai bang ten.
     * Khi nguoi do vao lai, ho co mot handler hoan toan moi, nen phai thay
     * ca tham chieu {@code playerA}/{@code playerB} va ca {@code turn} neu
     * dang la luot cua ho - bo sot {@code turn} la loi kho thay nhat: van
     * dau se khong bao gio chap nhan phat ban nao cua nguoi vua vao lai nua,
     * vi {@code shooter != turn}.
     *
     * @return true neu thay duoc
     */
    public boolean thayNguoiChoi(String ten, ClientHandler moi) {
        ClientHandler cu = timTheoTen(ten);
        if (cu == null || moi == null) {
            return false;
        }
        if (cu == playerA) {
            playerA = moi;
        } else {
            playerB = moi;
        }
        if (turn == cu) {
            turn = moi;
        }
        return true;
    }

    /**
     * GUI TOAN BO TRANG THAI VAN DAU cho nguoi vua vao lai.
     *
     * <p>Client cua ho vua khoi dong lai nen khong con nho gi: khong biet
     * tau cua minh o dau, khong biet da ban nhung o nao. Server la noi duy
     * nhat con giu du lieu that, nen phai gui lai het.
     *
     * <p>Hai thong tin gui di co muc do khac nhau:
     * <ul>
     *   <li>Ban do CUA CHINH HO: gui nguyen doi tuong {@link Board}, vi do
     *       la du lieu cua ho, ho duoc biet het.</li>
     *   <li>Ban do DOI THU: chi gui chuoi
     *       {@link Board#maDoiThuThay()} - dung bang luong thong tin ho da
     *       ban ra duoc, khong tiet lo o chua ban.</li>
     * </ul>
     */
    public void guiLaiTrangThai(ClientHandler c) {
        Board cuaMinh = banDoCua(c);
        Board cuaDoiThu = banDoCua(doiThuCua(c));
        if (cuaMinh == null || cuaDoiThu == null) {
            return;
        }
        c.send(Packet.withPayload(PacketType.RESUME_DATA, cuaMinh,
                doiThuCua(c).getUsername(),
                cuaDoiThu.maDoiThuThay(),
                tenNguoiDangDanh(),
                String.valueOf(giayConLaiCuaLuot())));
    }

    /**
     * Het thoi gian an han ma nguoi rot mang khong vao lai - nguoi con lai
     * duoc xu thang, va tran dau duoc ghi vao CSDL nhu mot tran that.
     */
    public void ketThucViKhongVaoLai(ClientHandler conLai) {
        if (finished || conLai == null) {
            return;
        }
        finished = true;
        if (started) {
            conLai.send(Packet.of(PacketType.GAME_OVER,
                    Protocol.RESULT_WIN, Protocol.REASON_OPPONENT_LOST));
            ghiKetQua(conLai.getUsername(), Protocol.REASON_OPPONENT_LOST);
        }
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
            ghiKetQua(conLai.getUsername(), Protocol.REASON_OPPONENT_LEFT);
        }
    }

    /**
     * Ghi ket qua van dau xuong noi luu tru: mot dong trong bang matches,
     * va cong thang/thua cho hai nguoi choi.
     *
     * <p>Van dau chua bat dau (hai ben chua dat xong tau) thi khong ghi gi,
     * vi do khong phai mot tran that.
     */
    /** Tai khoan nay co phai doi thu may khong - nhan ra bang tien to ten. */
    private static boolean laBot(String ten) {
        return ten != null
                && ten.toLowerCase().startsWith(Protocol.BOT_PREFIX.toLowerCase());
    }

    private void ghiKetQua(String nguoiThang, String lyDo) {
        if (!started || batDau == null) {
            return;
        }
        String tenA = playerA.getUsername();
        String tenB = playerB.getUsername();

        // TRAN VOI MAY KHONG TINH VAO THANH TICH. Neu tinh thi bang xep hang
        // mat y nghia: ai cung co the keo ty so len bang cach danh voi bot
        // ca ngay. Tai khoan bot cung la tai khoan dung mot lan roi bo, de
        // lai thi bang xep hang day ten rac.
        if (laBot(tenA) || laBot(tenB)) {
            System.out.println("Tran voi doi thu may, khong ghi vao thanh tich.");
            return;
        }

        String nguoiThua = nguoiThang.equals(tenA) ? tenB : tenA;

        ServerMain.matches().luuTran(tenA, tenB, nguoiThang, lyDo,
                soPhatBan, batDau, LocalDateTime.now());
        ServerMain.players().congThang(nguoiThang);
        ServerMain.players().congThua(nguoiThua);
    }
}
