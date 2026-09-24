package bantau.common;

import java.io.Serializable;

/**
 * BAN SAO THONG TIN MOT PHONG, dung de hien thi o lobby.
 *
 * <p>Day la vi du ro nhat cho loi ich cua Java Serialization: server gui
 * nguyen mot doi tuong sang client, khong phai noi chuoi
 * "1,Phong cua Khanh,2,PLAYING" roi ben kia tu tach ra.
 *
 * <p>Day la doi tuong BAT BIEN (immutable) - moi truong deu final, khong co
 * ham set. Nho vay nhieu thread cung doc khong can dong bo, va client khong
 * the vo tinh sua doi tuong roi tuong da sua duoc du lieu tren server.
 */
public final class RoomInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int id;
    private final String name;
    private final int playerCount;
    private final RoomState state;

    public RoomInfo(int id, String name, int playerCount, RoomState state) {
        this.id = id;
        this.name = name;
        this.playerCount = playerCount;
        this.state = state;
    }

    public int id() {
        return id;
    }

    public String name() {
        return name;
    }

    public int playerCount() {
        return playerCount;
    }

    public RoomState state() {
        return state;
    }

    @Override
    public String toString() {
        return String.format("#%-3d %-24s %d/2  %s",
                id, name, playerCount, state.moTa());
    }
}