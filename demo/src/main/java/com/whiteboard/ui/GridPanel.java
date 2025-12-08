package com.whiteboard.ui;

import com.whiteboard.drawing.GridManager;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

/**
 * Grid panel - Điều khiển grid và alignment
 */
public class GridPanel extends JPanel {
    private GridManager gridManager;
    private JCheckBox gridVisibleCheck, snapToGridCheck;
    private JComboBox<Integer> gridSizeCombo;
    private JButton alignLeftBtn, alignRightBtn, alignTopBtn, alignBottomBtn;
    private JButton distributeHBtn, distributeVBtn;
    private GridChangeListener listener;

    public interface GridChangeListener {
        void onGridSettingsChanged();
        void onAlignLeft();
        void onAlignRight();
        void onAlignTop();
        void onAlignBottom();
        void onDistributeHorizontally();
        void onDistributeVertically();
    }

    public GridPanel(GridManager gridManager) {
        this.gridManager = gridManager;
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(5, 5));
        setBorder(new TitledBorder("Grid & Alignment"));
        setPreferredSize(new Dimension(250, 120));

        // Grid settings
        JPanel gridPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        gridPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 5));

        // Grid visible
        gridVisibleCheck = new JCheckBox("Show Grid", false);
        gridVisibleCheck.addActionListener(e -> {
            gridManager.setGridVisible(gridVisibleCheck.isSelected());
            if (listener != null) listener.onGridSettingsChanged();
        });
        gridPanel.add(gridVisibleCheck);

        // Snap to grid
        snapToGridCheck = new JCheckBox("Snap to Grid", false);
        snapToGridCheck.addActionListener(e -> {
            gridManager.setSnapToGrid(snapToGridCheck.isSelected());
            if (listener != null) listener.onGridSettingsChanged();
        });
        gridPanel.add(snapToGridCheck);

        // Grid size
        JPanel gridSizePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        gridSizePanel.add(new JLabel("Grid Size:"));
        gridSizeCombo = new JComboBox<>();
        for (int size : GridManager.GRID_SIZES) {
            gridSizeCombo.addItem(size);
        }
        gridSizeCombo.setSelectedItem(10);
        gridSizeCombo.addActionListener(e -> {
            gridManager.setGridSize((Integer) gridSizeCombo.getSelectedItem());
            if (listener != null) listener.onGridSettingsChanged();
        });
        gridSizePanel.add(gridSizeCombo);
        gridPanel.add(gridSizePanel);

        add(gridPanel, BorderLayout.NORTH);

        // Alignment buttons
        JPanel alignPanel = new JPanel(new GridLayout(2, 3, 5, 5));
        alignPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));

        alignLeftBtn = createButton("⬅ Left", e -> listener.onAlignLeft());
        alignTopBtn = createButton("⬆ Top", e -> listener.onAlignTop());
        alignRightBtn = createButton("Right ➡", e -> listener.onAlignRight());
        alignBottomBtn = createButton("⬇ Bottom", e -> listener.onAlignBottom());
        distributeHBtn = createButton("≈ Dist. H", e -> listener.onDistributeHorizontally());
        distributeVBtn = createButton("≈ Dist. V", e -> listener.onDistributeVertically());

        alignPanel.add(alignLeftBtn);
        alignPanel.add(alignTopBtn);
        alignPanel.add(alignRightBtn);
        alignPanel.add(alignBottomBtn);
        alignPanel.add(distributeHBtn);
        alignPanel.add(distributeVBtn);

        add(alignPanel, BorderLayout.CENTER);
    }

    private JButton createButton(String text, java.awt.event.ActionListener listener) {
        JButton btn = new JButton(text);
        btn.addActionListener(e -> {
            if (this.listener != null) {
                listener.actionPerformed(e);
            }
        });
        return btn;
    }

    public void setListener(GridChangeListener listener) {
        this.listener = listener;
    }
}
