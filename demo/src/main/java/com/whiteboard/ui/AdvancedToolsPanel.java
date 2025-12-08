package com.whiteboard.ui;

import com.whiteboard.drawing.TextManager;
import com.whiteboard.drawing.TextManager.TextAlign;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

/**
 * Advanced tools panel - Font, text styling, shapes nâng cao
 */
public class AdvancedToolsPanel extends JPanel {
    private TextManager textManager;
    private JComboBox<String> fontCombo;
    private JSpinner fontSizeSpinner;
    private JButton boldBtn, italicBtn, underlineBtn;
    private JButton alignLeftBtn, alignCenterBtn, alignRightBtn;
    private JButton polygonBtn, starBtn, arrowBtn, curveBtn, fillBtn;
    private AdvancedToolListener listener;

    public interface AdvancedToolListener {
        void onFontChanged(String font);
        void onFontSizeChanged(int size);
        void onFontStyleChanged();
        void onTextAlignChanged(TextAlign align);
        void onPolygonTool();
        void onStarTool();
        void onArrowTool();
        void onCurveTool();
        void onFillTool();
    }

    public AdvancedToolsPanel(TextManager textManager) {
        this.textManager = textManager;
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(5, 5));
        setBorder(new TitledBorder("Advanced Tools"));
        setPreferredSize(new Dimension(250, 200));

        // Text formatting
        JPanel textPanel = new JPanel(new BorderLayout(5, 5));
        textPanel.setBorder(BorderFactory.createTitledBorder("Text Formatting"));
        textPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Font selection
        JPanel fontPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        fontPanel.add(new JLabel("Font:"));
        fontCombo = new JComboBox<>(TextManager.FONTS);
        fontCombo.addActionListener(e -> {
            textManager.setFont((String) fontCombo.getSelectedItem());
            if (listener != null) listener.onFontChanged((String) fontCombo.getSelectedItem());
        });
        fontPanel.add(fontCombo);
        textPanel.add(fontPanel, BorderLayout.NORTH);

        // Font size and styles
        JPanel stylePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        stylePanel.add(new JLabel("Size:"));
        fontSizeSpinner = new JSpinner(new SpinnerNumberModel(16, 8, 72, 1));
        fontSizeSpinner.addChangeListener(e -> {
            textManager.setFontSize((Integer) fontSizeSpinner.getValue());
            if (listener != null) listener.onFontSizeChanged((Integer) fontSizeSpinner.getValue());
        });
        stylePanel.add(fontSizeSpinner);

        boldBtn = createIconButton("B");
        italicBtn = createIconButton("I");
        underlineBtn = createIconButton("U");

        boldBtn.addActionListener(e -> {
            if (listener != null) listener.onFontStyleChanged();
        });
        italicBtn.addActionListener(e -> {
            if (listener != null) listener.onFontStyleChanged();
        });
        underlineBtn.addActionListener(e -> {
            if (listener != null) listener.onFontStyleChanged();
        });

        stylePanel.add(boldBtn);
        stylePanel.add(italicBtn);
        stylePanel.add(underlineBtn);
        textPanel.add(stylePanel, BorderLayout.CENTER);

        // Text alignment
        JPanel alignPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        alignLeftBtn = createIconButton("⬅");
        alignCenterBtn = createIconButton("⬌");
        alignRightBtn = createIconButton("➡");

        alignLeftBtn.addActionListener(e -> {
            textManager.setTextAlign(TextAlign.LEFT);
            if (listener != null) listener.onTextAlignChanged(TextAlign.LEFT);
        });
        alignCenterBtn.addActionListener(e -> {
            textManager.setTextAlign(TextAlign.CENTER);
            if (listener != null) listener.onTextAlignChanged(TextAlign.CENTER);
        });
        alignRightBtn.addActionListener(e -> {
            textManager.setTextAlign(TextAlign.RIGHT);
            if (listener != null) listener.onTextAlignChanged(TextAlign.RIGHT);
        });

        alignPanel.add(alignLeftBtn);
        alignPanel.add(alignCenterBtn);
        alignPanel.add(alignRightBtn);
        textPanel.add(alignPanel, BorderLayout.SOUTH);

        add(textPanel, BorderLayout.NORTH);

        // Advanced shapes
        JPanel shapesPanel = new JPanel(new BorderLayout(5, 5));
        shapesPanel.setBorder(BorderFactory.createTitledBorder("Advanced Shapes"));
        shapesPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JPanel shapeButtonPanel = new JPanel(new GridLayout(2, 3, 5, 5));

        polygonBtn = new JButton("⬟ Polygon");
        polygonBtn.addActionListener(e -> {
            if (listener != null) listener.onPolygonTool();
        });

        starBtn = new JButton("⭐ Star");
        starBtn.addActionListener(e -> {
            if (listener != null) listener.onStarTool();
        });

        arrowBtn = new JButton("➜ Arrow");
        arrowBtn.addActionListener(e -> {
            if (listener != null) listener.onArrowTool();
        });

        curveBtn = new JButton("∿ Curve");
        curveBtn.addActionListener(e -> {
            if (listener != null) listener.onCurveTool();
        });

        fillBtn = new JButton("🪣 Fill");
        fillBtn.addActionListener(e -> {
            if (listener != null) listener.onFillTool();
        });

        JButton moreBtn = new JButton("⋯ More");
        moreBtn.setEnabled(false);

        shapeButtonPanel.add(polygonBtn);
        shapeButtonPanel.add(starBtn);
        shapeButtonPanel.add(arrowBtn);
        shapeButtonPanel.add(curveBtn);
        shapeButtonPanel.add(fillBtn);
        shapeButtonPanel.add(moreBtn);

        shapesPanel.add(shapeButtonPanel, BorderLayout.CENTER);
        add(shapesPanel, BorderLayout.CENTER);
    }

    private JButton createIconButton(String text) {
        JButton btn = new JButton(text);
        btn.setPreferredSize(new Dimension(35, 25));
        btn.setFont(new Font("Arial", Font.BOLD, 12));
        return btn;
    }

    public void setListener(AdvancedToolListener listener) {
        this.listener = listener;
    }

    public TextManager getTextManager() {
        return textManager;
    }
}
