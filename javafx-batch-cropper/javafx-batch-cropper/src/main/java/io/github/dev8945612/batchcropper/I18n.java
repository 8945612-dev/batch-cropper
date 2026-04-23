package io.github.dev8945612.batchcropper;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.Locale;
import java.util.ResourceBundle;

public final class I18n {
    private static final StringProperty localeKey = new SimpleStringProperty("ru");
    private static ResourceBundle bundle = load("ru");

    private static ResourceBundle load(String lang) {
        return ResourceBundle.getBundle("io.github.dev8945612.batchcropper.messages", Locale.of(lang));
    }

    public static StringBinding bind(String key) {
        return Bindings.createStringBinding(
            () -> bundle.containsKey(key) ? bundle.getString(key) : key,
            localeKey
        );
    }

    public static String get(String key) {
        return bundle.containsKey(key) ? bundle.getString(key) : key;
    }

    public static String format(String key, Object... args) {
        return String.format(get(key), args);
    }

    public static void setLocale(String lang) {
        bundle = load(lang);
        localeKey.set(lang);
    }

    public static StringProperty localeProperty() {
        return localeKey;
    }

    private I18n() {}
}
