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

package pixelitor.tools.pen;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import pixelitor.Build;
import pixelitor.TestHelper;
import pixelitor.gui.View;
import pixelitor.history.History;
import pixelitor.utils.Shapes;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;

import static pixelitor.assertions.PixelitorAssertions.assertThat;

public class PathTest {
    private View view;

    @BeforeClass
    public static void setupClass() {
        Build.setUnitTestingMode();
    }

    @Before
    public void setUp() {
        var comp = TestHelper.createMockComposition();
        view = comp.getView();
    }

    @Test
    public void testDeletingSubPathPoints() {
        var shape = new Rectangle(10, 10, 100, 100);
        Path path = Shapes.shapeToPath(shape, view);
        SubPath sp = path.getActiveSubpath();
        assertThat(sp).numAnchorsIs(4);
        sp.getAnchor(3).delete(); // delete last
        sp.getAnchor(2).delete(); // delete last
        sp.getAnchor(0).delete(); // delete first
        sp.getAnchor(0).delete(); // delete first (and last)
        assertThat(path.getActiveSubpath()).numAnchorsIs(0);

        History.undo("Delete Anchor Point");
        History.undo("Delete Anchor Point");
        // check the active subpath because the undo replaced
        // the subpath reference in the path
        assertThat(path.getActiveSubpath()).numAnchorsIs(2);

        History.redo("Delete Anchor Point");
        History.redo("Delete Anchor Point");
        assertThat(path.getActiveSubpath()).numAnchorsIs(0);
    }

    @Test
    public void testConversionsForRectangle() {
        testConversionsFor(
                new Rectangle(20, 20, 40, 10));
    }

    @Test
    public void testConversionsForEllipse() {
        testConversionsFor(
                new Ellipse2D.Double(20, 20, 40, 10));
    }

    @Test
    public void testTransform() {
        Rectangle shape = new Rectangle(10, 10, 100, 100);
        Path path = Shapes.shapeToPath(shape, view);
        SubPath sp = path.getActiveSubpath();
        assertThat(sp).firstAnchorIsAt(10, 10);

        sp.storeTransformRefPoints(); // the ref point for the first anchor is 10, 10

        var at = AffineTransform.getTranslateInstance(20, 10);
        sp.refTransform(at);
        assertThat(sp).firstAnchorIsAt(30, 20);

        at = AffineTransform.getTranslateInstance(10, 20);
        sp.refTransform(at);
        assertThat(sp).firstAnchorIsAt(20, 30);

        sp.transform(at); // absolute transform
        assertThat(sp).firstAnchorIsAt(30, 50);
    }

    private void testConversionsFor(Shape shape) {
        Path path = Shapes.shapeToPath(shape, view);
        Path copy = path.deepCopy(view.getComp());
        Shape convertedShape = copy.toImageSpaceShape();
        assertThat(Shapes.pathIteratorIsEqual(shape, convertedShape, 0.01)).isTrue();
    }
}