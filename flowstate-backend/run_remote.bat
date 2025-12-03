@echo off
REM Start the Flask backend in remote inference mode
setlocal enabledelayedexpansion
set REMOTE_PRED_URL=https://router.huggingface.co/models/google/timesfm-1.0-200m
python app.py
