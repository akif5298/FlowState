import os
import sys
import torch
from pathlib import Path
import shutil

print("=" * 60)
print("WARNING: Model Download Issue on Windows")
print("=" * 60)
print()
print("The TimesFM model uses Git LFS which has known issues on Windows.")
print("We recommend using WSL2 or Linux to download the model initially.")
print()
print("QUICKEST SOLUTION:")
print("1. Use online prediction service instead, or")
print("2. Download via WSL2 with: git lfs clone ...")
print()
print("=" * 60)
print()

# WINDOWS CONFIGURATION
if sys.platform == 'win32':
    os.environ["HF_HUB_DISABLE_SYMLINKS"] = "1"
    os.environ["HF_HUB_ENABLE_XET_SYMLINK"] = "0"
    os.environ["HF_HUB_DOWNLOAD_TIMEOUT"] = "3600"

# Monkey-patch torch.load
_original_torch_load = torch.load
def patched_torch_load(f, *args, **kwargs):
    kwargs['weights_only'] = False
    return _original_torch_load(f, *args, **kwargs)
torch.load = patched_torch_load

print("=" * 60)
print("TimesFM Configuration")
print("=" * 60)
print()

# IMPORTS
import os
from flask import Flask, request, jsonify
import timesfm
import numpy as np
import requests
from dotenv import load_dotenv

# Load .env file
load_dotenv()

app = Flask(__name__)

# --- MODEL SETTINGS ---
CONTEXT_LEN = 48 
HORIZON_LEN = 12

# Remote inference configuration (optional)
REMOTE_PRED_URL = os.environ.get("REMOTE_PRED_URL")
REMOTE_API_KEY = os.environ.get("REMOTE_API_KEY")
USE_REMOTE = bool(REMOTE_PRED_URL)

if USE_REMOTE:
    print(f"[REMOTE MODE] Forwarding requests to: {REMOTE_PRED_URL}")
    if REMOTE_API_KEY:
        print("[AUTH] Using provided REMOTE_API_KEY for authorization")
    print("Note: ensure the remote endpoint accepts the same JSON body as this server (/predict).\n")

# --- INITIALIZE LOCAL MODEL (skipped when remote inference is enabled) ---
tfm = None
if not USE_REMOTE:
    print("[LOCAL MODE] Loading TimesFM Model locally (this may take several minutes)...\n")
    # Attempt to locate an LFS-checkout-style checkpoint under the HF cache and
    # create a copy named `torch_model.ckpt` at the snapshot root so TimesFM
    # can find it on Windows (avoids xet/symlink issues).
    try:
        hf_snapshots = Path.home() / ".cache" / "huggingface" / "hub" / "models--google--timesfm-1.0-200m" / "snapshots"
        if hf_snapshots.exists():
            found = False
            for commit_dir in hf_snapshots.iterdir():
                if not commit_dir.is_dir():
                    continue
                # Look for an internal checkpoint file (common layout: checkpoints/*/state/checkpoint)
                candidates = list(commit_dir.rglob('state/checkpoint'))
                if not candidates:
                    continue
                src_checkpoint = candidates[0]
                dest_ckpt = commit_dir / 'torch_model.ckpt'
                if not dest_ckpt.exists():
                    try:
                        shutil.copy2(src_checkpoint, dest_ckpt)
                        print(f"Copied local checkpoint from '{src_checkpoint}' to '{dest_ckpt}' to help TimesFM load on Windows.")
                    except Exception as copy_exc:
                        print(f"Warning: failed to copy checkpoint file: {copy_exc}")
                else:
                    print(f"Found existing root checkpoint file at: {dest_ckpt}")
                found = True
                break
            if not found:
                print("No internal checkpoint found under HF snapshots; proceeding to let TimesFM attempt download.")
        else:
            print(f"HF snapshots cache not found at expected location: {hf_snapshots}")
    except Exception as e:
        print(f"Warning while scanning HF cache for local checkpoints: {e}")
    try:
        tfm = timesfm.TimesFm(
            hparams=timesfm.TimesFmHparams(
                backend="cpu",
                per_core_batch_size=32,
                context_len=CONTEXT_LEN,
                horizon_len=HORIZON_LEN,
            ),
            checkpoint=timesfm.TimesFmCheckpoint(
                huggingface_repo_id="google/timesfm-1.0-200m"
            ),
        )
        print("✅ Local model loaded. Server ready for predictions.")

    except FileNotFoundError as e:
        print("[ERROR] Model files not found!\n")
        print("This is a known issue with HuggingFace Git LFS on Windows.\n")
        print("Recommendations: use WSL2 to download the model or use remote inference (set REMOTE_PRED_URL).\n")
        print(f"Error details: {e}\n")

    except Exception as e:
        print(f"[ERROR] Unexpected error while loading model: {type(e).__name__}: {e}\n")
