/*
 * Copyright 2009-2014 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor.  If not, see <http://www.gnu.org/licenses/>.
 */
package pixelitor.tools;

import pixelitor.Canvas;
import pixelitor.Composition;
import pixelitor.ImageComponent;
import pixelitor.ImageComponents;
import pixelitor.filters.gui.RangeParam;
import pixelitor.transform.TransformSupport;
import pixelitor.transform.TransformToolChangeListener;
import pixelitor.utils.ImageSwitchListener;
import pixelitor.utils.SliderSpinner;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;

/**
 * The crop tool
 */
public class CropTool extends Tool implements ImageSwitchListener, TransformToolChangeListener {
    private CropToolState state = CropToolState.INITIAL;

    private TransformSupport transformSupport;

    private final RangeParam maskOpacityParam = new RangeParam("Mask Opacity (%)", 0, 100, 75);

    private Composite hideComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, maskOpacityParam.getValueAsPercentage());

    private JButton cancelButton = new JButton("Cancel");
    private JButton cropButton;

    // The crop rectangle in image space.
    // This variable is used only while the image component is resized
    private Rectangle lastCropRectangle;
    private JCheckBox allowGrowingCB;

    CropTool() {
        super('c', "Crop", "crop_tool_icon.png",
                "Click and drag to define the crop area. Hold SPACE down to move the entire region.",
                Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR), true, true, true, ClipStrategy.FULL_AREA);
        spaceDragBehavior = true;
        maskOpacityParam.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                hideComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, maskOpacityParam.getValueAsPercentage());
                ImageComponents.repaintActive();
            }
        });
        ImageComponents.addImageSwitchListener(this);
    }

    @Override
    public void initSettingsPanel() {
        SliderSpinner maskOpacitySpinner = new SliderSpinner(maskOpacityParam, false, SliderSpinner.TextPosition.WEST);
        toolSettingsPanel.add(maskOpacitySpinner);

        allowGrowingCB = new JCheckBox("Allow Growing", false);
        toolSettingsPanel.add(allowGrowingCB);

        cropButton = new JButton("Crop");
        cropButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ImageComponents.toolCropActiveImage(allowGrowingCB.isSelected());
                ImageComponents.repaintActive();
                resetStateToInitial();
            }
        });
        cropButton.setEnabled(false);
        toolSettingsPanel.add(cropButton);

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                state.cancelPressed(CropTool.this);
            }
        });
        cancelButton.setEnabled(false);
        toolSettingsPanel.add(cancelButton);
    }

    @Override
    public void toolMousePressed(MouseEvent e, ImageComponent ic) {
        // in case of crop/image change the ended is set to true even if the tool is not ended
        // if a new drag is started, then reset it
        ended = false;

        state = state.getNextAfterMousePressed();

        if(state == CropToolState.TRANSFORM) {
            assert transformSupport != null;
            transformSupport.mousePressed(e, ic);
            cropButton.setEnabled(true);
            cancelButton.setEnabled(true);
        } else if(state == CropToolState.USERDRAG) {
            cropButton.setEnabled(true);
            cancelButton.setEnabled(true);
        }
    }

    @Override
    public void toolMouseDragged(MouseEvent e, ImageComponent ic) {
        ic.repaint();
        if(state == CropToolState.TRANSFORM) {
            transformSupport.mouseDragged(e, ic);
        }
    }

    // TODO: this is not done with the "toolMouse" mechanism
    @Override
    public void mouseMoved(MouseEvent e, ImageComponent ic) {
        super.mouseMoved(e, ic);
        if(state == CropToolState.TRANSFORM) {
            transformSupport.mouseMoved(e, ic);
        }
    }

    @Override
    public void toolMouseReleased(MouseEvent e, ImageComponent ic) {
        Composition comp = ic.getComp();
        comp.imageChanged(true, true);

        switch (state) {
            case INITIAL:
                break;
            case USERDRAG:
                if(transformSupport != null) {
                    throw new IllegalStateException();
                }
                Rectangle imageSpaceRectangle = userDrag.createPositiveRectangle();
                Rectangle compSpaceRectangle = ic.fromImageToComponentSpace(imageSpaceRectangle);

                transformSupport = new TransformSupport(compSpaceRectangle, imageSpaceRectangle, this);

                state = CropToolState.TRANSFORM;
                break;
            case TRANSFORM:
                if(transformSupport == null) {
                    throw new IllegalStateException();
                }
                transformSupport.mouseReleased();
                break;
        }
    }

    @Override
    public boolean mouseClicked(MouseEvent e, ImageComponent ic) {
        return false;
    }

    @Override
    public void paintOverImage(Graphics2D g2, Canvas canvas, ImageComponent callingIC, AffineTransform unscaledTransform) {
        if (ended) {
            return;
        }
        if (callingIC != ImageComponents.getActiveImageComponent()) {
            return;
        }
        Rectangle cropRectangle = getCropRectangle(callingIC);
        if (cropRectangle == null) {
            return;
        }

        // paint the semi-transparent dark area outside the crop rectangle
        Shape previousClip = g2.getClip();  // save for later use

        Rectangle canvasBounds = canvas.getBounds();
        // We are here in image space because g2 has the transforms applied.
        // We are overriding the clip of g2, therefore we must manually
        // make sure that we don't paint anything outside the internal frame.
        // canvas.getBounds() is not reliable because the internal frame might be smaller
        // so we have to intersect with the view rectangle...
        Rectangle componentSpaceViewRectangle = callingIC.getViewRectangle();
        // ...but first get this to image space...
        Rectangle imageSpaceViewRectangle = callingIC.fromComponentToImageSpace(componentSpaceViewRectangle);
        // ... and now we can intersect
        canvasBounds = canvasBounds.intersection(imageSpaceViewRectangle);
        Path2D darkAreaClip = new Path2D.Double(Path2D.WIND_EVEN_ODD);
        darkAreaClip.append(canvasBounds, false);
        darkAreaClip.append(cropRectangle, false);
        g2.setClip(darkAreaClip);

        Color previousColor = g2.getColor();
        g2.setColor(Color.BLACK);

        Composite previousComposite = g2.getComposite();
        g2.setComposite(hideComposite);

        g2.fill(canvasBounds);

        g2.setColor(previousColor);
        g2.setComposite(previousComposite);

        if (state == CropToolState.TRANSFORM) {
            // Paint the handles.
            // The zooming is temporarily reset because the transformSupport works in component space
            AffineTransform scaledTransform = g2.getTransform();
            g2.setTransform(unscaledTransform);
            // prevents drawing outside the InternalImageFrame/ImageComponent
            // it is important to call this AFTER setting the unscaled transform
            g2.setClip(componentSpaceViewRectangle);
            transformSupport.paintHandles(g2);
            g2.setTransform(scaledTransform);
        }

        g2.setClip(previousClip);
    }

    /**
     * Returns the crop rectangle in image space
     * @param zoomLevel
     */
    public Rectangle getCropRectangle(ImageComponent ic) {
        switch (state) {
            case INITIAL:
                lastCropRectangle = null;
                break;
            case USERDRAG:
                lastCropRectangle = userDrag.createPositiveRectangle();
                break;
            case TRANSFORM:
                lastCropRectangle = transformSupport.getImageSpaceRectangle(ic);
                break;
        }

        return lastCropRectangle;
    }

    @Override
    protected void toolEnded() {
        super.toolEnded();
        resetStateToInitial();
    }

    @Override
    public void noOpenImageAnymore() {
        resetStateToInitial();
    }

    @Override
    public void newImageOpened() {
        resetStateToInitial();
    }

    @Override
    public void activeImageHasChanged(ImageComponent oldIC, ImageComponent newIC) {
        oldIC.repaint();
        resetStateToInitial();
    }

    @Override
    public void transformToolChangeHappened() {
        // TODO is this necessary?
    }

    public void resetStateToInitial() {
        ended = true;
        transformSupport = null;
        state = CropToolState.INITIAL;
        cancelButton.setEnabled(false);
        cropButton.setEnabled(false);

        ImageComponents.repaintActive();
    }

    public void imageComponentResized(ImageComponent ic) {
        if(transformSupport != null && lastCropRectangle != null && state == CropToolState.TRANSFORM) {
            transformSupport.setComponentSpaceRect(ic.fromImageToComponentSpace(lastCropRectangle));
        }
        ic.repaint();
    }
}