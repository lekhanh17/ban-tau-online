package bantau.client;

import bantau.common.RoomInfo;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

/** Danh sach phong: tao phong moi hoac vao phong dang cho. */
public class LobbyPanel extends JPanel {

    private final ClientMain app;

    private final DefaultTableModel model = new DefaultTableModel(
            new Object[] { "ID", "Ten phong", "Nguoi choi", "Trang thai" }, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);
    private final JLabel welcomeLabel = new JLabel(" ");

    public LobbyPanel(ClientMain app) {
        this.app = app;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        welcomeLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        add(welcomeLabel, BorderLayout.NORTH);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(26);
        table.getColumnModel().getColumn(0).setMaxWidth(60);
        table.getColumnModel().getColumn(2).setMaxWidth(100);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JButton rankButton = new JButton("Bang xep hang");
        JButton historyButton = new JButton("Lich su dau");
        JButton refreshButton = new JButton("Lam moi");
        JButton createButton = new JButton("Tao phong moi");
        JButton joinButton = new JButton("Vao phong");

        rankButton.addActionListener(e -> app.doXemBangXepHang());
        historyButton.addActionListener(e -> app.doXemLichSu());
        refreshButton.addActionListener(e -> app.doRefreshRooms());
        createButton.addActionListener(e -> {
            String ten = JOptionPane.showInputDialog(this,
                    "Ten phong:", "Tao phong", JOptionPane.PLAIN_MESSAGE);
            if (ten != null) {
                app.doCreateRoom(ten.trim());
            }
        });
        joinButton.addActionListener(e -> vaoPhongDangChon());

        // Ben trai la hai nut xem thong ke tu CSDL, ben phai la nut thao tac phong.
        JPanel thongKe = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        thongKe.add(rankButton);
        thongKe.add(historyButton);

        JPanel thaoTac = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        thaoTac.add(refreshButton);
        thaoTac.add(createButton);
        thaoTac.add(joinButton);

        JPanel buttons = new JPanel(new BorderLayout());
        buttons.add(thongKe, BorderLayout.WEST);
        buttons.add(thaoTac, BorderLayout.EAST);
        add(buttons, BorderLayout.SOUTH);

        // Nhay dup chuot de vao phong cho nhanh.
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    vaoPhongDangChon();
                }
            }
        });
    }

    private void vaoPhongDangChon() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Hay chon mot phong trong danh sach.");
            return;
        }
        int id = Integer.parseInt(String.valueOf(model.getValueAt(row, 0)));
        app.doJoinRoom(id);
    }

    public void setUsername(String username) {
        welcomeLabel.setText("Xin chao " + username
                + " - chon mot phong hoac tao phong moi");
    }

    /**
     * Cap nhat bang tu danh sach doi tuong RoomInfo.
     *
     * <p>Khong phai tach chuoi gi ca - server gui thang doi tuong sang.
     */
    public void updateRooms(List<RoomInfo> danhSach) {
        // Nho lai phong dang chon de chon lai sau khi lam moi.
        int idDangChon = -1;
        int row = table.getSelectedRow();
        if (row >= 0) {
            idDangChon = Integer.parseInt(String.valueOf(model.getValueAt(row, 0)));
        }

        model.setRowCount(0);
        if (danhSach != null) {
            for (RoomInfo r : danhSach) {
                model.addRow(new Object[] {
                        r.id(), r.name(), r.playerCount() + "/2", r.state().moTa() });
            }
        }

        if (idDangChon >= 0) {
            for (int i = 0; i < model.getRowCount(); i++) {
                if (String.valueOf(idDangChon).equals(String.valueOf(model.getValueAt(i, 0)))) {
                    table.setRowSelectionInterval(i, i);
                    break;
                }
            }
        }
    }
}
