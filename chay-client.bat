@echo off
rem ===================================================================
rem Mo mot cua so nguoi choi.
rem Nhay dup file nay HAI LAN de co hai nguoi choi dau voi nhau.
rem ===================================================================
cd /d "%~dp0"
title Client Ban tau online

if not exist bin (
    echo Chua bien dich. Chay bien-dich.bat truoc.
    pause
    exit /b 1
)

java -cp "bin;lib/*" bantau.client.ClientMain
