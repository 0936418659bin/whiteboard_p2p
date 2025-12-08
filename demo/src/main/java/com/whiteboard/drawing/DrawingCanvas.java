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
                updateDrawing(e);
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                lastMouseX = e.getX();
                lastMouseY = e.getY();

                // hover: show tooltip với thông tin peer của shape dưới con trỏ
                int canvasX = zoomManager.screenToCanvasX(e.getX());
                int canvasY = zoomManager.screenToCanvasY(e.getY());
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

            // PENCIL gửi realtime 100% cho peer khác, ERASER không cần realtime
            if (tool.getCurrentTool() == DrawingTool.Tool.PENCIL && onShapeDrawn != null) {
                onShapeDrawn.accept(currentShape.clone());
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
        for (Shape shape : selectionManager.getSelectedShapes()) {
            layerManager.removeShapeFromActiveLayer(shape);
        }
        selectionManager.deleteSelection();
        repaint();
    }

    public void undo() {
        Shape lastShape = history.undo();
        if (lastShape != null) {
            layerManager.removeShapeFromActiveLayer(lastShape);
            repaint();
        }
    }

    public void redo() {
        Shape redoShape = history.redo();
        if (redoShape != null) {
            layerManager.addShapeToActiveLayer(redoShape);
            repaint();
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

        Shape old = remoteStrokeMap.get(key);
        if (old != null) {
            layerManager.removeShapeFromActiveLayer(old);
        }

        layerManager.addShapeToActiveLayer(shape);
        remoteStrokeMap.put(key, shape);
        repaint();
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

    public BufferedImage getCanvasImage() {
        return canvas;
    }

    public void loadShapes(List<Shape> shapes) {
        layerManager.clearAllLayers();
        for (Shape shape : shapes) {
            layerManager.addShapeToActiveLayer(shape);
        }
        repaint();
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
