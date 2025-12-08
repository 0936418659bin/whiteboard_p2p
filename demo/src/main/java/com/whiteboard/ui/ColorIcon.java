package com.whiteboard.ui;

import javax.swing.*;
import java.awt.*;

/**
 * Simple square color icon used to display peer colors in lists.
 */
public class ColorIcon implements Icon {
    private final int size;
    private final Color color;

    public ColorIcon(Color color) {
        this(color, 12);
    }

    public ColorIcon(Color color, int size) {
        this.color = color;
        this.size = size;
    }

    @Override
    public int getIconWidth() {
        return size;
    }

    @Override
    public int getIconHeight() {
        return size;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(color);
        g2.fillRoundRect(x, y, size, size, 4, 4);

        g2.setColor(new Color(0, 0, 0, 60));
        g2.drawRoundRect(x, y, size, size, 4, 4);

        g2.dispose();
    }
}
