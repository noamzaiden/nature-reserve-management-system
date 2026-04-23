@echo off
setlocal

set "SCRIPT_DIR=%~dp0"
set "BACKEND_DIR=%SCRIPT_DIR%backend"
set "WEB_APP_DIR=%SCRIPT_DIR%web-app"

if /i "%~1"=="backend" goto :runBackend
if /i "%~1"=="web" goto :runWeb

if not exist "%BACKEND_DIR%\mvnw.cmd" (
    echo mvnw.cmd was not found in the backend directory.
    exit /b 1
)

where docker >nul 2>&1
if errorlevel 1 (
    echo Docker was not found in PATH. Install Docker Desktop and try again.
    exit /b 1
)

where java >nul 2>&1
if errorlevel 1 (
    echo Java was not found in PATH. Install Java 17 and try again.
    exit /b 1
)

where npm >nul 2>&1
if errorlevel 1 (
    echo npm was not found in PATH. Install Node.js and try again.
    exit /b 1
)

if not exist "%WEB_APP_DIR%\package.json" (
    echo package.json was not found in the web-app directory.
    exit /b 1
)

if not exist "%WEB_APP_DIR%\node_modules" (
    echo Web app dependencies were not found.
    echo Run npm install inside the web-app directory and try again.
    exit /b 1
)

pushd "%BACKEND_DIR%" >nul
if errorlevel 1 (
    echo Failed to open the backend directory.
    exit /b 1
)

echo Starting PostgreSQL with Docker...
docker compose up -d
if errorlevel 1 (
    echo Failed to start the database container.
    echo Make sure Docker Desktop is running and the Docker daemon is available.
    popd
    exit /b 1
)

popd

echo Launching Spring Boot backend in a new window...
start "Reserve Backend" cmd /k ""%~f0" backend"

echo Launching web app in a new window...
start "Reserve Web App" cmd /k ""%~f0" web"

echo Backend and web app are starting.
echo Backend URL: http://localhost:8080
echo Web app URL: http://localhost:5173
echo Close the opened terminal windows when you want to stop them.
exit /b 0

:runBackend
pushd "%BACKEND_DIR%" >nul
if errorlevel 1 (
    echo Failed to open the backend directory.
    exit /b 1
)

where java >nul 2>&1
if errorlevel 1 (
    echo Java was not found in PATH. Install Java 17 and try again.
    popd
    exit /b 1
)

if not exist "mvnw.cmd" (
    echo mvnw.cmd was not found in the backend directory.
    popd
    exit /b 1
)

echo Starting Spring Boot backend on http://localhost:8080 ...
echo Press Ctrl+C to stop the backend when you are done.
call mvnw.cmd spring-boot:run
set "EXIT_CODE=%ERRORLEVEL%"
popd
exit /b %EXIT_CODE%

:runWeb
pushd "%WEB_APP_DIR%" >nul
if errorlevel 1 (
    echo Failed to open the web-app directory.
    exit /b 1
)

where npm >nul 2>&1
if errorlevel 1 (
    echo npm was not found in PATH. Install Node.js and try again.
    popd
    exit /b 1
)

if not exist "package.json" (
    echo package.json was not found in the web-app directory.
    popd
    exit /b 1
)

if not exist "node_modules" (
    echo Web app dependencies were not found.
    echo Run npm install inside the web-app directory and try again.
    popd
    exit /b 1
)

echo Starting Vite web app on http://localhost:5173 ...
echo Press Ctrl+C to stop the web app when you are done.
call npm run dev
set "EXIT_CODE=%ERRORLEVEL%"
popd
exit /b %EXIT_CODE%
