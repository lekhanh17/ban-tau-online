package bantau.tools;

import bantau.common.Board;
import bantau.common.FireResult;
import bantau.common.Orientation;
import bantau.common.ShipType;

import java.util.Random;

/**
 * KIEM THU LOP Board - KHONG CAN MANG, KHONG CAN SERVER
 *
 * <p>Day la vi du cho mot nguyen tac quan trong: tach logic game ra khoi code
 * mang. Nho vay co the kiem thu toan bo luat choi chi bang mot ham main,
 * chay trong vai phan nghin giay, khong phai bat server va mo hai client.
 *
 * <p>Cach chay: bam Run phia tren ham main.
 */
public class BoardTest {

    private static int dat = 0;
    private static int truot = 0;

    public static void main(String[] args) {
        System.out.println("=== KIEM THU LOP Board ===\n");

        testDatTau();
        testKiemTraHopLe();
        testBan();
        testThang();
        testVeBanDo();

        System.out.println("\n===============================");
        System.out.println("KET QUA: " + dat + " dat / " + truot + " truot");
        System.out.println("===============================");
    }

    /* ------------------------------------------------------------------ */

    private static void kiemTra(String nhan, boolean dieuKien) {
        if (dieuKien) {
            dat++;
            System.out.println("  [OK]   " + nhan);
        } else {
            truot++;
            System.out.println("  [SAI]  " + nhan);
        }
    }

    /* ------------------------------------------------------------------ */

    private static void testDatTau() {
        System.out.println("1. DAT TAU");
        Board b = new Board();

        kiemTra("Ban do moi chua dat du tau",
                !b.isComplete());

        kiemTra("Dat tau san bay tai (0,0) nam ngang",
                b.place(ShipType.CARRIER, 0, 0, Orientation.HORIZONTAL));

        kiemTra("O (0,0) den (4,0) thuoc tau san bay",
                b.shipAt(0, 0) == ShipType.CARRIER && b.shipAt(4, 0) == ShipType.CARRIER);

        kiemTra("O (5,0) van trong",
                b.shipAt(5, 0) == null);

        kiemTra("Chan dat tau tran vien phai",
                !b.canPlace(ShipType.BATTLESHIP, 8, 5, Orientation.HORIZONTAL));

        kiemTra("Chan dat tau tran vien duoi",
                !b.canPlace(ShipType.BATTLESHIP, 5, 8, Orientation.VERTICAL));

        kiemTra("Chan dat tau chong len tau san bay",
                !b.canPlace(ShipType.BATTLESHIP, 2, 0, Orientation.HORIZONTAL));

        kiemTra("place() tu choi vi tri chong nhau",
                !b.place(ShipType.BATTLESHIP, 2, 0, Orientation.HORIZONTAL));

        kiemTra("Dat lai chinh tau san bay sang cho khac duoc",
                b.place(ShipType.CARRIER, 0, 9, Orientation.HORIZONTAL));

        kiemTra("Vi tri cu da duoc giai phong",
                b.shipAt(0, 0) == null);

        System.out.println();
    }

    private static void testKiemTraHopLe() {
        System.out.println("2. KIEM TRA HOP LE (chong gian lan)");

        Board trong = new Board();
        String loi = trong.kiemTraHopLe();
        kiemTra("Ban do rong bi tu choi", loi != null);
        System.out.println("         -> " + loi);

        Board thieu = new Board();
        thieu.place(ShipType.CARRIER, 0, 0, Orientation.HORIZONTAL);
        thieu.place(ShipType.BATTLESHIP, 0, 1, Orientation.HORIZONTAL);
        thieu.place(ShipType.CRUISER, 0, 2, Orientation.HORIZONTAL);
        thieu.place(ShipType.SUBMARINE, 0, 3, Orientation.HORIZONTAL);
        loi = thieu.kiemTraHopLe();
        kiemTra("Ban do thieu 1 tau bi tu choi", loi != null);
        System.out.println("         -> " + loi);

        Board du = new Board();
        du.randomPlace(new Random(42));
        kiemTra("Ban do dat ngau nhien du 5 tau la hop le",
                du.kiemTraHopLe() == null);

        Board daBan = new Board();
        daBan.randomPlace(new Random(7));
        daBan.fire(0, 0);
        loi = daBan.kiemTraHopLe();
        kiemTra("Ban do da co vet ban bi tu choi", loi != null);
        System.out.println("         -> " + loi);

        System.out.println();
    }

