package com.whiteboard.drawing;

import java.util.*;

public class DrawingHistory {
    private final LinkedList<Shape> shapeHistory;
    private final LinkedList<Shape> redoHistory;
    private final int maxHistorySize;

    public DrawingHistory(int maxSize) {
        this.shapeHistory = new LinkedList<>();
        this.redoHistory = new LinkedList<>();
        this.maxHistorySize = maxSize;
    }

    public synchronized void addShape(Shape shape) {
        shapeHistory.add(shape);
        redoHistory.clear(); // Clear redo history when new shape is added
        if (shapeHistory.size() > maxHistorySize) {
            shapeHistory.removeFirst();
        }
    }

    public synchronized Shape undo() {
        if (!shapeHistory.isEmpty()) {
            Shape shape = shapeHistory.removeLast();
            redoHistory.add(shape);
            return shape;
        }
        return null;
    }

    public synchronized Shape redo() {
        if (!redoHistory.isEmpty()) {
            Shape shape = redoHistory.removeLast();
            shapeHistory.add(shape);
            return shape;
        }
        return null;
    }

    public synchronized boolean canUndo() {
        return !shapeHistory.isEmpty();
    }

    public synchronized boolean canRedo() {
        return !redoHistory.isEmpty();
    }

    public synchronized List<Shape> getAllShapes() {
        return new ArrayList<>(shapeHistory);
    }

    public synchronized void clear() {
        shapeHistory.clear();
        redoHistory.clear();
    }

    public synchronized int size() {
        return shapeHistory.size();
    }

    public synchronized void addAllShapes(List<Shape> shapes) {
        for (Shape shape : shapes) {
            addShape(shape);
        }
    }
}
