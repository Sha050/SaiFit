@echo off
cd /d "%~dp0"
echo Starting SAI Fit Backend...
echo.
"venv\Scripts\python.exe" -m uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
echo.
echo Backend stopped. Press any key to close...
pause >nul
