package bantau.client;

import bantau.common.Protocol;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

/**
 * MAN HINH DAU TIEN: dia chi server, ten nguoi choi va mat khau.
 *
 * <p>Hai nut: "Dang nhap" cho tai khoan da co, "Dang ky" tao tai khoan moi.
 *
 * <p>Dung {@link JPasswordField} chu khong phai {@link JTextField} de mat
 * khau hien thanh dau cham. Lay noi dung bang {@code getPassword()} tra ve
 * mang char chu khong phai String - vi String nam lai trong bo nho Java
 * cho toi khi bo thu gom rac don, con mang char thi xoa trang duoc ngay.
 */
public class LoginPanel extends JPanel {

    private final JTextField hostField = new JTextField(Protocol.DEFAULT_HOST, 14);
    private final JTextField portField = new JTextField(String.valueOf(Protocol.DEFAULT_PORT), 6);
    private final JTextField nameField = new JTextField(12);
    private final JPasswordField passField = new JPasswordField(12);
    private final JButton loginButton = new JButton("Dang nhap");
    private final JButton registerButton = new JButton("Dang ky");
    private final JLabel statusLabel = new JLabel(" ", SwingConstants.CENTER);

    public LoginPanel(ClientMain app) {
        setLayout(new BorderLayout(0, 10));
        setBorder(BorderFactory.createEmptyBorder(40, 60, 40, 60));

        JLabel title = new JLabel("BAN TAU ONLINE", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 28));
        title.setForeground(new Color(0x1F618D));
        add(title, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 6, 6, 6);
        gc.anchor = GridBagConstraints.WEST;

        gc.gridx = 0;
        gc.gridy = 0;
        form.add(new JLabel("Dia chi server:"), gc);
        gc.gridx = 1;
        form.add(hostField, gc);

        gc.gridx = 0;
        gc.gridy = 1;
        form.add(new JLabel("Cong:"), gc);
        gc.gridx = 1;
        form.add(portField, gc);

        gc.gridx = 0;
        gc.gridy = 2;
        form.add(new JLabel("Ten nguoi choi:"), gc);
        gc.gridx = 1;
        form.add(nameField, gc);

        gc.gridx = 0;
        gc.gridy = 3;
        form.add(new JLabel("Mat khau:"), gc);
        gc.gridx = 1;
        form.add(passField, gc);

        JPanel nutRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        loginButton.setPreferredSize(new Dimension(130, 34));
        registerButton.setPreferredSize(new Dimension(130, 34));
        nutRow.add(loginButton);
        nutRow.add(registerButton);

        gc.gridx = 0;
        gc.gridy = 4;
        gc.gridwidth = 2;
        gc.fill = GridBagConstraints.HORIZONTAL;
        form.add(nutRow, gc);

        gc.gridy = 5;
        JLabel ghiChu = new JLabel(
                "<html><i>Chua co tai khoan? Nhap ten va mat khau roi bam Dang ky."
                        + "<br>Mat khau dai " + Protocol.PASS_MIN + "-" + Protocol.PASS_MAX
                        + " ky tu.</i></html>", SwingConstants.CENTER);
        ghiChu.setFont(new Font("SansSerif", Font.PLAIN, 11));
        ghiChu.setForeground(new Color(0x7F8C8D));
        form.add(ghiChu, gc);

        add(form, BorderLayout.CENTER);

        statusLabel.setForeground(new Color(0xC0392B));
        add(statusLabel, BorderLayout.SOUTH);

        loginButton.addActionListener(e -> gui(app, false));
        registerButton.addActionListener(e -> gui(app, true));
        // Go Enter o o mat khau la dang nhap luon cho nhanh.
        passField.addActionListener(e -> gui(app, false));
        nameField.addActionListener(e -> passField.requestFocusInWindow());
    }

    /** Doc o nhap roi goi ClientMain. {@code dangKy} = true thi tao tai khoan moi. */
    private void gui(ClientMain app, boolean dangKy) {
        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException e) {
            setStatus("Cong phai la so");
            return;
        }

        char[] mk = passField.getPassword();
        String matKhau = new String(mk);
        java.util.Arrays.fill(mk, '\0');   // xoa dau vet trong bo nho

        app.doConnect(hostField.getText().trim(), port,
                nameField.getText().trim(), matKhau, dangKy);
    }

    public void setStatus(String text) {
        statusLabel.setText(text);
    }

    /** Doi mau chu trang thai: xanh la bao tin vui, do la bao loi. */
    public void setStatusOk(String text) {
        statusLabel.setForeground(new Color(0x1E8449));
        statusLabel.setText(text);
    }

    public void setStatusLoi(String text) {
        statusLabel.setForeground(new Color(0xC0392B));
        statusLabel.setText(text);
    }

    public void setBusy(boolean busy) {
        loginButton.setEnabled(!busy);
        registerButton.setEnabled(!busy);
    }

    public void focusName() {
        nameField.requestFocusInWindow();
    }

    /** Dien san dia chi server truyen qua tham so dong lenh. */
    public void presetServer(String host, String port) {
        hostField.setText(host);
        portField.setText(port);
    }
}