else:
    # When remote is enabled we don't attempt to load local model
    print("[REMOTE MODE] Skipping local model load because REMOTE_PRED_URL is set.\n")

# ============================================================
# STARTUP USAGE GUIDE
# ============================================================
print("=" * 60)
print("SERVER READY FOR PREDICTIONS")
print("=" * 60)
print()
print("USAGE:")
print()
if USE_REMOTE:
    print("  Mode: REMOTE INFERENCE")
    print(f"  Endpoint: {REMOTE_PRED_URL}")
    print()
    print("  Example (PowerShell - multivariate):")
    print('    $body = @{')
    print('      past_values = @{')
    print('        heart_rate = @(72,76,80,74,78,82,88,75,79,84,81,77)')
    print('        hrv = @(55,53,52,60,58,56,54,62,59,57,55,60)')
    print('        steps = @(6000,8500,4000,12000,7000,9000,11000,5500,8000,10000,7500,6800)')
    print('      }')
    print('      forecast_horizon = 12')
    print('    } | ConvertTo-Json')
    print('    Invoke-RestMethod -Uri http://127.0.0.1:5000/predict -Method POST -Body $body -ContentType "application/json"')
    print()
    print("  Example (legacy format):")
    print('    $body = @{ history = @(1,2,3,4,5) } | ConvertTo-Json')
    print('    Invoke-RestMethod -Uri http://127.0.0.1:5000/predict -Method POST -Body $body -ContentType "application/json"')
else:
    print("  Mode: LOCAL INFERENCE")
    print("  Model: google/timesfm-1.0-200m")
    print("  Context Length: {} | Forecast Horizon: {}".format(CONTEXT_LEN, HORIZON_LEN))
    print()
    print("  Example (PowerShell - multivariate):")
    print('    $body = @{')
    print('      past_values = @{')
    print('        heart_rate = @(72,76,80,74,78,82,88,75,79,84,81,77)')
    print('        hrv = @(55,53,52,60,58,56,54,62,59,57,55,60)')
    print('      }')
    print('      forecast_horizon = 12')
    print('    } | ConvertTo-Json')
    print('    Invoke-RestMethod -Uri http://127.0.0.1:5000/predict -Method POST -Body $body -ContentType "application/json"')
    print()
    print("  Example (legacy format):")
    print('    $body = @{ history = @(1,2,3,4,5) } | ConvertTo-Json')
    print('    Invoke-RestMethod -Uri http://127.0.0.1:5000/predict -Method POST -Body $body -ContentType "application/json"')
print()
print("  To use REMOTE INFERENCE instead, set environment variables:")
print('    set REMOTE_PRED_URL=https://router.huggingface.co/models/google/timesfm-1.0-200m')
print('    set REMOTE_API_KEY=hf_YOUR_TOKEN_HERE')
print("    python app.py")
print()
print("=" * 60)
print()

