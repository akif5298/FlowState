#!/usr/bin/env python3
"""
Clone the google/timesfm-1.0-200m repository using git-lfs and copy the checked-out
files into the HuggingFace cache so `timesfm` can load them on Windows.

Usage (PowerShell):
  python download_with_gitlfs.py

This script will:
- ensure git-lfs is installed (via `git lfs install`)
- clone the repo into a temporary folder
- determine the commit id (snapshot)
- copy the repo into HF cache at: ~/.cache/huggingface/hub/models--google--timesfm-1.0-200m/snapshots/<commit>/
- verify presence of checkpoint files

Note: Run this in PowerShell as a user with filesystem access. This avoids xet/git-lfs transfer issues
by using the git-lfs client to fetch LFS objects.
"""
import os
import sys
import subprocess
import shutil
import tempfile
from pathlib import Path

REPO = "https://huggingface.co/google/timesfm-1.0-200m"
MODEL_DIR_NAME = "models--google--timesfm-1.0-200m"

def run(cmd, cwd=None, check=True):
    print(f"> {' '.join(cmd)}")
    res = subprocess.run(cmd, cwd=cwd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
    print(res.stdout)
    if check and res.returncode != 0:
        raise RuntimeError(f"Command failed: {' '.join(cmd)}\nOutput:\n{res.stdout}")
    return res


def main():
    # Determine HF cache dir
    hf_home_env = os.environ.get("HF_HOME")
    if hf_home_env:
        cache_base = Path(hf_home_env)
    else:
        cache_base = Path.home() / ".cache" / "huggingface" / "hub"

    target_model_dir = cache_base / MODEL_DIR_NAME
    snapshots_dir = target_model_dir / "snapshots"

    tmpdir = Path(tempfile.mkdtemp(prefix="timesfm_clone_"))
    try:
        # Ensure git-lfs installed/configured
        print("Ensuring git-lfs is installed and configured...")
        run(["git", "lfs", "install"])  # global install (no --local)

        # Clone the repo using git (this will cause git-lfs to fetch large files)
        clone_dir = tmpdir / "timesfm_repo"
        print(f"Cloning {REPO} into {clone_dir} (this may take a while)...")
        run(["git", "clone", REPO, str(clone_dir)])

        # Get commit id
        commit = None
        try:
            out = subprocess.check_output(["git", "-C", str(clone_dir), "rev-parse", "HEAD"], text=True).strip()
            commit = out
            print(f"Repository cloned at commit: {commit}")
        except subprocess.CalledProcessError:
            print("Could not determine commit id; using 'local' as snapshot name")
            commit = "local"

        # Prepare target snapshot path
        dest = snapshots_dir / commit
        if dest.exists():
            print(f"Removing existing cache snapshot at {dest}")
            shutil.rmtree(dest)
        print(f"Copying files to cache location: {dest}")
        dest.parent.mkdir(parents=True, exist_ok=True)
        shutil.copytree(clone_dir, dest)

        # Verify checkpoint presence
        expected_paths = [
            dest / "checkpoints" / "checkpoint_1100000",
            dest / "checkpoints" / "checkpoint_1100000" / "state",
        ]
        found = False
        for p in expected_paths:
            if p.exists():
                print(f"Found expected path: {p}")
                found = True
        # Also check common checkpoint file names
        for root, dirs, files in os.walk(dest):
            for fname in files:
                if fname.endswith(('.ckpt', '.pt', '.bin', '.safetensors', 'checkpoint')):
                    print(f"Found model file: {Path(root) / fname}")
                    found = True
        if not found:
            print("WARNING: No checkpoint files found in the cloned repo. The clone may not have fetched LFS objects.")
            print("Try running 'git lfs pull' inside the cloned directory or ensure your git-lfs is allowed to download objects.")
        else:
            print("✅ Model files copied to HuggingFace cache location.")
            print(f"Cache location: {dest}")

    except Exception as e:
        print(f"Error: {e}")
        sys.exit(1)
    finally:
        print(f"Cleaning up temporary folder: {tmpdir}")
        try:
            shutil.rmtree(tmpdir)
        except Exception:
            pass

if __name__ == '__main__':
    main()
