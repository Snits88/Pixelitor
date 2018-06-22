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

package pixelitor.filters.jhlabsproxies;

import com.jhlabs.image.VariableBlurFilter;
import pixelitor.filters.ParametrizedFilter;
import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.GroupedRangeParam;
import pixelitor.filters.gui.ImagePositionParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.ShowOriginal;
import pixelitor.utils.BlurredShape;
import pixelitor.utils.ImageUtils;

import java.awt.image.BufferedImage;

/**
 * Focus filter based on the JHLabs VariableBlurFilter
 */
public class JHFocus extends ParametrizedFilter {
    public static final String NAME = "Focus";

    private final ImagePositionParam center = new ImagePositionParam("Focused Area Center");
    private final GroupedRangeParam radius = new GroupedRangeParam("Focused Area Radius (Pixels)", 1, 200, 1000, false);
    private final RangeParam softness = new RangeParam("Transition Softness", 0, 20, 99);
    private final GroupedRangeParam blurRadius = new GroupedRangeParam("Blur Radius", 0, 10, 50);
    private final RangeParam numberOfIterations = new RangeParam("Blur Iterations (Quality)", 1, 3, 10);
    private final BooleanParam invert = new BooleanParam("Invert", false);
    private final BooleanParam hpSharpening = BooleanParam.forHPSharpening();
    private final IntChoiceParam shape = BlurredShape.getChoices();

    private FocusImpl filter;

    public JHFocus() {
        super(ShowOriginal.YES);

        setParamSet(new ParamSet(
                center,
                radius,
                softness,
                shape,
                blurRadius,
                numberOfIterations,
                invert,
                hpSharpening
        ));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        int hRadius = blurRadius.getValue(0);
        int vRadius = blurRadius.getValue(1);
        if ((hRadius == 0) && (vRadius == 0)) {
            return src;
        }

        // TODO copied from JHBoxBlur, but is it necessary?
        if ((src.getWidth() == 1) || (src.getHeight() == 1)) {
            // otherwise we get ArrayIndexOutOfBoundsException in BoxBlurFilter
            return src;
        }

        if (filter == null) {
            filter = new FocusImpl(NAME);
        }

        filter.setCenter(
                src.getWidth() * center.getRelativeX(),
                src.getHeight() * center.getRelativeY()
        );

        double radiusX = radius.getValueAsDouble(0);
        double radiusY = radius.getValueAsDouble(1);
        double softnessFactor = softness.getValueAsDouble() / 100.0;
        filter.setRadius(radiusX, radiusY, softnessFactor);

        filter.setInverted(invert.isChecked());

        // TODO unlike BoxBlurFilter, VariableBlurFilter supports only integer radii
        filter.setHRadius(blurRadius.getValueAsFloat(0));
        filter.setVRadius(blurRadius.getValueAsFloat(1));

        filter.setIterations(numberOfIterations.getValue());
        filter.setPremultiplyAlpha(false);
        filter.setShape(shape.getValue());

        dest = filter.filter(src, dest);

        if (hpSharpening.isChecked()) {
            dest = ImageUtils.getHighPassSharpenedImage(src, dest);
        }

        return dest;
    }

    private static class FocusImpl extends VariableBlurFilter {
        private double cx;
        private double cy;
        private double innerRadiusX;
        private double innerRadiusY;
        private double outerRadiusX;
        private double outerRadiusY;
        private boolean inverted;

        private BlurredShape shape;

        public FocusImpl(String filterName) {
            super(filterName);
        }

        public void setCenter(double cx, double cy) {
            this.cx = cx;
            this.cy = cy;
        }

        public void setRadius(double radiusX, double radiusY, double softness) {
            this.innerRadiusX = radiusX - radiusX * softness;
            this.innerRadiusY = radiusY - radiusY * softness;

            this.outerRadiusX = radiusX + radiusX * softness;
            this.outerRadiusY = radiusY + radiusY * softness;
        }

        @Override
        protected float blurRadiusAt(int x, int y) {
            double outside = shape.isOutside(x, y);
            if (inverted) {
                return (float) (1 - outside);
            }
            return (float) outside;
        }

        public void setInverted(boolean inverted) {
            this.inverted = inverted;
        }

        // must be called after the shape arguments!
        public void setShape(int type) {
            shape = BlurredShape.create(type, cx, cy,
                    innerRadiusX, innerRadiusY,
                    outerRadiusX, outerRadiusY);
        }
    }
}