def generate_sample_forecast(history_values, horizon, multivariate_data=None):
    """
    Generate realistic energy level forecasts accounting for multiple biometric factors.
    
    Features considered:
    - Biometrics: Heart Rate, HRV, Sleep Duration, Sleep Quality, Resting HR
    - Activity: Step Count, Activity Level / Calories
    - Cognitive: Typing Speed, Reaction Time
    - Contextual: Time of Day, Mood Score
    
    Args:
        history_values: Array of energy level readings (0-100 scale)
        horizon: Number of future time periods to predict
        multivariate_data: Optional dict with features {feature_name: [values...]}
    
    Returns:
        List of predicted energy levels (0-100 scale)
    """
    import numpy as np
    
    history_array = np.array(history_values, dtype=float)
    
    # Calculate base trend and statistics
    if len(history_array) >= 2:
        recent_trend = (history_array[-1] - history_array[-5:].mean()) / 5 if len(history_array) >= 5 else 0
    else:
        recent_trend = 0
    
    mean_val = float(history_array.mean())
    std_val = float(history_array.std())
    
    # Initialize multivariate factors (0-1 scale, 0.5 = neutral)
    factors = {
        'heart_rate': 0.5,           # Normal resting HR ~60-100 BPM
        'hrv': 0.5,                  # Good recovery (HRV 50-100ms)
        'sleep_hours': 0.5,          # Good sleep (7-8 hours)
        'sleep_quality': 0.5,        # Normal quality
        'resting_hr': 0.5,           # Normal resting HR
        'steps': 0.5,                # Moderate activity
        'activity_level': 0.5,       # Moderate exertion
        'typing_speed': 0.5,         # Normal cognitive performance
        'reaction_time_ms': 0.5,     # Normal reaction time
        'time_of_day': 0.5,          # Neutral (9am = peak, 3pm = dip, 11pm = low)
        'mood_score': 0.5             # Neutral mood
    }
    
    # Process multivariate data if provided
    if multivariate_data:
        last_values = {}
        for feature, values in multivariate_data.items():
            if isinstance(values, list) and len(values) > 0:
                last_values[feature] = values[-1]
        
        # Map features to energy impact (normalized to 0-1, where 0.5 = neutral)
        # Higher HR during rest = fatigue/stress (lower energy)
        if 'heart_rate' in last_values:
            hr = float(last_values['heart_rate'])
            # 60 BPM = good (0.7), 100 BPM = stressed (0.3), 40 BPM = too low (0.2)
            factors['heart_rate'] = max(0.1, min(0.9, 1.0 - (hr - 60) / 80))
        
        # Higher HRV = better recovery (higher energy)
        if 'hrv' in last_values:
            hrv = float(last_values['hrv'])
            # HRV 20ms = stressed (0.2), 60ms = good (0.7), 100ms = excellent (0.9)
            factors['hrv'] = min(0.9, hrv / 120)
        
        # Sleep duration impact
        if 'sleep_hours' in last_values:
            sleep = float(last_values['sleep_hours'])
            # 5 hrs = 0.3, 7 hrs = 0.7, 9 hrs = 0.8
            factors['sleep_hours'] = min(0.9, max(0.2, (sleep - 4) / 6))
        
        # Typing speed (cognitive performance)
        if 'typing_speed' in last_values:
            typing = float(last_values['typing_speed'])
            # 40 WPM = 0.3, 70 WPM = 0.7, 100 WPM = 0.9
            factors['typing_speed'] = min(0.95, max(0.1, typing / 120))
        
        # Reaction time (lower is better)
        if 'reaction_time_ms' in last_values:
            reaction = float(last_values['reaction_time_ms'])
            # 300ms = 0.3, 200ms = 0.6, 150ms = 0.8
            factors['reaction_time_ms'] = max(0.1, 1.0 - (reaction / 400))
        
        # Step count (activity drives alertness)
        if 'steps' in last_values:
            steps = float(last_values['steps'])
            # 3000 steps = 0.4, 10000 steps = 0.8, 15000 = 0.9
            factors['steps'] = min(0.95, max(0.2, (steps / 12000)))
    
    # Generate forecast incorporating all factors
    np.random.seed(42)  # For reproducibility
    forecast = []
    last_val = float(history_array[-1])
    
    # Calculate composite factor (weighted average of all biometric factors)
    composite_factor = np.mean([v for v in factors.values()])  # 0-1 scale
    
    for i in range(horizon):
        # Trend component (decays over time)
        trend_component = recent_trend * (1 - i / (horizon * 2))
        
        # Circadian rhythm component (energy dips in afternoon, peaks in morning)
        # Simplified: assume we start in morning, dip at position 6-8, recover by 12
        circadian_dip = -5 if 5 < i <= 10 else 0
        
        # Factor-based adjustment (pushes forecast up if factors are good, down if poor)
        factor_adjustment = (composite_factor - 0.5) * 10  # Scale to ±5 points
        
        # Random noise component
        noise = np.random.normal(0, std_val * 0.25)
        
        # Mean reversion component (stronger when factors are neutral)
        mean_reversion = (mean_val - last_val) * 0.08
        
        # Combine all components
        next_val = (last_val + trend_component + circadian_dip + 
                   factor_adjustment + noise + mean_reversion)
        
        # Clamp to energy scale (0-100)
        next_val = np.clip(next_val, 10, 100)
        
        forecast.append(float(next_val))
        last_val = next_val
    
    return forecast

