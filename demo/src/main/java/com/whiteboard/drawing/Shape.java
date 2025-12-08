package com.whiteboard.drawing;

import java.awt.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Shape implements Serializable, Cloneable {
    public enum ShapeType {
        POINT, LINE, RECTANGLE, CIRCLE, ELLIPSE, FREE_DRAW, TEXT,
        POLYGON, STAR, ARROW, CURVE, FILL
    }

    // Public properties cho dễ truy cập
    public int x1, y1, x2, y2;
    public Color color;
    public int strokeWidth;
    public String text;
    public List<Point> freeDrawPoints;
    public int[] polyX, polyY;
    public int polySides;
    public float rotation;
    // Fill / gradient support
    public Color fillColor; // if not null, fill the shape
    public boolean useGradient = false;
    public Color gradientTo; // if useGradient true, gradient from color -> gradientTo

    // Public properties - type cần public để truy cập
    public ShapeType type;
    public long timestamp;
    public String peerId;

    public Shape(ShapeType type, int startX, int startY, int endX, int endY,
            Color color, int strokeWidth, String peerId) {
        this.type = type;
        this.x1 = startX;
        this.y1 = startY;
        this.x2 = endX;
        this.y2 = endY;
        this.color = color;
        this.strokeWidth = strokeWidth;
        this.timestamp = System.currentTimeMillis();
        this.peerId = peerId;
        this.freeDrawPoints = new ArrayList<>();
        this.rotation = 0;
    }

    public Shape(ShapeType type, int x, int y, Color color, int strokeWidth, String peerId) {
        this(type, x, y, x, y, color, strokeWidth, peerId);
    }

    public Shape() {
        this(ShapeType.POINT, 0, 0, 0, 0, Color.BLACK, 1, "");
    }

    // Getters and Setters
    public ShapeType getType() {
        return type;
    }

    public void setType(ShapeType type) {
        this.type = type;
    }

    public int getStartX() {
        return x1;
    }

    public void setStartX(int x) {
        this.x1 = x;
    }

    public int getStartY() {
        return y1;
    }

    public void setStartY(int y) {
        this.y1 = y;
    }

    public int getEndX() {
        return x2;
    }

    public void setEndX(int x) {
        this.x2 = x;
    }

    public int getEndY() {
        return y2;
    }

    public void setEndY(int y) {
        this.y2 = y;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public int getStrokeWidth() {
        return strokeWidth;
    }

    public void setStrokeWidth(int strokeWidth) {
        this.strokeWidth = strokeWidth;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getPeerId() {
        return peerId;
    }

    public void setPeerId(String peerId) {
        this.peerId = peerId;
    }

    public boolean contains(int px, int py) {
        switch (type) {
            case POINT:
                return Math.hypot(px - x1, py - y1) <= strokeWidth;
            case LINE:
                return distanceToLine(px, py) <= strokeWidth;
            case FREE_DRAW:
                if (freeDrawPoints == null || freeDrawPoints.size() < 2) {
                    return false;
                }
                double maxDist = strokeWidth + 2;
                for (int i = 1; i < freeDrawPoints.size(); i++) {
                    Point p1 = freeDrawPoints.get(i - 1);
                    Point p2 = freeDrawPoints.get(i);
                    if (distanceToSegment(px, py, p1.x, p1.y, p2.x, p2.y) <= maxDist) {
                        return true;
                    }
                }
                return false;
            case RECTANGLE:
                int minX = Math.min(x1, x2);
                int maxX = Math.max(x1, x2);
                int minY = Math.min(y1, y2);
                int maxY = Math.max(y1, y2);
                return px >= minX && px <= maxX && py >= minY && py <= maxY;
            case CIRCLE:
                return Math.hypot(px - x1, py - y1) <= Math.abs(x2 - x1);
            case TEXT:
                return Math.abs(px - x1) <= 50 && Math.abs(py - y1) <= 20;
            default:
                return false;
        }
    }

    private double distanceToLine(int px, int py) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        if (dx == 0 && dy == 0) return Math.hypot(px - x1, py - y1);
        double t = ((px - x1) * dx + (py - y1) * dy) / (dx * dx + dy * dy);
        t = Math.max(0, Math.min(1, t));
        double closestX = x1 + t * dx;
        double closestY = y1 + t * dy;
        return Math.hypot(px - closestX, py - closestY);
    }

    private double distanceToSegment(int px, int py, int x1, int y1, int x2, int y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        if (dx == 0 && dy == 0) return Math.hypot(px - x1, py - y1);
        double t = ((px - x1) * dx + (py - y1) * dy) / (dx * dx + dy * dy);
        t = Math.max(0, Math.min(1, t));
        double closestX = x1 + t * dx;
        double closestY = y1 + t * dy;
        return Math.hypot(px - closestX, py - closestY);
    }

    public void draw(Graphics2D g, Color drawColor, int width) {
        g.setColor(drawColor);
        g.setStroke(new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        switch (type) {
            case POINT:
                g.fillOval(x1 - width/2, y1 - width/2, width, width);
                break;
            case LINE:
                g.drawLine(x1, y1, x2, y2);
                break;
            case RECTANGLE:
                int w = Math.abs(x2 - x1);
                int h = Math.abs(y2 - y1);
                int sx = Math.min(x1, x2);
                int sy = Math.min(y1, y2);
                g.drawRect(sx, sy, w, h);
                break;
            case CIRCLE:
                int r = Math.abs(x2 - x1);
                g.drawOval(x1 - r, y1 - r, 2*r, 2*r);
                break;
            case ELLIPSE:
                int w2 = Math.abs(x2 - x1);
                int h2 = Math.abs(y2 - y1);
                int sx2 = Math.min(x1, x2);
                int sy2 = Math.min(y1, y2);
                g.drawOval(sx2, sy2, w2, h2);
                break;
            case FREE_DRAW:
                if (freeDrawPoints != null && freeDrawPoints.size() > 1) {
                    for (int i = 1; i < freeDrawPoints.size(); i++) {
                        Point p1 = freeDrawPoints.get(i-1);
                        Point p2 = freeDrawPoints.get(i);
                        g.drawLine(p1.x, p1.y, p2.x, p2.y);
                    }
                }
                break;
            case TEXT:
                if (text != null && !text.isEmpty()) {
                    g.setFont(new Font("Arial", Font.PLAIN, width * 2));
                    g.drawString(text, x1, y1);
                }
                break;
            case POLYGON: {
                // Polygon: if polyX/polyY provided, draw; otherwise create regular polygon from center and sides
                if (polyX != null && polyY != null && polyX.length > 2) {
                    if (fillColor != null || useGradient) {
                        Paint prev = g.getPaint();
                        applyFillPaint(g, sx(), sy(), ex(), ey());
                        g.fillPolygon(polyX, polyY, polyX.length);
                        g.setPaint(prev);
                    }
                    g.drawPolygon(polyX, polyY, polyX.length);
                } else if (polySides > 2) {
                    int cx = (x1 + x2) / 2;
                    int cy = (y1 + y2) / 2;
                    int rx = Math.abs(x2 - x1) / 2;
                    int ry = Math.abs(y2 - y1) / 2;
                    int n = polySides;
                    int[] px = new int[n];
                    int[] py = new int[n];
                    for (int i = 0; i < n; i++) {
                        double angle = rotation + 2 * Math.PI * i / n;
                        px[i] = cx + (int) (rx * Math.cos(angle));
                        py[i] = cy + (int) (ry * Math.sin(angle));
                    }
                    if (fillColor != null || useGradient) {
                        Paint prev = g.getPaint();
                        applyFillPaint(g, sx(), sy(), ex(), ey());
                        g.fillPolygon(px, py, n);
                        g.setPaint(prev);
                    }
                    g.drawPolygon(px, py, n);
                }
                break;
            }
            case STAR: {
                // Star: 5-point star by default, alternate outer/inner radii
                int points = Math.max(5, polySides == 0 ? 5 : polySides);
                int cx = (x1 + x2) / 2;
                int cy = (y1 + y2) / 2;
                int outerR = Math.max(1, Math.abs(x2 - x1) / 2);
                int innerR = Math.max(1, outerR / 2);
                int n = points * 2;
                int[] px = new int[n];
                int[] py = new int[n];
                for (int i = 0; i < n; i++) {
                    double a = rotation + Math.PI * i / points;
                    int rad = (i % 2 == 0) ? outerR : innerR;
                    px[i] = cx + (int) (rad * Math.cos(a));
                    py[i] = cy + (int) (rad * Math.sin(a));
                }
                if (fillColor != null || useGradient) {
                    Paint prev = g.getPaint();
                    applyFillPaint(g, sx(), sy(), ex(), ey());
                    g.fillPolygon(px, py, n);
                    g.setPaint(prev);
                }
                g.drawPolygon(px, py, n);
                break;
            }
            case ARROW: {
                // Arrow: draw line then arrowhead at end
                g.drawLine(x1, y1, x2, y2);
                double dx = x2 - x1;
                double dy = y2 - y1;
                double ang = Math.atan2(dy, dx);
                int ah = Math.max(6, width * 3); // arrowhead size
                int ax1 = x2 - (int) (ah * Math.cos(ang - Math.PI / 6));
                int ay1 = y2 - (int) (ah * Math.sin(ang - Math.PI / 6));
                int ax2 = x2 - (int) (ah * Math.cos(ang + Math.PI / 6));
                int ay2 = y2 - (int) (ah * Math.sin(ang + Math.PI / 6));
                int[] ax = new int[]{x2, ax1, ax2};
                int[] ay = new int[]{y2, ay1, ay2};
                if (fillColor != null || useGradient) {
                    Paint prev = g.getPaint();
                    applyFillPaint(g, sx(), sy(), ex(), ey());
                    g.fillPolygon(ax, ay, 3);
                    g.setPaint(prev);
                }
                g.drawPolygon(ax, ay, 3);
                break;
            }
            case CURVE: {
                // Curve: draw smooth path through freeDrawPoints or use quadratic/cubic
                if (freeDrawPoints != null && freeDrawPoints.size() > 1) {
                    java.awt.geom.Path2D path = new java.awt.geom.Path2D.Double();
                    Point p0 = freeDrawPoints.get(0);
                    path.moveTo(p0.x, p0.y);
                    for (int i = 1; i < freeDrawPoints.size(); i++) {
                        Point p = freeDrawPoints.get(i);
                        path.lineTo(p.x, p.y);
                    }
                    if (fillColor != null || useGradient) {
                        Paint prev = g.getPaint();
                        applyFillPaint(g, sx(), sy(), ex(), ey());
                        g.fill(path);
                        g.setPaint(prev);
                    }
                    g.draw(path);
                } else {
                    g.drawLine(x1, y1, x2, y2);
                }
                break;
            }
            case FILL: {
                // Generic fill: fill bounding rectangle
                int fx = Math.min(x1, x2);
                int fy = Math.min(y1, y2);
                int fw = Math.abs(x2 - x1);
                int fh = Math.abs(y2 - y1);
                if (useGradient) {
                    Paint prev = g.getPaint();
                    applyFillPaint(g, fx, fy, fx + fw, fy + fh);
                    g.fillRect(fx, fy, fw, fh);
                    g.setPaint(prev);
                } else if (fillColor != null) {
                    Color prev = g.getColor();
                    g.setColor(fillColor);
                    g.fillRect(fx, fy, fw, fh);
                    g.setColor(prev);
                }
                break;
            }
        }
    }

    public void draw(Graphics2D g) {
        draw(g, color, strokeWidth);
    }

    @Override
    public Shape clone() {
        try {
            Shape clone = (Shape) super.clone();
            if (freeDrawPoints != null) {
                clone.freeDrawPoints = new ArrayList<>(freeDrawPoints);
            }
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    // Helper methods
    private int sx() { return Math.min(x1, x2); }
    private int sy() { return Math.min(y1, y2); }
    private int ex() { return Math.max(x1, x2); }
    private int ey() { return Math.max(y1, y2); }

    private void applyFillPaint(Graphics2D g, int x1, int y1, int x2, int y2) {
        if (useGradient && gradientTo != null) {
            GradientPaint gp = new GradientPaint(x1, y1, this.color == null ? Color.BLACK : this.color,
                    x2, y2, gradientTo);
            g.setPaint(gp);
        } else if (fillColor != null) {
            g.setColor(fillColor);
        }
    }

    @Override
    public String toString() {
        return String.format("Shape{type=%s, pos=(%d,%d)-(%d,%d), color=%s, width=%d, peer=%s}",
                type, x1, y1, x2, y2, color, strokeWidth, peerId);
    }
}
