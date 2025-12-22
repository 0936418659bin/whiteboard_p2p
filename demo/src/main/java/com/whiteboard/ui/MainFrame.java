package com.whiteboard.ui;

import com.whiteboard.drawing.*;
import com.whiteboard.network.*;

import com.formdev.flatlaf.FlatDarkLaf;
import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
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
    private ChatPanel chatPanel;

    private JLabel connectionStatusLabel;
    private JLabel peerCountLabel;
    private JLabel roomInfoLabel;
    private JLabel zoomStatusLabel;

    // Peer management
    private final Map<String, Color> peerColors = new HashMap<>();
    private DefaultListModel<NetworkProtocol.PeerInfo> peerListModel;
    private JList<NetworkProtocol.PeerInfo> peerList;
    private final Set<String> connectedPeers = new HashSet<>();
    private String hostPeerId; // ID of the room host (null if no host)
    private boolean isHost = false;

    private String peerId;
    private String peerName;
    private String roomId;
    private String roomPassword;
    private boolean joinMode;
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
        String defaultName = "Peer-" + peerId;
        String defaultRoom = "room-1";

        StartupDialog startup = new StartupDialog(this, defaultName, defaultRoom);
        startup.setVisible(true);
        if (!startup.isConfirmed()) {
            System.exit(0);
        }

        peerName = startup.getPeerName();
        roomId = startup.getRoomCode();
        roomPassword = startup.getRoomPassword();
        joinMode = startup.isJoinMode();
        // xác định chủ phòng: ai chọn Create room là host
        isHost = !joinMode;
        hostPeerId = isHost ? peerId : null;
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
        newItem.addActionListener(e -> performClearWithBroadcast());
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> shutdown());
        fileMenu.add(newItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        // Edit Menu
        JMenu editMenu = new JMenu("Edit");
        JMenuItem undoItem = new JMenuItem("Undo (Ctrl+Z)");
        undoItem.addActionListener(e -> performUndoWithBroadcast());
        JMenuItem redoItem = new JMenuItem("Redo (Ctrl+Y)");
        redoItem.addActionListener(e -> canvas.redo());
        JMenuItem selectAllItem = new JMenuItem("Select All (Ctrl+A)");
        selectAllItem.addActionListener(e -> canvas.selectAll());
        JMenuItem deleteItem = new JMenuItem("Delete (Del)");
        deleteItem.addActionListener(e -> canvas.deleteSelection());
        JMenuItem renameItem = new JMenuItem("Change Name...");
        renameItem.addActionListener(e -> changePeerName());
        editMenu.add(undoItem);
        editMenu.add(redoItem);
        editMenu.addSeparator();
        editMenu.add(selectAllItem);
        editMenu.add(deleteItem);
        editMenu.addSeparator();
        editMenu.add(renameItem);

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
        JMenuItem leaveRoomItem = new JMenuItem("Leave Room");
        leaveRoomItem.addActionListener(e -> leaveRoom());
        networkMenu.add(connectItem);
        networkMenu.add(hostItem);
        networkMenu.addSeparator();
        networkMenu.add(leaveRoomItem);

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
        toolPanel.getUndoButton().addActionListener(e -> performUndoWithBroadcast());
        toolPanel.getClearButton().addActionListener(e -> performClearWithBroadcast());

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

        // Khi ở chế độ PAN click vào một nét vẽ sẽ hiển thị thông tin peer trên status
        // bar
        canvas.setOnShapeClicked(shape -> {
            if (shape != null && shape.peerId != null) {
                String id = shape.peerId;
                String shortId = id.length() > 8 ? id.substring(0, 8) : id;
                String label = "Peer-" + shortId;
                SwingUtilities.invokeLater(() -> connectionStatusLabel.setText("✏ From: " + label));
            }
        });

        // Mặc định tất cả peers vẽ màu đen; ColorPanel vẫn dùng để đổi màu,
        // nhưng không còn gán màu riêng cho từng peer.
        Color defaultColor = Color.BLACK;
        colorPanel.setSelectedColor(defaultColor);
        canvas.getDrawingTool().setCurrentColor(defaultColor);

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
        // Chat tab (kept compact so it doesn't disrupt canvas layout)
        chatPanel = new ChatPanel();
        rightTabs.addTab("Chat", chatPanel);
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
                performClearWithBroadcast();
            }

            @Override
            public void onSaveProject(String filePath) {
                System.out.println("[MainFrame] Save project requested: " + filePath);
                List<com.whiteboard.drawing.Shape> allShapes = canvas.getAllShapes();
                System.out.println("[MainFrame] Total shapes to save: " + allShapes.size());

                FileManager.ProjectData data = new FileManager.ProjectData("Drawing",
                        canvas.getPreferredSize().width, canvas.getPreferredSize().height);
                data.shapes = allShapes;
                data.layers = canvas.getLayerManager().getAllLayers();

                System.out.println("[MainFrame] ProjectData created - shapes: " +
                        (data.shapes != null ? data.shapes.size() : 0) +
                        ", layers: " + (data.layers != null ? data.layers.size() : 0));

                if (FileManager.saveProjectAsJSON(filePath, data)) {
                    File savedFile = new File(filePath);
                    String message = "Project saved successfully!\n\n" +
                            "File: " + savedFile.getName() + "\n" +
                            "Path: " + savedFile.getAbsolutePath() + "\n" +
                            "Size: " + savedFile.length() + " bytes\n" +
                            "Shapes: " + allShapes.size();
                    JOptionPane.showMessageDialog(MainFrame.this, message, "Save Success",
                            JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(MainFrame.this,
                            "Failed to save project!\n\nCheck console for details.",
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }

            @Override
            public void onLoadProject(String filePath) {
                System.out.println("[MainFrame] Load project requested: " + filePath);
                FileManager.ProjectData data = FileManager.loadProjectFromJSON(filePath);

                if (data == null) {
                    System.err.println("[MainFrame] Failed to load project - data is null");
                    JOptionPane.showMessageDialog(MainFrame.this,
                            "Failed to load project!\n\nFile may be corrupted or in unsupported format.\n" +
                                    "Check console for details.",
                            "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                System.out.println("[MainFrame] ProjectData loaded - shapes: " +
                        (data.shapes != null ? data.shapes.size() : 0));

                if (data.shapes != null && !data.shapes.isEmpty()) {
                    System.out.println("[MainFrame] Loading " + data.shapes.size() + " shapes into canvas...");
                    canvas.loadShapes(data.shapes);
                    System.out.println("[MainFrame] Shapes loaded. Canvas now has: " +
                            canvas.getAllShapes().size() + " shapes");

                    JOptionPane.showMessageDialog(MainFrame.this,
                            "Project loaded successfully!\n\n" +
                                    "Shapes: " + data.shapes.size() + "\n" +
                                    "Canvas: " + data.canvasWidth + "x" + data.canvasHeight,
                            "Load Success", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    System.err.println("[MainFrame] No shapes in loaded project");
                    JOptionPane.showMessageDialog(MainFrame.this,
                            "Project loaded but contains no shapes!\n\n" +
                                    "The file may be empty or corrupted.",
                            "Warning", JOptionPane.WARNING_MESSAGE);
                }
            }

            @Override
            public void onExportPNG(String filePath) {
                System.out.println("[MainFrame] Export PNG requested: " + filePath);
                BufferedImage image = canvas.getCanvasImage();
                if (image == null) {
                    System.err.println("[MainFrame] Canvas image is null!");
                    JOptionPane.showMessageDialog(MainFrame.this,
                            "Failed to export PNG!\n\nCanvas image is null.",
                            "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (FileManager.exportAsPNG(filePath, image)) {
                    File exportedFile = new File(filePath);
                    String message = "PNG exported successfully!\n\n" +
                            "File: " + exportedFile.getName() + "\n" +
                            "Path: " + exportedFile.getAbsolutePath() + "\n" +
                            "Size: " + exportedFile.length() + " bytes\n" +
                            "Image: " + image.getWidth() + "x" + image.getHeight();
                    JOptionPane.showMessageDialog(MainFrame.this, message, "Export Success",
                            JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(MainFrame.this,
                            "Failed to export PNG!\n\nCheck console for details.",
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }

            @Override
            public void onExportSVG(String filePath) {
                System.out.println("[MainFrame] Export SVG requested: " + filePath);
                List<com.whiteboard.drawing.Shape> shapes = canvas.getAllShapes();
                System.out.println("[MainFrame] Exporting " + shapes.size() + " shapes to SVG");

                if (FileManager.exportAsSVG(filePath, shapes,
                        canvas.getPreferredSize().width, canvas.getPreferredSize().height)) {
                    File exportedFile = new File(filePath);
                    String message = "SVG exported successfully!\n\n" +
                            "File: " + exportedFile.getName() + "\n" +
                            "Path: " + exportedFile.getAbsolutePath() + "\n" +
                            "Size: " + exportedFile.length() + " bytes\n" +
                            "Shapes: " + shapes.size();
                    JOptionPane.showMessageDialog(MainFrame.this, message, "Export Success",
                            JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(MainFrame.this,
                            "Failed to export SVG!\n\nCheck console for details.",
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }

            @Override
            public void onImportImage(String filePath) {
                BufferedImage image = FileManager.importImage(filePath);
                if (image != null) {
                    // Hỏi user muốn đặt image ở đâu
                    String[] options = { "Top Left (0,0)", "Center", "Custom Position" };
                    int choice = JOptionPane.showOptionDialog(MainFrame.this,
                            "Image imported successfully!\n" +
                                    "Size: " + image.getWidth() + "x" + image.getHeight() + "\n\n" +
                                    "Where do you want to place the image?",
                            "Import Image",
                            JOptionPane.DEFAULT_OPTION,
                            JOptionPane.QUESTION_MESSAGE,
                            null,
                            options,
                            options[0]);

                    int x = -1, y = -1;
                    if (choice == 0) {
                        // Top Left
                        x = 0;
                        y = 0;
                    } else if (choice == 1) {
                        // Center
                        int canvasWidth = canvas.getPreferredSize().width;
                        int canvasHeight = canvas.getPreferredSize().height;
                        x = (canvasWidth - image.getWidth()) / 2;
                        y = (canvasHeight - image.getHeight()) / 2;
                    } else if (choice == 2) {
                        // Custom Position - hỏi user nhập x, y
                        String xStr = JOptionPane.showInputDialog(MainFrame.this,
                                "Enter X position:", "0");
                        String yStr = JOptionPane.showInputDialog(MainFrame.this,
                                "Enter Y position:", "0");
                        try {
                            x = Integer.parseInt(xStr != null ? xStr : "0");
                            y = Integer.parseInt(yStr != null ? yStr : "0");
                        } catch (NumberFormatException e) {
                            x = 0;
                            y = 0;
                        }
                    } else {
                        // User cancelled
                        return;
                    }

                    // Import image vào canvas
                    canvas.importImage(image, x, y);

                    JOptionPane.showMessageDialog(MainFrame.this,
                            "Image placed at (" + x + ", " + y + ")\n" +
                                    "You can draw on top of it!",
                            "Import Image",
                            JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(MainFrame.this,
                            "Failed to import image!\n\n" +
                                    "Please check:\n" +
                                    "- File exists\n" +
                                    "- File is a valid image format (PNG, JPG, GIF, BMP)",
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });
    }

    private void changePeerName() {
        String newName = JOptionPane.showInputDialog(this,
                "Enter new display name:", peerName);
        if (newName == null)
            return; // user cancelled
        newName = newName.trim();
        if (newName.isEmpty() || newName.equals(peerName))
            return;

        peerName = newName;

        // cập nhật status bar
        if (connectionStatusLabel != null) {
            connectionStatusLabel.setText("🔵 " + peerName + " (" + peerId + ")");
        }

        // cập nhật entry của chính mình trong danh sách peers
        if (peerListModel != null) {
            for (int i = 0; i < peerListModel.size(); i++) {
                NetworkProtocol.PeerInfo info = peerListModel.getElementAt(i);
                if (info.peerId.equals(peerId)) {
                    info.name = peerName;
                    peerListModel.set(i, info);
                    break;
                }
            }
        }

        // khởi động lại discovery với tên mới
        if (peerDiscovery != null) {
            peerDiscovery.stop();
        }
        peerDiscovery = new PeerDiscovery(peerId, peerName, listenPort, roomId, roomPassword);
        try {
            peerDiscovery.start();
        } catch (IOException e) {
            System.err.println("Failed to restart discovery: " + e.getMessage());
        }
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
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected,
                        cellHasFocus);
                if (value instanceof NetworkProtocol.PeerInfo) {
                    NetworkProtocol.PeerInfo peer = (NetworkProtocol.PeerInfo) value;
                    String role = (hostPeerId != null && hostPeerId.equals(peer.peerId)) ? "HOST" : "GUEST";
                    label.setText("[" + role + "] " + peer.name + " (" + peer.peerId + ")");
                    Color c = getPeerColor(peer.peerId);
                    label.setIcon(new ColorIcon(c));
                }
                return label;
            }
        });

        JScrollPane scrollPane = new JScrollPane(peerList);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Nếu là host, cho phép kick peer khác bằng menu chuột phải
        peerList.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!isHost || SwingUtilities.isLeftMouseButton(e))
                    return;
                int index = peerList.locationToIndex(e.getPoint());
                if (index < 0)
                    return;
                peerList.setSelectedIndex(index);
                NetworkProtocol.PeerInfo selected = peerListModel.getElementAt(index);
                if (selected == null || selected.peerId.equals(peerId))
                    return; // không kick chính mình

                if (SwingUtilities.isRightMouseButton(e)) {
                    JPopupMenu menu = new JPopupMenu();
                    JMenuItem kickItem = new JMenuItem("Kick " + selected.name);
                    kickItem.addActionListener(ev -> kickPeer(selected.peerId));
                    menu.add(kickItem);
                    menu.show(peerList, e.getX(), e.getY());
                }
            }
        });

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
                new Color(0xE91E63) // pink
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

        // Connection / self info
        connectionStatusLabel = new JLabel("🔵 " + peerName + " (" + peerId + ")");

        // Room info
        roomInfoLabel = new JLabel();
        updateRoomInfo();

        // Peer count with tooltip
        peerCountLabel = new JLabel("Peers: 0");
        peerCountLabel.setToolTipText("Click to view connected peers");
        peerCountLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        peerCountLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showConnectionDialog();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                updatePeerCountTooltip();
            }
        });

        // Zoom status
        zoomStatusLabel = new JLabel("Zoom: 100%");

        panel.add(connectionStatusLabel);
        panel.add(new JSeparator(SwingConstants.VERTICAL));
        panel.add(roomInfoLabel);
        panel.add(new JSeparator(SwingConstants.VERTICAL));
        panel.add(peerCountLabel);
        panel.add(new JSeparator(SwingConstants.VERTICAL));
        panel.add(zoomStatusLabel);

        return panel;
    }

    private void updateRoomInfo() {
        String passwordDisplay = roomPassword != null && !roomPassword.isEmpty() ? "***" : "none";
        String mode = isHost ? "Host" : "Join";
        roomInfoLabel.setText(String.format("Room: %s (pass: %s) | Mode: %s",
                roomId, passwordDisplay, mode));
    }

    private void updatePeerCountTooltip() {
        if (connectedPeers.isEmpty()) {
            peerCountLabel.setToolTipText("No peers connected");
            return;
        }

        StringBuilder tooltip = new StringBuilder("<html>Connected peers:<br>");
        for (String peerId : connectedPeers) {
            String shortId = peerId.length() > 8 ? peerId.substring(0, 8) : peerId;
            tooltip.append("• Peer-").append(shortId).append("<br>");
        }
        tooltip.append("</html>");
        peerCountLabel.setToolTipText(tooltip.toString());
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
            System.out.println("[MainFrame] Server socket created at port " + listenPort);
        } catch (IOException e) {
            System.err.println("Failed to open server socket: " + e.getMessage());
        }

        messageHandler.setOnShapesReceived(shapeData -> {
            if (shapeData.shapes != null && !shapeData.shapes.isEmpty()) {
                SwingUtilities.invokeLater(() -> {
                    // Batch process tất cả shapes trong một lần để giảm số lần repaint
                    String drawingPeerId = null;
                    for (com.whiteboard.drawing.Shape shape : shapeData.shapes) {
                        if (shape.peerId != null) {
                            drawingPeerId = shape.peerId;
                            String id = shape.peerId;
                            String name = "Peer-" + (id.length() > 8 ? id.substring(0, 8) : id);
                            // chỉ đăng ký peer để hiển thị trong danh sách, không đổi màu shape nữa
                            registerPeer(id, name);
                        }
                        canvas.drawRemoteShape(shape);
                    }
                    // Hiển thị peer đang vẽ trên status bar
                    if (drawingPeerId != null && connectionStatusLabel != null) {
                        String shortId = drawingPeerId.length() > 8 ? drawingPeerId.substring(0, 8) : drawingPeerId;
                        String label = "Peer-" + shortId;
                        connectionStatusLabel.setText("✏ " + label + " is drawing...");
                    }
                    // drawRemoteShape đã có debounce timer, không cần repaint thủ công ở đây
                });
            }
        });

        messageHandler.setOnClearReceived(senderId -> SwingUtilities.invokeLater(canvas::clearCanvas));
        messageHandler.setOnUndoReceived(senderId -> SwingUtilities.invokeLater(() -> canvas.undoForPeer(senderId)));
        messageHandler.setOnDisconnectReceived(
                reason -> SwingUtilities.invokeLater(() -> handleRemoteRoomDisconnect(reason)));

        // Chat messages
        messageHandler.setOnChatReceived(chat -> {
            if (chat == null) return;
            SwingUtilities.invokeLater(() -> {
                // ensure peer is registered in the list
                if (chat.senderId != null) {
                    registerPeer(chat.senderId, chat.senderName != null ? chat.senderName : ("Peer-" + chat.senderId));
                    updatePeerCount();
                }
                if (chatPanel != null) {
                    chatPanel.addMessage(chat);
                }
            });
        });

        // Realtime: gửi trực tiếp shape đang vẽ mỗi khi canvas cập nhật
        canvas.setOnShapeDrawn(shape -> {
            try {
                if (shape != null) {
                    int count = messageHandler.getConnectionCount();
                    System.out.println("[MainFrame] Broadcasting shape to " + count + " peer(s)");
                    messageHandler.broadcastShapes(java.util.Collections.singletonList(shape), peerId);
                    // Cập nhật status bar hiển thị peer local đang vẽ
                    if (connectionStatusLabel != null) {
                        connectionStatusLabel.setText("✏ You are drawing...");
                    }
                }
            } catch (Exception e) {
                System.err.println("[MainFrame] Error broadcasting shape: " + e.getMessage());
                e.printStackTrace();
            }
        });

        System.out.println("[MainFrame] Starting PeerDiscovery with config: peerId=" + peerId
                + ", name=" + peerName
                + ", roomId=" + roomId
                + ", roomPassword=" + (roomPassword == null || roomPassword.isEmpty() ? "(empty)" : "***")
                + ", listenPort=" + listenPort
                + ", joinMode=" + joinMode);

        peerDiscovery = new PeerDiscovery(peerId, peerName, listenPort, roomId, roomPassword);
        // callback từ discovery: log và auto-join (mọi mode) nếu chưa có kết nối tới
        // peer đó
        peerDiscovery.setPeerFoundCallback(info -> {
            System.out.println("[MainFrame] Peer found in same room: " + info);
            // tránh tạo trùng nhiều kết nối 2 chiều
            if (messageHandler.getConnection(info.peerId) != null) {
                System.out.println("[MainFrame] Already connected to peer " + info.peerId + ", skip auto-connect.");
                return;
            }
            System.out.println("[MainFrame] Auto-connecting to peer " + info.peerId
                    + " at " + info.ipAddress + ":" + info.port
                    + " (joinMode=" + joinMode + ")");
            SwingUtilities.invokeLater(() -> connectToPeer(info));
        });
        try {
            peerDiscovery.start();
        } catch (IOException e) {
            System.err.println("Failed to start discovery: " + e.getMessage());
        }

        // Hook chat send action to broadcast via MessageHandler
        if (chatPanel != null) {
            chatPanel.setOnSend(text -> {
                try {
                    if (messageHandler != null) {
                        messageHandler.broadcastChat(text, peerId, peerName);
                    }
                    // show local message immediately
                    NetworkProtocol.ChatMessage local = new NetworkProtocol.ChatMessage(peerId, peerName, text);
                    chatPanel.addMessage(local);
                } catch (Exception ex) {
                    System.err.println("Failed to send chat: " + ex.getMessage());
                }
            });
        }

        // Nếu user chọn JOIN room thì bắt buộc phải có ít nhất 1 kết nối TCP
        // trong một khoảng thời gian ngắn (không phụ thuộc vào UDP listen được hay
        // không).
        if (joinMode) {
            new Thread(() -> {
                try {
                    // chờ một chút cho discovery kịp nhận broadcast từ các peer khác
                    Thread.sleep(3500);
                } catch (InterruptedException ignored) {
                }

                if (peerDiscovery != null) {
                    int discoveredCount = peerDiscovery.getDiscoveredPeers().size();
                    int connectionCount = messageHandler.getConnectionCount();
                    System.out.println("[MainFrame] Join mode check: discoveredPeers=" + discoveredCount
                            + ", connectionCount=" + connectionCount);

                    if (connectionCount == 0) {
                        if (discoveredCount == 0) {
                            System.out.println("[MainFrame] Join mode: no peers discovered via UDP for room=" + roomId
                                    + ". Possible issues: firewall blocking UDP broadcast, host not running, or room/password mismatch.");
                        } else {
                            System.out.println("[MainFrame] Join mode: found " + discoveredCount
                                    + " peer(s) via UDP but TCP connection failed. Check firewall for TCP ports.");
                        }
                        SwingUtilities.invokeLater(() -> {
                            String message = "Không tìm thấy phòng \"" + roomId + "\" với mật khẩu đã nhập.\n";
                            if (discoveredCount == 0) {
                                message += "\nGợi ý:\n- Kiểm tra firewall có chặn UDP port 55556 không\n";
                                message += "- Đảm bảo máy host đã chạy và chọn Create room\n";
                                message += "- Kiểm tra cả 2 máy cùng mạng LAN";
                            } else {
                                message += "\nĐã tìm thấy " + discoveredCount + " peer nhưng không kết nối được TCP.\n";
                                message += "Kiểm tra firewall có chặn TCP ports không.";
                            }
                            JOptionPane.showMessageDialog(this, message,
                                    "Room not found",
                                    JOptionPane.ERROR_MESSAGE);
                            dispose();
                            new MainFrame();
                        });
                    }
                }
            }, "JoinRoomValidator").start();
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
                System.out.println("[MainFrame] Incoming TCP connection from " + remotePeerId
                        + ":" + socket.getPort());

                try {
                    // Tìm peerId thật từ UDP discovery dựa trên IP
                    String realPeerId = remotePeerId;
                    for (NetworkProtocol.PeerInfo info : peerDiscovery.getDiscoveredPeers()) {
                        if (info.ipAddress.equals(remotePeerId)) {
                            realPeerId = info.peerId;
                            System.out.println("[MainFrame] Matched incoming connection IP " + remotePeerId
                                    + " to peerId " + realPeerId + " from UDP discovery");
                            break;
                        }
                    }

                    PeerConnection connection = new PeerConnection(socket, realPeerId);
                    messageHandler.addConnection(connection);
                    registerPeer(realPeerId, "Peer-" + realPeerId.substring(0, Math.min(8, realPeerId.length())));
                    updatePeerCount();

                    connection.setDisconnectHandler(disconnected -> {
                        messageHandler.removeConnection(disconnected);
                        updatePeerCount();
                        // Không tự động coi là ROOM_CLOSED khi socket ngắt; chỉ khi nhận DISCONNECT
                        // message
                    });

                    // Gửi HELLO để peer kia biết peerId của mình
                    connection.sendMessage(new NetworkProtocol.Message(
                            NetworkProtocol.MessageType.HELLO, peerId, null));

                    if (!canvas.getAllShapes().isEmpty()) {
                        connection.sendMessage(new NetworkProtocol.Message(
                                NetworkProtocol.MessageType.SHAPES, peerId,
                                new NetworkProtocol.ShapeData(canvas.getAllShapes(),
                                        System.currentTimeMillis())));
                        System.out.println("[MainFrame] Sent initial SHAPES sync to " + realPeerId);
                    }
                } catch (IOException e) {
                    System.err.println("Connection error: " + e.getMessage());
                }
            }
        } catch (java.net.SocketException e) {
            if (serverSocket != null && serverSocket.isClosed()) {
                System.out.println("[MainFrame] Server socket closed normally");
            } else {
                System.err.println("[MainFrame] Server socket error: " + e.getMessage());
            }
        } catch (IOException e) {
            System.err.println("[MainFrame] Server error: " + e.getMessage());
        }
    }

    private void showConnectionDialog() {
        ConnectionDialog dialog = new ConnectionDialog(this);
        // chỉ hiển thị peers cùng phòng
        java.util.List<NetworkProtocol.PeerInfo> sameRoom = new java.util.ArrayList<>();
        for (NetworkProtocol.PeerInfo info : peerDiscovery.getDiscoveredPeers()) {
            if (roomId == null || roomId.isEmpty() || roomId.equals(info.roomId)) {
                sameRoom.add(info);
            }
        }
        dialog.updatePeerList(sameRoom);

        Timer timer = new Timer(1000, e -> {
            java.util.List<NetworkProtocol.PeerInfo> filtered = new java.util.ArrayList<>();
            for (NetworkProtocol.PeerInfo info : peerDiscovery.getDiscoveredPeers()) {
                if (roomId == null || roomId.isEmpty() || roomId.equals(info.roomId)) {
                    filtered.add(info);
                }
            }
            dialog.updatePeerList(filtered);
        });
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
                System.out.println("[MainFrame] Connecting to peer " + peerInfo.peerId
                        + " at " + peerInfo.ipAddress + ":" + targetPort
                        + " (local listenPort=" + listenPort + ")");
                Socket socket = new Socket(peerInfo.ipAddress, targetPort);
                PeerConnection connection = new PeerConnection(socket, peerInfo.peerId);
                messageHandler.addConnection(connection);
                // đăng ký peer để hiển thị và gán màu cố định
                registerPeer(peerInfo.peerId, peerInfo.name);
                // nếu là client (guest) thì peer đầu tiên kết nối đến được xem là host
                if (!isHost && hostPeerId == null) {
                    hostPeerId = peerInfo.peerId;
                    updateRoomInfo();
                }
                updatePeerCount();

                connection.sendMessage(new NetworkProtocol.Message(
                        NetworkProtocol.MessageType.HELLO, peerId, null));

                connectionStatusLabel.setText("🟢 Connected: " + peerInfo.name);

                connection.setDisconnectHandler(disconnected -> {
                    messageHandler.removeConnection(disconnected);
                    updatePeerCount();
                    connectionStatusLabel.setText("🔴 Disconnected");
                    // Không tự động coi là ROOM_CLOSED khi socket ngắt; chỉ khi nhận DISCONNECT
                    // message
                });
            } catch (IOException e) {
                System.err.println("[MainFrame] Failed to connect to peer " + peerInfo + ": " + e.getMessage());
                JOptionPane.showMessageDialog(this,
                        "Connection failed: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }).start();
    }

    private void leaveRoom() {
        int result = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc muốn thoát phòng hiện tại và chọn phòng khác không?",
                "Leave Room",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (result != JOptionPane.YES_OPTION) {
            return;
        }

        // nếu là host, thông báo cho tất cả peers còn lại rằng phòng đã đóng
        if (isHost && messageHandler != null) {
            int peerCount = messageHandler.getConnectionCount();
            System.out.println("[MainFrame] Host leaving room, sending ROOM_CLOSED to "
                    + peerCount + " peer(s)");
            for (PeerConnection conn : messageHandler.getConnections()) {
                try {
                    conn.sendMessage(new NetworkProtocol.Message(
                            NetworkProtocol.MessageType.DISCONNECT, peerId, "ROOM_CLOSED"));
                    conn.flush(); // Force flush để đảm bảo message được gửi ngay
                    System.out.println("[MainFrame] Sent ROOM_CLOSED to peer " + conn.getPeerId());
                } catch (IOException e) {
                    System.err.println(
                            "[MainFrame] Failed to send ROOM_CLOSED to " + conn.getPeerId() + ": " + e.getMessage());
                }
            }
            // Đợi một chút để message được flush và gửi qua network
            try {
                Thread.sleep(500); // 500ms để đảm bảo message được gửi
                System.out.println("[MainFrame] Waited for messages to be sent");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // dừng discovery và đóng tất cả kết nối hiện tại
        if (peerDiscovery != null) {
            peerDiscovery.stop();
            peerDiscovery = null;
        }
        if (messageHandler != null) {
            messageHandler.closeAll();
        }

        // mở lại StartupDialog để chọn/join/create room mới
        String defaultName = peerName;
        String defaultRoom = roomId != null ? roomId : "room-1";
        StartupDialog startup = new StartupDialog(this, defaultName, defaultRoom);
        startup.setVisible(true);
        if (!startup.isConfirmed()) {
            // nếu user bấm Exit thì đóng app luôn
            shutdown();
            return;
        }

        peerName = startup.getPeerName();
        roomId = startup.getRoomCode();
        roomPassword = startup.getRoomPassword();
        joinMode = startup.isJoinMode();

        // cập nhật status bar với tên mới (peerId giữ nguyên để không thay đổi màu)
        if (connectionStatusLabel != null) {
            connectionStatusLabel.setText("🔵 " + peerName + " (" + peerId + ")");
        }

        // khởi động lại discovery với room/password mới
        peerDiscovery = new PeerDiscovery(peerId, peerName, listenPort, roomId, roomPassword);
        try {
            peerDiscovery.start();
        } catch (IOException e) {
            System.err.println("Failed to restart discovery: " + e.getMessage());
        }

        // nếu đang ở chế độ JOIN thì áp dụng lại logic kiểm tra bắt buộc phải có kết
        // nối
        if (joinMode) {
            new Thread(() -> {
                try {
                    Thread.sleep(3500);
                } catch (InterruptedException ignored) {
                }

                if (peerDiscovery != null && messageHandler.getConnectionCount() == 0) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this,
                                "Không tìm thấy phòng \"" + roomId + "\" với mật khẩu đã nhập.\n" +
                                        "Hãy kiểm tra lại room code và mật khẩu, hoặc chọn Create room.",
                                "Room not found",
                                JOptionPane.ERROR_MESSAGE);
                        dispose();
                        new MainFrame();
                    });
                }
            }, "JoinRoomValidator-Leave").start();
        }
    }

    private void setupEventHandlers() {
        colorPanel.widthSlider
                .addChangeListener(e -> canvas.getDrawingTool().setStrokeWidth(colorPanel.getStrokeWidth()));
    }

    /**
     * Host có thể kick 1 peer ra khỏi phòng.
     */
    private void kickPeer(String targetPeerId) {
        if (!isHost || messageHandler == null)
            return;
        PeerConnection conn = messageHandler.getConnection(targetPeerId);
        if (conn == null)
            return;
        int confirm = JOptionPane.showConfirmDialog(this,
                "Kick peer " + targetPeerId + " khỏi phòng?",
                "Kick Peer", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION)
            return;
        try {
            conn.sendMessage(new NetworkProtocol.Message(
                    NetworkProtocol.MessageType.DISCONNECT, peerId, "KICK"));
        } catch (IOException ignored) {
        }
        conn.disconnect();
        messageHandler.removeConnection(targetPeerId);
        updatePeerCount();
    }

    /**
     * Xử lý khi host gửi tín hiệu đóng phòng hoặc kick.
     */
    private void handleRemoteRoomDisconnect(String reason) {
        System.out.println("[MainFrame] Received DISCONNECT message with reason: " + reason);
        String message;
        if ("ROOM_CLOSED".equals(reason)) {
            message = "Chủ phòng đã đóng phòng. Bạn sẽ được đưa về màn hình chọn phòng.";
        } else if ("KICK".equals(reason)) {
            message = "Bạn đã bị chủ phòng kick khỏi phòng.";
        } else {
            message = "Bạn đã bị ngắt kết nối khỏi phòng.";
        }
        System.out.println("[MainFrame] Showing disconnect dialog: " + message);
        JOptionPane.showMessageDialog(this, message, "Room Closed",
                JOptionPane.INFORMATION_MESSAGE);

        // Dọn dẹp và trở về màn hình chọn room
        if (peerDiscovery != null) {
            peerDiscovery.stop();
            peerDiscovery = null;
        }
        if (messageHandler != null) {
            messageHandler.closeAll();
        }
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException ignored) {
        }

        dispose();
        new MainFrame();
    }

    /**
     * Thực hiện Undo local và thông báo cho tất cả peers khác.
     */
    private void performUndoWithBroadcast() {
        canvas.undoForPeer(peerId);
        try {
            messageHandler.broadcastUndo(peerId);
        } catch (Exception ex) {
            System.err.println("Failed to broadcast undo: " + ex.getMessage());
        }
    }

    /**
     * Thực hiện Clear Canvas local và thông báo cho tất cả peers khác.
     */
    private void performClearWithBroadcast() {
        canvas.clearCanvas();
        try {
            messageHandler.broadcastClear(peerId);
        } catch (Exception ex) {
            System.err.println("Failed to broadcast clear: " + ex.getMessage());
        }
    }

    private void updatePeerCount() {
        SwingUtilities.invokeLater(() -> peerCountLabel.setText("Peers: " + messageHandler.getConnectionCount()));
    }

    private void shutdown() {
        // nếu là host, thông báo đóng phòng cho các peers
        if (isHost && messageHandler != null) {
            int peerCount = messageHandler.getConnectionCount();
            System.out.println("[MainFrame] Host shutting down, sending ROOM_CLOSED to "
                    + peerCount + " peer(s)");
            for (PeerConnection conn : messageHandler.getConnections()) {
                try {
                    conn.sendMessage(new NetworkProtocol.Message(
                            NetworkProtocol.MessageType.DISCONNECT, peerId, "ROOM_CLOSED"));
                    conn.flush(); // Force flush để đảm bảo message được gửi ngay
                    System.out.println("[MainFrame] Sent ROOM_CLOSED to peer " + conn.getPeerId());
                } catch (IOException e) {
                    System.err.println(
                            "[MainFrame] Failed to send ROOM_CLOSED to " + conn.getPeerId() + ": " + e.getMessage());
                }
            }
            // Đợi một chút để message được flush và gửi qua network trước khi đóng
            // connection
            try {
                Thread.sleep(500); // 500ms để đảm bảo message được gửi
                System.out.println("[MainFrame] Waited for messages to be sent, closing connections...");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        if (peerDiscovery != null)
            peerDiscovery.stop();
        if (messageHandler != null)
            messageHandler.closeAll();
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
