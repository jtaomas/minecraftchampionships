@echo off
setlocal
cd /d "%~dp0"
title Minecraft Monday Championship Server (Paper 1.21.1)

set "JAVA_BIN=java"
if exist "%~dp0..\java\bin\java.exe" (
    set "JAVA_BIN=%~dp0..\java\bin\java.exe"
) else (
    where java >nul 2>&1
    if errorlevel 1 (
        echo Java 21 is required to run this server.
        echo Install Eclipse Temurin 21 from https://adoptium.net/ and try again.
        pause
        exit /b 1
    )
)

"%JAVA_BIN%" -Xms2G -Xmx6G -XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 -XX:+UnlockExperimentalVMOptions -XX:+DisableExplicitGC -XX:G1NewSizePercent=30 -XX:G1MaxNewSizePercent=40 -XX:G1HeapRegionSize=8M -XX:G1ReservePercent=20 -XX:G1HeapWastePercent=5 -jar paper.jar nogui %*
pause
