package com.whiteboard.drawing;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Quản lý selection và manipulation của shapes
 * Hỗ trợ: select, move, resize, rotate, delete, copy/paste
 */
public class SelectionManager {
    private Set<Shape> selectedShapes = new HashSet<>();
    private Shape primarySelection = null;
    private SelectionBounds bounds = null;
    private int dragMode = DRAG_MOVE; // DRAG_MOVE, DRAG_RESIZE_*
    
    public static final int DRAG_MOVE = 0;
    public static final int DRAG_RESIZE_NW = 1;
    public static final int DRAG_RESIZE_N = 2;
    public static final int DRAG_RESIZE_NE = 3;
    public static final int DRAG_RESIZE_W = 4;
    public static final int DRAG_RESIZE_E = 5;
    public static final int DRAG_RESIZE_SW = 6;
    public static final int DRAG_RESIZE_S = 7;
    public static final int DRAG_RESIZE_SE = 8;

    public static class SelectionBounds {
        public int x, y, width, height;
        public Shape[] resizeHandles = new Shape[8];

        public SelectionBounds(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            updateHandles();
        }

        private void updateHandles() {
            int handleSize = 6;
            // NW, N, NE, W, E, SW, S, SE
            int[] hx = {x, x + width/2, x + width, x, x + width, x, x + width/2, x + width};
            int[] hy = {y, y, y, y + height/2, y + height/2, y + height, y + height, y + height};

            for (int i = 0; i < 8; i++) {
                resizeHandles[i] = new Shape();
                resizeHandles[i].type = Shape.ShapeType.RECTANGLE;
                resizeHandles[i].x1 = hx[i] - handleSize/2;
                resizeHandles[i].y1 = hy[i] - handleSize/2;
                resizeHandles[i].x2 = hx[i] + handleSize/2;
                resizeHandles[i].y2 = hy[i] + handleSize/2;
                resizeHandles[i].color = Color.BLUE;
            }
        }

        public void draw(Graphics2D g) {
            g.setColor(new Color(0, 0, 255, 100));
            g.setStroke(new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                    1, new float[]{5}, 0));
            g.drawRect(x, y, width, height);

            g.setColor(Color.BLUE);
            for (Shape handle : resizeHandles) {
                g.fillRect(handle.x1, handle.y1, handle.x2 - handle.x1, handle.y2 - handle.y1);
            }
        }
    }

    public void selectShape(Shape shape, boolean addToSelection) {
        if (!addToSelection) {
            selectedShapes.clear();
        }
        selectedShapes.add(shape);
        primarySelection = shape;
        updateBounds();
    }

    public void deselectShape(Shape shape) {
        selectedShapes.remove(shape);
        if (primarySelection == shape) {
            primarySelection = selectedShapes.isEmpty() ? null : selectedShapes.iterator().next();
        }
        updateBounds();
    }

    public void deselectAll() {
        selectedShapes.clear();
        primarySelection = null;
        bounds = null;
    }

    public boolean isSelected(Shape shape) {
        return selectedShapes.contains(shape);
    }

    public Set<Shape> getSelectedShapes() {
        return new HashSet<>(selectedShapes);
    }

    public Shape getPrimarySelection() {
        return primarySelection;
    }

    public SelectionBounds getBounds() {
        return bounds;
    }

    public int getDragMode(int mouseX, int mouseY) {
        if (bounds == null) return DRAG_MOVE;

        int handle = getResizeHandleAt(mouseX, mouseY);
        if (handle >= 0) return handle + 1;

        return DRAG_MOVE;
    }

    private int getResizeHandleAt(int mouseX, int mouseY) {
        if (bounds == null) return -1;
        for (int i = 0; i < 8; i++) {
            if (bounds.resizeHandles[i].contains(mouseX, mouseY)) {
                return i;
            }
        }
        return -1;
    }

    public void moveSelection(int deltaX, int deltaY) {
        for (Shape shape : selectedShapes) {
            shape.x1 += deltaX;
            shape.y1 += deltaY;
            shape.x2 += deltaX;
            shape.y2 += deltaY;
        }
        updateBounds();
    }

    public void resizeSelection(int deltaX, int deltaY, int mode) {
        if (bounds == null) return;

        switch (mode) {
            case DRAG_RESIZE_NW:
                bounds.x += deltaX;
                bounds.y += deltaY;
                bounds.width -= deltaX;
                bounds.height -= deltaY;
                break;
            case DRAG_RESIZE_N:
                bounds.y += deltaY;
                bounds.height -= deltaY;
                break;
            case DRAG_RESIZE_NE:
                bounds.y += deltaY;
                bounds.width += deltaX;
                bounds.height -= deltaY;
                break;
            case DRAG_RESIZE_W:
                bounds.x += deltaX;
                bounds.width -= deltaX;
                break;
            case DRAG_RESIZE_E:
                bounds.width += deltaX;
                break;
            case DRAG_RESIZE_SW:
                bounds.x += deltaX;
                bounds.width -= deltaX;
                bounds.height += deltaY;
                break;
            case DRAG_RESIZE_S:
                bounds.height += deltaY;
                break;
            case DRAG_RESIZE_SE:
                bounds.width += deltaX;
                bounds.height += deltaY;
                break;
        }

        bounds.updateHandles();
        applyBoundsToSelection();
    }

    public void rotateSelection(double angle) {
        if (bounds == null) return;
        
        int centerX = bounds.x + bounds.width / 2;
        int centerY = bounds.y + bounds.height / 2;

        for (Shape shape : selectedShapes) {
            int x1 = shape.x1 - centerX;
            int y1 = shape.y1 - centerY;
            int x2 = shape.x2 - centerX;
            int y2 = shape.y2 - centerY;

            double cos = Math.cos(angle);
            double sin = Math.sin(angle);

            shape.x1 = (int) (x1 * cos - y1 * sin) + centerX;
            shape.y1 = (int) (x1 * sin + y1 * cos) + centerY;
            shape.x2 = (int) (x2 * cos - y2 * sin) + centerX;
            shape.y2 = (int) (x2 * sin + y2 * cos) + centerY;
        }

        updateBounds();
    }

    public void deleteSelection() {
        selectedShapes.clear();
        primarySelection = null;
        bounds = null;
    }

    public List<Shape> copySelection() {
        return new ArrayList<>(selectedShapes);
    }

    private void applyBoundsToSelection() {
        if (selectedShapes.isEmpty()) return;

        for (Shape shape : selectedShapes) {
            shape.x1 = bounds.x;
            shape.y1 = bounds.y;
            shape.x2 = bounds.x + bounds.width;
            shape.y2 = bounds.y + bounds.height;
        }
    }

    private void updateBounds() {
        if (selectedShapes.isEmpty()) {
            bounds = null;
            return;
        }

        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;

        for (Shape shape : selectedShapes) {
            minX = Math.min(minX, Math.min(shape.x1, shape.x2));
            minY = Math.min(minY, Math.min(shape.y1, shape.y2));
            maxX = Math.max(maxX, Math.max(shape.x1, shape.x2));
            maxY = Math.max(maxY, Math.max(shape.y1, shape.y2));
        }

        bounds = new SelectionBounds(minX, minY, maxX - minX, maxY - minY);
    }
}
