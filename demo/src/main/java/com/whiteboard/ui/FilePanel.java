package com.whiteboard.ui;

import com.whiteboard.drawing.FileManager;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.File;

/**
 * File panel - Quản lý save/load/export
 */
public class FilePanel extends JPanel {
    private JButton saveBtn, loadBtn, exportPngBtn, exportSvgBtn, newBtn;
    private JLabel statusLabel;
    private FileOperationListener listener;

    public interface FileOperationListener {
        void onNewProject();
        void onSaveProject(String filePath);
        void onLoadProject(String filePath);
        void onExportPNG(String filePath);
        void onExportSVG(String filePath);
        void onImportImage(String filePath);
    }

    public FilePanel() {
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(5, 5));
        setBorder(new TitledBorder("File Operations"));

        JPanel buttonPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Buttons
        newBtn = createButton("📄 New", "Create new project", e -> newProject());
        saveBtn = createButton("💾 Save", "Save project as JSON", e -> saveProject());
        loadBtn = createButton("📂 Load", "Load project from JSON", e -> loadProject());
        exportPngBtn = createButton("🖼️ Export PNG", "Export as PNG image", e -> exportPNG());
        exportSvgBtn = createButton("⬆️ Export SVG", "Export as SVG vector", e -> exportSVG());
        JButton importBtn = createButton("📥 Import Image", "Import image from file", e -> importImage());

        buttonPanel.add(newBtn);
        buttonPanel.add(saveBtn);
        buttonPanel.add(loadBtn);
        buttonPanel.add(exportPngBtn);
        buttonPanel.add(exportSvgBtn);
        buttonPanel.add(importBtn);

        add(buttonPanel, BorderLayout.CENTER);

        // Status bar
        statusLabel = new JLabel("Ready");
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        add(statusLabel, BorderLayout.SOUTH);
    }

    private JButton createButton(String text, String tooltip, java.awt.event.ActionListener listener) {
        JButton btn = new JButton(text);
        btn.setToolTipText(tooltip);
        btn.addActionListener(listener);
        return btn;
    }

    private void newProject() {
        if (listener != null) {
            listener.onNewProject();
            setStatus("New project created");
        }
    }

    private void saveProject() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "JSON Files", "json"));
        chooser.setSelectedFile(new File("drawing.json"));

        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            if (listener != null) {
                listener.onSaveProject(chooser.getSelectedFile().getAbsolutePath());
                setStatus("Project saved: " + chooser.getSelectedFile().getName());
            }
        }
    }

    private void loadProject() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "JSON Files", "json"));

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            if (listener != null) {
                listener.onLoadProject(chooser.getSelectedFile().getAbsolutePath());
                setStatus("Project loaded: " + chooser.getSelectedFile().getName());
            }
        }
    }

    private void exportPNG() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "PNG Files", "png"));
        chooser.setSelectedFile(new File("drawing.png"));

        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            if (listener != null) {
                listener.onExportPNG(chooser.getSelectedFile().getAbsolutePath());
                setStatus("Exported as PNG: " + chooser.getSelectedFile().getName());
            }
        }
    }

    private void exportSVG() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "SVG Files", "svg"));
        chooser.setSelectedFile(new File("drawing.svg"));

        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            if (listener != null) {
                listener.onExportSVG(chooser.getSelectedFile().getAbsolutePath());
                setStatus("Exported as SVG: " + chooser.getSelectedFile().getName());
            }
        }
    }

    private void importImage() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Image Files", "png", "jpg", "jpeg", "gif", "bmp"));
        chooser.setDialogTitle("Import Image");

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            if (listener != null) {
                listener.onImportImage(chooser.getSelectedFile().getAbsolutePath());
                setStatus("Imported: " + chooser.getSelectedFile().getName());
            }
        }
    }

    public void setListener(FileOperationListener listener) {
        this.listener = listener;
    }

    public void setStatus(String message) {
        statusLabel.setText(message);
    }
}
