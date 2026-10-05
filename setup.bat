@echo off
cd /d "%~dp0"
if not exist data mkdir data
where java >nul 2>nul || (echo Please install JDK 17 or newer first.&pause&exit /b 1)
if not exist lib mkdir lib
if not exist lib\sqlite-jdbc.jar powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.53.4.0/sqlite-jdbc-3.53.4.0.jar' -OutFile 'lib\sqlite-jdbc.jar'"
if exist lib\sqlite-jdbc.jar (echo SQLite JDBC driver ready.) else (echo Could not download driver.)
pause
