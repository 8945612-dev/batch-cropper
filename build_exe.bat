@echo off
setlocal

set "ROOT=%~dp0"
set "PROJECT=%ROOT%javafx-batch-cropper\javafx-batch-cropper"
set "DIST=%ROOT%dist"
set "LOCAL_MVN=%ROOT%apache-maven-3.9.15-bin\apache-maven-3.9.15\bin\mvn.cmd"
set "JAVAFX_VERSION=21.0.8"

if exist "%LOCAL_MVN%" (
    set "MVN=%LOCAL_MVN%"
) else (
    set "MVN=mvn"
)

set "JPACKAGE=jpackage"
if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\jpackage.exe" (
        set "JPACKAGE=%JAVA_HOME%\bin\jpackage.exe"
    )
)

echo === Batch Cropper - build EXE ===
echo.

echo [1/3] Build and jlink...
call "%MVN%" -f "%PROJECT%\pom.xml" clean javafx:jlink
if errorlevel 1 (
    echo.
    echo ERROR: build failed
    pause
    exit /b 1
)

echo.
echo [2/3] Create EXE...

if exist "%DIST%\Batch Cropper" rd /s /q "%DIST%\Batch Cropper"
if not exist "%DIST%" mkdir "%DIST%"

"%JPACKAGE%" ^
  --type app-image ^
  --runtime-image "%PROJECT%\target\app-image" ^
  --name "Batch Cropper" ^
  --app-version 1.0.0 ^
  --module "io.github.batchcropper/io.github.dev8945612.batchcropper.BatchCropperApp" ^
  --dest "%DIST%"

if errorlevel 1 (
    echo.
    echo ERROR: jpackage failed
    pause
    exit /b 1
)

echo.
echo [3/3] Copy JavaFX native DLLs...
set "M2_JAVAFX=%USERPROFILE%\.m2\repository\org\openjfx"
set "RUNTIME_BIN=%DIST%\Batch Cropper\runtime\bin"
set "TMP_EXTRACT=%TEMP%\javafx-extract-dlls"

if exist "%TMP_EXTRACT%" rd /s /q "%TMP_EXTRACT%"
mkdir "%TMP_EXTRACT%"

powershell -NoProfile -Command ^
  "$jars = @('%M2_JAVAFX%\javafx-graphics\%JAVAFX_VERSION%\javafx-graphics-%JAVAFX_VERSION%-win.jar', '%M2_JAVAFX%\javafx-base\%JAVAFX_VERSION%\javafx-base-%JAVAFX_VERSION%-win.jar', '%M2_JAVAFX%\javafx-controls\%JAVAFX_VERSION%\javafx-controls-%JAVAFX_VERSION%-win.jar') | Where-Object { Test-Path $_ }; if ($jars.Count -eq 0) { Write-Host 'No JavaFX native jars found; skipping DLL copy.'; exit 0 }; Add-Type -A System.IO.Compression.FileSystem; foreach ($j in $jars) { try { [IO.Compression.ZipFile]::ExtractToDirectory($j, '%TMP_EXTRACT%') } catch {} }; Get-ChildItem '%TMP_EXTRACT%' -Recurse -Filter '*.dll' | ForEach-Object { Copy-Item $_.FullName '%RUNTIME_BIN%' -Force }"

if exist "%TMP_EXTRACT%" rd /s /q "%TMP_EXTRACT%"

echo.
echo === Done! ===
echo App image: "%DIST%\Batch Cropper"
pause
