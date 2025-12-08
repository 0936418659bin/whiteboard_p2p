package com.whiteboard.ui;

import com.whiteboard.drawing.*;
import com.whiteboard.network.*;

import com.formdev.flatlaf.FlatDarkLaf;
import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.*;
import java.util.UUID;

/**
 * Main application frame with modern docking layout
 * Features: Layers, File operations, Zoom, Grid, Advanced tools
 */
public class MainFrame extends JFrame {
    private DrawingCanvas canvas;
    private ToolPanel toolPanel;
    private ColorPanel colorPanel;
    private LayerPanel layerPanel;
    private FilePanel filePanel;
    private ZoomPanel zoomPanel;
    private GridPanel gridPanel;
    private AdvancedToolsPanel advancedToolsPanel;
    
    private JLabel connectionStatusLabel;
    private JLabel peerCountLabel;
    private JLabel zoomStatusLabel;

    // Peer management
    private final Map<String, Color> peerColors = new HashMap<>();
    private DefaultListModel<NetworkProtocol.PeerInfo> peerListModel;
    private JList<NetworkProtocol.PeerInfo> peerList;

    private String peerId;
    private String peerName;
    private int listenPort;
    private ServerSocket serverSocket;
    private MessageHandler messageHandler;
    private PeerDiscovery peerDiscovery;

    public MainFrame() {
        setTitle("P2P Whiteboard Pro");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1600, 1000);
        setLocationRelativeTo(null);
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        peerId = UUID.randomUUID().toString().substring(0, 8);
        peerName = "Peer-" + peerId;
        listenPort = 0; // sẽ được gán sau khi tạo ServerSocket

        // register local peer with a default color
        registerPeer(peerId, peerName);

