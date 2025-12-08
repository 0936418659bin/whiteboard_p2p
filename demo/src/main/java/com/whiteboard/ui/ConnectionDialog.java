package com.whiteboard.ui;

import com.whiteboard.network.NetworkProtocol;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.function.Consumer;

public class ConnectionDialog extends JDialog {
    private JList<NetworkProtocol.PeerInfo> peerList;
    private DefaultListModel<NetworkProtocol.PeerInfo> listModel;
    private JButton connectButton;
    private JButton cancelButton;
    private JLabel statusLabel;
    private NetworkProtocol.PeerInfo selectedPeer;
    private Consumer<NetworkProtocol.PeerInfo> onConnect;

    public ConnectionDialog(Frame owner) {
        super(owner, "Connect to Peer", true);
        setSize(400, 350);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        initComponents();
    }

    private void initComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Status label
        statusLabel = new JLabel("Available Peers:");
        mainPanel.add(statusLabel, BorderLayout.NORTH);

        // Peer list
        listModel = new DefaultListModel<>();
        peerList = new JList<>(listModel);
        peerList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        peerList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                if (value instanceof NetworkProtocol.PeerInfo) {
                    NetworkProtocol.PeerInfo peer = (NetworkProtocol.PeerInfo) value;
                    value = peer.toString();
                }
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
        });

        peerList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int index = peerList.getSelectedIndex();
                if (index >= 0) {
                    selectedPeer = listModel.getElementAt(index);
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(peerList);
        scrollPane.setPreferredSize(new Dimension(380, 200));
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        connectButton = new JButton("Connect");
        cancelButton = new JButton("Cancel");

        connectButton.addActionListener(e -> {
            if (selectedPeer != null && onConnect != null) {
                onConnect.accept(selectedPeer);
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Please select a peer to connect to");
            }
        });

        cancelButton.addActionListener(e -> dispose());

        buttonPanel.add(connectButton);
        buttonPanel.add(cancelButton);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    public void updatePeerList(Collection<NetworkProtocol.PeerInfo> peers) {
        listModel.clear();
        for (NetworkProtocol.PeerInfo peer : peers) {
            listModel.addElement(peer);
        }
        statusLabel.setText("Available Peers: " + peers.size());
    }

    public void setOnConnect(Consumer<NetworkProtocol.PeerInfo> callback) {
        this.onConnect = callback;
    }
}
