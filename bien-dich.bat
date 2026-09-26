@echo off
rem ===================================================================
rem Bien dich toan bo ma nguon vao thu muc bin.
rem Chi can chay lai file nay MOI KHI SUA CODE.
rem ===================================================================
cd /d "%~dp0"

if exist bin rmdir /s /q bin
mkdir bin

echo Dang bien dich...
dir /s /b src\*.java > sources.txt
javac -encoding UTF-8 -d bin -cp "lib/*" @sources.txt
set KETQUA=%ERRORLEVEL%
del sources.txt

if %KETQUA% NEQ 0 (
    echo.
    echo === BIEN DICH THAT BAI - xem loi o tren ===
    pause
    exit /b 1
)

echo.
echo === BIEN DICH XONG ===
ping -n 3 127.0.0.1 >nul 2>&1
