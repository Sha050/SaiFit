@echo off
cd /d "%~dp0"
echo Starting SAI Fit Backend...
echo.

if not exist ".env" (
    if exist ".env.example" (
        echo No backend .env found. Creating a local copy from .env.example...
        copy /Y ".env.example" ".env" >nul
        echo Review backend\.env and set your private values before using external services.
        echo.
    )
)

set "VENV_PY=venv\Scripts\python.exe"

if not exist "%VENV_PY%" (
    echo Backend virtual environment not found. Creating it now...

    where py >nul 2>nul
    if not errorlevel 1 (
        py -m venv venv
    ) else (
        where python >nul 2>nul
        if errorlevel 1 (
            echo Python was not found on this machine.
            echo Install Python and rerun this script.
            goto :end
        )
        python -m venv venv
    )

    if errorlevel 1 (
        echo Failed to create the virtual environment.
        goto :end
    )

    echo Installing backend dependencies...
    "%VENV_PY%" -m pip install --upgrade pip
    if errorlevel 1 goto :end

    "%VENV_PY%" -m pip install -r requirements.txt
    if errorlevel 1 goto :end
)

"%VENV_PY%" -m uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
:end
echo.
echo Backend stopped. Press any key to close...
pause >nul
