package com.whiteboard.ui;

import com.whiteboard.network.NetworkProtocol;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.Consumer;

public class ChatPanel extends JPanel {
    private final JTextPane messagePane;
    private final JTextField inputField;
    private final JButton sendButton;
    private Consumer<String> onSend;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

    public ChatPanel() {
        setLayout(new BorderLayout(6, 6));
        messagePane = new JTextPane();
        messagePane.setEditable(false);
        messagePane.setContentType("text/plain");
        JScrollPane scroll = new JScrollPane(messagePane);
        DefaultCaret caret = (DefaultCaret) messagePane.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        inputField = new JTextField();
        sendButton = new JButton("Send");

        JPanel bottom = new JPanel(new BorderLayout(4, 4));
        bottom.add(inputField, BorderLayout.CENTER);
        bottom.add(sendButton, BorderLayout.EAST);

        add(scroll, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendCurrent();
            }
        });

        inputField.addActionListener(e -> sendCurrent());
    }

    private void sendCurrent() {
        String text = inputField.getText();
        if (text == null)
            return;
        text = text.trim();
        if (text.isEmpty())
            return;
        if (onSend != null) {
            onSend.accept(text);
        }
        inputField.setText("");
    }

    public void setOnSend(Consumer<String> callback) {
        this.onSend = callback;
    }

    public void addMessage(NetworkProtocol.ChatMessage chat) {
        if (chat == null)
            return;
        String time = timeFormat.format(new Date(chat.timestamp));
        String header = String.format("[%s] %s: ", time, chat.senderName != null ? chat.senderName : chat.senderId);
        String full = header + chat.text + "\n";
        SwingUtilities.invokeLater(() -> {
            try {
                messagePane.getDocument().insertString(messagePane.getDocument().getLength(), full, null);
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        });
    }
}
