package com.whiteboard.ui;

import com.whiteboard.drawing.ZoomManager;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

/**
 * Zoom panel - Điều khiển zoom và pan
 */
public class ZoomPanel extends JPanel {
    private ZoomManager zoomManager;
    private JSlider zoomSlider;
    private JLabel zoomLabel;
    private JButton fitBtn, resetBtn;
    private ZoomChangeListener listener;

    public interface ZoomChangeListener {
        void onZoomChanged(float newZoom);
        void onFitToWindow();
        void onReset();
    }

    public ZoomPanel(ZoomManager zoomManager) {
        this.zoomManager = zoomManager;
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(5, 5));
        setBorder(new TitledBorder("Zoom & View"));
        setPreferredSize(new Dimension(250, 80));

        // Zoom slider
        JPanel zoomSliderPanel = new JPanel(new BorderLayout(5, 0));
        zoomSlider = new JSlider(10, 500, 100);
        zoomSlider.addChangeListener(e -> {
            float newZoom = zoomSlider.getValue() / 100.0f;
            zoomManager.setZoom(newZoom);
            zoomLabel.setText(zoomManager.getZoomPercentage());
            if (listener != null) listener.onZoomChanged(newZoom);
        });
        zoomSliderPanel.add(new JLabel("Zoom:"), BorderLayout.WEST);
        zoomSliderPanel.add(zoomSlider, BorderLayout.CENTER);

        zoomLabel = new JLabel("100%");
        zoomLabel.setPreferredSize(new Dimension(60, 20));
        zoomLabel.setHorizontalAlignment(SwingConstants.CENTER);
        zoomSliderPanel.add(zoomLabel, BorderLayout.EAST);

        add(zoomSliderPanel, BorderLayout.NORTH);

        // Buttons
        JPanel buttonPanel = new JPanel(new GridLayout(1, 4, 5, 0));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JButton zoomInBtn = new JButton("🔍+");
        zoomInBtn.setToolTipText("Zoom in");
        zoomInBtn.addActionListener(e -> {
            zoomManager.zoomIn();
            updateSlider();
            if (listener != null) listener.onZoomChanged(zoomManager.getZoom());
        });

        JButton zoomOutBtn = new JButton("🔍-");
        zoomOutBtn.setToolTipText("Zoom out");
        zoomOutBtn.addActionListener(e -> {
            zoomManager.zoomOut();
            updateSlider();
            if (listener != null) listener.onZoomChanged(zoomManager.getZoom());
        });

        fitBtn = new JButton("📐 Fit");
        fitBtn.setToolTipText("Fit to window");
        fitBtn.addActionListener(e -> {
            if (listener != null) listener.onFitToWindow();
        });

        resetBtn = new JButton("⟲ Reset");
        resetBtn.setToolTipText("Reset zoom");
        resetBtn.addActionListener(e -> {
            zoomManager.resetZoom();
            updateSlider();
            if (listener != null) listener.onReset();
        });

        buttonPanel.add(zoomInBtn);
        buttonPanel.add(zoomOutBtn);
        buttonPanel.add(fitBtn);
        buttonPanel.add(resetBtn);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void updateSlider() {
        zoomSlider.setValue((int) (zoomManager.getZoom() * 100));
        zoomLabel.setText(zoomManager.getZoomPercentage());
    }

    public void setListener(ZoomChangeListener listener) {
        this.listener = listener;
    }
}
