@echo off
rem ===================================================================
rem Mo NGAY HAI cua so nguoi choi bang mot cu nhay dup.
rem Khong dung toi server - server phai dang chay san.
rem
rem Muon ket noi toi may khac trong mang LAN thi keo tha file nay
rem vao cua so cmd roi go them dia chi IP, vi du:
rem     chay-2-client.bat 192.168.1.15
rem ===================================================================
cd /d "%~dp0"

if not exist bin (
    echo Chua bien dich. Chay bien-dich.bat truoc.
    pause
    exit /b 1
)

rem Khong truyen gi thi mac dinh la may nay (127.0.0.1), cong 5000
set HOST=%1
if "%HOST%"=="" set HOST=127.0.0.1
set PORT=%2
if "%PORT%"=="" set PORT=5000

echo Dang mo hai cua so nguoi choi toi %HOST%:%PORT% ...
start "Nguoi choi 1" cmd /c "java -cp "bin;lib/*" bantau.client.ClientMain %HOST% %PORT%"
ping -n 2 127.0.0.1 >nul 2>&1
start "Nguoi choi 2" cmd /c "java -cp "bin;lib/*" bantau.client.ClientMain %HOST% %PORT%"
