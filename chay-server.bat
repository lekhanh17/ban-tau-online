@echo off
rem ===================================================================
rem Khoi dong server. Nho bat MySQL trong XAMPP truoc.
rem Cua so nay phai de MO trong suot luc choi.
rem Dong server: bam Ctrl+C hoac dong cua so.
rem ===================================================================
cd /d "%~dp0"
title Server Ban tau online

if not exist bin (
    echo Chua bien dich. Chay bien-dich.bat truoc.
    pause
    exit /b 1
)

java -cp "bin;lib/*" bantau.server.ServerMain
pause
