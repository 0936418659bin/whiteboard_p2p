package com.whiteboard.drawing;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Quản lý layers cho canvas vẽ
 * Hỗ trợ: tạo/xóa layer, thay đổi thứ tự, ẩn/hiện, độ mờ
 */
public class LayerManager implements Serializable {
    private static final long serialVersionUID = 1L;

    public static class Layer implements Serializable, Cloneable {
        public String id;
        public String name;
        public List<Shape> shapes;
        public boolean visible = true;
        public float opacity = 1.0f; // 0.0-1.0
        public long createdTime;

        public Layer(String name) {
            this.id = UUID.randomUUID().toString().substring(0, 8);
            this.name = name;
            this.shapes = new CopyOnWriteArrayList<>();
            this.createdTime = System.currentTimeMillis();
        }

        @Override
        public Layer clone() {
            try {
                Layer clone = (Layer) super.clone();
                clone.shapes = new CopyOnWriteArrayList<>(this.shapes);
                return clone;
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private List<Layer> layers = new CopyOnWriteArrayList<>();
    private int activeLayerIndex = 0;

    public LayerManager() {
        // Tạo layer mặc định
        Layer defaultLayer = new Layer("Background");
        layers.add(defaultLayer);
    }

    public void createLayer(String name) {
        Layer newLayer = new Layer(name);
        layers.add(newLayer);
        setActiveLayer(layers.size() - 1);
    }

    public void deleteLayer(int index) {
        if (layers.size() > 1) {
            layers.remove(index);
            if (activeLayerIndex >= layers.size()) {
                activeLayerIndex = layers.size() - 1;
            }
        }
    }

    public void setActiveLayer(int index) {
        if (index >= 0 && index < layers.size()) {
            activeLayerIndex = index;
        }
    }

    public void moveLayer(int fromIndex, int toIndex) {
        if (fromIndex >= 0 && fromIndex < layers.size() &&
            toIndex >= 0 && toIndex < layers.size()) {
            Layer layer = layers.remove(fromIndex);
            layers.add(toIndex, layer);
        }
    }

    public void setLayerVisibility(int index, boolean visible) {
        if (index >= 0 && index < layers.size()) {
            layers.get(index).visible = visible;
        }
    }

    public void setLayerOpacity(int index, float opacity) {
        if (index >= 0 && index < layers.size()) {
            layers.get(index).opacity = Math.max(0.0f, Math.min(1.0f, opacity));
        }
    }

    public void renameLayer(int index, String newName) {
        if (index >= 0 && index < layers.size()) {
            layers.get(index).name = newName;
        }
    }

    public void addShapeToActiveLayer(Shape shape) {
        if (activeLayerIndex >= 0 && activeLayerIndex < layers.size()) {
            layers.get(activeLayerIndex).shapes.add(shape);
        }
    }

    public void removeShapeFromActiveLayer(Shape shape) {
        if (activeLayerIndex >= 0 && activeLayerIndex < layers.size()) {
            layers.get(activeLayerIndex).shapes.remove(shape);
        }
    }

    public Layer getActiveLayer() {
        if (activeLayerIndex >= 0 && activeLayerIndex < layers.size()) {
            return layers.get(activeLayerIndex);
        }
        return null;
    }

    public Layer getLayer(int index) {
        if (index >= 0 && index < layers.size()) {
            return layers.get(index);
        }
        return null;
    }

    public int getActiveLayerIndex() {
        return activeLayerIndex;
    }

    public List<Layer> getAllLayers() {
        return new CopyOnWriteArrayList<>(layers);
    }

    public int getLayerCount() {
        return layers.size();
    }

    public void clearAllLayers() {
        layers.clear();
        activeLayerIndex = 0;
        Layer defaultLayer = new Layer("Background");
        layers.add(defaultLayer);
    }

    public List<Shape> getAllShapes() {
        List<Shape> allShapes = new ArrayList<>();
        for (Layer layer : layers) {
            if (layer.visible) {
                allShapes.addAll(layer.shapes);
            }
        }
        return allShapes;
    }

    public void mergeAllLayers() {
        if (layers.isEmpty()) return;
        
        Layer mergedLayer = new Layer("Merged");
        for (Layer layer : layers) {
            mergedLayer.shapes.addAll(layer.shapes);
        }
        layers.clear();
        layers.add(mergedLayer);
        activeLayerIndex = 0;
    }
}
