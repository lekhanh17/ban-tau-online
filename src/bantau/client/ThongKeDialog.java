package bantau.client;

import bantau.common.MatchRecord;
import bantau.common.PlayerStats;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;

/**
 * HAI HOP THOAI DOC DU LIEU TU CSDL: bang xep hang va lich su dau.
 *
 * <p>Lop nay chi co ham tinh, khong tao doi tuong. No nhan danh sach doi
 * tuong server gui sang roi do vao {@link JTable} de hien thi.
 */
public final class ThongKeDialog {

    private ThongKeDialog() {
    }

    /** Bang chi de xem, khong sua duoc o nao. */
    private static JTable taoBang(DefaultTableModel model) {
        JTable t = new JTable(model) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        t.setRowHeight(24);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        return t;
    }

    private static JPanel khung(String tieuDe, JTable bang, String ghiChu, int rong) {
        JPanel p = new JPanel(new BorderLayout(0, 8));

        JLabel nhan = new JLabel(tieuDe, SwingConstants.CENTER);
        nhan.setFont(new Font("SansSerif", Font.BOLD, 15));
        p.add(nhan, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(bang);
        scroll.setPreferredSize(new Dimension(rong, 300));
        p.add(scroll, BorderLayout.CENTER);

        if (ghiChu != null) {
            JLabel gc = new JLabel(ghiChu, SwingConstants.CENTER);
            gc.setFont(new Font("SansSerif", Font.ITALIC, 11));
            p.add(gc, BorderLayout.SOUTH);
        }
        return p;
    }

    /** Hien bang xep hang: hang, ten, thang, thua, tong tran, ty le thang. */
    public static void hienBangXepHang(Component cha, List<PlayerStats> ds) {
        DefaultTableModel model = new DefaultTableModel(
                new Object[] { "Hạng", "Người chơi", "Thắng", "Thua", "Tổng trận", "Tỷ lệ thắng" }, 0);

        if (ds != null) {
            int hang = 1;
            for (PlayerStats p : ds) {
                model.addRow(new Object[] {
                        hang++,
                        p.username(),
                        p.wins(),
                        p.losses(),
                        p.soTran(),
                        String.format("%.0f%%", p.tyLeThang())
                });
            }
        }

        JTable bang = taoBang(model);
        bang.getColumnModel().getColumn(0).setMaxWidth(50);
        bang.getColumnModel().getColumn(2).setMaxWidth(70);
        bang.getColumnModel().getColumn(3).setMaxWidth(70);
        bang.getColumnModel().getColumn(4).setMaxWidth(80);

        String ghiChu = model.getRowCount() == 0
                ? "Chưa có ai đánh trận nào."
                : "Sắp theo số trận thắng giảm dần.";

        JOptionPane.showMessageDialog(cha,
                khung("BẢNG XẾP HẠNG", bang, ghiChu, 560),
                "Bảng xếp hạng", JOptionPane.PLAIN_MESSAGE);
    }

    /** Hien lich su dau cua chinh nguoi dang dang nhap. */
    public static void hienLichSu(Component cha, String toi, List<MatchRecord> ds) {
        DefaultTableModel model = new DefaultTableModel(
                new Object[] { "Thời gian", "Đối thủ", "Kết quả", "Lý do", "Lượt bắn", "Thời lượng" }, 0);

        int thang = 0;
        if (ds != null) {
            for (MatchRecord m : ds) {
                boolean win = m.thangBoi(toi);
                if (win) {
                    thang++;
                }
                model.addRow(new Object[] {
                        m.thoiGianBatDau(),
                        m.doiThuCua(toi),
                        win ? "THẮNG" : "THUA",
                        m.moTaLyDo(),
                        m.shots(),
                        m.soPhut() + " phút"
                });
            }
        }

        JTable bang = taoBang(model);
        // Dat be rong cho tung cot de chu khong bi cat.
        int[] rongCot = { 130, 110, 80, 150, 80, 90 };
        for (int i = 0; i < rongCot.length; i++) {
            bang.getColumnModel().getColumn(i).setPreferredWidth(rongCot[i]);
        }

        int tong = model.getRowCount();
        String ghiChu = tong == 0
                ? "Bạn chưa đánh trận nào."
                : tong + " trận gần nhất: thắng " + thang + ", thua " + (tong - thang);

        JOptionPane.showMessageDialog(cha,
                khung("LỊCH SỬ ĐẤU CỦA " + toi.toUpperCase(), bang, ghiChu, 660),
                "Lịch sử đấu", JOptionPane.PLAIN_MESSAGE);
    }
}
