package com.whiteboard.drawing;

import java.awt.*;

public class DrawingTool {
    public enum Tool {
        PENCIL("Pencil"),
        LINE("Line"),
        RECTANGLE("Rectangle"),
        CIRCLE("Circle"),
        ELLIPSE("Ellipse"),
        ERASER("Eraser"),
        TEXT("Text"),
        PAN("Pan"),
        POLYGON("Polygon"),
        STAR("Star"),
        ARROW("Arrow"),
        CURVE("Curve"),
        FILL("Fill");

        private final String label;

        Tool(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    private Tool currentTool;
    private Color currentColor;
    private int strokeWidth;
    // Fill and gradient options
    private Color fillColor;
    private boolean useGradient;
    private Color gradientTo;

    public DrawingTool() {
        this.currentTool = Tool.PENCIL;
        this.currentColor = Color.BLACK;
        this.strokeWidth = 2;
        this.fillColor = null;
        this.useGradient = false;
        this.gradientTo = Color.WHITE;
    }

    public Tool getCurrentTool() {
        return currentTool;
    }

    public void setCurrentTool(Tool tool) {
        this.currentTool = tool;
    }

    public Color getCurrentColor() {
        return currentColor;
    }

    public void setCurrentColor(Color color) {
        this.currentColor = color;
    }

    public int getStrokeWidth() {
        return strokeWidth;
    }

    public void setStrokeWidth(int width) {
        this.strokeWidth = Math.max(1, Math.min(width, 50));
    }

    public Color getFillColor() {
        return fillColor;
    }

    public void setFillColor(Color fillColor) {
        this.fillColor = fillColor;
    }

    public boolean isUseGradient() {
        return useGradient;
    }

    public void setUseGradient(boolean useGradient) {
        this.useGradient = useGradient;
    }

    public Color getGradientTo() {
        return gradientTo;
    }

    public void setGradientTo(Color gradientTo) {
        this.gradientTo = gradientTo;
    }

    public Shape.ShapeType getShapeType() {
        return switch (currentTool) {
            case PENCIL -> Shape.ShapeType.FREE_DRAW;
            case LINE -> Shape.ShapeType.LINE;
            case RECTANGLE -> Shape.ShapeType.RECTANGLE;
            case CIRCLE -> Shape.ShapeType.CIRCLE;
            case ELLIPSE -> Shape.ShapeType.ELLIPSE;
            case TEXT -> Shape.ShapeType.TEXT;
            case PAN -> Shape.ShapeType.POINT; // không vẽ, chỉ pan
            case POLYGON -> Shape.ShapeType.POLYGON;
            case STAR -> Shape.ShapeType.STAR;
            case ARROW -> Shape.ShapeType.ARROW;
            case CURVE -> Shape.ShapeType.CURVE;
            case FILL -> Shape.ShapeType.FILL;
            default -> Shape.ShapeType.POINT;
        };
    }
}
