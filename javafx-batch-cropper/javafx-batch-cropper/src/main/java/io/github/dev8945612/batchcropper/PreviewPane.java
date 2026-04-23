package io.github.dev8945612.batchcropper;

import javafx.geometry.Bounds;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

public final class PreviewPane extends Pane {
    private static final double HANDLE_TOLERANCE = 8.0;

    private final ImageView imageView = new ImageView();
    private final Canvas overlay = new Canvas();

    private GridModel gridModel;
    private CropShape cellShape = CropShape.RECTANGLE;
    private DragTarget hoverTarget = DragTarget.none();
    private DragTarget activeTarget = DragTarget.none();
    private Runnable onGridChanged;

    public PreviewPane() {
        getStyleClass().add("preview-pane");
        imageView.setPreserveRatio(false);
        imageView.setSmooth(true);
        imageView.setCache(true);
        getChildren().addAll(imageView, overlay);

        widthProperty().addListener((obs, oldV, newV) -> requestLayout());
        heightProperty().addListener((obs, oldV, newV) -> requestLayout());

        overlay.addEventHandler(MouseEvent.MOUSE_MOVED, this::handleMouseMoved);
        overlay.addEventHandler(MouseEvent.MOUSE_PRESSED, this::handleMousePressed);
        overlay.addEventHandler(MouseEvent.MOUSE_DRAGGED, this::handleMouseDragged);
        overlay.addEventHandler(MouseEvent.MOUSE_RELEASED, event -> {
            activeTarget = DragTarget.none();
            updateCursor();
        });
        overlay.addEventHandler(MouseEvent.MOUSE_EXITED, event -> {
            if (!activeTarget.present()) {
                hoverTarget = DragTarget.none();
                updateCursor();
                redraw();
            }
        });
    }

    public void setOnGridChanged(Runnable onGridChanged) {
        this.onGridChanged = onGridChanged;
    }

    public void setImage(Image image) {
        imageView.setImage(image);
        requestLayout();
        redraw();
    }

    public void setGridModel(GridModel gridModel) {
        this.gridModel = gridModel;
        redraw();
    }

    public void setCellShape(CropShape shape) {
        this.cellShape = shape;
        redraw();
    }

    @Override
    protected void layoutChildren() {
        double w = getWidth();
        double h = getHeight();
        overlay.setWidth(w);
        overlay.setHeight(h);
        overlay.relocate(0, 0);

        Image img = imageView.getImage();
        if (img != null && img.getWidth() > 0 && img.getHeight() > 0) {
            double imgRatio = img.getWidth() / img.getHeight();
            double containerRatio = w / h;
            double renderW, renderH, offX, offY;
            if (imgRatio > containerRatio) {
                renderW = w;
                renderH = w / imgRatio;
                offX = 0;
                offY = (h - renderH) / 2.0;
            } else {
                renderH = h;
                renderW = h * imgRatio;
                offX = (w - renderW) / 2.0;
                offY = 0;
            }
            imageView.setFitWidth(renderW);
            imageView.setFitHeight(renderH);
            imageView.relocate(offX, offY);
        } else {
            imageView.setFitWidth(w);
            imageView.setFitHeight(h);
            imageView.relocate(0, 0);
        }
        redraw();
    }

    private void handleMouseMoved(MouseEvent event) {
        hoverTarget = locateTarget(event.getX(), event.getY());
        updateCursor();
        redraw();
    }

    private void handleMousePressed(MouseEvent event) {
        activeTarget = locateTarget(event.getX(), event.getY());
        updateCursor();
        redraw();
    }

    private void handleMouseDragged(MouseEvent event) {
        if (!activeTarget.present() || gridModel == null) {
            return;
        }
        Bounds bounds = getDisplayedImageBounds();
        if (bounds == null || bounds.getWidth() <= 0 || bounds.getHeight() <= 0) {
            return;
        }

        if (activeTarget.orientation == Orientation.VERTICAL) {
            double normalized = (event.getX() - bounds.getMinX()) / bounds.getWidth();
            gridModel.moveVerticalLine(activeTarget.index, normalized);
        } else if (activeTarget.orientation == Orientation.HORIZONTAL) {
            double normalized = (event.getY() - bounds.getMinY()) / bounds.getHeight();
            gridModel.moveHorizontalLine(activeTarget.index, normalized);
        }

        if (onGridChanged != null) {
            onGridChanged.run();
        }
        redraw();
    }