    private static void testBan() {
        System.out.println("3. BAN");
        Board b = new Board();
        // Dat co dinh de biet truoc ket qua
        b.place(ShipType.CARRIER, 0, 0, Orientation.HORIZONTAL);    // (0,0)-(4,0)
        b.place(ShipType.BATTLESHIP, 0, 1, Orientation.HORIZONTAL); // (0,1)-(3,1)
        b.place(ShipType.CRUISER, 0, 2, Orientation.HORIZONTAL);    // (0,2)-(2,2)
        b.place(ShipType.SUBMARINE, 0, 3, Orientation.HORIZONTAL);  // (0,3)-(2,3)
        b.place(ShipType.DESTROYER, 0, 4, Orientation.HORIZONTAL);  // (0,4)-(1,4)

        kiemTra("Ban vao o trong -> TRUOT",
                b.fire(9, 9) == FireResult.MISS);

        kiemTra("Ban vao than tau -> TRUNG",
                b.fire(0, 0) == FireResult.HIT);

        kiemTra("Ban lai o cu -> DA BAN ROI",
                b.fire(0, 0) == FireResult.ALREADY);

        kiemTra("Ban ra ngoai ban do -> DA BAN ROI",
                b.fire(99, 99) == FireResult.ALREADY);

        kiemTra("Ban o thu hai cua tau khu truc -> TRUNG",
                b.fire(0, 4) == FireResult.HIT);

        kiemTra("Ban o cuoi cung cua tau khu truc -> CHIM",
                b.fire(1, 4) == FireResult.SUNK);

        kiemTra("Tau khu truc da chim",
                b.isSunk(ShipType.DESTROYER));

        kiemTra("Tau san bay chua chim",
                !b.isSunk(ShipType.CARRIER));

        kiemTra("Con lai 4 tau",
                b.remainingShips() == 4);

        System.out.println();
    }

    private static void testThang() {
        System.out.println("4. DIEU KIEN THANG");
        Board b = new Board();
        b.randomPlace(new Random(2024));

        kiemTra("Dau van chua thang", !b.allSunk());

        // Ban het ca ban do
        int soPhatTrung = 0;
        for (int x = 0; x < Board.SIZE; x++) {
            for (int y = 0; y < Board.SIZE; y++) {
                FireResult r = b.fire(x, y);
                if (r == FireResult.HIT || r == FireResult.SUNK) {
                    soPhatTrung++;
                }
            }
        }

        kiemTra("Ban het ban do thi trung dung " + ShipType.totalCells() + " phat",
                soPhatTrung == ShipType.totalCells());

        kiemTra("Da chim het tau -> thang", b.allSunk());

        kiemTra("Khong con tau nao", b.remainingShips() == 0);

        System.out.println();
    }

    private static void testVeBanDo() {
        System.out.println("5. VE BAN DO");
        Board b = new Board();
        b.place(ShipType.CARRIER, 0, 0, Orientation.HORIZONTAL);
        b.place(ShipType.DESTROYER, 8, 8, Orientation.VERTICAL);
        b.fire(0, 0);   // trung tau san bay
        b.fire(5, 5);   // truot
        b.fire(8, 8);   // trung tau khu truc
        b.fire(8, 9);   // chim tau khu truc

        System.out.println("\n--- BAN DO CUA MINH (hien tau) ---");
        System.out.print(b.veBanDo(true));

        System.out.println("\n--- BAN DO DOI THU (giau tau) ---");
        System.out.print(b.veBanDo(false));

        System.out.println("\n" + Board.chuGiai());

        kiemTra("Ve ban do khong loi", b.veBanDo(true).length() > 100);
        System.out.println();
    }
}