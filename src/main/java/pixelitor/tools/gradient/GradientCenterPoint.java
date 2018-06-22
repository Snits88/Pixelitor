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

package pixelitor.tools.gradient;

import pixelitor.gui.ImageComponent;
import pixelitor.tools.DraggablePoint;
import pixelitor.utils.Utils;

import java.awt.Color;
import java.awt.geom.Point2D;

/**
 * The point at the half distance between the gradient
 * start point and the gradient end point.
 */
public class GradientCenterPoint extends DraggablePoint {
    private final GradientDefiningPoint start;
    private final GradientDefiningPoint end;

    public GradientCenterPoint(GradientDefiningPoint start, GradientDefiningPoint end, ImageComponent ic, Color color, Color activeColor) {
        super("center", (start.x + end.x) / 2, (start.y + end.y) / 2, ic, color, activeColor);
        this.start = start;
        this.end = end;
    }

    @Override
    public void setLocation(double x, double y) {
        double oldX = this.x;
        double oldY = this.y;
        super.setLocation(x, y);

        double dx = x - oldX;
        double dy = y - oldY;

        // also move the start and end points
        start.translateOnlyThis(dx, dy);
        end.translateOnlyThis(dx, dy);
    }

    public void setLocationWithoutMovingChildren(double cx, double cy) {
        super.setLocation(cx, cy);
    }

    @Override
    public void setConstrainedLocation(double mouseX, double mouseY) {
        // constrain it relative to its former position
        Point2D p = Utils.constrainEndPoint(dragStartX, dragStartY, mouseX, mouseY);
        setLocation(p.getX(), p.getY());
    }

    @Override
    protected void afterMouseReleasedActions() {
        calcImCoords();
        start.calcImCoords();
        end.calcImCoords();
    }
}
