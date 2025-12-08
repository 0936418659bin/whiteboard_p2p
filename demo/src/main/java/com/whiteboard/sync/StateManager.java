package com.whiteboard.sync;

import com.whiteboard.drawing.Shape;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class StateManager {
    private final Map<Long, Shape> shapeStates;
    private long versionNumber;
    private final Object lock = new Object();

    public StateManager() {
        this.shapeStates = new ConcurrentHashMap<>();
        this.versionNumber = 0;
    }

    public synchronized void addOrUpdateShape(Shape shape) {
        shapeStates.put(shape.getTimestamp(), shape);
        versionNumber++;
    }

    public synchronized void removeShape(long timestamp) {
        shapeStates.remove(timestamp);
        versionNumber++;
    }

    public synchronized List<Shape> getShapesSince(long version) {
        return new ArrayList<>(shapeStates.values());
    }

    public synchronized List<Shape> getAllShapes() {
        return new ArrayList<>(shapeStates.values());
    }

    public synchronized long getCurrentVersion() {
        return versionNumber;
    }

    public synchronized void clear() {
        shapeStates.clear();
        versionNumber = 0;
    }

    public synchronized void sync(List<Shape> remoteShapes) {
        for (Shape shape : remoteShapes) {
            if (!shapeStates.containsKey(shape.getTimestamp())) {
                shapeStates.put(shape.getTimestamp(), shape);
                versionNumber++;
            }
        }
    }
}
