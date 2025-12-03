#!/usr/bin/env python3
"""
Standalone model downloader for TimesFM - works around Windows permission issues
"""
import os
import sys
from pathlib import Path

# Configure before importing huggingface_hub
current_dir = Path(__file__).parent
cache_dir = current_dir / "my_model_cache"
os.environ["HF_HOME"] = str(cache_dir)
os.environ["HF_HUB_DISABLE_SYMLINKS"] = "1"
os.environ["HF_HUB_ENABLE_XET_SYMLINK"] = "0"

print(f"📁 Model cache: {cache_dir}")
print(f"🔧 Downloading TimesFM model (814MB)...")
print(f"⏱️  This may take 10-30 minutes depending on your internet connection")
print()

try:
    from huggingface_hub import snapshot_download
    
    # Ensure cache directory exists
    cache_dir.mkdir(parents=True, exist_ok=True)
    
    # Download the model
    repo_id = "google/timesfm-1.0-200m"
    print(f"⬇️  Downloading {repo_id}...")
    
    local_dir = snapshot_download(
        repo_id=repo_id,
        local_dir=str(cache_dir / "hub" / f"models--{repo_id.replace('/', '--')}"),
        resume_download=True,
        force_download=False,
    )
    
    print()
    print(f"✅ Download complete!")
    print(f"📦 Model saved to: {local_dir}")
    print()
    print("You can now run: python app.py")
    
except Exception as e:
    print(f"❌ Download failed: {e}")
    print()
    print("TROUBLESHOOTING:")
    print("1. Check your internet connection")
    print("2. Ensure you have at least 2GB free disk space")
    print("3. Try again - the download can be resumed")
    sys.exit(1)
