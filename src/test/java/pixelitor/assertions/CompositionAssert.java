/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.assertions;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.util.Objects;
import pixelitor.Composition;
import pixelitor.layers.ContentLayer;
import pixelitor.layers.ImageLayer;
import pixelitor.layers.Layer;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import static org.junit.Assert.assertEquals;
import static pixelitor.assertions.PixelitorAssertions.assertThat;

/**
 * Custom AssertJ assertions for {@link Composition} objects.
 * Based partially on the code generated by CustomAssertionGenerator.
 */
public class CompositionAssert extends AbstractAssert<CompositionAssert, Composition> {
    /**
     * Creates a new <code>{@link CompositionAssert}</code> to make assertions on actual Composition.
     *
     * @param actual the Composition we want to make assertions on.
     */
    public CompositionAssert(Composition actual) {
        super(actual, CompositionAssert.class);
    }

    /**
     * Verifies that the actual Composition is empty.
     *
     * @return this assertion object.
     * @throws AssertionError - if the actual Composition is not empty.
     */
    public CompositionAssert isEmpty() {
        isNotNull();

        // check that property call/field access is true
        if (!actual.isEmpty()) {
            failWithMessage("\nExpecting that actual Composition is empty but is not.");
        }

        return this;
    }

    /**
     * Verifies that the actual Composition is not empty.
     *
     * @return this assertion object.
     * @throws AssertionError - if the actual Composition is empty.
     */
    public CompositionAssert isNotEmpty() {
        isNotNull();

        // check that property call/field access is false
        if (actual.isEmpty()) {
            failWithMessage("\nExpecting that actual Composition is not empty but is.");
        }

        return this;
    }

    /**
     * Verifies that the actual Composition is dirty.
     *
     * @return this assertion object.
     * @throws AssertionError - if the actual Composition is not dirty.
     */
    public CompositionAssert isDirty() {
        isNotNull();

        // check that property call/field access is true
        if (!actual.isDirty()) {
            failWithMessage("\nExpecting that actual Composition is dirty but is not.");
        }

        return this;
    }

    /**
     * Verifies that the actual Composition is not dirty.
     *
     * @return this assertion object.
     * @throws AssertionError - if the actual Composition is dirty.
     */
    public CompositionAssert isNotDirty() {
        isNotNull();

        // check that property call/field access is false
        if (actual.isDirty()) {
            failWithMessage("\nExpecting that actual Composition is not dirty but is.");
        }

        return this;
    }

    /**
     * Verifies that the actual Composition's number of layers is equal to the given one.
     *
     * @param numLayers the given number of layers to compare the actual Composition's number of layers to.
     * @return this assertion object.
     * @throws AssertionError - if the actual Composition's number of layers is not equal to the given one.
     */
    public CompositionAssert numLayersIs(int numLayers) {
        isNotNull();

        // overrides the default error message with a more explicit one
        String assertjErrorMessage = "\nExpecting number of layers of:\n  <%s>\nto be:\n  <%s>\nbut was:\n  <%s>";

        // check
        int actualNumLayers = actual.getNumLayers();
        if (actualNumLayers != numLayers) {
            failWithMessage(assertjErrorMessage, actual, numLayers, actualNumLayers);
        }

        return this;
    }

    public CompositionAssert layerNamesAre(String... expected) {
        isNotNull();

        int expectedLayerCount = expected.length;
        if (expectedLayerCount != actual.getNumLayers()) {
            failWithMessage(String.format(
                    "\nFound %d layers instead of the expected %d.",
                    actual.getNumLayers(), expectedLayerCount));
        }
        for (int i = 0; i < expectedLayerCount; i++) {
            Layer layer = actual.getLayer(i);
            if (!layer.getName()
                    .equals(expected[i])) {
                failWithMessage(String.format(
                        "\nIn layer nr. %d the layer name was '%s', while expecting '%s'.",
                        i, layer.getName(), expected[i]));
            }
        }

        return this;
    }

    public CompositionAssert activeLayerNameIs(String expected) {
        isNotNull();

        assertThat(actual.getActiveLayer()
                .getName())
                .isEqualTo(expected);

        return this;
    }

    public CompositionAssert layerNHasMask(int n) {
        isNotNull();

        assertThat(actual.getLayer(n)
                .hasMask())
                .isTrue();

        return this;
    }

    public CompositionAssert firstLayerHasMask() {
        return layerNHasMask(0);
    }

    public CompositionAssert secondLayerHasMask() {
        return layerNHasMask(1);
    }

    /**
     * Verifies that the actual Composition's expected is equal to the given one.
     *
     * @param expected the given active layer index to compare the actual Composition's expected to.
     * @return this assertion object.
     * @throws AssertionError - if the actual Composition's active layer index is not equal to the given one.
     */
    public CompositionAssert activeLayerIndexIs(int expected) {
        isNotNull();

        // overrides the default error message with a more explicit one
        String assertjErrorMessage = "\nExpecting expected of:\n  <%s>\nto be:\n  <%s>\nbut was:\n  <%s>";

        // check
        int actualActiveLayerIndex = actual.getActiveLayerIndex();
        if (actualActiveLayerIndex != expected) {
            failWithMessage(assertjErrorMessage, actual, expected, actualActiveLayerIndex);
        }

        return this;
    }

