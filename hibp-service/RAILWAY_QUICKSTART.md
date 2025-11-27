# Railway Quick Start Checklist

Follow these steps in order:

## ✅ Pre-Deployment Checklist

- [ ] Code is pushed to GitHub (including `hibp-service/` directory)
- [ ] Railway account created at [railway.app](https://railway.app)
- [ ] GitHub account connected to Railway

## 🚀 Deployment Steps

### 1. Create Railway Project
- [ ] Go to Railway dashboard
- [ ] Click **"New Project"**
- [ ] Select **"Deploy from GitHub repo"**
- [ ] Choose your FlowState repository
- [ ] Click **"Deploy Now"**

### 2. Configure Root Directory
- [ ] Click on the service in Railway
- [ ] Go to **Settings** tab
- [ ] Find **"Root Directory"** section
- [ ] Set to: `hibp-service`
- [ ] Click **"Save"**

### 3. Wait for Build
- [ ] Watch **Deployments** tab
- [ ] Wait for build to complete (2-5 minutes)
- [ ] Check that status is **"Active"** (green)

### 4. Get Service URL
- [ ] Go to **Settings** → **Networking**
- [ ] Click **"Generate Domain"**
- [ ] Copy the URL (e.g., `https://hibp-service-production-xxxx.up.railway.app`)

### 5. Test Service
- [ ] Open terminal/command prompt
- [ ] Run: `curl https://your-url.railway.app/actuator/health`
- [ ] Should return: `{"status":"UP"}`

### 6. Configure Android App
- [ ] Open `local.properties` in project root
- [ ] Add: `HIBP_SERVICE_URL=https://your-url.railway.app`
- [ ] Save file
- [ ] Rebuild app: `./gradlew clean assembleDebug`

## 🎉 Done!

Your service is now live and integrated with your Android app!

## Need Help?

- See `RAILWAY_DEPLOYMENT.md` for detailed instructions
- Check Railway logs if something fails
- Verify Root Directory is set correctly

## Common Issues

**Build fails?** → Check Root Directory is `hibp-service`

**Service not accessible?** → Generate domain in Settings → Networking

**502 error?** → Check logs, verify service started successfully

