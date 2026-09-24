package bantau.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * BAN DO 10x10 CUA MOT NGUOI CHOI
 *
 * <p>Lop nay duoc dung o CA HAI phia:
 * <ul>
 *   <li>Client: nguoi choi keo tha dat tau, xem truoc so do cua minh.</li>
 *   <li>Server: ban sao DUY NHAT co gia tri phap ly. Moi phat ban deu tinh
 *       tren ban do cua server nen client khong the gian lan.</li>
 * </ul>
 *
 * <p>Quy uoc toa do: {@code grid[x][y]} voi x la cot (trai sang phai),
 * y la hang (tren xuong duoi), goc trai tren la (0,0).
 *
 * <p><b>CANH BAO BAO MAT:</b> lop nay Serializable nen client co the gui
 * nguyen doi tuong Board len server. Nhung Java khoi phuc doi tuong ma
 * KHONG GOI CONSTRUCTOR - moi rang buoc trong constructor bi bo qua.
 * Vi vay server BAT BUOC phai goi {@link #kiemTraHopLe()} sau khi nhan,
 * khong duoc tin ban do client gui len.
 */
public class Board implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final int SIZE = 10;

    /** O nao thuoc tau nao. null la o trong. */
    private final ShipType[][] grid = new ShipType[SIZE][SIZE];
    /** O nao da bi ban. */
    private final boolean[][] shot = new boolean[SIZE][SIZE];
    /** Vi tri goc cua tung tau: {x, y, ma huong}. */
    private final Map<ShipType, int[]> origins = new EnumMap<>(ShipType.class);
    /** So o da trung cua tung tau. */
    private final Map<ShipType, Integer> hits = new EnumMap<>(ShipType.class);

    /* ------------------------------------------------------------------ */
    /* Dat tau                                                            */
    /* ------------------------------------------------------------------ */

    public static boolean inBounds(int x, int y) {
        return x >= 0 && x < SIZE && y >= 0 && y < SIZE;
    }

    /** Danh sach o ma tau se chiem neu dat tai (x, y) theo huong o. */
    public static List<int[]> cellsOf(ShipType type, int x, int y, Orientation o) {
        List<int[]> cells = new ArrayList<>(type.size());
        for (int i = 0; i < type.size(); i++) {
            int cx = (o == Orientation.HORIZONTAL) ? x + i : x;
            int cy = (o == Orientation.VERTICAL) ? y + i : y;
            cells.add(new int[] { cx, cy });
        }
        return cells;
    }

    /** Co the dat tau o vi tri nay khong: khong tran vien, khong chong tau khac. */
    public boolean canPlace(ShipType type, int x, int y, Orientation o) {
        if (type == null || o == null) {
            return false;
        }
        for (int[] c : cellsOf(type, x, y, o)) {
            if (!inBounds(c[0], c[1])) {
                return false;
            }
            ShipType daCo = grid[c[0]][c[1]];
            // Cho phep de len chinh no (truong hop dat lai cung mot tau).
            if (daCo != null && daCo != type) {
                return false;
            }
        }
        return true;
    }

    /** Dat tau. Neu tau da duoc dat truoc do thi tu dong go ra roi dat lai. */
    public boolean place(ShipType type, int x, int y, Orientation o) {
        if (!canPlace(type, x, y, o)) {
            return false;
        }
        remove(type);
        for (int[] c : cellsOf(type, x, y, o)) {
            grid[c[0]][c[1]] = type;
        }
        origins.put(type, new int[] { x, y, o.code() });
        hits.put(type, 0);
        return true;
    }

    /** Go mot tau ra khoi ban do. */
    public void remove(ShipType type) {
        if (!origins.containsKey(type)) {
            return;
        }
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                if (grid[x][y] == type) {
                    grid[x][y] = null;
                }
            }
        }
        origins.remove(type);
        hits.remove(type);
    }

    public void clear() {
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                grid[x][y] = null;
                shot[x][y] = false;
            }
        }
        origins.clear();
        hits.clear();
    }

    /** Da dat du ca 5 tau chua. */
    public boolean isComplete() {
        return origins.size() == ShipType.values().length;
    }

    /** Dat ngau nhien ca 5 tau. Dung cho nut "Dat ngau nhien" va cho bot test. */
    public void randomPlace(Random rnd) {
        clear();
        for (ShipType type : ShipType.values()) {
            boolean placed = false;
            while (!placed) {
                Orientation o = rnd.nextBoolean() ? Orientation.HORIZONTAL : Orientation.VERTICAL;
                placed = place(type, rnd.nextInt(SIZE), rnd.nextInt(SIZE), o);
            }
        }
    }

    /* ------------------------------------------------------------------ */
    /* Kiem tra hop le - LA CHAN CHONG GIAN LAN                           */
    /* ------------------------------------------------------------------ */

    /**
     * Kiem tra ban do do client gui len co hop le khong.
     *
     * <p>Day la ham quan trong nhat ve mat bao mat cua ca do an. Server phai
     * goi ham nay truoc khi chap nhan so do dat tau.
     *
     * @return null neu hop le, nguoc lai la mo ta loi
     */
    public String kiemTraHopLe() {
        // 1. Du 5 tau chua
        if (origins.size() != ShipType.values().length) {
            return "Thieu tau: moi dat " + origins.size() + "/"
                    + ShipType.values().length + " tau";
        }

        // 2. Chua duoc ban phat nao (client khong duoc gui ban do da danh do)
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                if (shot[x][y]) {
                    return "Ban do da co vet ban, khong phai ban do moi";
                }
            }
        }
        for (Integer h : hits.values()) {
            if (h != null && h != 0) {
                return "Ban do da co tau bi trung, khong phai ban do moi";
            }
        }

        // 3. Tung tau: vi tri hop le, so o dung, khop voi luoi
        int tongO = 0;
        for (ShipType type : ShipType.values()) {
            int[] o = origins.get(type);
            if (o == null || o.length != 3) {
                return "Tau " + type.label() + " thieu thong tin vi tri";
            }
            Orientation huong = Orientation.fromCode((char) o[2]);

            for (int[] c : cellsOf(type, o[0], o[1], huong)) {
                if (!inBounds(c[0], c[1])) {
                    return "Tau " + type.label() + " tran ra ngoai ban do";
                }
                if (grid[c[0]][c[1]] != type) {
                    return "Tau " + type.label() + " khong khop voi luoi o vi tri ("
                            + c[0] + "," + c[1] + ")";
                }
            }
            tongO += type.size();
        }

        // 4. Tong so o bi chiem phai dung bang 17.
        // Buoc nay bat duoc truong hop hai tau chong len nhau: khi do so o
        // thuc te tren luoi se it hon tong kich thuoc cac tau.
        int oThucTe = 0;
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                if (grid[x][y] != null) {
                    oThucTe++;
                }
            }
        }
        if (oThucTe != tongO) {
            return "Cac tau chong len nhau: dem duoc " + oThucTe
                    + " o, dang le phai co " + tongO + " o";
        }

        return null;
    }

    /* ------------------------------------------------------------------ */
    /* Ban                                                                */
    /* ------------------------------------------------------------------ */

    /**
     * Ban vao o (x, y) va cap nhat trang thai.
     *
     * @return ALREADY neu o ngoai ban do hoac da ban roi
     */
    public FireResult fire(int x, int y) {
        if (!inBounds(x, y) || shot[x][y]) {
            return FireResult.ALREADY;
        }
        shot[x][y] = true;

        ShipType type = grid[x][y];
        if (type == null) {
            return FireResult.MISS;
        }
        int h = hits.getOrDefault(type, 0) + 1;
        hits.put(type, h);
        return h >= type.size() ? FireResult.SUNK : FireResult.HIT;
    }

    public boolean isShot(int x, int y) {
        return inBounds(x, y) && shot[x][y];
    }

    public ShipType shipAt(int x, int y) {
        return inBounds(x, y) ? grid[x][y] : null;
    }

    public boolean isSunk(ShipType type) {
        return hits.getOrDefault(type, 0) >= type.size();
    }

    /** Da chim het 5 tau chua - dieu kien ket thuc van dau. */
    public boolean allSunk() {
        if (!isComplete()) {
            return false;
        }
        for (ShipType t : ShipType.values()) {
            if (!isSunk(t)) {
                return false;
            }
        }
        return true;
    }

    public int remainingShips() {
        int n = 0;
        for (ShipType t : ShipType.values()) {
            if (origins.containsKey(t) && !isSunk(t)) {
                n++;
            }
        }
        return n;
    }

    public int[] originOf(ShipType type) {
        int[] o = origins.get(type);
        return o == null ? null : new int[] { o[0], o[1], o[2] };
    }

    /* ------------------------------------------------------------------ */
    /* Ve ban do bang ky tu                                               */
    /* ------------------------------------------------------------------ */

    /**
     * Ve ban do ra chuoi de in len man hinh console.
     *
     * @param hienTau true thi hien vi tri tau (ban do cua minh),
     *                false thi giau di (ban do doi thu)
     */
    public String veBanDo(boolean hienTau) {
        StringBuilder sb = new StringBuilder();
        sb.append("    A B C D E F G H I J\n");
        for (int y = 0; y < SIZE; y++) {
            sb.append(String.format("%2d  ", y + 1));
            for (int x = 0; x < SIZE; x++) {
                sb.append(kyTuO(x, y, hienTau)).append(' ');
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    private char kyTuO(int x, int y, boolean hienTau) {
        ShipType t = grid[x][y];
        if (shot[x][y]) {
            if (t == null) {
                return 'o';                       // ban truot
            }
            return isSunk(t) ? '#' : 'X';         // chim / trung
        }
        if (hienTau && t != null) {
            return t.code();                      // tau cua minh chua bi ban
        }
        return '.';                               // o chua biet
    }

    /** Chu giai cho ban do ky tu. */
    public static String chuGiai() {
        return ".  o chua ban    o  ban truot    X  trung    #  tau da chim\n"
                + "C  Tau san bay  B  Thiet giap ham  R  Tuan duong ham  "
                + "S  Tau ngam  D  Tau khu truc";
    }
}