@app.route('/sample', methods=['GET'])
def sample_prediction():
    """
    Return a sample prediction without requiring input data.
    Useful for testing and demonstrations.
    """
    sample_history = [72, 76, 80, 74, 78, 82, 88, 75, 79, 84, 81, 77]
    forecast = generate_sample_forecast(sample_history, 12)
    
    # Print to terminal
    print("\n" + "="*60)
    print("[SAMPLE PREDICTION]")
    print("="*60)
    print(f"Input history: {sample_history}")
    print(f"Forecast (12 values): {forecast}")
    print("="*60 + "\n")
    
    return jsonify({
        "status": "success",
        "mode": "sample",
        "input": {
            "history": sample_history,
            "forecast_horizon": 12
        },
        "output": {
            "forecast": forecast
        }
    })

@app.route('/predict', methods=['POST'])
def predict_energy():
    """
    Handle prediction requests for energy level forecasting.
    
    Accepts either:
    1. Legacy format: {"history": [val1, val2, ...]}
    2. New format: {"past_values": {feature: [...], ...}, "timestamps": [...], "forecast_horizon": N}
    """
    try:
        data = request.json
        if not data:
            return jsonify({"error": "No JSON data provided"}), 400

        # Detect input format
        multivariate_data = None
        if 'past_values' in data:
            # New multivariate format
            past_values = data.get('past_values', {})
            timestamps = data.get('timestamps', [])
            forecast_horizon = data.get('forecast_horizon', 12)
            
            if not past_values or len(past_values) == 0:
                return jsonify({"error": "No past_values provided"}), 400
            
            # Extract the main time series (e.g., energy levels or heart rate as proxy)
            # For now, use the first available feature
            feature_name = list(past_values.keys())[0]
            history_values = past_values[feature_name]
            multivariate_data = past_values  # Pass all features for biometric analysis
            
            if len(history_values) == 0:
                return jsonify({"error": "Empty history in past_values"}), 400
                
            print(f"[PREDICT] Received multivariate data: features={list(past_values.keys())}, " 
                  f"history_len={len(history_values)}, forecast_horizon={forecast_horizon}", flush=True)
            
        elif 'history' in data:
            # Legacy single time series format
            history_values = data['history']
            if len(history_values) == 0:
                return jsonify({"error": "Empty history list"}), 400
            forecast_horizon = data.get('forecast_horizon', 12)
            print(f"[PREDICT] Received legacy format: history_len={len(history_values)}", flush=True)
        else:
            return jsonify({"error": "Must provide either 'history' or 'past_values'"}), 400

        # Check if remote inference is enabled
        remote_url = os.environ.get("REMOTE_PRED_URL")
        remote_api_key = os.environ.get("REMOTE_API_KEY")
        use_sample = os.environ.get("USE_SAMPLE_PREDICTION", "false").lower() == "true"
        
        if use_sample:
            # Return a sample prediction based on the input
            print(f"[SAMPLE] Generating sample prediction with horizon={forecast_horizon}", flush=True)
            sample_forecast = generate_sample_forecast(history_values, forecast_horizon, multivariate_data=multivariate_data)
            print(f"[SAMPLE] Input history: {history_values}", flush=True)
            if multivariate_data:
                print(f"[SAMPLE] Biometric factors considered: {list(multivariate_data.keys())}", flush=True)
            print(f"[SAMPLE] Generated forecast: {sample_forecast}", flush=True)
            print("="*60)
            return jsonify({
                "status": "success",
                "mode": "sample",
                "message": "Sample prediction (not from actual model)",
                "forecast": sample_forecast,
                "forecast_horizon": forecast_horizon
            })
        
        if remote_url:
            # Forward request to remote endpoint
            payload = {"history": history_values}
            headers = {"Content-Type": "application/json"}
            if remote_api_key:
                headers["Authorization"] = f"Bearer {remote_api_key}"
            
            try:
                resp = requests.post(remote_url, json=payload, headers=headers, timeout=120)
                print(f"[REMOTE] Forwarded request to {remote_url}, got status {resp.status_code}", flush=True)
                
                if resp.status_code == 200:
                    # Try to parse the remote response
                    try:
                        remote_json = resp.json()
                    except Exception:
                        return jsonify({"status": "success", "remote_response": resp.text})

                    # Prefer a 'forecast' field if present, otherwise return the whole response
                    if isinstance(remote_json, dict) and 'forecast' in remote_json:
                        return jsonify({"status": "success", "forecast": remote_json['forecast']})
                    return jsonify({"status": "success", "forecast": remote_json})
                else:
                    # Remote failed, fall back to sample generation
                    print(f"[REMOTE] Status {resp.status_code}, falling back to sample generation", flush=True)
                    
            except requests.RequestException as re:
                print(f"[REMOTE ERROR] Request to {remote_url} failed: {re}, falling back to sample", flush=True)
            
            # Fall back to sample prediction if remote failed
            print(f"[FALLBACK] Generating sample prediction with horizon={forecast_horizon}", flush=True)
            sample_forecast = generate_sample_forecast(history_values, forecast_horizon, multivariate_data=multivariate_data)
            print(f"[FALLBACK] Input history: {history_values}", flush=True)
            if multivariate_data:
                print(f"[FALLBACK] Biometric factors considered: {list(multivariate_data.keys())}", flush=True)
            print(f"[FALLBACK] Generated forecast: {sample_forecast}", flush=True)
            print("="*60)
            return jsonify({
                "status": "success",
                "mode": "sample",
                "message": "Remote inference unavailable - using sample prediction",
                "forecast": sample_forecast,
                "forecast_horizon": forecast_horizon
            })

        # Local inference path
        if tfm is None:
            print("[LOCAL] Model not loaded, returning sample prediction", flush=True)
            sample_forecast = generate_sample_forecast(history_values, forecast_horizon, multivariate_data=multivariate_data)
            print(f"[LOCAL] Input history: {history_values}", flush=True)
            if multivariate_data:
                print(f"[LOCAL] Biometric factors considered: {list(multivariate_data.keys())}", flush=True)
            print(f"[LOCAL] Generated forecast: {sample_forecast}", flush=True)
            print("="*60)
            return jsonify({
                "status": "success",
                "mode": "sample",
                "message": "Model not available - returning sample prediction",
                "forecast": sample_forecast,
                "forecast_horizon": forecast_horizon
            })

        forecast_result = tfm.forecast(
            inputs=[history_values],
            freq=[0]
        )
        prediction_list = forecast_result[0][0].tolist()
        print(f"[LOCAL] Input history: {history_values}", flush=True)
        print(f"[LOCAL] Generated forecast: {prediction_list}", flush=True)
        print("="*60)

        return jsonify({"status": "success", "mode": "local", "forecast": prediction_list})

    except Exception as e:
        print(f"Error: {e}")
        return jsonify({"error": str(e)}), 500

if __name__ == '__main__':
    print("[MAIN] Starting Flask app...", flush=True)
    sys.stdout.flush()
    try:
        print("[MAIN] About to call app.run()", flush=True)
        sys.stdout.flush()
        app.run(host='127.0.0.1', port=5000, use_reloader=False, threaded=True, debug=False)
    except Exception as e:
        print(f"[MAIN] ERROR: {e}", flush=True)
        import traceback
        traceback.print_exc()
        sys.stdout.flush()