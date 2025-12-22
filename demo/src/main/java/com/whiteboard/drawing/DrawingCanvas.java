package com.whiteboard.drawing;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.Timer;

public class DrawingCanvas extends JPanel {
    private DrawingHistory history;
    private DrawingTool tool;
    private TextManager textManager;
    private LayerManager layerManager;
    private SelectionManager selectionManager;
    private ZoomManager zoomManager;
    private GridManager gridManager;
    private BufferedImage canvas;
    private Graphics2D g2d;
    private Shape currentShape;
    private List<Point> freeDrawPoints;
    private String peerId;
    private Consumer<Shape> onShapeDrawn;
    private Consumer<Shape> onShapeClicked;
    private boolean isDrawing;
    private int freeDrawPointsSinceBroadcast;
    private int lastMouseX, lastMouseY;
    private final Map<String, Shape> remoteStrokeMap = new HashMap<>();
    // Debounce repaint cho remote updates để giảm lag
    private Timer repaintTimer;
    private volatile boolean pendingRepaint = false;
    // Background image (imported image)
    private BufferedImage backgroundImage;
    private int backgroundImageX, backgroundImageY;
    private int backgroundImageWidth, backgroundImageHeight; // Current display size
    private boolean imageSelected = false;
    private boolean imageResizing = false;
    private int resizeHandle = -1; // 0=NW, 1=NE, 2=SW, 3=SE, 4=N, 5=S, 6=W, 7=E
    private int imageDragStartX, imageDragStartY;
    // Lưu original size và position khi bắt đầu resize
    private int resizeStartX, resizeStartY, resizeStartWidth, resizeStartHeight;
    // Image history để undo/redo
    private java.util.LinkedList<ImageState> imageHistory = new java.util.LinkedList<>();
    private java.util.LinkedList<ImageState> imageRedoHistory = new java.util.LinkedList<>();
    
    /**
     * Class để lưu trạng thái image cho undo/redo
     */
    private static class ImageState {
        BufferedImage image;
        int x, y, width, height;
        boolean hasImage;
        
        ImageState(BufferedImage img, int x, int y, int w, int h, boolean has) {
            this.image = img;
            this.x = x;
            this.y = y;
            this.width = w;
            this.height = h;
            this.hasImage = has;
        }
        
        ImageState copy() {
            BufferedImage imgCopy = image != null ? deepCopyImage(image) : null;
            return new ImageState(imgCopy, x, y, width, height, hasImage);
        }
        
        private BufferedImage deepCopyImage(BufferedImage original) {
            BufferedImage copy = new BufferedImage(original.getWidth(), original.getHeight(), original.getType());
            Graphics2D g = copy.createGraphics();
            g.drawImage(original, 0, 0, null);
            g.dispose();
            return copy;
        }
    }

