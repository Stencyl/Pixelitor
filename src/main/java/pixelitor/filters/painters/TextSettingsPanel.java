/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor. If not, see <http://www.gnu.org/licenses/>.
 */

package pixelitor.filters.painters;

import org.jdesktop.swingx.VerticalLayout;
import org.jdesktop.swingx.painter.AbstractLayoutPainter.HorizontalAlignment;
import org.jdesktop.swingx.painter.AbstractLayoutPainter.VerticalAlignment;
import pixelitor.filters.gui.AngleParam;
import pixelitor.filters.gui.ColorParam;
import pixelitor.filters.gui.ColorParamGUI;
import pixelitor.filters.gui.FilterGUI;
import pixelitor.filters.gui.ParamAdjustmentListener;
import pixelitor.filters.gui.RangeParam;
import pixelitor.gui.utils.DialogBuilder;
import pixelitor.gui.utils.GUIUtils;
import pixelitor.gui.utils.GridBagHelper;
import pixelitor.gui.utils.SliderSpinner;
import pixelitor.layers.Drawable;
import pixelitor.layers.TextLayer;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.font.TextAttribute;
import java.util.HashMap;
import java.util.Map;

import static java.awt.Color.WHITE;
import static java.awt.FlowLayout.LEFT;
import static javax.swing.BorderFactory.createTitledBorder;
import static pixelitor.filters.gui.ColorParam.TransparencyPolicy.USER_ONLY_TRANSPARENCY;

/**
 * Customization panel for the text filter and for text layers
 */