        initComponents();
        setupMenuBar();
        initNetwork();
        setupEventHandlers();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                shutdown();
            }
        });

        setVisible(true);
    }

    private void setupMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        
        // File Menu
        JMenu fileMenu = new JMenu("File");
        JMenuItem newItem = new JMenuItem("New");
        newItem.addActionListener(e -> canvas.clearCanvas());
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> shutdown());
        fileMenu.add(newItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);
        
        // Edit Menu
        JMenu editMenu = new JMenu("Edit");
        JMenuItem undoItem = new JMenuItem("Undo (Ctrl+Z)");
        undoItem.addActionListener(e -> canvas.undo());
        JMenuItem redoItem = new JMenuItem("Redo (Ctrl+Y)");
        redoItem.addActionListener(e -> canvas.redo());
        JMenuItem selectAllItem = new JMenuItem("Select All (Ctrl+A)");
        selectAllItem.addActionListener(e -> canvas.selectAll());
        JMenuItem deleteItem = new JMenuItem("Delete (Del)");
        deleteItem.addActionListener(e -> canvas.deleteSelection());
        editMenu.add(undoItem);
        editMenu.add(redoItem);
        editMenu.addSeparator();
        editMenu.add(selectAllItem);
        editMenu.add(deleteItem);
        
        // View Menu
        JMenu viewMenu = new JMenu("View");
        JCheckBoxMenuItem gridItem = new JCheckBoxMenuItem("Show Grid", false);
        gridItem.addActionListener(e -> canvas.getGridManager().setGridVisible(gridItem.isSelected()));
        JCheckBoxMenuItem snapItem = new JCheckBoxMenuItem("Snap to Grid", false);
        snapItem.addActionListener(e -> canvas.getGridManager().setSnapToGrid(snapItem.isSelected()));
        viewMenu.add(gridItem);
        viewMenu.add(snapItem);
        
        // Network Menu
        JMenu networkMenu = new JMenu("Network");
        JMenuItem connectItem = new JMenuItem("Connect to Peer");
        connectItem.addActionListener(e -> showConnectionDialog());
        JMenuItem hostItem = new JMenuItem("Host Session");
        hostItem.addActionListener(e -> showHostDialog());
        networkMenu.add(connectItem);
        networkMenu.add(hostItem);
        
        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(viewMenu);
        menuBar.add(networkMenu);
        setJMenuBar(menuBar);
    }

    private void initComponents() {
        // Canvas
        canvas = new DrawingCanvas(peerId);
        canvas.setBackground(Color.WHITE);
        JScrollPane canvasScroll = new JScrollPane(canvas,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        canvasScroll.getVerticalScrollBar().setUnitIncrement(16);
        canvasScroll.getHorizontalScrollBar().setUnitIncrement(16);

        // Panels
        toolPanel = new ToolPanel();
        colorPanel = new ColorPanel();
        layerPanel = new LayerPanel(canvas.getLayerManager());
        filePanel = new FilePanel();
        zoomPanel = new ZoomPanel(canvas.getZoomManager());
        gridPanel = new GridPanel(canvas.getGridManager());
        advancedToolsPanel = new AdvancedToolsPanel(canvas.getTextManager());

        // Setup callbacks
        toolPanel.setOnToolSelected(tool -> canvas.getDrawingTool().setCurrentTool(tool));

        // Quick actions trên thanh công cụ
        toolPanel.getUndoButton().addActionListener(e -> canvas.undo());
        toolPanel.getClearButton().addActionListener(e -> canvas.clearCanvas());

        colorPanel.setColorChangeListener(e -> {
            DrawingTool dt = canvas.getDrawingTool();
            dt.setCurrentColor(colorPanel.getSelectedColor());
            dt.setFillColor(colorPanel.isFillEnabled() ? colorPanel.getSelectedColor() : null);
            dt.setUseGradient(colorPanel.isGradientEnabled());
            dt.setGradientTo(colorPanel.getGradientTo());
        });

        layerPanel.setOnLayerChanged(() -> canvas.repaint());

        setupFilePanel();
        setupZoomPanel();
        setupGridPanel();

        // Khi ở chế độ PAN click vào một nét vẽ sẽ hiển thị thông tin peer trên status bar
        canvas.setOnShapeClicked(shape -> {
            if (shape != null && shape.peerId != null) {
                String id = shape.peerId;
                String shortId = id.length() > 8 ? id.substring(0, 8) : id;
                String label = "Peer-" + shortId;
                SwingUtilities.invokeLater(() ->
                        connectionStatusLabel.setText("✏ From: " + label));
            }
        });

        // Sync local drawing color với màu đại diện của peer này
        Color selfColor = getPeerColor(peerId);
        colorPanel.setSelectedColor(selfColor);
        canvas.getDrawingTool().setCurrentColor(selfColor);

        // Docking layout
        setLayout(new BorderLayout(5, 5));

        // Left panel with tabs (simplified: Tools first, Layers second)
        JTabbedPane leftTabs = new JTabbedPane();
        leftTabs.addTab("Tools", new JScrollPane(toolPanel));
        leftTabs.addTab("Layers", new JScrollPane(layerPanel));
        leftTabs.setPreferredSize(new Dimension(220, 0));

        // Right panel with tabs: Colors, File, Peers, Grid
        JTabbedPane rightTabs = new JTabbedPane();
        rightTabs.addTab("Colors", colorPanel);
        rightTabs.addTab("File", filePanel);
        rightTabs.addTab("Peers", createPeersPanel());
        rightTabs.addTab("Grid", gridPanel);
        rightTabs.setPreferredSize(new Dimension(250, 0));

        // Bottom panel - status bar
        JPanel statusPanel = createStatusPanel();

        add(leftTabs, BorderLayout.WEST);
        add(canvasScroll, BorderLayout.CENTER);
        add(rightTabs, BorderLayout.EAST);
        add(statusPanel, BorderLayout.SOUTH);
    }

    private void setupFilePanel() {
        filePanel.setListener(new FilePanel.FileOperationListener() {
            @Override
            public void onNewProject() {
                canvas.clearCanvas();
            }

            @Override
            public void onSaveProject(String filePath) {
                FileManager.ProjectData data = new FileManager.ProjectData("Drawing", 
                    canvas.getWidth(), canvas.getHeight());
                data.shapes = canvas.getAllShapes();
                data.layers = canvas.getLayerManager().getAllLayers();
                FileManager.saveProjectAsJSON(filePath, data);
                JOptionPane.showMessageDialog(MainFrame.this, "Project saved!");
            }

            @Override
            public void onLoadProject(String filePath) {
                FileManager.ProjectData data = FileManager.loadProjectFromJSON(filePath);
                if (data != null && data.shapes != null) {
                    canvas.loadShapes(data.shapes);
                    JOptionPane.showMessageDialog(MainFrame.this, "Project loaded!");
                }
            }

            @Override
            public void onExportPNG(String filePath) {
                FileManager.exportAsPNG(filePath, canvas.getCanvasImage());
                JOptionPane.showMessageDialog(MainFrame.this, "Exported as PNG!");
            }

            @Override
            public void onExportSVG(String filePath) {
                FileManager.exportAsSVG(filePath, canvas.getAllShapes(), 
                    canvas.getWidth(), canvas.getHeight());
                JOptionPane.showMessageDialog(MainFrame.this, "Exported as SVG!");
            }
        });
    }

    private JPanel createPeersPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JLabel title = new JLabel("Connected Peers");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 12f));
        panel.add(title, BorderLayout.NORTH);

        peerListModel = new DefaultListModel<>();
        peerList = new JList<>(peerListModel);
        peerList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof NetworkProtocol.PeerInfo) {
                    NetworkProtocol.PeerInfo peer = (NetworkProtocol.PeerInfo) value;
                    label.setText(peer.name + " (" + peer.peerId + ")");
                    Color c = getPeerColor(peer.peerId);
                    label.setIcon(new ColorIcon(c));
                }
                return label;
            }
        });

        JScrollPane scrollPane = new JScrollPane(peerList);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private void registerPeer(String id, String name) {
        if (!peerColors.containsKey(id)) {
            peerColors.put(id, generatePeerColor(peerColors.size()));
        }

        if (peerListModel != null) {
            // tránh trùng entry
            for (int i = 0; i < peerListModel.size(); i++) {
                NetworkProtocol.PeerInfo info = peerListModel.getElementAt(i);
                if (info.peerId.equals(id)) {
                    return;
                }
            }
            peerListModel.addElement(new NetworkProtocol.PeerInfo(id, name, ""));
        }
    }

    private Color getPeerColor(String id) {
        Color c = peerColors.get(id);
        if (c == null) {
            c = generatePeerColor(peerColors.size());
            peerColors.put(id, c);
        }
        return c;
    }

    private Color generatePeerColor(int index) {
        // Một bảng màu cố định thân thiện
        Color[] palette = new Color[] {
                new Color(0xFF6B6B), // red
                new Color(0x4ECDC4), // teal
                new Color(0xFFD93D), // yellow
                new Color(0x1A73E8), // blue
                new Color(0x9B51E0), // purple
                new Color(0x2ECC71), // green
                new Color(0xF2994A), // orange
                new Color(0xE91E63)  // pink
        };
        return palette[index % palette.length];
    }

    private void setupZoomPanel() {
        zoomPanel.setListener(new ZoomPanel.ZoomChangeListener() {
            @Override
            public void onZoomChanged(float newZoom) {
                updateZoomStatus();
            }

            @Override
            public void onFitToWindow() {
                canvas.getZoomManager().zoomToFit(canvas.getWidth(), canvas.getHeight(), 1000, 1000);
                canvas.repaint();
            }

            @Override
            public void onReset() {
                canvas.repaint();
            }
        });
    }

    private void setupGridPanel() {
        gridPanel.setListener(new GridPanel.GridChangeListener() {
            @Override
            public void onGridSettingsChanged() {
                canvas.repaint();
            }

            @Override
            public void onAlignLeft() {
                GridManager.alignLeft(canvas.getSelectionManager().getSelectedShapes());
                canvas.repaint();
            }

            @Override
            public void onAlignRight() {
                GridManager.alignRight(canvas.getSelectionManager().getSelectedShapes());
                canvas.repaint();
            }

            @Override
            public void onAlignTop() {
                GridManager.alignTop(canvas.getSelectionManager().getSelectedShapes());
                canvas.repaint();
            }

            @Override
            public void onAlignBottom() {
                GridManager.alignBottom(canvas.getSelectionManager().getSelectedShapes());
                canvas.repaint();
            }

            @Override
            public void onDistributeHorizontally() {
                GridManager.distributeHorizontally(canvas.getSelectionManager().getSelectedShapes());
                canvas.repaint();
            }

            @Override
            public void onDistributeVertically() {
                GridManager.distributeVertically(canvas.getSelectionManager().getSelectedShapes());
                canvas.repaint();
            }
        });
    }

    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        panel.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));

        connectionStatusLabel = new JLabel("🔵 ID: " + peerId);
        peerCountLabel = new JLabel("Peers: 0");
        zoomStatusLabel = new JLabel("Zoom: 100%");

        JButton connectBtn = new JButton("Connect");
        connectBtn.addActionListener(e -> showConnectionDialog());

        JButton hostBtn = new JButton("Host");
        hostBtn.addActionListener(e -> showHostDialog());

        panel.add(connectionStatusLabel);
        panel.add(new JSeparator(SwingConstants.VERTICAL));
        panel.add(peerCountLabel);
        panel.add(new JSeparator(SwingConstants.VERTICAL));
        panel.add(zoomStatusLabel);
        panel.add(new JSeparator(SwingConstants.VERTICAL));
        panel.add(connectBtn);
        panel.add(hostBtn);

        return panel;
    }

    private void updateZoomStatus() {
        zoomStatusLabel.setText("Zoom: " + canvas.getZoomManager().getZoomPercentage());
    }

    private void initNetwork() {
        messageHandler = new MessageHandler();

        // tạo server socket một lần, để OS tự chọn cổng rảnh (port 0)
        try {
            serverSocket = new ServerSocket(0);
            listenPort = serverSocket.getLocalPort();
        } catch (IOException e) {
            System.err.println("Failed to open server socket: " + e.getMessage());
        }

        messageHandler.setOnShapesReceived(shapeData -> {
            if (shapeData.shapes != null) {
                for (com.whiteboard.drawing.Shape shape : shapeData.shapes) {
                    // đảm bảo peer được đăng ký và áp dụng màu đại diện cho peer gửi shape
                    if (shape.peerId != null) {
                        String id = shape.peerId;
                        String name = "Peer-" + (id.length() > 8 ? id.substring(0, 8) : id);
                        registerPeer(id, name);
                        shape.color = getPeerColor(id);
                    }
                    canvas.drawRemoteShape(shape);
                }
            }
        });

        messageHandler.setOnClearReceived(senderId -> canvas.clearCanvas());
        messageHandler.setOnUndoReceived(senderId -> canvas.undo());

        // Realtime: gửi trực tiếp shape đang vẽ mỗi khi canvas cập nhật
        canvas.setOnShapeDrawn(shape -> {
            try {
                if (shape != null) {
                    messageHandler.broadcastShapes(java.util.Collections.singletonList(shape), peerId);
                }
            } catch (Exception e) {
                System.err.println("Error broadcasting: " + e.getMessage());
            }
        });

        peerDiscovery = new PeerDiscovery(peerId, peerName, listenPort);
        try {
            peerDiscovery.start();
        } catch (IOException e) {
            System.err.println("Failed to start discovery: " + e.getMessage());
        }

        new Thread(this::acceptConnections).start();
    }

    private void acceptConnections() {
        if (serverSocket == null) {
            System.err.println("Server socket not initialized; cannot accept connections.");
            return;
        }

        try {
            while (true) {
                Socket socket = serverSocket.accept();
                String remotePeerId = socket.getInetAddress().getHostAddress();

                try {
                    PeerConnection connection = new PeerConnection(socket, remotePeerId);
                    messageHandler.addConnection(connection);
                    updatePeerCount();

                    connection.setDisconnectHandler(disconnected -> {
                        messageHandler.removeConnection(disconnected);
                        updatePeerCount();
                    });

                    if (!canvas.getAllShapes().isEmpty()) {
                        connection.sendMessage(new NetworkProtocol.Message(
                                NetworkProtocol.MessageType.SHAPES, peerId,
                                new NetworkProtocol.ShapeData(canvas.getAllShapes(), 
                                    System.currentTimeMillis())));
                    }
                } catch (IOException e) {
                    System.err.println("Connection error: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }

    private void showConnectionDialog() {
        ConnectionDialog dialog = new ConnectionDialog(this);
        dialog.updatePeerList(peerDiscovery.getDiscoveredPeers());

        Timer timer = new Timer(1000, e ->
            dialog.updatePeerList(peerDiscovery.getDiscoveredPeers()));
        timer.start();

        dialog.setOnConnect(peerInfo -> {
            timer.stop();
            connectToPeer(peerInfo);
        });

        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                timer.stop();
            }
        });

        dialog.setVisible(true);
    }

    private void showHostDialog() {
        JOptionPane.showMessageDialog(this,
                "🌐 Hosting Server\n" +
                "Port: " + listenPort + "\n" +
                "ID: " + peerId + "\n" +
                "Name: " + peerName,
                "Host Session", JOptionPane.INFORMATION_MESSAGE);
    }

    private void connectToPeer(NetworkProtocol.PeerInfo peerInfo) {
        new Thread(() -> {
            try {
                int targetPort = peerInfo.port > 0 ? peerInfo.port : listenPort;
                Socket socket = new Socket(peerInfo.ipAddress, targetPort);
                PeerConnection connection = new PeerConnection(socket, peerInfo.peerId);
                messageHandler.addConnection(connection);
                // đăng ký peer để hiển thị và gán màu cố định
                registerPeer(peerInfo.peerId, peerInfo.name);
                updatePeerCount();

                connection.sendMessage(new NetworkProtocol.Message(
                        NetworkProtocol.MessageType.HELLO, peerId, null));

                connectionStatusLabel.setText("🟢 Connected: " + peerInfo.name);

                connection.setDisconnectHandler(disconnected -> {
                    messageHandler.removeConnection(disconnected);
                    updatePeerCount();
                    connectionStatusLabel.setText("🔴 Disconnected");
                });
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this,
                        "Connection failed: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }).start();
    }

    private void setupEventHandlers() {
        colorPanel.widthSlider.addChangeListener(e ->
            canvas.getDrawingTool().setStrokeWidth(colorPanel.getStrokeWidth()));
    }

    private void updatePeerCount() {
        SwingUtilities.invokeLater(() ->
            peerCountLabel.setText("Peers: " + messageHandler.getConnectionCount()));
    }

    private void shutdown() {
        if (peerDiscovery != null) peerDiscovery.stop();
        if (messageHandler != null) messageHandler.closeAll();
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                System.err.println("Error: " + e.getMessage());
            }
        }
        System.exit(0);
    }

    public static void main(String[] args) {
        try {
            FlatDarkLaf.setup();
        } catch (Exception e) {
            System.err.println("Failed to initialize FlatLaf, falling back to system LAF: " + e.getMessage());
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ex) {
                System.err.println("Theme error: " + ex.getMessage());
            }
        }

        SwingUtilities.invokeLater(MainFrame::new);
    }
}
