package io.github.dev8945612.batchcropper;

import java.util.Arrays;

public final class GridModel {
    private static final double MIN_GAP = 0.01;

    private int rows;
    private int cols;
    private double[] xLines;
    private double[] yLines;

    public GridModel(int rows, int cols) {
        this.rows = Math.max(1, rows);
        this.cols = Math.max(1, cols);
        resetToFullImage();
    }

    public void resetToFullImage() {
        xLines = new double[cols + 1];
        yLines = new double[rows + 1];
        for (int i = 0; i <= cols; i++) {
            xLines[i] = i / (double) cols;
        }
        for (int i = 0; i <= rows; i++) {
            yLines[i] = i / (double) rows;
        }
    }

    public void repartitionWithinCurrentBounds(int newRows, int newCols) {
        double left = xLines[0];
        double right = xLines[xLines.length - 1];
        double top = yLines[0];
        double bottom = yLines[yLines.length - 1];

        rows = Math.max(1, newRows);
        cols = Math.max(1, newCols);
        xLines = new double[cols + 1];
        yLines = new double[rows + 1];

        for (int i = 0; i <= cols; i++) {
            xLines[i] = left + (right - left) * i / (double) cols;
        }
        for (int i = 0; i <= rows; i++) {
            yLines[i] = top + (bottom - top) * i / (double) rows;
        }
    }

    public void moveVerticalLine(int index, double value) {
        if (index < 0 || index >= xLines.length) {
            return;
        }
        double min = index == 0 ? 0.0 : xLines[index - 1] + MIN_GAP;
        double max = index == xLines.length - 1 ? 1.0 : xLines[index + 1] - MIN_GAP;
        xLines[index] = clamp(value, min, max);
    }

    public void moveHorizontalLine(int index, double value) {
        if (index < 0 || index >= yLines.length) {
            return;
        }
        double min = index == 0 ? 0.0 : yLines[index - 1] + MIN_GAP;
        double max = index == yLines.length - 1 ? 1.0 : yLines[index + 1] - MIN_GAP;
        yLines[index] = clamp(value, min, max);
    }

    public int getRows() {
        return rows;
    }

    public int getCols() {
        return cols;
    }

    public double[] getXLines() {
        return Arrays.copyOf(xLines, xLines.length);
    }

    public double[] getYLines() {
        return Arrays.copyOf(yLines, yLines.length);
    }

    public double getLeft() {
        return xLines[0];
    }

    public double getRight() {
        return xLines[xLines.length - 1];
    }

    public double getTop() {
        return yLines[0];
    }

    public double getBottom() {
        return yLines[yLines.length - 1];
    }

    public CropRect getCell(int row, int col) {
        return new CropRect(
                xLines[col],
                yLines[row],
                xLines[col + 1],
                yLines[row + 1]
        );
    }

    public GridModel copy() {
        GridModel copy = new GridModel(rows, cols);
        copy.xLines = Arrays.copyOf(xLines, xLines.length);
        copy.yLines = Arrays.copyOf(yLines, yLines.length);
        return copy;
    }

    public void copyFrom(GridModel other) {
        this.rows = other.rows;
        this.cols = other.cols;
        this.xLines = Arrays.copyOf(other.xLines, other.xLines.length);
        this.yLines = Arrays.copyOf(other.yLines, other.yLines.length);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public record CropRect(double left, double top, double right, double bottom) {
        public double width() {
            return right - left;
        }

        public double height() {
            return bottom - top;
        }
    }
}
