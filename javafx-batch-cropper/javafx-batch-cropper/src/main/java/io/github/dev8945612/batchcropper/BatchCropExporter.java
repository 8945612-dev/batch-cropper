package io.github.dev8945612.batchcropper;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class BatchCropExporter {

    public ExportResult export(List<File> files, Function<File, GridModel> gridResolver, Path outputDir, String format, CropShape shape) throws IOException {
        Files.createDirectories(outputDir);

        int written = 0;
        List<String> warnings = new ArrayList<>();

        for (File file : files) {
            GridModel grid = gridResolver.apply(file);
            BufferedImage source = ImageIO.read(file);
            if (source == null) {
                warnings.add("Не удалось прочитать: " + file.getName());
                continue;
            }

            int imageWidth = source.getWidth();
            int imageHeight = source.getHeight();
            String baseName = stripExtension(file.getName());

            for (int row = 0; row < grid.getRows(); row++) {
                for (int col = 0; col < grid.getCols(); col++) {
                    GridModel.CropRect cell = grid.getCell(row, col);

                    int x1 = clampInt((int) Math.round(cell.left() * imageWidth), 0, imageWidth - 1);
                    int y1 = clampInt((int) Math.round(cell.top() * imageHeight), 0, imageHeight - 1);
                    int x2 = clampInt((int) Math.round(cell.right() * imageWidth), x1 + 1, imageWidth);
                    int y2 = clampInt((int) Math.round(cell.bottom() * imageHeight), y1 + 1, imageHeight);

                    int cropWidth = Math.max(1, x2 - x1);
                    int cropHeight = Math.max(1, y2 - y1);

                    boolean needsAlpha = shape != CropShape.RECTANGLE && !format.equalsIgnoreCase("jpg");
                    int bufferType = needsAlpha ? BufferedImage.TYPE_INT_ARGB
                            : (format.equalsIgnoreCase("jpg") ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB);

                    BufferedImage cropped = new BufferedImage(cropWidth, cropHeight, bufferType);
                    Graphics2D g2d = cropped.createGraphics();
                    try {
                        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

                        if (format.equalsIgnoreCase("jpg")) {
                            g2d.setColor(Color.WHITE);
                            g2d.fillRect(0, 0, cropWidth, cropHeight);
                        }

                        if (shape != CropShape.RECTANGLE) {
                            applyShapeClip(g2d, shape, cropWidth, cropHeight);
                        }

                        g2d.drawImage(source,
                                0, 0, cropWidth, cropHeight,
                                x1, y1, x2, y2,
                                null);
                    } finally {
                        g2d.dispose();
                    }

                    Path target = outputDir.resolve(baseName + "__r" + (row + 1) + "_c" + (col + 1) + "." + format);
                    if (!ImageIO.write(cropped, format, target.toFile())) {
                        warnings.add("Could not write " + format.toUpperCase() + ": " + target.getFileName());
                        continue;
                    }
                    written++;
                }
            }
        }

        return new ExportResult(written, warnings);
    }

    private static void applyShapeClip(Graphics2D g2d, CropShape shape, int w, int h) {
        switch (shape) {
            case ELLIPSE -> g2d.setClip(new java.awt.geom.Ellipse2D.Float(0, 0, w, h));
            case ROUNDED_RECTANGLE -> {
                int arc = Math.round(Math.min(w, h) * 0.2f);
                g2d.setClip(new java.awt.geom.RoundRectangle2D.Float(0, 0, w, h, arc, arc));
            }
            case DIAMOND -> g2d.setClip(new java.awt.Polygon(
                    new int[]{w / 2, w, w / 2, 0},
                    new int[]{0, h / 2, h, h / 2},
                    4
            ));
            default -> {}
        }
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String stripExtension(String name) {
        int dotIndex = name.lastIndexOf('.');
        return dotIndex > 0 ? name.substring(0, dotIndex) : name;
    }

    public record ExportResult(int writtenFiles, List<String> warnings) {
    }
}
