@echo off
rem ===================================================================
rem DUNG KHI DEMO: bien dich, mo server, roi mo hai cua so nguoi choi.
rem Chi can nhay dup MOT file nay la xong.
rem Nho bat MySQL trong XAMPP truoc.
rem ===================================================================
cd /d "%~dp0"

call bien-dich.bat
if %ERRORLEVEL% NEQ 0 exit /b 1

echo Dang mo server...
start "Server Ban tau" cmd /k "java -cp "bin;lib/*" bantau.server.ServerMain"

rem Cho server kip lang nghe truoc khi client ket noi
ping -n 4 127.0.0.1 >nul 2>&1

echo Dang mo hai cua so nguoi choi...
start "Nguoi choi 1" cmd /c "java -cp "bin;lib/*" bantau.client.ClientMain"
ping -n 2 127.0.0.1 >nul 2>&1
start "Nguoi choi 2" cmd /c "java -cp "bin;lib/*" bantau.client.ClientMain"

echo.
echo === DA MO XONG ===
ping -n 3 127.0.0.1 >nul 2>&1
