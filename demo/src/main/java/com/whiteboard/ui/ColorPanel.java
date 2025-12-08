package com.whiteboard.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;

/**
 * Enhanced Color Panel với color history, presets, gradient
 */
public class ColorPanel extends JPanel {
    private JButton colorButton;
    public JSlider widthSlider;
    private JLabel widthLabel;
    private ActionListener colorChangeListener;
    private List<Color> colorHistory;
    private JPanel colorHistoryPanel;
    private int maxHistorySize = 8;
    // Fill / gradient controls
    private JCheckBox fillCheckBox;
    private JCheckBox gradientCheckBox;
    private JButton gradientToButton;

    // Preset colors
    private static final Color[] PRESETS = {
        Color.BLACK, Color.WHITE, Color.RED, Color.GREEN,
        Color.BLUE, Color.YELLOW, Color.CYAN, Color.MAGENTA,
        new Color(255, 165, 0), new Color(128, 0, 128),
        new Color(165, 42, 42), new Color(0, 128, 128)
    };

    public ColorPanel() {
        colorHistory = new ArrayList<>();
        setLayout(new BorderLayout(5, 5));
        // Border phẳng, gọn hơn cho FlatLaf
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // Top panel: Main color and size
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

        // Color button
        colorButton = new JButton("  ");
        colorButton.setBackground(Color.BLACK);
        colorButton.setOpaque(true);
        colorButton.setPreferredSize(new Dimension(50, 30));
        colorButton.addActionListener(e -> showColorChooser());

        // Width slider (gọn, không vẽ tick/label để UI sạch hơn)
        widthSlider = new JSlider(1, 50, 2);
        widthSlider.setPreferredSize(new Dimension(140, 30));

        widthLabel = new JLabel("2px");

        widthSlider.addChangeListener(e -> {
            widthLabel.setText(widthSlider.getValue() + "px");
        });

        topPanel.add(new JLabel("Color"));
        topPanel.add(colorButton);
        
        // Fill and gradient controls
        fillCheckBox = new JCheckBox("Fill");
        gradientCheckBox = new JCheckBox("Gradient");
        gradientToButton = new JButton("→");
        gradientToButton.setPreferredSize(new Dimension(30, 30));
        gradientToButton.addActionListener(e -> {
            Color c = JColorChooser.showDialog(this, "Gradient To", gradientToButton.getBackground());
            if (c != null) {
                gradientToButton.setBackground(c);
                if (colorChangeListener != null) colorChangeListener.actionPerformed(null);
            }
        });
        
        topPanel.add(fillCheckBox);
        topPanel.add(gradientCheckBox);
        topPanel.add(gradientToButton);

        topPanel.add(new JLabel("Width:"));
        topPanel.add(widthSlider);
        topPanel.add(widthLabel);

        add(topPanel, BorderLayout.NORTH);

        // Middle panel: Presets
        JPanel presetsPanel = createPresetsPanel();
        add(presetsPanel, BorderLayout.CENTER);

        // Bottom panel: History
        colorHistoryPanel = createHistoryPanel();
        add(colorHistoryPanel, BorderLayout.SOUTH);
    }

    private JPanel createPresetsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(3, 4, 5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Presets"));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        for (Color color : PRESETS) {
            JButton btn = new JButton("  ");
            btn.setBackground(color);
            btn.setOpaque(true);
            btn.setPreferredSize(new Dimension(40, 30));
            btn.addActionListener(e -> selectColor(color));
            panel.add(btn);
        }

        return panel;
    }

    private JPanel createHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("History"));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        colorHistoryPanel = new JPanel();
        colorHistoryPanel.setLayout(new GridLayout(1, maxHistorySize, 3, 0));

        JScrollPane scroll = new JScrollPane(colorHistoryPanel);
        scroll.setPreferredSize(new Dimension(0, 40));
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    private void showColorChooser() {
        Color selectedColor = JColorChooser.showDialog(
                this, "Choose Color", colorButton.getBackground());
        if (selectedColor != null) {
            selectColor(selectedColor);
        }
    }

    private void selectColor(Color color) {
        colorButton.setBackground(color);
        addToHistory(color);
        
        if (colorChangeListener != null) {
            colorChangeListener.actionPerformed(null);
        }
    }

    public boolean isFillEnabled() {
        return fillCheckBox != null && fillCheckBox.isSelected();
    }

    public boolean isGradientEnabled() {
        return gradientCheckBox != null && gradientCheckBox.isSelected();
    }

    public Color getGradientTo() {
        return gradientToButton != null ? gradientToButton.getBackground() : Color.WHITE;
    }

    private void addToHistory(Color color) {
        // Remove if already exists
        colorHistory.remove(color);
        
        // Add to front
        colorHistory.add(0, color);
        
        // Keep size limit
        if (colorHistory.size() > maxHistorySize) {
            colorHistory.remove(colorHistory.size() - 1);
        }
        
        updateHistoryDisplay();
    }

    private void updateHistoryDisplay() {
        colorHistoryPanel.removeAll();
        
        for (Color color : colorHistory) {
            JButton btn = new JButton("  ");
            btn.setBackground(color);
            btn.setOpaque(true);
            btn.setPreferredSize(new Dimension(30, 30));
            btn.addActionListener(e -> selectColor(color));
            colorHistoryPanel.add(btn);
        }
        
        colorHistoryPanel.revalidate();
        colorHistoryPanel.repaint();
    }

    public Color getSelectedColor() {
        return colorButton.getBackground();
    }

    public void setSelectedColor(Color color) {
        colorButton.setBackground(color);
    }

    public int getStrokeWidth() {
        return widthSlider.getValue();
    }

    public void setStrokeWidth(int width) {
        widthSlider.setValue(Math.max(1, Math.min(50, width)));
    }

    public void setColorChangeListener(ActionListener listener) {
        this.colorChangeListener = listener;
    }

    public List<Color> getColorHistory() {
        return new ArrayList<>(colorHistory);
    }
}
