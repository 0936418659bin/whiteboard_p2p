package com.whiteboard.sync;

import com.whiteboard.drawing.Shape;
import java.util.*;

public class ConflictResolver {

    public enum ResolutionStrategy {
        TIMESTAMP_BASED,
        PEER_ID_BASED,
        MERGE
    }

    private ResolutionStrategy strategy;

    public ConflictResolver(ResolutionStrategy strategy) {
        this.strategy = strategy;
    }

    public List<Shape> resolveConflict(List<Shape> localShapes, List<Shape> remoteShapes) {
        Map<Long, Shape> resolved = new HashMap<>();

        // Add all local shapes
        for (Shape shape : localShapes) {
            resolved.put(shape.getTimestamp(), shape);
        }

        // Resolve conflicts with remote shapes
        for (Shape remoteShape : remoteShapes) {
            Long timestamp = remoteShape.getTimestamp();

            if (!resolved.containsKey(timestamp)) {
                resolved.put(timestamp, remoteShape);
            } else {
                Shape localShape = resolved.get(timestamp);
                Shape winner = resolveShapeConflict(localShape, remoteShape);
                resolved.put(timestamp, winner);
            }
        }

        return new ArrayList<>(resolved.values());
    }

    private Shape resolveShapeConflict(Shape local, Shape remote) {
        return switch (strategy) {
            case TIMESTAMP_BASED -> local.getTimestamp() <= remote.getTimestamp() ? local : remote;
            case PEER_ID_BASED -> local.getPeerId().compareTo(remote.getPeerId()) <= 0 ? local : remote;
            case MERGE -> local; // Default: keep local
        };
    }

    public void setStrategy(ResolutionStrategy strategy) {
        this.strategy = strategy;
    }
}