    public DrawingCanvas(String peerId) {
        this.peerId = peerId;
        this.history = new DrawingHistory(1000);
        this.tool = new DrawingTool();
        this.textManager = new TextManager();
        this.layerManager = new LayerManager();
        this.selectionManager = new SelectionManager();
        this.zoomManager = new ZoomManager();
        this.gridManager = new GridManager();
        this.freeDrawPoints = new ArrayList<>();
        this.isDrawing = false;
        this.freeDrawPointsSinceBroadcast = 0;

        // cho phép canvas lớn hơn vùng nhìn, phục vụ scrollbar
        setPreferredSize(new Dimension(3000, 2000));

        setBackground(Color.WHITE);
        setDoubleBuffered(true);
        setFocusable(true);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocus();
                startDrawing(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                endDrawing(e);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) {
                    showContextMenu(e);
                }
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                // Handle image resize/move first
                if (imageSelected && backgroundImage != null) {
                    int canvasX = zoomManager.screenToCanvasX(e.getX());
                    int canvasY = zoomManager.screenToCanvasY(e.getY());
                    
                    if (imageResizing) {
                        // Resize image
                        resizeImage(canvasX, canvasY);
                    } else {
                        // Move image
                        backgroundImageX = canvasX - imageDragStartX;
                        backgroundImageY = canvasY - imageDragStartY;
                        repaint();
                    }
                    return;
                }
                
                updateDrawing(e);
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                lastMouseX = e.getX();
                lastMouseY = e.getY();
                
                int canvasX = zoomManager.screenToCanvasX(e.getX());
                int canvasY = zoomManager.screenToCanvasY(e.getY());
                
                // Update cursor khi hover vào image resize handles
                if (backgroundImage != null && imageSelected) {
                    int imgX = backgroundImageX;
                    int imgY = backgroundImageY;
                    int imgW = backgroundImageWidth > 0 ? backgroundImageWidth : backgroundImage.getWidth();
                    int imgH = backgroundImageHeight > 0 ? backgroundImageHeight : backgroundImage.getHeight();
                    
                    int handle = getResizeHandleAt(canvasX, canvasY, imgX, imgY, imgW, imgH);
                    if (handle >= 0) {
                        // Set cursor dựa trên handle
                        Cursor cursor = getResizeCursor(handle);
                        setCursor(cursor);
                        setToolTipText(null);
                        return;
                    } else if (canvasX >= imgX && canvasX <= imgX + imgW && 
                               canvasY >= imgY && canvasY <= imgY + imgH) {
                        // Hover vào image nhưng không phải handle - cursor move
                        setCursor(new Cursor(Cursor.MOVE_CURSOR));
                        setToolTipText(null);
                        return;
                    }
                }

                // Reset cursor nếu không hover vào image
                setCursor(new Cursor(Cursor.DEFAULT_CURSOR));

                // hover: show tooltip với thông tin peer của shape dưới con trỏ
                Shape hovered = findShapeAt(canvasX, canvasY);
                if (hovered != null && hovered.peerId != null && !hovered.peerId.isEmpty()) {
                    String shortId = hovered.peerId.length() > 4
                            ? hovered.peerId.substring(0, 4)
                            : hovered.peerId;
                    setToolTipText("Peer-" + shortId);
                } else {
                    setToolTipText(null);
                }
            }
        });

        addMouseWheelListener(new MouseAdapter() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                if ((e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0) {
                    if (e.getWheelRotation() < 0) {
                        zoomManager.zoomIn();
                    } else {
                        zoomManager.zoomOut();
                    }
                    repaint();
                }
            }
        });

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                createCanvas();
            }
        });

        setupKeyboardShortcuts();

        // Cấu hình tooltip hiển thị sau ~2s khi hover lên nét vẽ (thông tin peer)
        javax.swing.ToolTipManager.sharedInstance().setInitialDelay(2000);
        javax.swing.ToolTipManager.sharedInstance().setDismissDelay(4000);

        // Timer để debounce repaint khi nhận nhiều remote updates liên tiếp
        repaintTimer = new Timer(16, e -> { // ~60fps
            if (pendingRepaint) {
                pendingRepaint = false;
                repaint();
            }
        });
        repaintTimer.setRepeats(false);
    }

    private void setupKeyboardShortcuts() {
        InputMap inputMap = getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap actionMap = getActionMap();

        // Ctrl+Z: Undo
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK), "undo");
        actionMap.put("undo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                undo();
            }
        });

        // Ctrl+Y: Redo
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK), "redo");
        actionMap.put("redo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                redo();
            }
        });

        // Delete: Delete selection
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete");
        actionMap.put("delete", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteSelection();
            }
        });

        // Ctrl+A: Select all
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_DOWN_MASK), "selectAll");
        actionMap.put("selectAll", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectAll();
            }
        });
    }

    private void createCanvas() {
        int width = Math.max(getWidth(), 1);
        int height = Math.max(getHeight(), 1);

        if (canvas == null || canvas.getWidth() != width || canvas.getHeight() != height) {
            BufferedImage oldCanvas = canvas;
            canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            g2d = canvas.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, width, height);

            if (oldCanvas != null) {
                g2d.drawImage(oldCanvas, 0, 0, null);
            }
        }
    }

    private void startDrawing(MouseEvent e) {
        if (canvas == null) createCanvas();
        
        int canvasX = zoomManager.screenToCanvasX(e.getX());
        int canvasY = zoomManager.screenToCanvasY(e.getY());

        // Kiểm tra click vào background image
        if (backgroundImage != null) {
            int imgX = backgroundImageX;
            int imgY = backgroundImageY;
            int imgW = backgroundImageWidth > 0 ? backgroundImageWidth : backgroundImage.getWidth();
            int imgH = backgroundImageHeight > 0 ? backgroundImageHeight : backgroundImage.getHeight();
            
            if (canvasX >= imgX && canvasX <= imgX + imgW && 
                canvasY >= imgY && canvasY <= imgY + imgH) {
                // Click vào image - check resize handles hoặc move
                int handle = getResizeHandleAt(canvasX, canvasY, imgX, imgY, imgW, imgH);
                if (handle >= 0) {
                    // Click vào resize handle
                    System.out.println("[DrawingCanvas] Image resize handle clicked: " + handle);
                    imageSelected = true;
                    imageResizing = true;
                    resizeHandle = handle;
                    // Lưu original size và position khi bắt đầu resize
                    resizeStartX = backgroundImageX;
                    resizeStartY = backgroundImageY;
                    resizeStartWidth = backgroundImageWidth > 0 ? backgroundImageWidth : backgroundImage.getWidth();
                    resizeStartHeight = backgroundImageHeight > 0 ? backgroundImageHeight : backgroundImage.getHeight();
                    imageDragStartX = canvasX; // Vị trí click ban đầu
                    imageDragStartY = canvasY;
                    System.out.println("[DrawingCanvas] Resize start - pos: (" + resizeStartX + "," + resizeStartY + 
                            "), size: " + resizeStartWidth + "x" + resizeStartHeight);
                    repaint();
                    return;
                } else {
                    // Click vào image - start moving
                    System.out.println("[DrawingCanvas] Image move started");
                    imageSelected = true;
                    imageResizing = false;
                    imageDragStartX = canvasX - imgX;
                    imageDragStartY = canvasY - imgY;
                    repaint();
                    return;
                }
            } else {
                // Click outside image - deselect
                imageSelected = false;
                imageResizing = false;
                repaint();
            }
        }

        if (tool.getCurrentTool() == DrawingTool.Tool.PAN) {
            // PAN mode: trước tiên kiểm tra xem click trúng shape nào không để hiển thị thông tin peer
            Shape clicked = findShapeAt(canvasX, canvasY);
            if (clicked != null && onShapeClicked != null) {
                onShapeClicked.accept(clicked);
            }

            // sau đó mới bắt đầu pan nếu tiếp tục kéo chuột
            isDrawing = true;
            lastMouseX = e.getX();
            lastMouseY = e.getY();
            return;
        }

        if (e.getButton() == MouseEvent.BUTTON1) {
            // Text tool: click -> hỏi text, tạo shape ngay, không cần drag
            if (tool.getCurrentTool() == DrawingTool.Tool.TEXT) {
                // Cửa sổ nhập text xuất hiện giữa màn hình
                String text = JOptionPane.showInputDialog(null, "Enter text:", "Add Text",
                        JOptionPane.PLAIN_MESSAGE);
                if (text != null && !text.trim().isEmpty()) {
                    text = text.trim();
                    Shape textShape = new Shape(Shape.ShapeType.TEXT, canvasX, canvasY,
                            canvasX, canvasY, tool.getCurrentColor(), tool.getStrokeWidth(), peerId);
                    textShape.text = text;
                    // Sao chép thông tin font từ TextManager (giống các whiteboard khác)
                    Font f = textManager.getFont();
                    textShape.fontName = f.getFamily();
                    textShape.fontSize = f.getSize();
                    textShape.fontStyle = f.getStyle();
                    switch (textManager.getTextAlign()) {
                        case CENTER -> textShape.textAlign = 1;
                        case RIGHT -> textShape.textAlign = 2;
                        default -> textShape.textAlign = 0;
                    }
                    layerManager.addShapeToActiveLayer(textShape);
                    history.addShape(textShape);
                    if (onShapeDrawn != null) {
                        onShapeDrawn.accept(textShape.clone());
                    }
                    repaint();
                }
                return;
            }

            isDrawing = true;
            freeDrawPoints.clear();
            freeDrawPointsSinceBroadcast = 0;
            freeDrawPoints.add(new Point(canvasX, canvasY));

            currentShape = new Shape(tool.getShapeType(), canvasX, canvasY, canvasX, canvasY,
                tool.getCurrentColor(), tool.getStrokeWidth(), peerId);
            // apply fill/gradient options from tool
            currentShape.fillColor = tool.getFillColor();
            currentShape.useGradient = tool.isUseGradient();
            currentShape.gradientTo = tool.getGradientTo();
        }
    }

    private void updateDrawing(MouseEvent e) {
        if (!isDrawing) return;

        // Pan tool: kéo để di chuyển vùng nhìn
        if (tool.getCurrentTool() == DrawingTool.Tool.PAN) {
            int dx = e.getX() - lastMouseX;
            int dy = e.getY() - lastMouseY;
            zoomManager.pan(dx, dy);
            lastMouseX = e.getX();
            lastMouseY = e.getY();
            repaint();
            return;
        }

        if (currentShape == null) return;

        int canvasX = zoomManager.screenToCanvasX(e.getX());
        int canvasY = zoomManager.screenToCanvasY(e.getY());

        if (gridManager.isSnapToGrid()) {
            canvasX = gridManager.snapX(canvasX);
            canvasY = gridManager.snapY(canvasY);
        }

        if (tool.getCurrentTool() == DrawingTool.Tool.PENCIL ||
                tool.getCurrentTool() == DrawingTool.Tool.ERASER) {
            freeDrawPoints.add(new Point(canvasX, canvasY));
            currentShape.setEndX(canvasX);
            currentShape.setEndY(canvasY);
            // cập nhật đường vẽ tự do để hiển thị realtime trên canvas local
            currentShape.freeDrawPoints = new ArrayList<>(freeDrawPoints);

            // PENCIL: gửi realtime nhưng được "throttle" để tránh spam mạng và lag bên peer
            if (tool.getCurrentTool() == DrawingTool.Tool.PENCIL && onShapeDrawn != null) {
                freeDrawPointsSinceBroadcast++;
                // Giảm threshold xuống 2 điểm để mượt hơn khi remote xem
                if (freeDrawPointsSinceBroadcast >= 2) {
                    onShapeDrawn.accept(currentShape.clone());
                    freeDrawPointsSinceBroadcast = 0;
                }
            }
        } else {
            // Các tool khác (LINE, RECT, CIRCLE, v.v.) chỉ cập nhật local preview,
            // không gửi realtime trong lúc kéo chuột. Peer khác sẽ nhận full nét
            // khi mouseReleased trong endDrawing.
            currentShape.setEndX(canvasX);
            currentShape.setEndY(canvasY);
        }

        repaint();
    }

    private void endDrawing(MouseEvent e) {
        if (!isDrawing) return;

        // nếu đang ở pan mode thì chỉ dừng pan
        if (tool.getCurrentTool() == DrawingTool.Tool.PAN) {
            isDrawing = false;
            return;
        }

        if (currentShape == null) return;

        isDrawing = false;

        int w = Math.abs(currentShape.x2 - currentShape.x1);
        int h = Math.abs(currentShape.y2 - currentShape.y1);
        
        if (w > 2 || h > 2) {
            if (tool.getCurrentTool() == DrawingTool.Tool.PENCIL ||
                    tool.getCurrentTool() == DrawingTool.Tool.ERASER) {
                currentShape.freeDrawPoints = new ArrayList<>(freeDrawPoints);
            }

            layerManager.addShapeToActiveLayer(currentShape);
            history.addShape(currentShape);

            // gửi phát cuối cùng sau khi mouseReleased để peer nhận được toàn bộ nét vẽ
            if (onShapeDrawn != null) {
                onShapeDrawn.accept(currentShape.clone());
            }
        }

        freeDrawPoints.clear();
        currentShape = null;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (gridManager.isGridVisible()) {
            drawGrid(g2);
        }

        g2.translate(zoomManager.getPanX(), zoomManager.getPanY());
        g2.scale(zoomManager.getZoom(), zoomManager.getZoom());

        // Draw background image first (if exists)
        if (backgroundImage != null) {
            int imgW = backgroundImageWidth > 0 ? backgroundImageWidth : backgroundImage.getWidth();
            int imgH = backgroundImageHeight > 0 ? backgroundImageHeight : backgroundImage.getHeight();
            
            g2.drawImage(backgroundImage, backgroundImageX, backgroundImageY, imgW, imgH, null);
            
            // Draw selection border and resize handles if selected
            if (imageSelected) {
                g2.setColor(new Color(0, 120, 215)); // Blue selection color
                g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawRect(backgroundImageX, backgroundImageY, imgW, imgH);
                
                // Draw resize handles
                drawResizeHandles(g2, backgroundImageX, backgroundImageY, imgW, imgH);
            }
        }

        for (LayerManager.Layer layer : layerManager.getAllLayers()) {
            if (!layer.visible) continue;

            float alpha = layer.opacity;
            for (Shape shape : layer.shapes) {
                drawShape(g2, shape, alpha);
            }
        }

        if (currentShape != null) {
            // chỉ vẽ nét hiện tại, KHÔNG vẽ label peer để tránh rối và giảm lag
            drawShape(g2, currentShape, 1.0f);
        }

        SelectionManager.SelectionBounds bounds = selectionManager.getBounds();
        if (bounds != null) {
            bounds.draw(g2);
        }
    }

    private void drawGrid(Graphics2D g2) {
        int size = gridManager.getGridSize();
        g2.setColor(new Color(200, 200, 200, 100));
        g2.setStroke(new BasicStroke(0.5f));

        int width = getWidth();
        int height = getHeight();

        for (int x = 0; x < width; x += size) {
            g2.drawLine(x, 0, x, height);
        }
        for (int y = 0; y < height; y += size) {
            g2.drawLine(0, y, width, y);
        }
    }

    private void drawShape(Graphics2D g2, Shape shape, float alpha) {
        if (shape == null) return;

        Color color = shape.color;
        if (alpha < 1.0f && color != null) {
            color = new Color(color.getRed(), color.getGreen(), color.getBlue(),
                    (int) (color.getAlpha() * alpha));
        }
        shape.draw(g2, color, shape.strokeWidth);
    }

    private void showContextMenu(MouseEvent e) {
        JPopupMenu menu = new JPopupMenu();
        
        JMenuItem selectItem = new JMenuItem("Select All");
        selectItem.addActionListener(ev -> selectAll());
        menu.add(selectItem);
        
        JMenuItem deleteItem = new JMenuItem("Delete Selection");
        deleteItem.addActionListener(ev -> deleteSelection());
        menu.add(deleteItem);
        
        menu.addSeparator();
        
        JMenuItem undoItem = new JMenuItem("Undo");
        undoItem.addActionListener(ev -> undo());
        menu.add(undoItem);
        
        menu.show(this, e.getX(), e.getY());
    }

    public void selectAll() {
        selectionManager.deselectAll();
        for (Shape shape : getAllShapes()) {
            selectionManager.selectShape(shape, true);
        }
        repaint();
    }

    public void deleteSelection() {
        // Nếu đang select image thì xóa image
        if (imageSelected && backgroundImage != null) {
            deleteImage();
            return;
        }
        
        // Xóa các shapes được select
        for (Shape shape : selectionManager.getSelectedShapes()) {
            layerManager.removeShapeFromActiveLayer(shape);
        }
        selectionManager.deleteSelection();
        repaint();
    }

    public void undo() {
        // Undo image trước (nếu có)
        if (!imageHistory.isEmpty()) {
            ImageState currentState = new ImageState(backgroundImage, backgroundImageX, backgroundImageY,
                    backgroundImageWidth, backgroundImageHeight, backgroundImage != null);
            imageRedoHistory.addFirst(currentState);
            
            ImageState prevState = imageHistory.removeLast();
            restoreImageState(prevState);
            repaint();
            return; // Undo image thì không undo shape
        }
        
        // Undo shape
        Shape lastShape = history.undo();
        if (lastShape != null) {
            layerManager.removeShapeFromActiveLayer(lastShape);
            repaint();
        }
    }

    /**
     * Undo theo peer: xoá nét vẽ gần nhất của peerId (dùng cho sync mạng).
     */
    public void undoForPeer(String targetPeerId) {
        if (targetPeerId == null) return;
        java.util.List<LayerManager.Layer> layers = layerManager.getAllLayers();
        for (int li = layers.size() - 1; li >= 0; li--) {
            LayerManager.Layer layer = layers.get(li);
            java.util.List<Shape> shapes = layer.shapes;
            for (int si = shapes.size() - 1; si >= 0; si--) {
                Shape s = shapes.get(si);
                if (s != null && targetPeerId.equals(s.peerId)) {
                    shapes.remove(si);
                    repaint();
                    return;
                }
            }
        }
    }

    public void redo() {
        // Redo image trước (nếu có)
        if (!imageRedoHistory.isEmpty()) {
            ImageState currentState = new ImageState(backgroundImage, backgroundImageX, backgroundImageY,
                    backgroundImageWidth, backgroundImageHeight, backgroundImage != null);
            imageHistory.addLast(currentState);
            
            ImageState nextState = imageRedoHistory.removeFirst();
            restoreImageState(nextState);
            repaint();
            return; // Redo image thì không redo shape
        }
        
        // Redo shape
        Shape redoShape = history.redo();
        if (redoShape != null) {
            layerManager.addShapeToActiveLayer(redoShape);
            repaint();
        }
    }
    
    /**
     * Restore image state từ ImageState
     */
    private void restoreImageState(ImageState state) {
        if (state.hasImage && state.image != null) {
            this.backgroundImage = state.image;
            this.backgroundImageX = state.x;
            this.backgroundImageY = state.y;
            this.backgroundImageWidth = state.width;
            this.backgroundImageHeight = state.height;
            this.imageSelected = true;
        } else {
            this.backgroundImage = null;
            this.imageSelected = false;
        }
    }
    
    /**
     * Save current image state vào history
     */
    private void saveImageState() {
        ImageState state = new ImageState(backgroundImage, backgroundImageX, backgroundImageY,
                backgroundImageWidth, backgroundImageHeight, backgroundImage != null);
        imageHistory.addLast(state.copy());
        imageRedoHistory.clear(); // Clear redo khi có action mới
        
        // Giới hạn history size
        if (imageHistory.size() > 50) {
            imageHistory.removeFirst();
        }
    }

    public void clearCanvas() {
        layerManager.clearAllLayers();
        history.clear();
        selectionManager.deselectAll();
        repaint();
    }

    public void drawRemoteShape(Shape shape) {
        if (shape == null) return;
        // Mỗi stroke từ 1 peer (peerId + timestamp) chỉ giữ 1 shape
        String peer = shape.peerId == null ? "" : shape.peerId;
        String key = peer + ":" + shape.timestamp;

        Shape existing = remoteStrokeMap.get(key);
        if (existing == null) {
            // lần đầu nhận stroke này: thêm vào layer và map
            layerManager.addShapeToActiveLayer(shape);
            remoteStrokeMap.put(key, shape);
        } else {
            // cập nhật in-place để tránh thêm/xoá khỏi layer liên tục (giảm giật lag)
            existing.x1 = shape.x1;
            existing.y1 = shape.y1;
            existing.x2 = shape.x2;
            existing.y2 = shape.y2;
            existing.strokeWidth = shape.strokeWidth;
            existing.color = shape.color;
            existing.type = shape.type;
            existing.text = shape.text;
            existing.freeDrawPoints = shape.freeDrawPoints == null
                    ? null
                    : new ArrayList<>(shape.freeDrawPoints);
            existing.fillColor = shape.fillColor;
            existing.useGradient = shape.useGradient;
            existing.gradientTo = shape.gradientTo;
        }

        // Debounce repaint để tránh repaint quá nhiều lần khi nhận nhiều updates liên tiếp
        pendingRepaint = true;
        if (!repaintTimer.isRunning()) {
            repaintTimer.start();
        }
    }

    public List<Shape> getAllShapes() {
        return layerManager.getAllShapes();
    }

    public DrawingTool getDrawingTool() {
        return tool;
    }

    public LayerManager getLayerManager() {
        return layerManager;
    }

    public SelectionManager getSelectionManager() {
        return selectionManager;
    }

    public ZoomManager getZoomManager() {
        return zoomManager;
    }

    public GridManager getGridManager() {
        return gridManager;
    }

    public TextManager getTextManager() {
        return textManager;
    }

    public void setOnShapeDrawn(Consumer<Shape> callback) {
        this.onShapeDrawn = callback;
    }

    public void setOnShapeClicked(Consumer<Shape> callback) {
        this.onShapeClicked = callback;
    }

    /**
     * Render tất cả shapes vào BufferedImage để export PNG.
     * Tạo image mới với kích thước canvas và render tất cả shapes.
     */
    public BufferedImage getCanvasImage() {
        int width = getPreferredSize().width;
        int height = getPreferredSize().height;
        
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Fill nền trắng
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width, height);
        
        // Render tất cả shapes từ layers
        for (LayerManager.Layer layer : layerManager.getAllLayers()) {
            if (!layer.visible) continue;
            
            float alpha = layer.opacity;
            for (Shape shape : layer.shapes) {
                if (shape == null) continue;
                
                Color color = shape.color;
                if (alpha < 1.0f && color != null) {
                    color = new Color(color.getRed(), color.getGreen(), color.getBlue(),
                            (int) (color.getAlpha() * alpha));
                }
                shape.draw(g2d, color, shape.strokeWidth);
            }
        }
        
        g2d.dispose();
        return image;
    }

    public void loadShapes(List<Shape> shapes) {
        layerManager.clearAllLayers();
        for (Shape shape : shapes) {
            layerManager.addShapeToActiveLayer(shape);
        }
        repaint();
    }
    
    /**
     * Import image và vẽ vào canvas tại vị trí (x, y)
     * Nếu x, y = -1, sẽ đặt ở góc trên bên trái (0, 0)
     */
    public void importImage(BufferedImage image, int x, int y) {
        if (image == null) {
            System.err.println("Cannot import null image");
            return;
        }
        
        // Save current state để undo
        if (backgroundImage != null) {
            saveImageState();
        }
        
        this.backgroundImage = image;
        this.backgroundImageX = (x < 0) ? 0 : x;
        this.backgroundImageY = (y < 0) ? 0 : y;
        this.backgroundImageWidth = image.getWidth();
        this.backgroundImageHeight = image.getHeight();
        this.imageSelected = true; // Auto-select khi import
        
        // Save new state
        saveImageState();
        
        // Repaint để hiển thị image
        repaint();
        
        System.out.println("[DrawingCanvas] Image imported: " + image.getWidth() + "x" + image.getHeight() 
                + " at (" + backgroundImageX + ", " + backgroundImageY + ")");
    }
    
    /**
     * Xóa background image
     */
    public void deleteImage() {
        if (backgroundImage == null) {
            return;
        }
        
        // Save current state để undo
        saveImageState();
        
        this.backgroundImage = null;
        this.imageSelected = false;
        this.imageResizing = false;
        this.resizeHandle = -1;
        
        repaint();
        
        System.out.println("[DrawingCanvas] Image deleted");
    }
    
    /**
     * Kiểm tra xem điểm (x, y) có nằm trên resize handle không
     * Returns: 0=NW, 1=NE, 2=SW, 3=SE, 4=N, 5=S, 6=W, 7=E, -1=none
     */
    private int getResizeHandleAt(int x, int y, int imgX, int imgY, int imgW, int imgH) {
        int handleSize = 12; // Tăng size để dễ click hơn
        int halfHandle = handleSize / 2;
        
        // Corner handles - kiểm tra trước để ưu tiên
        if (Math.abs(x - imgX) <= halfHandle && Math.abs(y - imgY) <= halfHandle) {
            System.out.println("[DrawingCanvas] Handle NW detected at (" + x + "," + y + ")");
            return 0; // NW
        }
        if (Math.abs(x - (imgX + imgW)) <= halfHandle && Math.abs(y - imgY) <= halfHandle) {
            System.out.println("[DrawingCanvas] Handle NE detected");
            return 1; // NE
        }
        if (Math.abs(x - imgX) <= halfHandle && Math.abs(y - (imgY + imgH)) <= halfHandle) {
            System.out.println("[DrawingCanvas] Handle SW detected");
            return 2; // SW
        }
        if (Math.abs(x - (imgX + imgW)) <= halfHandle && Math.abs(y - (imgY + imgH)) <= halfHandle) {
            System.out.println("[DrawingCanvas] Handle SE detected");
            return 3; // SE
        }
        
        // Edge handles
        if (Math.abs(x - (imgX + imgW / 2)) <= halfHandle && Math.abs(y - imgY) <= halfHandle) {
            System.out.println("[DrawingCanvas] Handle N detected");
            return 4; // N
        }
        if (Math.abs(x - (imgX + imgW / 2)) <= halfHandle && Math.abs(y - (imgY + imgH)) <= halfHandle) {
            System.out.println("[DrawingCanvas] Handle S detected");
            return 5; // S
        }
        if (Math.abs(x - imgX) <= halfHandle && Math.abs(y - (imgY + imgH / 2)) <= halfHandle) {
            System.out.println("[DrawingCanvas] Handle W detected");
            return 6; // W
        }
        if (Math.abs(x - (imgX + imgW)) <= halfHandle && Math.abs(y - (imgY + imgH / 2)) <= halfHandle) {
            System.out.println("[DrawingCanvas] Handle E detected");
            return 7; // E
        }
        
        return -1;
    }
    
    /**
     * Lấy cursor phù hợp cho resize handle
     */
    private Cursor getResizeCursor(int handle) {
        switch (handle) {
            case 0: // NW
                return new Cursor(Cursor.NW_RESIZE_CURSOR);
            case 1: // NE
                return new Cursor(Cursor.NE_RESIZE_CURSOR);
            case 2: // SW
                return new Cursor(Cursor.SW_RESIZE_CURSOR);
            case 3: // SE
                return new Cursor(Cursor.SE_RESIZE_CURSOR);
            case 4: // N
                return new Cursor(Cursor.N_RESIZE_CURSOR);
            case 5: // S
                return new Cursor(Cursor.S_RESIZE_CURSOR);
            case 6: // W
                return new Cursor(Cursor.W_RESIZE_CURSOR);
            case 7: // E
                return new Cursor(Cursor.E_RESIZE_CURSOR);
            default:
                return new Cursor(Cursor.DEFAULT_CURSOR);
        }
    }
    
    /**
     * Vẽ resize handles
     */
    private void drawResizeHandles(Graphics2D g2, int x, int y, int w, int h) {
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(1.0f));
        
        int handleSize = 8;
        int halfHandle = handleSize / 2;
        
        // Corner handles
        g2.fillRect(x - halfHandle, y - halfHandle, handleSize, handleSize);
        g2.fillRect(x + w - halfHandle, y - halfHandle, handleSize, handleSize);
        g2.fillRect(x - halfHandle, y + h - halfHandle, handleSize, handleSize);
        g2.fillRect(x + w - halfHandle, y + h - halfHandle, handleSize, handleSize);
        
        // Edge handles
        g2.fillRect(x + w / 2 - halfHandle, y - halfHandle, handleSize, handleSize);
        g2.fillRect(x + w / 2 - halfHandle, y + h - halfHandle, handleSize, handleSize);
        g2.fillRect(x - halfHandle, y + h / 2 - halfHandle, handleSize, handleSize);
        g2.fillRect(x + w - halfHandle, y + h / 2 - halfHandle, handleSize, handleSize);
        
        // Draw border around handles
        g2.setColor(new Color(0, 120, 215));
        for (int i = 0; i < 8; i++) {
            int hx = 0, hy = 0;
            switch (i) {
                case 0: hx = x; hy = y; break; // NW
                case 1: hx = x + w; hy = y; break; // NE
                case 2: hx = x; hy = y + h; break; // SW
                case 3: hx = x + w; hy = y + h; break; // SE
                case 4: hx = x + w / 2; hy = y; break; // N
                case 5: hx = x + w / 2; hy = y + h; break; // S
                case 6: hx = x; hy = y + h / 2; break; // W
                case 7: hx = x + w; hy = y + h / 2; break; // E
            }
            g2.drawRect(hx - halfHandle, hy - halfHandle, handleSize, handleSize);
        }
    }
    
    /**
     * Resize image dựa trên handle được kéo
     */
    private void resizeImage(int newX, int newY) {
        if (backgroundImage == null || resizeHandle < 0) {
            System.err.println("[DrawingCanvas] Cannot resize: image=" + (backgroundImage != null) + 
                    ", handle=" + resizeHandle);
            return;
        }
        
        // Tính delta từ vị trí bắt đầu drag (trong canvas coordinates)
        int deltaX = newX - imageDragStartX;
        int deltaY = newY - imageDragStartY;
        
        // Tính scale factor dựa trên original size
        double scaleX = 1.0;
        double scaleY = 1.0;
        
        switch (resizeHandle) {
            case 0: // NW - resize từ góc trên bên trái
                scaleX = 1.0 - (double)deltaX / resizeStartWidth;
                scaleY = 1.0 - (double)deltaY / resizeStartHeight;
                break;
            case 1: // NE - resize từ góc trên bên phải
                scaleX = 1.0 + (double)deltaX / resizeStartWidth;
                scaleY = 1.0 - (double)deltaY / resizeStartHeight;
                break;
            case 2: // SW - resize từ góc dưới bên trái
                scaleX = 1.0 - (double)deltaX / resizeStartWidth;
                scaleY = 1.0 + (double)deltaY / resizeStartHeight;
                break;
            case 3: // SE - resize từ góc dưới bên phải
                scaleX = 1.0 + (double)deltaX / resizeStartWidth;
                scaleY = 1.0 + (double)deltaY / resizeStartHeight;
                break;
            case 4: // N - resize từ cạnh trên
                scaleY = 1.0 - (double)deltaY / resizeStartHeight;
                break;
            case 5: // S - resize từ cạnh dưới
                scaleY = 1.0 + (double)deltaY / resizeStartHeight;
                break;
            case 6: // W - resize từ cạnh trái
                scaleX = 1.0 - (double)deltaX / resizeStartWidth;
                break;
            case 7: // E - resize từ cạnh phải
                scaleX = 1.0 + (double)deltaX / resizeStartWidth;
                break;
        }
        
        // Giới hạn scale tối thiểu và tối đa
        scaleX = Math.max(0.1, Math.min(10.0, scaleX));
        scaleY = Math.max(0.1, Math.min(10.0, scaleY));
        
        // Tính new size
        int newW = (int)(resizeStartWidth * scaleX);
        int newH = (int)(resizeStartHeight * scaleY);
        
        // Đảm bảo minimum size
        newW = Math.max(10, newW);
        newH = Math.max(10, newH);
        
        // Tính new position dựa trên anchor point
        switch (resizeHandle) {
            case 0: // NW - anchor ở SE
                backgroundImageX = resizeStartX + resizeStartWidth - newW;
                backgroundImageY = resizeStartY + resizeStartHeight - newH;
                break;
            case 1: // NE - anchor ở SW
                backgroundImageY = resizeStartY + resizeStartHeight - newH;
                break;
            case 2: // SW - anchor ở NE
                backgroundImageX = resizeStartX + resizeStartWidth - newW;
                break;
            case 3: // SE - anchor ở NW (không đổi position)
                // Position giữ nguyên
                break;
            case 4: // N - anchor ở S
                backgroundImageY = resizeStartY + resizeStartHeight - newH;
                break;
            case 5: // S - anchor ở N (không đổi Y)
                // Y giữ nguyên
                break;
            case 6: // W - anchor ở E
                backgroundImageX = resizeStartX + resizeStartWidth - newW;
                break;
            case 7: // E - anchor ở W (không đổi X)
                // X giữ nguyên
                break;
        }
        
        backgroundImageWidth = newW;
        backgroundImageHeight = newH;
        
        repaint();
    }
    
    /**
     * Xóa background image
     */
    public void clearBackgroundImage() {
        this.backgroundImage = null;
        repaint();
    }
    
    /**
     * Lấy background image
     */
    public BufferedImage getBackgroundImage() {
        return backgroundImage;
    }

    private Shape findShapeAt(int x, int y) {
        // duyệt từ layer cuối cùng để ưu tiên shape trên cùng
        List<LayerManager.Layer> layers = layerManager.getAllLayers();
        for (int i = layers.size() - 1; i >= 0; i--) {
            LayerManager.Layer layer = layers.get(i);
            if (!layer.visible) continue;
            List<Shape> shapes = layer.shapes;
            for (int j = shapes.size() - 1; j >= 0; j--) {
                Shape s = shapes.get(j);
                if (s != null && s.contains(x, y)) {
                    return s;
                }
            }
        }
        return null;
    }
}
