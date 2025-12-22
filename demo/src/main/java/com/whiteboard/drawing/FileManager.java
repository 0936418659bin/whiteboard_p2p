package com.whiteboard.drawing;

import java.io.*;
import java.util.List;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.Color;

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
     * Lưu dự án thành file JSON (text format thật)
     */
    public static boolean saveProjectAsJSON(String filePath, ProjectData projectData) {
        System.out.println("[FileManager] Saving project to: " + filePath);
        System.out.println("[FileManager] Project data - shapes: " + 
                (projectData.shapes != null ? projectData.shapes.size() : 0) + 
                ", layers: " + (projectData.layers != null ? projectData.layers.size() : 0));
        
        // Đảm bảo file path có extension .json
        if (!filePath.toLowerCase().endsWith(".json")) {
            filePath = filePath + ".json";
            System.out.println("[FileManager] Added .json extension, new path: " + filePath);
        }
        
        File file = new File(filePath);
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
            System.out.println("[FileManager] Created directory: " + parentDir.getAbsolutePath());
        }
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            projectData.modifiedTime = System.currentTimeMillis();
            
            writer.println("{");
            writer.println("  \"projectName\": \"" + escapeJson(projectData.projectName) + "\",");
            writer.println("  \"projectId\": \"" + projectData.projectId + "\",");
            writer.println("  \"createdTime\": " + projectData.createdTime + ",");
            writer.println("  \"modifiedTime\": " + projectData.modifiedTime + ",");
            writer.println("  \"canvasWidth\": " + projectData.canvasWidth + ",");
            writer.println("  \"canvasHeight\": " + projectData.canvasHeight + ",");
            
            // Save shapes as JSON array
            writer.println("  \"shapes\": [");
            if (projectData.shapes != null && !projectData.shapes.isEmpty()) {
                System.out.println("[FileManager] Writing " + projectData.shapes.size() + " shapes...");
                for (int i = 0; i < projectData.shapes.size(); i++) {
                    Shape shape = projectData.shapes.get(i);
                    writer.print("    " + shapeToJson(shape));
                    if (i < projectData.shapes.size() - 1) {
                        writer.println(",");
                    } else {
                        writer.println();
                    }
                }
            } else {
                System.out.println("[FileManager] Warning: No shapes to save!");
            }
            writer.println("  ]");
            writer.println("}");
            
            writer.flush();
            System.out.println("[FileManager] File saved successfully. File exists: " + file.exists() + 
                    ", size: " + file.length() + " bytes");
            return true;
        } catch (IOException e) {
            System.err.println("[FileManager] Error saving JSON: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Tải dự án từ file JSON (text format hoặc binary format)
     */
    public static ProjectData loadProjectFromJSON(String filePath) {
        System.out.println("[FileManager] Loading project from: " + filePath);
        File file = new File(filePath);
        
        if (!file.exists()) {
            System.err.println("[FileManager] File does not exist: " + filePath);
            return null;
        }
        
        System.out.println("[FileManager] File exists, size: " + file.length() + " bytes");
        
        // Thử load binary format trước (backward compatibility)
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filePath))) {
            Object obj = ois.readObject();
            if (obj instanceof ProjectData) {
                ProjectData data = (ProjectData) obj;
                System.out.println("[FileManager] Loaded binary format project");
                System.out.println("[FileManager] Shapes count: " + 
                        (data.shapes != null ? data.shapes.size() : 0));
                return data;
            }
        } catch (Exception e) {
            // Không phải binary format, thử parse JSON text
            System.out.println("[FileManager] Not binary format, parsing JSON text...");
            System.out.println("[FileManager] Binary parse error: " + e.getMessage());
        }
        
        // Parse JSON text format
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            StringBuilder json = new StringBuilder();
            String line;
            int lineCount = 0;
            while ((line = reader.readLine()) != null) {
                json.append(line).append("\n");
                lineCount++;
            }
            System.out.println("[FileManager] Read " + lineCount + " lines from JSON file");
            System.out.println("[FileManager] JSON length: " + json.length() + " characters");
            
            ProjectData data = parseJSON(json.toString());
            if (data != null) {
                System.out.println("[FileManager] Parsed JSON successfully");
                System.out.println("[FileManager] Shapes count: " + 
                        (data.shapes != null ? data.shapes.size() : 0));
            } else {
                System.err.println("[FileManager] Failed to parse JSON");
            }
            return data;
        } catch (IOException e) {
            System.err.println("[FileManager] Error reading JSON file: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Parse JSON string thành ProjectData
     */
    private static ProjectData parseJSON(String json) {
        try {
            ProjectData data = new ProjectData("Loaded Project", 3000, 2000);
            data.shapes = new java.util.ArrayList<>();
            
            // Parse đơn giản bằng regex và string manipulation
            // Extract projectName
            java.util.regex.Pattern namePattern = java.util.regex.Pattern.compile("\"projectName\"\\s*:\\s*\"([^\"]+)\"");
            java.util.regex.Matcher nameMatcher = namePattern.matcher(json);
            if (nameMatcher.find()) {
                data.projectName = unescapeJson(nameMatcher.group(1));
            }
            
            // Extract canvasWidth và canvasHeight
            java.util.regex.Pattern widthPattern = java.util.regex.Pattern.compile("\"canvasWidth\"\\s*:\\s*(\\d+)");
            java.util.regex.Matcher widthMatcher = widthPattern.matcher(json);
            if (widthMatcher.find()) {
                data.canvasWidth = Integer.parseInt(widthMatcher.group(1));
            }
            
            java.util.regex.Pattern heightPattern = java.util.regex.Pattern.compile("\"canvasHeight\"\\s*:\\s*(\\d+)");
            java.util.regex.Matcher heightMatcher = heightPattern.matcher(json);
            if (heightMatcher.find()) {
                data.canvasHeight = Integer.parseInt(heightMatcher.group(1));
            }
            
            // Extract shapes array - tìm flexible hơn để handle whitespace
            int shapesStart = -1;
            // Thử nhiều pattern khác nhau
            String[] patterns = {"\"shapes\":[", "\"shapes\" : [", "\"shapes\": [", "\"shapes\" :["};
            for (String pattern : patterns) {
                shapesStart = json.indexOf(pattern);
                if (shapesStart != -1) {
                    System.out.println("[FileManager] Found shapes array with pattern: " + pattern);
                    break;
                }
            }
            
            if (shapesStart == -1) {
                // Thử tìm bằng regex
                java.util.regex.Pattern shapesPattern = java.util.regex.Pattern.compile("\"shapes\"\\s*:\\s*\\[");
                java.util.regex.Matcher shapesMatcher = shapesPattern.matcher(json);
                if (shapesMatcher.find()) {
                    shapesStart = shapesMatcher.start();
                    System.out.println("[FileManager] Found shapes array using regex");
                }
            }
            
            if (shapesStart == -1) {
                System.err.println("[FileManager] No shapes array found in JSON");
                System.err.println("[FileManager] JSON preview: " + json.substring(0, Math.min(200, json.length())));
                return data;
            }
            
            int bracketCount = 0;
            int arrayStart = json.indexOf('[', shapesStart);
            if (arrayStart == -1) {
                System.err.println("[FileManager] No opening bracket found after shapes");
                return data;
            }
            int arrayEnd = arrayStart;
            
            // Tìm vị trí kết thúc của shapes array
            for (int i = arrayStart; i < json.length(); i++) {
                char c = json.charAt(i);
                if (c == '[') bracketCount++;
                if (c == ']') {
                    bracketCount--;
                    if (bracketCount == 0) {
                        arrayEnd = i;
                        break;
                    }
                }
            }
            
            String shapesJson = json.substring(arrayStart + 1, arrayEnd);
            System.out.println("[FileManager] Shapes JSON substring length: " + shapesJson.length());
            
            // Parse từng shape object bằng cách tìm matching braces
            int pos = 0;
            while (pos < shapesJson.length()) {
                // Skip whitespace and commas
                while (pos < shapesJson.length() && 
                       (Character.isWhitespace(shapesJson.charAt(pos)) || shapesJson.charAt(pos) == ',')) {
                    pos++;
                }
                if (pos >= shapesJson.length()) break;
                
                // Tìm bắt đầu của shape object
                if (shapesJson.charAt(pos) != '{') {
                    pos++;
                    continue;
                }
                
                // Tìm kết thúc của shape object bằng cách đếm braces
                int braceCount = 0;
                int startPos = pos;
                int endPos = pos;
                
                for (int i = pos; i < shapesJson.length(); i++) {
                    char c = shapesJson.charAt(i);
                    if (c == '{') braceCount++;
                    if (c == '}') {
                        braceCount--;
                        if (braceCount == 0) {
                            endPos = i + 1;
                            break;
                        }
                    }
                }
                
                if (endPos > startPos) {
                    String shapeJson = shapesJson.substring(startPos, endPos);
                    System.out.println("[FileManager] Parsing shape " + (data.shapes.size() + 1) + 
                            ": " + shapeJson.substring(0, Math.min(50, shapeJson.length())) + "...");
                    Shape shape = parseShape(shapeJson);
                    if (shape != null) {
                        data.shapes.add(shape);
                        System.out.println("[FileManager] Successfully parsed shape type: " + shape.type + 
                                " (total: " + data.shapes.size() + ")");
                    } else {
                        System.err.println("[FileManager] Failed to parse shape: " + shapeJson.substring(0, Math.min(100, shapeJson.length())));
                    }
                    pos = endPos;
                } else {
                    System.err.println("[FileManager] No closing brace found for shape starting at position " + startPos);
                    break; // Không tìm thấy closing brace
                }
            }
            
            System.out.println("[FileManager] Parsed " + data.shapes.size() + " shapes from JSON");
            return data;
            
        } catch (Exception e) {
            System.err.println("Error parsing JSON: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Parse một shape object từ JSON string
     */
    private static Shape parseShape(String shapeJson) {
        try {
            System.out.println("[FileManager] parseShape called with: " + shapeJson.substring(0, Math.min(100, shapeJson.length())));
            
            // Extract type
            java.util.regex.Pattern typePattern = java.util.regex.Pattern.compile("\"type\"\\s*:\\s*\"([^\"]+)\"");
            java.util.regex.Matcher typeMatcher = typePattern.matcher(shapeJson);
            if (!typeMatcher.find()) {
                System.err.println("[FileManager] No type field found in shape JSON");
                return null;
            }
            
            String typeStr = typeMatcher.group(1);
            System.out.println("[FileManager] Found shape type: " + typeStr);
            Shape.ShapeType type;
            try {
                type = Shape.ShapeType.valueOf(typeStr);
            } catch (IllegalArgumentException e) {
                System.err.println("[FileManager] Unknown shape type: " + typeStr);
                return null;
            }
            
            // Extract coordinates
            int x1 = extractInt(shapeJson, "x1");
            int y1 = extractInt(shapeJson, "y1");
            int x2 = extractInt(shapeJson, "x2");
            int y2 = extractInt(shapeJson, "y2");
            System.out.println("[FileManager] Coordinates: (" + x1 + "," + y1 + ") to (" + x2 + "," + y2 + ")");
            
            // Extract color - handle negative RGB values (like -16777216 for black)
            int colorRGB = extractInt(shapeJson, "color");
            Color color = new Color(colorRGB, true); // Support alpha channel
            System.out.println("[FileManager] Color RGB: " + colorRGB);
            
            // Extract strokeWidth
            int strokeWidth = extractInt(shapeJson, "strokeWidth");
            if (strokeWidth == 0) strokeWidth = 2; // Default
            System.out.println("[FileManager] Stroke width: " + strokeWidth);
            
            // Extract peerId
            String peerId = extractString(shapeJson, "peerId");
            System.out.println("[FileManager] Peer ID: " + peerId);
            
            // Create shape
            Shape shape = new Shape(type, x1, y1, x2, y2, color, strokeWidth, peerId != null ? peerId : "");
            
            // Extract timestamp
            long timestamp = extractLong(shapeJson, "timestamp");
            if (timestamp > 0) {
                shape.timestamp = timestamp;
            }
            
            // Extract text if present
            String text = extractString(shapeJson, "text");
            if (text != null && !text.isEmpty()) {
                shape.text = text;
            }
            
            // For FREE_DRAW, create freeDrawPoints from x1,y1 to x2,y2
            if (type == Shape.ShapeType.FREE_DRAW) {
                shape.freeDrawPoints = new java.util.ArrayList<>();
                shape.freeDrawPoints.add(new java.awt.Point(x1, y1));
                shape.freeDrawPoints.add(new java.awt.Point(x2, y2));
            }
            
            System.out.println("[FileManager] Successfully created shape: " + type);
            return shape;
            
        } catch (Exception e) {
            System.err.println("[FileManager] Error parsing shape: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    private static int extractInt(String json, String key) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\"" + key + "\"\\s*:\\s*(-?\\d+)");
        java.util.regex.Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return 0;
    }
    
    private static long extractLong(String json, String key) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\"" + key + "\"\\s*:\\s*(-?\\d+)");
        java.util.regex.Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return Long.parseLong(matcher.group(1));
        }
        return 0;
    }
    
    private static String extractString(String json, String key) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]*)\"");
        java.util.regex.Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return unescapeJson(matcher.group(1));
        }
        return null;
    }
    
    private static String unescapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\\"", "\"")
                  .replace("\\\\", "\\")
                  .replace("\\n", "\n")
                  .replace("\\r", "\r")
                  .replace("\\t", "\t");
    }
    
    private static String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
    
    private static String shapeToJson(Shape shape) {
        if (shape == null) return "null";
        
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"type\":\"").append(shape.type).append("\",");
        sb.append("\"x1\":").append(shape.x1).append(",");
        sb.append("\"y1\":").append(shape.y1).append(",");
        sb.append("\"x2\":").append(shape.x2).append(",");
        sb.append("\"y2\":").append(shape.y2).append(",");
        sb.append("\"color\":").append(shape.color != null ? shape.color.getRGB() : 0).append(",");
        sb.append("\"strokeWidth\":").append(shape.strokeWidth).append(",");
        sb.append("\"timestamp\":").append(shape.timestamp).append(",");
        sb.append("\"peerId\":\"").append(escapeJson(shape.peerId != null ? shape.peerId : "")).append("\"");
        if (shape.text != null) {
            sb.append(",\"text\":\"").append(escapeJson(shape.text)).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * Xuất canvas thành PNG
     */
    public static boolean exportAsPNG(String filePath, BufferedImage image) {
        System.out.println("[FileManager] Exporting PNG to: " + filePath);
        
        if (image == null) {
            System.err.println("[FileManager] Error: Image is null, cannot export PNG");
            return false;
        }
        
        System.out.println("[FileManager] Image size: " + image.getWidth() + "x" + image.getHeight());
        
        // Đảm bảo file path có extension .png
        if (!filePath.toLowerCase().endsWith(".png")) {
            filePath = filePath + ".png";
            System.out.println("[FileManager] Added .png extension, new path: " + filePath);
        }
        
        File file = new File(filePath);
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
            System.out.println("[FileManager] Created directory: " + parentDir.getAbsolutePath());
        }
        
        try {
            boolean success = ImageIO.write(image, "PNG", file);
            if (success) {
                System.out.println("[FileManager] PNG exported successfully. File exists: " + 
                        file.exists() + ", size: " + file.length() + " bytes");
            } else {
                System.err.println("[FileManager] ImageIO.write returned false - no writer found");
            }
            return success;
        } catch (IOException e) {
            System.err.println("[FileManager] Error exporting PNG: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Xuất canvas thành SVG với đầy đủ shape types
     */
    public static boolean exportAsSVG(String filePath, List<Shape> shapes, int width, int height) {
        System.out.println("[FileManager] Exporting SVG to: " + filePath);
        System.out.println("[FileManager] Shapes count: " + (shapes != null ? shapes.size() : 0));
        System.out.println("[FileManager] Canvas size: " + width + "x" + height);
        
        // Đảm bảo file path có extension .svg
        if (!filePath.toLowerCase().endsWith(".svg")) {
            filePath = filePath + ".svg";
            System.out.println("[FileManager] Added .svg extension, new path: " + filePath);
        }
        
        File file = new File(filePath);
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
            System.out.println("[FileManager] Created directory: " + parentDir.getAbsolutePath());
        }
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            writer.println("<svg xmlns=\"http://www.w3.org/2000/svg\" " +
                          "width=\"" + width + "\" height=\"" + height + "\">");
            writer.println("<rect width=\"" + width + "\" height=\"" + height + "\" fill=\"white\"/>");

            int exportedCount = 0;
            if (shapes != null) {
                for (Shape shape : shapes) {
                    if (shape == null) continue;
                    String svgElement = shapeToSVG(shape);
                    if (svgElement != null && !svgElement.isEmpty()) {
                        writer.println(svgElement);
                        exportedCount++;
                    }
                }
            }
            
            writer.println("</svg>");
            writer.flush();
            
            System.out.println("[FileManager] SVG exported successfully. Exported " + exportedCount + 
                    " shapes. File exists: " + file.exists() + ", size: " + file.length() + " bytes");
            return true;
        } catch (IOException e) {
            System.err.println("[FileManager] Error exporting SVG: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private static String shapeToSVG(Shape shape) {
        if (shape == null || shape.color == null) return null;

        String colorHex = String.format("#%06X", shape.color.getRGB() & 0xFFFFFF);
        String fillColor = shape.fillColor != null 
            ? String.format("#%06X", shape.fillColor.getRGB() & 0xFFFFFF) 
            : "none";
        String fillAttr = shape.fillColor != null ? "fill=\"" + fillColor + "\"" : "fill=\"none\"";

        switch (shape.type) {
            case POINT:
                return String.format("<circle cx=\"%d\" cy=\"%d\" r=\"%d\" fill=\"%s\"/>",
                        shape.x1, shape.y1, Math.max(shape.strokeWidth / 2, 1), colorHex);

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
                        "stroke=\"%s\" stroke-width=\"%d\" %s/>",
                        x, y, w, h, colorHex, shape.strokeWidth, fillAttr);

            case CIRCLE:
                int diameter = Math.abs(shape.x2 - shape.x1);
                int radius = Math.max(diameter / 2, 1);
                int cx = shape.x1;
                int cy = shape.y1;
                return String.format("<circle cx=\"%d\" cy=\"%d\" r=\"%d\" " +
                        "stroke=\"%s\" stroke-width=\"%d\" %s/>",
                        cx, cy, radius, colorHex, shape.strokeWidth, fillAttr);

            case ELLIPSE:
                int rx = Math.abs(shape.x2 - shape.x1) / 2;
                int ry = Math.abs(shape.y2 - shape.y1) / 2;
                int ex = (shape.x1 + shape.x2) / 2;
                int ey = (shape.y1 + shape.y2) / 2;
                return String.format("<ellipse cx=\"%d\" cy=\"%d\" rx=\"%d\" ry=\"%d\" " +
                        "stroke=\"%s\" stroke-width=\"%d\" %s/>",
                        ex, ey, rx, ry, colorHex, shape.strokeWidth, fillAttr);

            case FREE_DRAW:
                if (shape.freeDrawPoints == null || shape.freeDrawPoints.isEmpty()) {
                    return null;
                }
                StringBuilder path = new StringBuilder();
                path.append("<path d=\"M ");
                for (int i = 0; i < shape.freeDrawPoints.size(); i++) {
                    java.awt.Point p = shape.freeDrawPoints.get(i);
                    path.append(p.x).append(" ").append(p.y);
                    if (i < shape.freeDrawPoints.size() - 1) {
                        path.append(" L ");
                    }
                }
                path.append("\" stroke=\"").append(colorHex)
                    .append("\" stroke-width=\"").append(shape.strokeWidth)
                    .append("\" fill=\"none\" stroke-linecap=\"round\" stroke-linejoin=\"round\"/>");
                return path.toString();

            case TEXT:
                if (shape.text == null || shape.text.isEmpty()) return null;
                int fontSize = shape.fontSize > 0 ? shape.fontSize : shape.strokeWidth * 2;
                String fontFamily = shape.fontName != null ? shape.fontName : "Arial";
                String textAlign = "left";
                if (shape.textAlign == 1) textAlign = "middle";
                else if (shape.textAlign == 2) textAlign = "end";
                return String.format("<text x=\"%d\" y=\"%d\" font-size=\"%d\" font-family=\"%s\" " +
                        "text-anchor=\"%s\" fill=\"%s\">%s</text>",
                        shape.x1, shape.y1, fontSize, fontFamily, textAlign, colorHex, 
                        escapeXml(shape.text));

            case POLYGON:
                if (shape.polyX == null || shape.polyY == null) return null;
                StringBuilder polyPoints = new StringBuilder();
                for (int i = 0; i < shape.polyX.length; i++) {
                    if (i > 0) polyPoints.append(" ");
                    polyPoints.append(shape.polyX[i]).append(",").append(shape.polyY[i]);
                }
                return String.format("<polygon points=\"%s\" stroke=\"%s\" stroke-width=\"%d\" %s/>",
                        polyPoints.toString(), colorHex, shape.strokeWidth, fillAttr);

            case STAR:
            case ARROW:
            case CURVE:
                // Fallback to polygon hoặc path đơn giản
                return String.format("<line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" " +
                        "stroke=\"%s\" stroke-width=\"%d\"/>",
                        shape.x1, shape.y1, shape.x2, shape.y2, colorHex, shape.strokeWidth);

            default:
                return null;
        }
    }
    
    private static String escapeXml(String str) {
        if (str == null) return "";
        return str.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&apos;");
    }

    /**
     * Lấy danh sách recent files từ thư mục.
     * Tự động tạo thư mục nếu chưa tồn tại.
     */
    public static File[] getRecentFiles(String directory, int limit) {
        File dir = new File(directory);
        if (!dir.exists()) {
            dir.mkdirs(); // Tạo thư mục nếu chưa có
        }

        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
        if (files == null || files.length == 0) {
            return new File[0];
        }

        // Sắp xếp theo thời gian sửa đổi (mới nhất trước)
        java.util.Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));

        return java.util.Arrays.copyOf(files, Math.min(limit, files.length));
    }
    
    /**
     * Import ảnh từ file vào BufferedImage
     */
    public static BufferedImage importImage(String filePath) {
        try {
            BufferedImage image = ImageIO.read(new File(filePath));
            if (image == null) {
                System.err.println("Failed to read image: " + filePath);
                return null;
            }
            return image;
        } catch (IOException e) {
            System.err.println("Error importing image: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
