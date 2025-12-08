package com.whiteboard.ui;

import com.whiteboard.drawing.LayerManager;
import com.whiteboard.drawing.LayerManager.Layer;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.List;

/**
 * Layer panel - Quản lý layers trực quan
 */
public class LayerPanel extends JPanel {
    private LayerManager layerManager;
    private JList<String> layerList;
    private DefaultListModel<String> layerModel;
    private JButton addLayerBtn, deleteLayerBtn, renameBtn;
    private JSlider opacitySlider;
    private JCheckBox visibilityCheckBox;
    private Runnable onLayerChanged;

    public LayerPanel(LayerManager layerManager) {
        this.layerManager = layerManager;
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(5, 5));
        setBorder(new TitledBorder("Layers"));
        setPreferredSize(new Dimension(200, 300));

        // Layer list
        layerModel = new DefaultListModel<>();
        layerList = new JList<>(layerModel);
        updateLayerList();
        layerList.addListSelectionListener(e -> {
            int index = layerList.getSelectedIndex();
            if (index >= 0) {
                layerManager.setActiveLayer(index);
                updateLayerProperties();
                if (onLayerChanged != null) onLayerChanged.run();
            }
        });

        JScrollPane scrollPane = new JScrollPane(layerList);
        add(scrollPane, BorderLayout.CENTER);

        // Control panel
        JPanel controlPanel = new JPanel(new GridLayout(4, 1, 5, 5));
        controlPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Layer buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        addLayerBtn = new JButton("+");
        addLayerBtn.setToolTipText("Add new layer");
        deleteLayerBtn = new JButton("-");
        deleteLayerBtn.setToolTipText("Delete layer");
        renameBtn = new JButton("✎");
        renameBtn.setToolTipText("Rename layer");

        addLayerBtn.addActionListener(e -> addNewLayer());
        deleteLayerBtn.addActionListener(e -> deleteSelectedLayer());
        renameBtn.addActionListener(e -> renameSelectedLayer());

        buttonPanel.add(addLayerBtn);
        buttonPanel.add(deleteLayerBtn);
        buttonPanel.add(renameBtn);
        controlPanel.add(buttonPanel);

        // Visibility checkbox
        visibilityCheckBox = new JCheckBox("Visible", true);
        visibilityCheckBox.addActionListener(e -> {
            int index = layerList.getSelectedIndex();
            if (index >= 0) {
                layerManager.setLayerVisibility(index, visibilityCheckBox.isSelected());
                if (onLayerChanged != null) onLayerChanged.run();
            }
        });
        controlPanel.add(visibilityCheckBox);

        // Opacity slider
        JPanel opacityPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        opacityPanel.add(new JLabel("Opacity:"));
        opacitySlider = new JSlider(0, 100, 100);
        opacitySlider.addChangeListener(e -> {
            int index = layerList.getSelectedIndex();
            if (index >= 0) {
                layerManager.setLayerOpacity(index, opacitySlider.getValue() / 100.0f);
                if (onLayerChanged != null) onLayerChanged.run();
            }
        });
        opacityPanel.add(opacitySlider);
        controlPanel.add(opacityPanel);

        add(controlPanel, BorderLayout.SOUTH);
    }

    private void addNewLayer() {
        String name = JOptionPane.showInputDialog(this, "Layer name:", "New Layer");
        if (name != null && !name.trim().isEmpty()) {
            layerManager.createLayer(name);
            updateLayerList();
            if (onLayerChanged != null) onLayerChanged.run();
        }
    }

    private void deleteSelectedLayer() {
        int index = layerList.getSelectedIndex();
        if (index >= 0) {
            layerManager.deleteLayer(index);
            updateLayerList();
            if (onLayerChanged != null) onLayerChanged.run();
        }
    }

    private void renameSelectedLayer() {
        int index = layerList.getSelectedIndex();
        if (index >= 0) {
            String newName = JOptionPane.showInputDialog(this, "New name:", 
                    layerManager.getLayer(index).name);
            if (newName != null && !newName.trim().isEmpty()) {
                layerManager.renameLayer(index, newName);
                updateLayerList();
            }
        }
    }

    public void updateLayerList() {
        layerModel.clear();
        List<Layer> layers = layerManager.getAllLayers();
        for (Layer layer : layers) {
            layerModel.addElement(layer.name);
        }
        layerList.setSelectedIndex(layerManager.getActiveLayerIndex());
    }

    private void updateLayerProperties() {
        int index = layerList.getSelectedIndex();
        if (index >= 0) {
            Layer layer = layerManager.getLayer(index);
            visibilityCheckBox.setSelected(layer.visible);
            opacitySlider.setValue((int) (layer.opacity * 100));
        }
    }

    public void setOnLayerChanged(Runnable callback) {
        this.onLayerChanged = callback;
    }
}