    public CompositionAssert firstLayerIsActive() {
        return activeLayerIndexIs(0);
    }

    public CompositionAssert secondLayerIsActive() {
        return activeLayerIndexIs(1);
    }

    public CompositionAssert thirdLayerIsActive() {
        return activeLayerIndexIs(2);
    }

    /**
     * Verifies that the actual Composition's canvasWidth is equal to the given one.
     *
     * @param canvasWidth the given canvasWidth to compare the actual Composition's canvasWidth to.
     * @return this assertion object.
     * @throws AssertionError - if the actual Composition's canvasWidth is not equal to the given one.
     */
    public CompositionAssert hasCanvasWidth(int canvasWidth) {
        isNotNull();

        // overrides the default error message with a more explicit one
        String assertjErrorMessage = "\nExpecting canvasWidth of:\n  <%s>\nto be:\n  <%s>\nbut was:\n  <%s>";

        // check
        int actualCanvasWidth = actual.getCanvasWidth();
        if (actualCanvasWidth != canvasWidth) {
            failWithMessage(assertjErrorMessage, actual, canvasWidth, actualCanvasWidth);
        }

        return this;
    }

    /**
     * Verifies that the actual Composition's canvasHeight is equal to the given one.
     *
     * @param canvasHeight the given canvasHeight to compare the actual Composition's canvasHeight to.
     * @return this assertion object.
     * @throws AssertionError - if the actual Composition's canvasHeight is not equal to the given one.
     */
    public CompositionAssert hasCanvasHeight(int canvasHeight) {
        isNotNull();

        // overrides the default error message with a more explicit one
        String assertjErrorMessage = "\nExpecting canvasHeight of:\n  <%s>\nto be:\n  <%s>\nbut was:\n  <%s>";

        // check
        int actualCanvasHeight = actual.getCanvasHeight();
        if (actualCanvasHeight != canvasHeight) {
            failWithMessage(assertjErrorMessage, actual, canvasHeight, actualCanvasHeight);
        }

        return this;
    }

    public CompositionAssert canvasSizeIs(int w, int h) {
        isNotNull();

        hasCanvasWidth(w);
        hasCanvasHeight(h);

        return this;
    }

    /**
     * Verifies that the actual Composition's name is equal to the given one.
     *
     * @param name the given name to compare the actual Composition's name to.
     * @return this assertion object.
     * @throws AssertionError - if the actual Composition's name is not equal to the given one.
     */
    public CompositionAssert hasName(String name) {
        isNotNull();

        // overrides the default error message with a more explicit one
        String assertjErrorMessage = "\nExpecting name of:\n  <%s>\nto be:\n  <%s>\nbut was:\n  <%s>";

        // null safe check
        String actualName = actual.getName();
        if (!Objects.areEqual(actualName, name)) {
            failWithMessage(assertjErrorMessage, actual, name, actualName);
        }

        return this;
    }

    /**
     * Verifies that the actual Composition has selection.
     *
     * @return this assertion object.
     * @throws AssertionError - if the actual Composition does not have selection.
     */
    public CompositionAssert hasSelection() {
        isNotNull();

        // check that property call/field access is true
        if (!actual.hasSelection()) {
            failWithMessage("\nExpecting that actual Composition has selection but does not have.");
        }

        return this;
    }

    /**
     * Verifies that the actual Composition does not have selection.
     *
     * @return this assertion object.
     * @throws AssertionError - if the actual Composition has selection.
     */
    public CompositionAssert doesNotHaveSelection() {
        isNotNull();

        // check that property call/field access is false
        if (actual.hasSelection()) {
            failWithMessage("\nExpecting that actual Composition does not have selection but has.");
        }

        return this;
    }

    public CompositionAssert activeLayerTranslationIs(int tx, int ty) {
        isNotNull();

        ContentLayer layer = (ContentLayer) actual.getActiveLayer();
        assertEquals("tx", tx, layer.getTX());
        assertEquals("ty", ty, layer.getTY());

        return this;
    }

    public CompositionAssert activeLayerAndMaskImageSizeIs(int w, int h) {
        isNotNull();

        ImageLayer layer = (ImageLayer) actual.getActiveLayer();
        BufferedImage image = layer.getImage();
        assertEquals("width", w, image.getWidth());
        assertEquals("height", h, image.getHeight());

        if (layer.hasMask()) {
            BufferedImage maskImage = layer.getMask()
                    .getImage();
            assertEquals("mask width", w, maskImage.getWidth());
            assertEquals("mask height", h, maskImage.getHeight());
        }

        return this;
    }

    public CompositionAssert selectionBoundsIs(Rectangle rect) {
        isNotNull();

        Rectangle bounds = actual.getSelection()
                .getShapeBounds();
        assertEquals("selection bounds", rect, bounds);

        return this;
    }

    public CompositionAssert invariantIsOK() {
        isNotNull();

        actual.checkInvariant();

        return this;
    }
}
