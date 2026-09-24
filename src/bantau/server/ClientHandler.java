package bantau.server;

import bantau.common.Message;
import bantau.common.Protocol;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * GIAI DOAN 3 - Mot thread phuc vu mot client, co phan giai lenh.
 *
 * <p>Diem moi: handler co TRANG THAI. Truoc khi dang nhap thi chi chap nhan
 * lenh LOGIN, sau khi dang nhap moi mo cac lenh con lai. Day la mam mong cua
 * may trang thai se dung o cac giai doan sau (cho phong, dat tau, danh nhau).
 */
public class ClientHandler implements Runnable {

    /** Bien dich san mau ten cho nhanh, thay vi bien dich lai moi lan kiem tra. */
    private static final Pattern NAME_RULE = Pattern.compile(Protocol.NAME_PATTERN);

    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;

    /**
     * Ten nguoi choi. null nghia la CHUA dang nhap.
     * Dung volatile vi thread khac co the doc bien nay khi broadcast.
     */
    private volatile String username;

    public ClientHandler(Socket socket) throws IOException {
        this.socket = socket;
        this.in = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.out = new PrintWriter(
                new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
    }

    public String getUsername() {
        return username == null ? "?" : username;
    }

    /** Gui ban tin xuong client nay. Dong bo vi nhieu thread cung goi. */
    public synchronized void send(String msg) {
        out.print(msg + "\n");
        out.flush();
    }

    private void sendError(String code, String moTa) {
        send(Message.build(Protocol.S_ERROR, code, moTa));
    }

    /* ------------------------------------------------------------------ */
    /* Vong doi                                                           */
    /* ------------------------------------------------------------------ */

    @Override
    public void run() {
        System.out.println("Ket noi moi tu " + socket.getRemoteSocketAddress());
        try {
            send(Message.build(Protocol.S_SYSTEM,
                    "Chao mung! Hay dang nhap: LOGIN|<ten cua ban>"));

            String line;
            while ((line = in.readLine()) != null) {
                Message msg = Message.parse(line);
                if (msg == null) {
                    continue;
                }
                System.out.println("  <-- " + getUsername() + " : " + msg);

                if (Protocol.C_QUIT.equals(msg.command())) {
                    break;
                }
                handle(msg);
            }

        } catch (IOException e) {
            System.out.println(getUsername() + " mat ket noi: " + e.getMessage());

        } finally {
            donDep();
        }
    }

    /**
     * PHAN GIAI LENH - trai tim cua giao thuc.
     *
     * <p>Chia lam hai vung: truoc dang nhap va sau dang nhap.
     */
    private void handle(Message msg) {
        String cmd = msg.command();

        // ----- VUNG 1: chua dang nhap, chi cho phep LOGIN -----
        if (username == null) {
            if (Protocol.C_LOGIN.equals(cmd)) {
                xuLyDangNhap(msg.arg(0));
            } else {
                sendError(Protocol.E_NOT_LOGGED_IN,
                        "Ban phai dang nhap truoc bang lenh LOGIN|<ten>");
            }
            return;
        }

        // ----- VUNG 2: da dang nhap -----
        switch (cmd) {
            case Protocol.C_LOGIN ->
                    sendError(Protocol.E_BAD_STATE, "Ban da dang nhap voi ten " + username);

            case Protocol.C_CHAT -> {
                String noiDung = msg.tail(0);
                if (!noiDung.isBlank()) {
                    ServerMain.broadcast(Message.build(Protocol.S_CHAT, username, noiDung));
                }
            }

            case Protocol.C_WHO ->
                    send(Message.build(Protocol.S_WHO, ServerMain.onlineNames()));

            default ->
                    sendError(Protocol.E_UNKNOWN_CMD, "Lenh khong ton tai: " + cmd);
        }
    }

    /**
     * Kiem tra va chap nhan ten dang nhap.
     *
     * <p>Hai tang kiem tra:
     * <ol>
     *   <li>Dinh dang co hop le khong (do dai, ky tu cho phep).</li>
     *   <li>Ten da co nguoi khac dung chua.</li>
     * </ol>
     */
    private void xuLyDangNhap(String ten) {
        String n = ten == null ? "" : ten.trim();

        if (!NAME_RULE.matcher(n).matches()) {
            sendError(Protocol.E_NAME_INVALID,
                    "Ten phai dai " + Protocol.NAME_MIN + "-" + Protocol.NAME_MAX
                            + " ky tu, chi gom chu cai, so va dau gach duoi");
            return;
        }

        if (!ServerMain.registerUser(n, this)) {
            sendError(Protocol.E_NAME_TAKEN, "Ten '" + n + "' dang co nguoi khac dung");
            return;
        }

        username = n;
        send(Message.build(Protocol.S_LOGIN_OK, n));
        send(Message.build(Protocol.S_SYSTEM,
                "Dang co " + ServerMain.onlineCount() + " nguoi online. "
                        + "Lenh: CHAT|<noi dung>, WHO, QUIT"));
        ServerMain.broadcast(Message.build(Protocol.S_SYSTEM, n + " da vao phong"));
        System.out.println("Dang nhap: " + n + " (online: " + ServerMain.onlineCount() + ")");
    }

    /** Luon chay du thoat binh thuong hay do loi. */
    private void donDep() {
        String ten = username;
        if (ten != null) {
            ServerMain.unregisterUser(ten);
            ServerMain.broadcast(Message.build(Protocol.S_SYSTEM, ten + " da roi di"));
        }
        try {
            socket.close();
        } catch (IOException ignored) {
            // khong con gi de lam
        }
        System.out.println(getUsername() + " da ngat. Con lai "
                + ServerMain.onlineCount() + " nguoi.");
    }
}