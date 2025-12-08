package com.whiteboard.drawing;

/**
 * Quản lý lưới grid và căn chỉnh đối tượng
 * Hỗ trợ: hiển thị grid, snap to grid, align shapes
 */
public class GridManager {
    private int gridSize = 10;
    private boolean gridVisible = false;
    private boolean snapToGrid = false;

    public static final int[] GRID_SIZES = {5, 10, 15, 20, 25, 50};

    public void setGridSize(int size) {
        if (size > 0) {
            gridSize = size;
        }
    }

    public void setGridVisible(boolean visible) {
        gridVisible = visible;
    }

    public void setSnapToGrid(boolean snap) {
        snapToGrid = snap;
    }

    public int snapX(int x) {
        if (!snapToGrid) return x;
        return Math.round((float) x / gridSize) * gridSize;
    }

    public int snapY(int y) {
        if (!snapToGrid) return y;
        return Math.round((float) y / gridSize) * gridSize;
    }

    public int getGridSize() {
        return gridSize;
    }

    public boolean isGridVisible() {
        return gridVisible;
    }

    public boolean isSnapToGrid() {
        return snapToGrid;
    }

    // Căn chỉnh shapes
    public static void alignLeft(java.util.Set<Shape> shapes) {
        if (shapes.isEmpty()) return;
        int minX = Integer.MAX_VALUE;
        for (Shape s : shapes) {
            minX = Math.min(minX, Math.min(s.x1, s.x2));
        }
        for (Shape s : shapes) {
            int offset = Math.min(s.x1, s.x2) - minX;
            s.x1 -= offset;
            s.x2 -= offset;
        }
    }

    public static void alignRight(java.util.Set<Shape> shapes) {
        if (shapes.isEmpty()) return;
        int maxX = Integer.MIN_VALUE;
        for (Shape s : shapes) {
            maxX = Math.max(maxX, Math.max(s.x1, s.x2));
        }
        for (Shape s : shapes) {
            int offset = Math.max(s.x1, s.x2) - maxX;
            s.x1 -= offset;
            s.x2 -= offset;
        }
    }

    public static void alignTop(java.util.Set<Shape> shapes) {
        if (shapes.isEmpty()) return;
        int minY = Integer.MAX_VALUE;
        for (Shape s : shapes) {
            minY = Math.min(minY, Math.min(s.y1, s.y2));
        }
        for (Shape s : shapes) {
            int offset = Math.min(s.y1, s.y2) - minY;
            s.y1 -= offset;
            s.y2 -= offset;
        }
    }

    public static void alignBottom(java.util.Set<Shape> shapes) {
        if (shapes.isEmpty()) return;
        int maxY = Integer.MIN_VALUE;
        for (Shape s : shapes) {
            maxY = Math.max(maxY, Math.max(s.y1, s.y2));
        }
        for (Shape s : shapes) {
            int offset = Math.max(s.y1, s.y2) - maxY;
            s.y1 -= offset;
            s.y2 -= offset;
        }
    }

    public static void distributeHorizontally(java.util.Set<Shape> shapes) {
        if (shapes.size() < 3) return;

        java.util.List<Shape> sortedShapes = new java.util.ArrayList<>(shapes);
        sortedShapes.sort((a, b) -> Integer.compare(
                Math.min(a.x1, a.x2), Math.min(b.x1, b.x2)));

        int minX = Math.min(sortedShapes.get(0).x1, sortedShapes.get(0).x2);
        int maxX = Math.max(sortedShapes.get(sortedShapes.size()-1).x1, 
                           sortedShapes.get(sortedShapes.size()-1).x2);
        int step = (maxX - minX) / (shapes.size() - 1);

        for (int i = 1; i < sortedShapes.size() - 1; i++) {
            Shape s = sortedShapes.get(i);
            int currentX = Math.min(s.x1, s.x2);
            int targetX = minX + i * step;
            int offset = targetX - currentX;
            s.x1 += offset;
            s.x2 += offset;
        }
    }

    public static void distributeVertically(java.util.Set<Shape> shapes) {
        if (shapes.size() < 3) return;

        java.util.List<Shape> sortedShapes = new java.util.ArrayList<>(shapes);
        sortedShapes.sort((a, b) -> Integer.compare(
                Math.min(a.y1, a.y2), Math.min(b.y1, b.y2)));

        int minY = Math.min(sortedShapes.get(0).y1, sortedShapes.get(0).y2);
        int maxY = Math.max(sortedShapes.get(sortedShapes.size()-1).y1,
                           sortedShapes.get(sortedShapes.size()-1).y2);
        int step = (maxY - minY) / (shapes.size() - 1);

        for (int i = 1; i < sortedShapes.size() - 1; i++) {
            Shape s = sortedShapes.get(i);
            int currentY = Math.min(s.y1, s.y2);
            int targetY = minY + i * step;
            int offset = targetY - currentY;
            s.y1 += offset;
            s.y2 += offset;
        }
    }
}