public class TextSettingsPanel extends FilterGUI
        implements ParamAdjustmentListener, ActionListener {
    private static final String DEFAULT_TEXT = "Pixelitor";
    private TextLayer textLayer;

    private JTextField textTF;
    private JComboBox<String> fontFamilyChooserCB;
    private SliderSpinner fontSizeSlider;
    private AngleParam rotationParam;

    private JCheckBox boldCB;
    private JCheckBox italicCB;

    private ColorParam color;

    private EffectsPanel effectsPanel;
    private JComboBox<VerticalAlignment> vAlignmentCB;
    private JComboBox<HorizontalAlignment> hAlignmentCB;

    private JCheckBox watermarkCB;

    private static String lastText = "";

    private Map<TextAttribute, Object> map;
    private JDialog advancedSettingsDialog;
    private AdvancedTextSettingsPanel advancedSettingsPanel;

    // called for image layers
    public TextSettingsPanel(TextFilter textFilter, Drawable dr) {
        super(textFilter, dr);
        createGUI(null, dr.getComp().getCanvasImHeight());

        if(!textTF.getText().isEmpty()) {
            // a "last text" was set
            paramAdjusted();
        }
    }

    // called for text layers
    public TextSettingsPanel(TextLayer textLayer) {
        super(null, null);
        this.textLayer = textLayer;
        createGUI(textLayer.getSettings(), textLayer.getComp().getCanvasImHeight());

        // make sure that the text layer has a settings object
        // even if the user presses OK without making any adjustments
        paramAdjusted();

        if(textTF.getText().equals(DEFAULT_TEXT)) {
            textTF.selectAll();
        }
    }

    private void createGUI(TextSettings settings, int canvasHeight) {
        setLayout(new VerticalLayout());

        add(createTextPanel(settings));
        add(createFontPanel(settings, canvasHeight));

        createEffectsPanel(settings);
        add(effectsPanel);

        add(createWatermarkPanel(settings));
    }

    private JPanel createTextPanel(TextSettings settings) {
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new GridBagLayout());

        var gbh = new GridBagHelper(textPanel);

        gbh.addLabel("Text:", 0, 0);
        createTextTF(settings);
        gbh.addLastControl(textTF);

        gbh.addLabel("Color:", 0, 1);
        Color defaultColor = settings == null ? WHITE : settings.getColor();
        color = new ColorParam("Color", defaultColor, USER_ONLY_TRANSPARENCY);
        gbh.addControl(new ColorParamGUI(color, false));
        color.setAdjustmentListener(this);

        gbh.addLabel("Rotation:", 2, 1);
        double defaultRotation = 0;
        if (settings != null) {
            defaultRotation = settings.getRotation();
        }
        rotationParam = new AngleParam("", defaultRotation);
        rotationParam.setAdjustmentListener(this);
        gbh.addControl(rotationParam.createGUI());

        hAlignmentCB = new JComboBox<>(HorizontalAlignment.values());
        if (settings != null) {
            hAlignmentCB.setSelectedItem(settings.getHorizontalAlignment());
        }
        gbh.addLabel("Horizontal Alignment:", 0, 2);
        hAlignmentCB.addActionListener(this);
        gbh.addControl(hAlignmentCB);

        vAlignmentCB = new JComboBox<>(VerticalAlignment.values());
        if (settings != null) {
            vAlignmentCB.setSelectedItem(settings.getVerticalAlignment());
        }
        gbh.addLabel("Vertical Alignment:", 0, 3);
        vAlignmentCB.addActionListener(this);
        gbh.addControl(vAlignmentCB);

        return textPanel;
    }

    private void createTextTF(TextSettings settings) {
        String defaultText;
        if (settings == null) {
            if (isInFilterMode()) { 
                // Remember the last text in filter mode.
                // This was a requested feature when we didn't have text layers,
                // probably it is not so useful now
                defaultText = lastText;
            } else { // text layer mode
                // no last text remembering when creating new text layers
                defaultText = DEFAULT_TEXT;
            }
        } else {
            defaultText = settings.getText();
        }

        textTF = new JTextField(defaultText, 20);
        textTF.setName("textTF");

        textTF.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                paramAdjusted();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                paramAdjusted();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                paramAdjusted();
            }
        });
    }

    private JPanel createFontPanel(TextSettings settings, int canvasHeight) {
        JPanel fontPanel = new JPanel();
        fontPanel.setBorder(createTitledBorder("Font"));
        fontPanel.setLayout(new GridBagLayout());

        var gbh = new GridBagHelper(fontPanel);

        int maxFontSize = 1000;
        int defaultFontSize;
        if (settings == null) {
            defaultFontSize = (int) (canvasHeight * 0.2);
            if (canvasHeight > maxFontSize) {
                maxFontSize = canvasHeight;
            }
        } else {
            defaultFontSize = settings.getFont().getSize();
            if (maxFontSize < defaultFontSize) {
                // can get here if the canvas is downsized
                // after the text later creation
                maxFontSize = defaultFontSize;
            }
        }

        gbh.addLabel("Font Size:", 0, 0);

        RangeParam fontSizeParam = new RangeParam("", 1, defaultFontSize, maxFontSize);
        fontSizeSlider = SliderSpinner.from(fontSizeParam);
        fontSizeSlider.setName("fontSize");
        fontSizeParam.setAdjustmentListener(this);
        gbh.addLastControl(fontSizeSlider);

        gbh.addLabel("Font Type:", 0, 1);
        String[] availableFonts = getAvailableFonts();
        fontFamilyChooserCB = new JComboBox<>(availableFonts);
        if (settings != null) {
            // it is important to use Font.getName(), and not Font.getFontName(),
            // otherwise it might not be in the combo box
            String fontName = settings.getFont().getName();
            fontFamilyChooserCB.setSelectedItem(fontName);
        }
        fontFamilyChooserCB.addActionListener(this);
        gbh.addLastControl(fontFamilyChooserCB);

        boolean defaultBold = false;
        boolean defaultItalic = false;
        if (settings != null) {
            Font font = settings.getFont();
            defaultBold = font.isBold();
            defaultItalic = font.isItalic();
            setAttributeMapFromFontSettings(font);
        }

        gbh.addLabel("Bold:", 0, 2);
        boldCB = createCheckBox("boldCB", gbh, defaultBold);

        gbh.addLabel("   Italic:", 2, 2);
        italicCB = createCheckBox("italicCB", gbh, defaultItalic);

        JButton showAdvancedSettingsButton = new JButton("Advanced...");
        showAdvancedSettingsButton.addActionListener(e -> onAdvancedSettingsClick());

        gbh.addLabel("      ", 4, 2);
        gbh.addControl(showAdvancedSettingsButton);

        return fontPanel;
    }

    private static String[] getAvailableFonts() {
        return GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .getAvailableFontFamilyNames();
    }

    @SuppressWarnings("unchecked")
    private void setAttributeMapFromFontSettings(Font font) {
        if (font.hasLayoutAttributes()) {
            map = (Map<TextAttribute, Object>) font.getAttributes();
        }
    }

    private void onAdvancedSettingsClick() {
        if (advancedSettingsDialog == null) {
            advancedSettingsPanel = new AdvancedTextSettingsPanel(
                    this, map);
            JDialog owner = GUIUtils.getDialogAncestor(this);
            advancedSettingsDialog = new DialogBuilder()
                    .owner(owner)
                    .content(advancedSettingsPanel)
                    .title("Advanced Text Settings")
                    .noCancelButton()
                    .okText("Close")
                    .build();
        }
        GUIUtils.showDialog(advancedSettingsDialog);
    }

    private JCheckBox createCheckBox(String name, GridBagHelper gbh, boolean selected) {
        JCheckBox cb = new JCheckBox("", selected);
        cb.setName(name);
        cb.addActionListener(this);
        gbh.addControl(cb);
        return cb;
    }

    private Font getSelectedFont() {
        String fontFamily = (String) fontFamilyChooserCB.getSelectedItem();
        int style = Font.PLAIN;
        if (boldCB.isSelected()) {
            style |= Font.BOLD;
        }
        if (italicCB.isSelected()) {
            style |= Font.ITALIC;
        }
        int size = fontSizeSlider.getCurrentValue();
        Font font = new Font(fontFamily, style, size);

        // It is important to create here a new Map, because
        // the old one stores old values in TextAttribute.SIZE
        // and other fields which would override the current ones.
        // TODO there has to be a simpler way, for example overwriting
        // however, it is not trivial, there is no single "style" TextAttribute
        var oldMap = map;
        map = new HashMap<>();

        if (advancedSettingsDialog != null) {
            advancedSettingsPanel.updateFontAttributesMap(map);
        } else if (oldMap != null) {
            // no dialog, copy manually the advanced settings
            TextAttribute[] advancedSettings = {
                    TextAttribute.STRIKETHROUGH,
                    TextAttribute.UNDERLINE,
                    TextAttribute.KERNING,
                    TextAttribute.LIGATURES,
                    TextAttribute.TRACKING
            };

            for (TextAttribute setting : advancedSettings) {
                map.put(setting, oldMap.get(setting));
            }
        }

        return font.deriveFont(map);
    }

    private void createEffectsPanel(TextSettings settings) {
        AreaEffects areaEffects = null;
        if (settings != null) {
            areaEffects = settings.getAreaEffects();
        }
        effectsPanel = new EffectsPanel(this, areaEffects);
        effectsPanel.setBorder(createTitledBorder("Effects"));
    }

    private JPanel createWatermarkPanel(TextSettings settings) {
        boolean hasWatermark = false;
        if (settings != null) {
            hasWatermark = settings.isWatermark();
        }
        watermarkCB = new JCheckBox("Use Text for Watermarking", hasWatermark);
        watermarkCB.addActionListener(this);

        var p = new JPanel(new FlowLayout(LEFT));
        p.add(watermarkCB);
        return p;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        paramAdjusted();
    }

    @Override
    public void paramAdjusted() {
        TextFilter textFilter = (TextFilter) filter;
        String text = textTF.getText();

        if (isInFilterMode()) {
            lastText = text;
        }

        AreaEffects areaEffects = null;
        double textRotationAngle = rotationParam.getValueInRadians();
        if (effectsPanel != null) {
            areaEffects = effectsPanel.getEffects();

//            // adjust the drop shadow angle so that it is
//            // in the right direction even if the text is rotated
//            ShadowPathEffect dropShadowEffect = areaEffects.getDropShadowEffect();
//            if (dropShadowEffect != null && textRotationAngle != 0) {
//                Point2D offset = dropShadowEffect.getOffset();
//                double distance = offset.distance(0, 0);
//                double angle = Math.atan2(offset.getY(), offset.getX());
//                angle -= textRotationAngle;
//                Point2D adjustedOffset = Utils.offsetFromPolar(distance, angle);
//                dropShadowEffect.setOffset(adjustedOffset);
//            }
        }

        Font selectedFont = getSelectedFont();

        var settings = new TextSettings(
                text, selectedFont, color.getColor(), areaEffects,
                (HorizontalAlignment) hAlignmentCB.getSelectedItem(),
                (VerticalAlignment) vAlignmentCB.getSelectedItem(),
                watermarkCB.isSelected(), textRotationAngle);

        if (textFilter != null) { // filter mode
            textFilter.setSettings(settings);
            runFilterPreview();
        } else {
            assert textLayer != null;
            textLayer.setSettings(settings);
            textLayer.getComp().imageChanged();
        }
    }

    private boolean isInFilterMode() {
        return filter != null;
    }
}
