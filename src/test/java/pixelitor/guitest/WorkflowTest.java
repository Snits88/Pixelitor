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

package pixelitor.guitest;

import org.assertj.swing.edt.FailOnThreadViolationRepaintManager;
import org.assertj.swing.fixture.FrameFixture;
import pixelitor.Composition;
import pixelitor.colors.FgBgColorSelector;
import pixelitor.filters.gui.ShowOriginal;
import pixelitor.guitest.AppRunner.Randomize;
import pixelitor.guitest.AppRunner.Reseed;
import pixelitor.tools.BrushType;
import pixelitor.tools.Tools;
import pixelitor.tools.shapes.ShapeType;
import pixelitor.tools.shapes.StrokeType;
import pixelitor.tools.shapes.TwoPointPaintType;
import pixelitor.utils.Utils;

import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static pixelitor.assertions.PixelitorAssertions.assertThat;
import static pixelitor.selection.SelectionModifyType.EXPAND;
import static pixelitor.tools.move.MoveMode.MOVE_SELECTION_ONLY;
import static pixelitor.tools.shapes.ShapesToolState.NO_INTERACTION;

/**
 * Assertj-Swing test focused on a longer workflow
 * rather than individual functionality.
 * Not a unit test.
 */
public class WorkflowTest {
    private final AppRunner app;
    private final Mouse mouse;
    private final FrameFixture pw;
    private final Keyboard keyboard;

    private static final int INITIAL_WIDTH = 700;
    private static final int INITIAL_HEIGHT = 500;
    private static final int EXTRA_HEIGHT = 20;
    private static final int EXTRA_WIDTH = 50;

    public static void main(String[] args) {
        Utils.makeSureAssertionsAreEnabled();
        FailOnThreadViolationRepaintManager.install();

        new WorkflowTest();
    }

    private WorkflowTest() {
        app = new AppRunner(null);
        mouse = app.getMouse();
        pw = app.getPW();
        keyboard = app.getKeyboard();

        createNewImage();
        renderWood();
        addTextLayer();
        setTextSize();
        selectionFromText();
        deleteTextLayer();
        rotate90();
        invertSelection();
        deselect();
        rotate270();
        drawTransparentZigzagRectangle();
        enlargeCanvas();
        addLayerBellow();
        renderCaustics();
        addDropShadowToTheWoodLayer();
        mergeDown();
        createEllipseSelection();
        expandSelection();
        selectionToPath();
        tracePath(BrushType.WOBBLE);
        pathToSelection();
        copySelection();
        moveSelection(-100);
        pasteSelection();
        moveSelection(50);
        selectionToPath();
        tracePath(BrushType.SHAPE);
    }

    private void createNewImage() {
        app.runMenuCommand("New Image...");
        var dialog = app.findDialogByTitle("New Image");
        dialog.textBox("widthTF").deleteText().enterText(String.valueOf(INITIAL_WIDTH));
        dialog.textBox("heightTF").deleteText().enterText(String.valueOf(INITIAL_HEIGHT));
        dialog.button("ok").click();
        Utils.sleep(1, TimeUnit.SECONDS);
        mouse.recalcCanvasBounds();
    }

    private void renderWood() {
        app.runFilterWithDialog("Wood", Randomize.NO, Reseed.NO, ShowOriginal.NO);
        keyboard.undoRedo("Wood");
    }

    private void addTextLayer() {
        pw.button("addTextLayer").click();

        var dialog = app.findDialogByTitle("Create Text Layer");
        dialog.textBox("textTF")
                .requireText("Pixelitor")
                .deleteText()
                .enterText("Wood");
        dialog.button("ok").click();

        keyboard.undoRedo("Add Text Layer");
    }

    private void setTextSize() {
        app.runMenuCommand("Edit...");

        var dialog = app.findDialogByTitle("Edit Text Layer");
        dialog.textBox("textTF").requireText("Wood");
        dialog.slider("fontSize").slideTo(200);
        dialog.button("ok").click();

        keyboard.undoRedo("Edit Text Layer");
    }

    private void selectionFromText() {
        EDT.assertThereIsNoSelection();

        app.runMenuCommand("Selection from Text");
        EDT.assertThereIsSelection();

        keyboard.undo("Create Selection");
        EDT.assertThereIsNoSelection();

        keyboard.redo("Create Selection");
        EDT.assertThereIsSelection();
    }

    private void deleteTextLayer() {
        EDT.assertNumLayersIs(2);

        pw.button("deleteLayer").click();
        EDT.assertNumLayersIs(1);

        keyboard.undo("Delete Layer");
        EDT.assertNumLayersIs(2);

        keyboard.redo("Delete Layer");
        EDT.assertNumLayersIs(1);
    }

    private void rotate90() {
        app.runMenuCommand("Rotate 90° CW");
        keyboard.undoRedo("Rotate 90° CW");
    }

    private void invertSelection() {
        app.runMenuCommand("Invert");
        keyboard.undoRedo("Invert");
    }

    private void deselect() {
        app.runMenuCommand("Deselect");
        keyboard.undoRedo("Deselect");
    }

