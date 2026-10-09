package bantau.common;

/**
 * BA MUC DO KHO CUA DOI THU MAY
 *
 * <p>Ba muc do nay khong chi de nguoi choi chon cho vui - chung la <b>ba
 * thuat toan khac nhau dat canh nhau de so sanh duoc</b>. Vi so phat can de
 * ban chim het 17 o tau la mot con so do duoc, nen ba muc do cho ra ba con
 * so, va chenh lech giua chung chung minh thuat toan co tac dung that chu
 * khong phai noi suong.
 *
 * <p>Xem {@code bantau.tools.MoPhongBot} de biet cach do, va
 * {@code bantau.bot.BanDoXacSuat} cho thuat toan cua muc KHO.
 */
public enum MucDoBot {

    /**
     * Ban hoan toan ngau nhien vao o chua ban.
     *
     * <p>Muc nay ton tai de lam <b>moc so sanh</b>: no khong dung mot chut
     * thong tin nao tu ket qua cac phat truoc. Moi thuat toan khac phai tot
     * hon no ro ret, neu khong thi coi nhu khong lam duoc gi.
     */
    DE("De", "Ban ngau nhien, khong suy luan gi"),

    /**
     * San va diet (hunt and target) + quet theo luoi ban co.
     *
     * <p>Ban trung thi thu bon o ke; het o ke thi ban mo moi vao cac o co
     * {@code (x + y)} chan. Dung thong tin tu phat truoc, nhung chi o muc
     * cuc bo - khong biet suy luan toan ban do.
     */
    THUONG("Thuong", "San - diet, quet theo luoi ban co"),

    /**
     * Thuat toan xac suat: dem so cach dat cac tau con lai di qua tung o.
     *
     * <p>Manh nhat trong ba muc va la muc duy nhat suy luan tren TOAN BO ban
     * do: no tu loai duoc nhung vung trong qua hep khong chua noi tau nao, va
     * tu biet con tau dai 5 o chi con nam duoc o dau khi ban do da bi ban
     * nhieu. Xem {@code bantau.bot.BanDoXacSuat}.
     */
    KHO("Kho", "Tinh xac suat tung o tu cac cach dat tau con lai");

    private final String ten;
    private final String moTa;

    MucDoBot(String ten, String moTa) {
        this.ten = ten;
        this.moTa = moTa;
    }

    /** Ten ngan de hien tren giao dien. */
    public String ten() {
        return ten;
    }

    /** Mot dong giai thich, dung lam tooltip. */
    public String moTa() {
        return moTa;
    }

    /**
     * Doi chuoi tu ban tin {@code ADD_BOT} ve muc do.
     *
     * <p>Chuoi rong hoac khong hop le thi tra ve {@link #THUONG}. Nho vay
     * mot client ban cu - khong gui tham so muc do - van choi duoc binh
     * thuong thay vi bi loi.
     */
    public static MucDoBot tuChuoi(String s) {
        if (s != null) {
            for (MucDoBot m : values()) {
                if (m.name().equalsIgnoreCase(s.trim())) {
                    return m;
                }
            }
        }
        return THUONG;
    }

    @Override
    public String toString() {
        return ten;
    }
}
