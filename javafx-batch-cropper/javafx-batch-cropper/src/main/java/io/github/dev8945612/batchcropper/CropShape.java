package io.github.dev8945612.batchcropper;

public enum CropShape {
    RECTANGLE("shape.rectangle"),
    ELLIPSE("shape.ellipse"),
    ROUNDED_RECTANGLE("shape.rounded"),
    DIAMOND("shape.diamond");

    private final String key;

    CropShape(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }

    @Override
    public String toString() {
        return I18n.get(key);
    }
}