    private DragTarget locateTarget(double x, double y) {
        if (gridModel == null) {
            return DragTarget.none();
        }
        Bounds bounds = getDisplayedImageBounds();
        if (bounds == null) {
            return DragTarget.none();
        }
        if (!bounds.contains(x, y)) {
            return DragTarget.none();
        }

        double[] verticals = gridModel.getXLines();
        double[] horizontals = gridModel.getYLines();

        DragTarget best = DragTarget.none();
        double bestDistance = Double.MAX_VALUE;

        for (int i = 0; i < verticals.length; i++) {
            double px = bounds.getMinX() + verticals[i] * bounds.getWidth();
            double distance = Math.abs(x - px);
            if (distance <= HANDLE_TOLERANCE && distance < bestDistance) {
                bestDistance = distance;
                best = new DragTarget(Orientation.VERTICAL, i);
            }
        }

        for (int i = 0; i < horizontals.length; i++) {
            double py = bounds.getMinY() + horizontals[i] * bounds.getHeight();
            double distance = Math.abs(y - py);
            if (distance <= HANDLE_TOLERANCE && distance < bestDistance) {
                bestDistance = distance;
                best = new DragTarget(Orientation.HORIZONTAL, i);
            }
        }

        return best;
    }

    private Bounds getDisplayedImageBounds() {
        Image image = imageView.getImage();
        if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
            return null;
        }
        double fitW = imageView.getFitWidth();
        double fitH = imageView.getFitHeight();
        if (fitW <= 0 || fitH <= 0) {
            return null;
        }
        return new javafx.geometry.BoundingBox(
                imageView.getLayoutX(), imageView.getLayoutY(), fitW, fitH);
    }

    private void updateCursor() {
        DragTarget target = activeTarget.present() ? activeTarget : hoverTarget;
        if (target.orientation == Orientation.VERTICAL) {
            setCursor(Cursor.H_RESIZE);
        } else if (target.orientation == Orientation.HORIZONTAL) {
            setCursor(Cursor.V_RESIZE);
        } else {
            setCursor(Cursor.DEFAULT);
        }
    }

    private void redraw() {
        GraphicsContext gc = overlay.getGraphicsContext2D();
        gc.clearRect(0, 0, overlay.getWidth(), overlay.getHeight());

        Bounds bounds = getDisplayedImageBounds();
        if (gridModel == null || bounds == null) {
            drawEmptyState(gc);
            return;
        }

        double minX = bounds.getMinX();
        double minY = bounds.getMinY();
        double width = bounds.getWidth();
        double height = bounds.getHeight();

        gc.setFill(Color.rgb(7, 11, 20, 0.55));
        gc.fillRect(0, 0, overlay.getWidth(), minY);
        gc.fillRect(0, minY, minX, height);
        gc.fillRect(minX + width, minY, overlay.getWidth() - (minX + width), height);
        gc.fillRect(0, minY + height, overlay.getWidth(), overlay.getHeight() - (minY + height));

        double[] xLines = gridModel.getXLines();
        double[] yLines = gridModel.getYLines();

        // Shadow pass for all lines
        gc.setLineCap(StrokeLineCap.ROUND);
        gc.setStroke(Color.rgb(0, 0, 0, 0.65));
        gc.setLineWidth(4.5);
        for (double xLine : xLines) {
            double x = minX + xLine * width;
            gc.strokeLine(x, minY, x, minY + height);
        }
        for (double yLine : yLines) {
            double y = minY + yLine * height;
            gc.strokeLine(minX, y, minX + width, y);
        }
        gc.setLineWidth(5.0);
        gc.strokeRect(minX, minY, width, height);

        // Color pass for all lines
        gc.setStroke(Color.rgb(104, 182, 255, 1.0));
        gc.setLineWidth(2.5);
        for (int i = 0; i < xLines.length; i++) {
            double x = minX + xLines[i] * width;
            gc.strokeLine(x, minY, x, minY + height);
            drawHandle(gc, x, minY + height / 2.0, isActiveOrHover(Orientation.VERTICAL, i));
        }
        for (int i = 0; i < yLines.length; i++) {
            double y = minY + yLines[i] * height;
            gc.strokeLine(minX, y, minX + width, y);
            drawHandle(gc, minX + width / 2.0, y, isActiveOrHover(Orientation.HORIZONTAL, i));
        }

        gc.setStroke(Color.rgb(255, 255, 255, 0.95));
        gc.setLineWidth(2.5);
        gc.strokeRect(minX, minY, width, height);

        if (cellShape != CropShape.RECTANGLE) {
            double pad = 6;
            gc.setLineCap(StrokeLineCap.ROUND);
            // Shadow pass
            gc.setStroke(Color.rgb(0, 0, 0, 0.65));
            gc.setLineWidth(4.0);
            for (int row = 0; row < gridModel.getRows(); row++) {
                for (int col = 0; col < gridModel.getCols(); col++) {
                    GridModel.CropRect cell = gridModel.getCell(row, col);
                    drawCellShape(gc,
                            minX + cell.left() * width + pad,
                            minY + cell.top() * height + pad,
                            cell.width() * width - 2 * pad,
                            cell.height() * height - 2 * pad);
                }
            }
            // Color pass — amber to distinguish from blue grid lines
            gc.setStroke(Color.rgb(255, 190, 70, 1.0));
            gc.setLineWidth(2.0);
            for (int row = 0; row < gridModel.getRows(); row++) {
                for (int col = 0; col < gridModel.getCols(); col++) {
                    GridModel.CropRect cell = gridModel.getCell(row, col);
                    drawCellShape(gc,
                            minX + cell.left() * width + pad,
                            minY + cell.top() * height + pad,
                            cell.width() * width - 2 * pad,
                            cell.height() * height - 2 * pad);
                }
            }
        }

        gc.setFont(Font.font("System", FontWeight.BOLD, 13));
        gc.setTextAlign(TextAlignment.CENTER);

        for (int row = 0; row < gridModel.getRows(); row++) {
            for (int col = 0; col < gridModel.getCols(); col++) {
                GridModel.CropRect cell = gridModel.getCell(row, col);
                double x1 = minX + cell.left() * width;
                double y1 = minY + cell.top() * height;
                double x2 = minX + cell.right() * width;
                double y2 = minY + cell.bottom() * height;
                double cx = (x1 + x2) / 2.0;
                double cy = (y1 + y2) / 2.0;
                String label = (row + 1) + "." + (col + 1);
                // Shadow
                gc.setFill(Color.rgb(0, 0, 0, 0.85));
                gc.fillText(label, cx + 1.5, cy + 5.5);
                // Text
                gc.setFill(Color.rgb(255, 255, 255, 1.0));
                gc.fillText(label, cx, cy + 4);
            }
        }
    }

    private boolean isActiveOrHover(Orientation orientation, int index) {
        return activeTarget.matches(orientation, index) || hoverTarget.matches(orientation, index);
    }

    private void drawHandle(GraphicsContext gc, double x, double y, boolean active) {
        double radius = active ? 9.0 : 7.0;
        // Shadow
        gc.setFill(Color.rgb(0, 0, 0, 0.5));
        gc.fillOval(x - radius + 1, y - radius + 1, radius * 2.0, radius * 2.0);
        // Fill
        gc.setFill(active ? Color.WHITE : Color.rgb(104, 182, 255, 1.0));
        gc.fillOval(x - radius, y - radius, radius * 2.0, radius * 2.0);
        gc.setStroke(Color.rgb(10, 15, 25, 0.9));
        gc.setLineWidth(1.5);
        gc.strokeOval(x - radius, y - radius, radius * 2.0, radius * 2.0);
    }

    private void drawCellShape(GraphicsContext gc, double x, double y, double w, double h) {
        if (w <= 0 || h <= 0) return;
        switch (cellShape) {
            case ELLIPSE -> gc.strokeOval(x, y, w, h);
            case ROUNDED_RECTANGLE -> {
                double arc = Math.min(w, h) * 0.2;
                gc.strokeRoundRect(x, y, w, h, arc, arc);
            }
            case DIAMOND -> gc.strokePolygon(
                    new double[]{x + w / 2, x + w, x + w / 2, x},
                    new double[]{y, y + h / 2, y + h, y + h / 2},
                    4
            );
            default -> {}
        }
    }

    private void drawEmptyState(GraphicsContext gc) {
        gc.setFill(Color.rgb(255, 255, 255, 0.75));
        gc.setFont(Font.font("System", FontWeight.MEDIUM, 18));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText(I18n.get("preview.empty"), overlay.getWidth() / 2.0, overlay.getHeight() / 2.0);
    }

    private enum Orientation {
        VERTICAL,
        HORIZONTAL,
        NONE
    }

    private record DragTarget(Orientation orientation, int index) {
        static DragTarget none() {
            return new DragTarget(Orientation.NONE, -1);
        }

        boolean present() {
            return orientation != Orientation.NONE;
        }

        boolean matches(Orientation orientation, int index) {
            return this.orientation == orientation && this.index == index;
        }
    }
}
