@echo off
setlocal
cd /d "%~dp0"
set "_JAVA_OPTIONS="
set "JAVA_TOOL_OPTIONS="
set "JDK_JAVA_OPTIONS="
echo ================================
echo        PROJECTDNA
 echo ================================
where java >nul 2>nul || (echo Java JDK not found. Install JDK 17 or newer and try again.&pause&exit /b 1)
if not exist "lib\sqlite-jdbc.jar" (
 echo SQLite JDBC driver not found. Downloading it automatically...
 powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.53.4.0/sqlite-jdbc-3.53.4.0.jar' -OutFile 'lib\sqlite-jdbc.jar'"
 if errorlevel 1 (echo Download failed. Check your internet connection.&pause&exit /b 1)
)
if not exist bin mkdir bin
if not exist data mkdir data
javac -encoding UTF-8 -cp "lib\sqlite-jdbc.jar" -d bin src\projectdna\*.java
if errorlevel 1 (echo Compilation failed.&pause&exit /b 1)
echo.
echo Server running at: http://localhost:8080
echo Database: SQLite
echo Status: Application started successfully
echo Open the URL in Chrome. Close this window to stop the server.
echo.
java -cp "bin;lib\sqlite-jdbc.jar" projectdna.Main
pause
