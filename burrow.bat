@echo off
setlocal

set "JAR=burrow-gui\target\burrow-gui-1.0-SNAPSHOT.jar"

if not exist "%JAR%" (
    echo ERROR: JAR not found at %JAR%
    echo Build first: mvnw.cmd clean package -DskipTests
    pause
    exit /b 1
)

java -jar "%JAR%" %*
