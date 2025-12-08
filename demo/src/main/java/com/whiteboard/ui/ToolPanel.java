package com.whiteboard.ui;

import com.whiteboard.drawing.DrawingTool;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public class ToolPanel extends JPanel {
    private JButton[] toolButtons;
    private JButton clearButton;
    private JButton undoButton;
    private DrawingTool.Tool selectedTool;
    private Consumer<DrawingTool.Tool> onToolSelected;

    public ToolPanel() {
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createTitledBorder("Tools"));

        selectedTool = DrawingTool.Tool.PENCIL;

        // Panel chứa các tool chính, xếp dạng lưới gọn theo chiều dọc
        JPanel toolsPanel = new JPanel(new GridLayout(0, 2, 5, 5));

        DrawingTool.Tool[] tools = new DrawingTool.Tool[] {
                DrawingTool.Tool.PENCIL,
                DrawingTool.Tool.LINE,
                DrawingTool.Tool.RECTANGLE,
                DrawingTool.Tool.CIRCLE,
                DrawingTool.Tool.ARROW,
                DrawingTool.Tool.TEXT,
                DrawingTool.Tool.PAN
        };
        toolButtons = new JButton[tools.length];

        for (int i = 0; i < tools.length; i++) {
            final DrawingTool.Tool tool = tools[i];
            JButton button = new JButton(tool.getLabel());
            button.setPreferredSize(new Dimension(110, 30));

            if (tool == DrawingTool.Tool.PENCIL) {
                button.setBackground(new Color(200, 200, 255));
                button.setOpaque(true);
            }

            button.addActionListener(e -> selectTool(tool, button));
            toolButtons[i] = button;
            toolsPanel.add(button);
        }

        add(toolsPanel, BorderLayout.CENTER);

        // Panel cho các action nhanh (Undo / Clear) nằm phía dưới
        JPanel actionsPanel = new JPanel(new GridLayout(1, 2, 5, 5));

        undoButton = new JButton("↶ Undo");
        clearButton = new JButton("🗑 Clear");

        actionsPanel.add(undoButton);
        actionsPanel.add(clearButton);

        add(actionsPanel, BorderLayout.SOUTH);
    }

    private void selectTool(DrawingTool.Tool tool, JButton button) {
        selectedTool = tool;

        // Reset all buttons
        for (JButton btn : toolButtons) {
            btn.setBackground(UIManager.getColor("Button.background"));
            btn.setOpaque(false);
        }

        // Highlight selected button
        button.setBackground(new Color(200, 200, 255));
        button.setOpaque(true);

        if (onToolSelected != null) {
            onToolSelected.accept(tool);
        }
    }

    public DrawingTool.Tool getSelectedTool() {
        return selectedTool;
    }

    public JButton getClearButton() {
        return clearButton;
    }

    public JButton getUndoButton() {
        return undoButton;
    }

    public void setOnToolSelected(Consumer<DrawingTool.Tool> callback) {
        this.onToolSelected = callback;
    }
}
