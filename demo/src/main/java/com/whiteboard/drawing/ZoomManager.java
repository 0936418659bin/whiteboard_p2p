package com.whiteboard.drawing;

/**
 * Quản lý zoom và pan (cuộn) trên canvas
 * Hỗ trợ: phóng to, thu nhỏ, fit to window, fit to selection
 */
public class ZoomManager {
    private float zoomLevel = 1.0f;
    private int panX = 0;
    private int panY = 0;
    
    private static final float MIN_ZOOM = 0.1f;
    private static final float MAX_ZOOM = 5.0f;
    private static final float ZOOM_STEP = 0.1f;

    public void zoomIn() {
        setZoom(zoomLevel + ZOOM_STEP);
    }

    public void zoomOut() {
        setZoom(zoomLevel - ZOOM_STEP);
    }

    public void setZoom(float level) {
        zoomLevel = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, level));
    }

    public void zoomToFit(int canvasWidth, int canvasHeight, int contentWidth, int contentHeight) {
        if (contentWidth == 0 || contentHeight == 0) {
            zoomLevel = 1.0f;
            return;
        }

        float scaleX = (float) canvasWidth / contentWidth;
        float scaleY = (float) canvasHeight / contentHeight;
        zoomLevel = Math.min(scaleX, scaleY) * 0.9f; // 90% để có margin
    }

    public void zoomToSelection(int selX, int selY, int selWidth, int selHeight,
                               int canvasWidth, int canvasHeight) {
        if (selWidth == 0 || selHeight == 0) return;

        float scaleX = (float) canvasWidth / selWidth;
        float scaleY = (float) canvasHeight / selHeight;
        zoomLevel = Math.min(scaleX, scaleY) * 0.8f;

        // Center selection
        panX = (int) (canvasWidth / 2 - (selX + selWidth / 2) * zoomLevel);
        panY = (int) (canvasHeight / 2 - (selY + selHeight / 2) * zoomLevel);
    }

    public void pan(int deltaX, int deltaY) {
        panX += deltaX;
        panY += deltaY;
    }

    public void resetZoom() {
        zoomLevel = 1.0f;
        panX = 0;
        panY = 0;
    }

    public float getZoom() {
        return zoomLevel;
    }

    public int getPanX() {
        return panX;
    }

    public int getPanY() {
        return panY;
    }

    public void setPan(int x, int y) {
        panX = x;
        panY = y;
    }

    public String getZoomPercentage() {
        return String.format("%.0f%%", zoomLevel * 100);
    }

    public int screenToCanvasX(int screenX) {
        return (int) ((screenX - panX) / zoomLevel);
    }

    public int screenToCanvasY(int screenY) {
        return (int) ((screenY - panY) / zoomLevel);
    }

    public int canvasToScreenX(int canvasX) {
        return (int) (canvasX * zoomLevel + panX);
    }

    public int canvasToScreenY(int canvasY) {
        return (int) (canvasY * zoomLevel + panY);
    }
}
