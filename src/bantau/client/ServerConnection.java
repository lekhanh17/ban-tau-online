package bantau.client;

import bantau.common.Packet;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import javax.swing.SwingUtilities;

/**
 * QUAN LY KET NOI TOI SERVER CHO PHIA GIAO DIEN
 *
 * <p>Lop nay la cau noi giua the gioi mang va the gioi Swing. No doc socket
 * tren MOT THREAD RIENG, sau do chuyen ban tin ve Event Dispatch Thread (EDT)
 * bang {@link SwingUtilities#invokeLater}.
 *
 * <p><b>Quy tac song con cua Swing:</b> chi duy nhat EDT duoc phep dung vao
 * giao dien. Thread doc socket ma goi thang setText() hay repaint() thi giao
 * dien se thinh thoang bi trang, treo, hoac ve sai - loi cuc ky kho tim vi
 * no khong xay ra deu dan.
 */
public class ServerConnection {

    /** Noi nhan ban tin. Cac ham nay LUON duoc goi tren EDT. */
    public interface Listener {
        void onPacket(Packet packet);

        void onDisconnected(String lyDo);
    }

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Thread readerThread;
    private Listener listener;
    private volatile boolean running;

    public boolean isConnected() {
        return running;
    }

    /**
     * Ket noi toi server.
     *
     * @throws IOException neu khong ket noi duoc trong 5 giay
     */
    public void connect(String host, int port, Listener listener) throws IOException {
        this.listener = listener;

        socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), 5000);
        socket.setTcpNoDelay(true);

        // THU TU BAT BUOC: luong ra truoc, flush(), roi moi luong vao.
        // Nguoc lai hai ben cung cho header cua nhau - treo vinh vien.
        out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        in = new ObjectInputStream(socket.getInputStream());

        running = true;
        readerThread = new Thread(this::readLoop, "server-reader");
        readerThread.setDaemon(true);
        readerThread.start();
    }

    private void readLoop() {
        String lyDo = "Server dong ket noi";
        try {
            while (running) {
                Object obj = in.readObject();
                if (obj instanceof Packet packet) {
                    // Chuyen ve EDT roi moi dung toi giao dien.
                    SwingUtilities.invokeLater(() -> listener.onPacket(packet));
                }
            }
        } catch (EOFException e) {
            lyDo = "Server da dong ket noi";
        } catch (IOException | ClassNotFoundException e) {
            lyDo = "Mat ket noi: " + e.getMessage();
        } finally {
            if (running) {
                running = false;
                final String r = lyDo;
                SwingUtilities.invokeLater(() -> listener.onDisconnected(r));
            }
        }
    }

    /** Gui mot ban tin len server. */
    public synchronized void send(Packet packet) {
        if (!running || out == null) {
            return;
        }
        try {
            out.writeObject(packet);
            out.flush();
            out.reset();
        } catch (IOException e) {
            running = false;
            final String r = "Khong gui duoc: " + e.getMessage();
            SwingUtilities.invokeLater(() -> listener.onDisconnected(r));
        }
    }

    public void close() {
        running = false;
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ignored) {
            // khong con gi de lam
        }
    }
}