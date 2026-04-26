@echo off
setlocal DisableDelayedExpansion

for %%I in ("%~dp0.") do set "SCRIPT_DIR=%%~fI"
set "ANDROID_APP_DIR=%SCRIPT_DIR%\mobile-app"
set "DEFAULT_GRADLE_PROPERTIES_PATH=%ANDROID_APP_DIR%\.gradle-user\gradle.properties"

set "MAPS_API_KEY="
set "OPEN_WEATHER_API_KEY="
set "BACKEND_API_BASE=http://10.0.2.2:8080/api/public"
set "OPEN_WEATHER_API_BASE=https://api.openweathermap.org/data/2.5/weather"
set "GRADLE_PROPERTIES_PATH=%DEFAULT_GRADLE_PROPERTIES_PATH%"

:parseArgs
if "%~1"=="" goto argsDone

if /I "%~1"=="-MapsApiKey" (
    if "%~2"=="" goto missingMapsValue
    set "MAPS_API_KEY=%~2"
    shift
    shift
    goto parseArgs
)

if /I "%~1"=="-OpenWeatherApiKey" (
    if "%~2"=="" goto missingWeatherValue
    set "OPEN_WEATHER_API_KEY=%~2"
    shift
    shift
    goto parseArgs
)

if /I "%~1"=="-BackendApiBase" (
    if "%~2"=="" goto missingBackendBaseValue
    set "BACKEND_API_BASE=%~2"
    shift
    shift
    goto parseArgs
)

if /I "%~1"=="-OpenWeatherApiBase" (
    if "%~2"=="" goto missingWeatherBaseValue
    set "OPEN_WEATHER_API_BASE=%~2"
    shift
    shift
    goto parseArgs
)

if /I "%~1"=="-GradlePropertiesPath" (
    if "%~2"=="" goto missingPathValue
    set "GRADLE_PROPERTIES_PATH=%~2"
    shift
    shift
    goto parseArgs
)

if /I "%~1"=="-h" goto usage
if /I "%~1"=="--help" goto usage
if /I "%~1"=="/?" goto usage

echo Unknown argument: %~1
goto usage

:argsDone
if not defined MAPS_API_KEY (
    set /p "MAPS_API_KEY=Enter your Google Maps API key: "
)

if not defined OPEN_WEATHER_API_KEY (
    set /p "OPEN_WEATHER_API_KEY=Enter your OpenWeather API key: "
)

if not defined MAPS_API_KEY (
    echo MAPS_API_KEY cannot be empty.
    exit /b 1
)

if not defined OPEN_WEATHER_API_KEY (
    echo OPEN_WEATHER_API_KEY cannot be empty.
    exit /b 1
)

if /I "%GRADLE_PROPERTIES_PATH%"=="%DEFAULT_GRADLE_PROPERTIES_PATH%" (
    if not exist "%ANDROID_APP_DIR%\app\build.gradle" (
        echo Could not find the Android app folder:
        echo   %ANDROID_APP_DIR%
        exit /b 1
    )
)

for %%I in ("%GRADLE_PROPERTIES_PATH%") do set "GRADLE_DIR=%%~dpI"

if not exist "%GRADLE_DIR%" (
    mkdir "%GRADLE_DIR%"
    if errorlevel 1 (
        echo Failed to create directory:
        echo   %GRADLE_DIR%
        exit /b 1
    )
)

set "TEMP_FILE=%TEMP%\android-keys-%RANDOM%-%RANDOM%.tmp"

if exist "%GRADLE_PROPERTIES_PATH%" (
    > "%TEMP_FILE%" (
        findstr /V /B /I /C:"MAPS_API_KEY=" /C:"OPEN_WEATHER_API_KEY=" /C:"BACKEND_API_BASE=" /C:"OPEN_WEATHER_API_BASE=" "%GRADLE_PROPERTIES_PATH%" 2>nul
    )
) else (
    break > "%TEMP_FILE%"
)

call :appendLine "%TEMP_FILE%" "MAPS_API_KEY=%MAPS_API_KEY%"
call :appendLine "%TEMP_FILE%" "OPEN_WEATHER_API_KEY=%OPEN_WEATHER_API_KEY%"
call :appendLine "%TEMP_FILE%" "BACKEND_API_BASE=%BACKEND_API_BASE%"
call :appendLine "%TEMP_FILE%" "OPEN_WEATHER_API_BASE=%OPEN_WEATHER_API_BASE%"

move /Y "%TEMP_FILE%" "%GRADLE_PROPERTIES_PATH%" >nul
if errorlevel 1 (
    echo Failed to write:
    echo   %GRADLE_PROPERTIES_PATH%
    if exist "%TEMP_FILE%" del "%TEMP_FILE%" >nul 2>&1
    exit /b 1
)

echo Updated Android app properties:
echo   %GRADLE_PROPERTIES_PATH%
echo.
echo Configured keys:
echo   MAPS_API_KEY
echo   OPEN_WEATHER_API_KEY
echo   BACKEND_API_BASE
echo   OPEN_WEATHER_API_BASE
exit /b 0

:appendLine
>> "%~1" <nul set /p "=%~2"
>> "%~1" echo(
exit /b 0

:missingMapsValue
echo Missing value for -MapsApiKey
goto usage

:missingWeatherValue
echo Missing value for -OpenWeatherApiKey
goto usage

:missingBackendBaseValue
echo Missing value for -BackendApiBase
goto usage

:missingWeatherBaseValue
echo Missing value for -OpenWeatherApiBase
goto usage

:missingPathValue
echo Missing value for -GradlePropertiesPath
goto usage

:usage
echo Usage:
echo   setup-android-keys.cmd [-MapsApiKey key] [-OpenWeatherApiKey key]
echo                          [-BackendApiBase url] [-OpenWeatherApiBase url]
echo                          [-GradlePropertiesPath path]
echo.
echo Default Gradle properties path:
echo   %DEFAULT_GRADLE_PROPERTIES_PATH%
exit /b 1
