package bantau.client;

import bantau.common.Protocol;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

/** Man hinh dau tien: nhap dia chi server va ten nguoi choi. */
public class LoginPanel extends JPanel {

    private final JTextField hostField = new JTextField(Protocol.DEFAULT_HOST, 14);
    private final JTextField portField = new JTextField(String.valueOf(Protocol.DEFAULT_PORT), 6);
    private final JTextField nameField = new JTextField(12);
    private final JButton connectButton = new JButton("Ket noi");
    private final JLabel statusLabel = new JLabel(" ", SwingConstants.CENTER);

    public LoginPanel(ClientMain app) {
        setLayout(new BorderLayout(0, 10));
        setBorder(BorderFactory.createEmptyBorder(50, 60, 50, 60));

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
        gc.gridwidth = 2;
        gc.fill = GridBagConstraints.HORIZONTAL;
        connectButton.setPreferredSize(new Dimension(120, 34));
        form.add(connectButton, gc);

        add(form, BorderLayout.CENTER);

        statusLabel.setForeground(new Color(0xC0392B));
        add(statusLabel, BorderLayout.SOUTH);

        Runnable guiDi = () -> {
            int port;
            try {
                port = Integer.parseInt(portField.getText().trim());
            } catch (NumberFormatException e) {
                setStatus("Cong phai la so");
                return;
            }
            app.doConnect(hostField.getText().trim(), port, nameField.getText().trim());
        };
        connectButton.addActionListener(e -> guiDi.run());
        nameField.addActionListener(e -> guiDi.run());
    }

    public void setStatus(String text) {
        statusLabel.setText(text);
    }

    public void setBusy(boolean busy) {
        connectButton.setEnabled(!busy);
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