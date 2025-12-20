package com.whiteboard.ui;

import javax.swing.*;
import java.awt.*;

public class StartupDialog extends JDialog {
    private final JTextField nameField;
    private final JTextField roomField;
    private final JPasswordField roomPasswordField;
    private final JRadioButton joinRadio;
    private final JRadioButton createRadio;
    private boolean confirmed = false;

    public StartupDialog(Frame owner, String defaultName, String defaultRoom) {
        super(owner, "Welcome", true);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        JPanel main = new JPanel(new BorderLayout(10, 10));
        main.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel title = new JLabel("P2P Whiteboard");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        JLabel subtitle = new JLabel("Choose your name and room to start drawing together");
        subtitle.setFont(subtitle.getFont().deriveFont(Font.PLAIN, 11f));
        JPanel header = new JPanel(new BorderLayout());
        header.add(title, BorderLayout.NORTH);
        header.add(subtitle, BorderLayout.SOUTH);
        main.add(header, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        form.add(new JLabel("Your name:"), gbc);
        gbc.gridx = 1;
        nameField = new JTextField(18);
        nameField.setText(defaultName);
        form.add(nameField, gbc);

        // Join/Create choice
        gbc.gridx = 0; gbc.gridy = 1;
        form.add(new JLabel("Mode:"), gbc);
        gbc.gridx = 1;
        JPanel modePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        joinRadio = new JRadioButton("Join room");
        createRadio = new JRadioButton("Create room");
        ButtonGroup group = new ButtonGroup();
        group.add(joinRadio);
        group.add(createRadio);
        createRadio.setSelected(true);
        modePanel.add(joinRadio);
        modePanel.add(createRadio);
        form.add(modePanel, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        form.add(new JLabel("Room code:"), gbc);
        gbc.gridx = 1;
        roomField = new JTextField(18);
        roomField.setText(defaultRoom);
        form.add(roomField, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        form.add(new JLabel("Room password:"), gbc);
        gbc.gridx = 1;
        roomPasswordField = new JPasswordField(18);
        form.add(roomPasswordField, gbc);

        main.add(form, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton ok = new JButton("Start");
        JButton cancel = new JButton("Exit");

        ok.addActionListener(e -> {
            confirmed = true;
            dispose();
        });
        cancel.addActionListener(e -> {
            confirmed = false;
            dispose();
        });

        buttons.add(ok);
        buttons.add(cancel);
        main.add(buttons, BorderLayout.SOUTH);

        setContentPane(main);
        pack();
        setMinimumSize(getSize());
        setLocationRelativeTo(owner);
        setResizable(false);
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public String getPeerName() {
        String text = nameField.getText().trim();
        return text.isEmpty() ? nameField.getText() : text;
    }

    public String getRoomCode() {
        String text = roomField.getText().trim();
        return text.isEmpty() ? "room-1" : text;
    }

    public String getRoomPassword() {
        String text = new String(roomPasswordField.getPassword()).trim();
        return text;
    }

    public boolean isJoinMode() {
        return joinRadio.isSelected();
    }
}
