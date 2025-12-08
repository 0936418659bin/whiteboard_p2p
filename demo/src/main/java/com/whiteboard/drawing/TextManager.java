package com.whiteboard.drawing;

import java.awt.*;

/**
 * Quản lý text tool và text styling
 * Hỗ trợ: font, size, bold, italic, underline, alignment
 */
public class TextManager {
    public enum FontStyle {
        REGULAR, BOLD, ITALIC, BOLD_ITALIC
    }

    public enum TextAlign {
        LEFT, CENTER, RIGHT
    }

    private String fontName = "Arial";
    private int fontSize = 16;
    private FontStyle fontStyle = FontStyle.REGULAR;
    private TextAlign textAlign = TextAlign.LEFT;
    private Color textColor = Color.BLACK;

    public static final String[] FONTS = {
            "Arial", "Times New Roman", "Courier New", "Georgia", 
            "Verdana", "Comic Sans MS", "Trebuchet MS", "Lucida Console"
    };

    public void setFont(String name) {
        this.fontName = name;
    }

    public void setFontSize(int size) {
        this.fontSize = Math.max(8, Math.min(72, size));
    }

    public void setFontStyle(FontStyle style) {
        this.fontStyle = style;
    }

    public void setTextAlign(TextAlign align) {
        this.textAlign = align;
    }

    public void setTextColor(Color color) {
        this.textColor = color;
    }

    public Font getFont() {
        int style = Font.PLAIN;
        if (fontStyle == FontStyle.BOLD) style = Font.BOLD;
        else if (fontStyle == FontStyle.ITALIC) style = Font.ITALIC;
        else if (fontStyle == FontStyle.BOLD_ITALIC) style = Font.BOLD | Font.ITALIC;

        return new Font(fontName, style, fontSize);
    }

    public String getFontName() {
        return fontName;
    }

    public int getFontSize() {
        return fontSize;
    }

    public FontStyle getFontStyle() {
        return fontStyle;
    }

    public TextAlign getTextAlign() {
        return textAlign;
    }

    public Color getTextColor() {
        return textColor;
    }

    public void drawText(Graphics2D g, String text, int x, int y) {
        Font font = getFont();
        g.setFont(font);
        g.setColor(textColor);

        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        int textHeight = fm.getAscent();

        int drawX = x;
        if (textAlign == TextAlign.CENTER) {
            drawX = x - textWidth / 2;
        } else if (textAlign == TextAlign.RIGHT) {
            drawX = x - textWidth;
        }

        g.drawString(text, drawX, y + textHeight);
    }

    public int getTextWidth(String text) {
        Font font = getFont();
        Canvas c = new Canvas();
        FontMetrics fm = c.getFontMetrics(font);
        return fm.stringWidth(text);
    }

    public int getTextHeight() {
        Font font = getFont();
        Canvas c = new Canvas();
        FontMetrics fm = c.getFontMetrics(font);
        return fm.getHeight();
    }
}

class Canvas extends javax.swing.JPanel {
    // Helper class for font metrics calculation
}
