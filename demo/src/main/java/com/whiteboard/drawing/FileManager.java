package com.whiteboard.drawing;

import java.io.*;
import java.util.List;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

/**
 * Quản lý lưu/tải dự án
 * Hỗ trợ: PNG, SVG, JSON format
 */
public class FileManager {
    private static final String JSON_FORMAT = "json";
    private static final String PNG_FORMAT = "png";
    private static final String SVG_FORMAT = "svg";

    public static class ProjectData implements Serializable {
        private static final long serialVersionUID = 1L;
        
        public String projectName;
        public String projectId;
        public long createdTime;
        public long modifiedTime;
        public String description;
        public List<Shape> shapes;
        public List<LayerManager.Layer> layers;
        public int canvasWidth;
        public int canvasHeight;

        public ProjectData(String projectName, int canvasWidth, int canvasHeight) {
            this.projectName = projectName;
            this.projectId = java.util.UUID.randomUUID().toString();
            this.createdTime = System.currentTimeMillis();
            this.modifiedTime = System.currentTimeMillis();
            this.canvasWidth = canvasWidth;
            this.canvasHeight = canvasHeight;
        }
    }

    /**
     * Lưu dự án thành file JSON
     */
    public static boolean saveProjectAsJSON(String filePath, ProjectData projectData) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath))) {
            oos.writeObject(projectData);
            projectData.modifiedTime = System.currentTimeMillis();
            return true;
        } catch (IOException e) {
            System.err.println("Error saving JSON: " + e.getMessage());
            return false;
        }
    }

    /**
     * Tải dự án từ file JSON
     */
    public static ProjectData loadProjectFromJSON(String filePath) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filePath))) {
            return (ProjectData) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error loading JSON: " + e.getMessage());
            return null;
        }
    }

    /**
     * Xuất canvas thành PNG
     */
    public static boolean exportAsPNG(String filePath, BufferedImage image) {
        try {
            return ImageIO.write(image, "PNG", new File(filePath));
        } catch (IOException e) {
            System.err.println("Error exporting PNG: " + e.getMessage());
            return false;
        }
    }

    /**
     * Xuất canvas thành SVG
     */
    public static boolean exportAsSVG(String filePath, List<Shape> shapes, int width, int height) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            writer.println("<svg xmlns=\"http://www.w3.org/2000/svg\" " +
                          "width=\"" + width + "\" height=\"" + height + "\">");
            writer.println("<rect width=\"" + width + "\" height=\"" + height + "\" fill=\"white\"/>");

            for (Shape shape : shapes) {
                String svgElement = shapeToSVG(shape);
                if (svgElement != null) {
                    writer.println(svgElement);
                }
            }

            writer.println("</svg>");
            return true;
        } catch (IOException e) {
            System.err.println("Error exporting SVG: " + e.getMessage());
            return false;
        }
    }

    private static String shapeToSVG(Shape shape) {
        if (shape == null) return null;

        String colorHex = String.format("#%06X", shape.color.getRGB() & 0xFFFFFF);

        switch (shape.type) {
            case POINT:
                return String.format("<circle cx=\"%d\" cy=\"%d\" r=\"%d\" fill=\"%s\"/>",
                        shape.x1, shape.y1, shape.strokeWidth / 2, colorHex);

            case LINE:
                return String.format("<line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" " +
                        "stroke=\"%s\" stroke-width=\"%d\"/>",
                        shape.x1, shape.y1, shape.x2, shape.y2, colorHex, shape.strokeWidth);

            case RECTANGLE:
                int w = Math.abs(shape.x2 - shape.x1);
                int h = Math.abs(shape.y2 - shape.y1);
                int x = Math.min(shape.x1, shape.x2);
                int y = Math.min(shape.y1, shape.y2);
                return String.format("<rect x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\" " +
                        "stroke=\"%s\" stroke-width=\"%d\" fill=\"none\"/>",
                        x, y, w, h, colorHex, shape.strokeWidth);

            case CIRCLE:
                int diameter = Math.abs(shape.x2 - shape.x1);
                int radius = diameter / 2;
                return String.format("<circle cx=\"%d\" cy=\"%d\" r=\"%d\" " +
                        "stroke=\"%s\" stroke-width=\"%d\" fill=\"none\"/>",
                        shape.x1, shape.y1, radius, colorHex, shape.strokeWidth);

            case TEXT:
                return String.format("<text x=\"%d\" y=\"%d\" font-size=\"%d\" fill=\"%s\">%s</text>",
                        shape.x1, shape.y1, shape.strokeWidth * 2, colorHex, shape.text);

            default:
                return null;
        }
    }

    public static File[] getRecentFiles(String directory, int limit) {
        File dir = new File(directory);
        if (!dir.exists()) {
            return new File[0];
        }

        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
        if (files == null) return new File[0];

        // Sắp xếp theo thời gian sửa đổi (mới nhất trước)
        java.util.Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));

        return java.util.Arrays.copyOf(files, Math.min(limit, files.length));
    }
}
