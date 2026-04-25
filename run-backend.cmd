@echo off
setlocal

set "SCRIPT_DIR=%~dp0"
set "BACKEND_DIR=%SCRIPT_DIR%backend"
set "WEB_APP_DIR=%SCRIPT_DIR%web-app"

if not exist "%BACKEND_DIR%\mvnw.cmd" (
    echo mvnw.cmd was not found in the backend directory.
    exit /b 1
)

if not exist "%WEB_APP_DIR%\package.json" (
    echo package.json was not found in the web-app directory.
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
start "Reserve Backend" cmd /k "cd /d ""%BACKEND_DIR%"" && mvnw.cmd spring-boot:run"

echo Launching web app in a new window...
start "Reserve Web App" cmd /k "cd /d ""%WEB_APP_DIR%"" && npm run dev"

echo Database, backend, and web app are starting.
echo Backend URL: http://localhost:8080
echo Web app URL: http://localhost:5173
echo Mobile app is not started by this script.
echo Close the opened terminal windows when you want to stop the backend or web app.
exit /b 0