@echo off
REM Start FlowState backend with remote inference enabled
setlocal enabledelayedexpansion
set REMOTE_PRED_URL=https://router.huggingface.co/models/google/timesfm-1.0-200m
cd /d "%~dp0"
echo Starting FlowState backend in REMOTE MODE...
echo Endpoint: %REMOTE_PRED_URL%
echo.
python app.py
