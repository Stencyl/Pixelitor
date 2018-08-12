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

package pixelitor.tools.brushes;

import pixelitor.tools.util.PPoint;
import pixelitor.utils.debug.DebugNode;

/**
 * An abstract superclass for brushes that work by putting down dabs
 */
public abstract class DabsBrush extends AbstractBrush {
    private final SpacingStrategy spacingStrategy;
    protected DabsBrushSettings settings;
    private final DabsStrategy dabsStrategy;

    protected DabsBrush(int radius, SpacingStrategy spacingStrategy,
                        AngleSettings angleSettings, boolean refreshBrushForEachDab) {
        super(radius);
        this.spacingStrategy = spacingStrategy;
        settings = new DabsBrushSettings(angleSettings, spacingStrategy);
        dabsStrategy = new LinearDabsStrategy(this,
                spacingStrategy,
                angleSettings,
                refreshBrushForEachDab);
        settings.registerBrush(this);
    }

    protected DabsBrush(int radius, DabsBrushSettings settings,
                        boolean refreshBrushForEachDab) {
        super(radius);
        this.settings = settings;
        this.spacingStrategy = settings.getSpacingStrategy();
        dabsStrategy = new LinearDabsStrategy(this,
                spacingStrategy,
                settings.getAngleSettings(),
                refreshBrushForEachDab);
        settings.registerBrush(this);
    }

    /**
     * Sets up the brush stamp. Depending on the type of brush, it can be
     * called at the beginning of a stroke or before each dab.
     */
    abstract void setupBrushStamp(PPoint p);

    public abstract void putDab(PPoint p, double theta);

    @Override
    public void onStrokeStart(PPoint p) {
        super.onStrokeStart(p);
        dabsStrategy.onStrokeStart(p);
        updateComp(p);
    }

    @Override
    public void onNewStrokePoint(PPoint p) {
        dabsStrategy.onNewStrokePoint(p);
        updateComp(p);
        rememberPrevious(p);
    }

    public DabsBrushSettings getSettings() {
        return settings;
    }

    public void setSettings(DabsBrushSettings settings) {
        this.settings = settings;
    }

    public void settingsChanged() {
        dabsStrategy.settingsChanged();
    }

    @Override
    public void dispose() {
        settings.unregisterBrush(this);
    }

    @Override
    public DebugNode getDebugNode() {
        DebugNode node = super.getDebugNode();

        node.addBoolean("Angle Aware", settings.isAngleAware());

        AngleSettings angleSettings = settings.getAngleSettings();
        node.addBoolean("Jitter Aware", angleSettings.shouldJitterAngle());

        node.addDouble("Spacing", spacingStrategy.getSpacing(radius));

        return node;
    }

    @Override
    public double getPreferredSpacing() {
        return spacingStrategy.getSpacing(radius);
    }
}
