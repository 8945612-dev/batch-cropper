@echo off
setlocal
set "PROJECT_DIR=%~dp0"
set "REPO_ROOT=%PROJECT_DIR%..\.."
set "LOCAL_MVN=%REPO_ROOT%\apache-maven-3.9.15-bin\apache-maven-3.9.15\bin\mvn.cmd"

if exist "%LOCAL_MVN%" (
    set "MVN=%LOCAL_MVN%"
) else (
    set "MVN=mvn"
)

pushd "%PROJECT_DIR%"
call "%MVN%" clean javafx:run
popd
pause
