# Batch Cropper

Desktop tool for cutting multiple images at once using an interactive grid.

**[⬇ Download for Windows](https://github.com/8945612-dev/batch-cropper/releases/latest)** · **[itch.io](https://8945612-dev.itch.io/batch-cropper)**

🌐 **Interface languages: English / Русский** — switch in the app sidebar.

---

## What it does

Draw a grid on one image. Drag the lines where you need them. Hit export — every cell from every image in your folder saves as a separate file.

The grid uses relative coordinates, not pixels. So the same layout works across images of different sizes, as long as the composition is the same.

## Who it's for

- **Photographers** — product shots, portraits, series with the same framing
- **Game developers** — cutting sprite sheets into individual frames
- **Artists** — splitting large artwork into print-ready tiles
- **Social media managers** — preparing multiple crop variants from the same photo set
- **Anyone with a stack of similar images** that need the same cut applied

## Features

- Load a whole folder of images at once
- Set up the grid visually by dragging lines in the preview
- Adjust rows, columns, crop area boundaries
- **Apply the same grid to all files** or **set individual grids per image** (toggle with checkbox)
- Crop shapes: Rectangle, Ellipse, Rounded Rectangle, Diamond
- Export as PNG or JPG
- Interface in English and Russian

## Download & run (no Java needed)

1. Download `Batch.Cropper.Windows.zip` from [Releases](https://github.com/8945612-dev/batch-cropper/releases/latest)
2. Unzip anywhere
3. Run `Batch Cropper.exe`

Windows 10 / 11 (64-bit) only. Mac/Linux: see Build from source below.

## Build from source

Requires JDK 21 and Maven 3.9+.

```bash
cd javafx-batch-cropper/javafx-batch-cropper
mvn clean javafx:run
```

## Current limitations

- Grid resets to even spacing when you change row/column count
- No WebP or EXIF rotation support (keeping dependencies minimal)