    private void rotate270() {
        app.runMenuCommand("Rotate 90° CCW");
        keyboard.undoRedo("Rotate 90° CCW");
    }

    private void drawTransparentZigzagRectangle() {
        app.clickTool(Tools.SHAPES);

        app.runMenuCommand("100%");
        mouse.recalcCanvasBounds();

        pw.comboBox("shapeTypeCB").selectItem(ShapeType.RECTANGLE.toString());
        pw.comboBox("fillPaintCB").selectItem(TwoPointPaintType.NONE.toString());
        pw.comboBox("strokePaintCB").selectItem(TwoPointPaintType.TRANSPARENT.toString());
        EDT.assertShapesToolStateIs(NO_INTERACTION);
        pw.button("convertToSelection").requireDisabled();

        AppRunner.findButtonByText(pw, "Stroke Settings...")
                .requireEnabled()
                .click();
        var dialog = app.findDialogByTitle("Stroke Settings");
        dialog.slider().slideTo(10);
        dialog.comboBox("strokeType").selectItem(StrokeType.ZIGZAG.toString());
        dialog.button("ok").click();
        dialog.requireNotVisible();

        int margin = 25;
        mouse.moveToCanvas(margin, margin);
        mouse.dragToCanvas(INITIAL_WIDTH - margin, INITIAL_HEIGHT - margin);

        keyboard.undoRedo("Create Shape");
        keyboard.pressEsc();
    }

    private void enlargeCanvas() {
        app.enlargeCanvas(EXTRA_HEIGHT, EXTRA_WIDTH, EXTRA_WIDTH, EXTRA_HEIGHT);
        keyboard.undoRedo("Enlarge Canvas");
    }

    private void addLayerBellow() {
        EDT.assertNumLayersIs(1);

        keyboard.pressCtrl();
        pw.button("addLayer").click();
        keyboard.releaseCtrl();

        EDT.assertNumLayersIs(2);
    }

    private void renderCaustics() {
        app.runFilterWithDialog("Caustics", Randomize.NO, Reseed.NO, ShowOriginal.NO);
        keyboard.undoRedo("Caustics");
    }

    private void addDropShadowToTheWoodLayer() {
        app.runMenuCommand("Raise Layer Selection");
        keyboard.undoRedo("Raise Layer Selection");

        app.runFilterWithDialog("Drop Shadow", Randomize.NO, Reseed.NO, ShowOriginal.NO);
        keyboard.undoRedo("Drop Shadow");
    }

    private void mergeDown() {
        app.runMenuCommand("Merge Down");
        keyboard.undoRedo("Merge Down");
    }

    private void createEllipseSelection() {
        app.clickTool(Tools.SELECTION);
        pw.comboBox("typeCB").selectItem("Ellipse");
        pw.button("toPathButton").requireDisabled();

        int canvasWidth = INITIAL_WIDTH + 2 * EXTRA_WIDTH;
        int canvasHeight = INITIAL_HEIGHT + 2 * EXTRA_HEIGHT;
        assertThat(EDT.active(Composition::getCanvasImWidth)).isEqualTo(canvasWidth);
        assertThat(EDT.active(Composition::getCanvasImHeight)).isEqualTo(canvasHeight);

        mouse.recalcCanvasBounds();
        int margin = 100;
        mouse.moveToCanvas(margin, margin);
        mouse.dragToCanvas(canvasWidth - margin, canvasHeight - margin);
    }

    private void expandSelection() {
        app.runModifySelection(50, EXPAND, 3);
        keyboard.undoRedo("Modify Selection");
    }

    private void selectionToPath() {
        app.clickTool(Tools.SELECTION);
        pw.button("toPathButton")
                .requireEnabled()
                .click();
        Utils.sleep(200, MILLISECONDS);
        EDT.assertActiveToolIs(Tools.PEN);

        keyboard.undoRedo("Convert Selection to Path");
    }

    private void tracePath(BrushType brushType) {
        app.clickTool(Tools.BRUSH);
        pw.comboBox("typeCB").selectItem(brushType.toString());
        pw.button(FgBgColorSelector.RESET_DEF_COLORS_BUTTON_NAME).click();

        app.clickTool(Tools.PEN);
        pw.button("toSelectionButton").requireEnabled();

        AppRunner.findButtonByText(pw, "Stroke with Current Brush")
                .requireEnabled()
                .click();
        keyboard.undoRedo("Brush");
    }

    private void pathToSelection() {
        pw.button("toSelectionButton")
                .requireEnabled()
                .click();
        keyboard.undoRedo("Convert Path to Selection");
    }

    private void copySelection() {
        app.runMenuCommand("Copy Selection");
    }

    private void moveSelection(int dy) {
        app.clickTool(Tools.MOVE);
        pw.comboBox("modeSelector").selectItem(MOVE_SELECTION_ONLY.toString());
        mouse.moveToCanvas(400, 400);
        mouse.dragToCanvas(400, 400 + dy);
    }

    private void pasteSelection() {
        app.runMenuCommand("Paste Selection");
        var dialog = app.findDialogByTitle("Existing Selection");
        AppRunner.findButtonByText(dialog, "Intersect").click();
    }
}
