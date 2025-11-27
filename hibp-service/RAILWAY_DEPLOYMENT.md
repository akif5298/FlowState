# Railway Deployment Guide - Step by Step

This guide will walk you through deploying the HIBP service to Railway from your monorepo.

## Prerequisites

- GitHub account
- Railway account (sign up at [railway.app](https://railway.app) - free tier available)
- Your project pushed to GitHub (with `hibp-service/` directory)

## Step 1: Push Your Code to GitHub

If you haven't already:

```bash
# From your project root
git add .
git commit -m "Add HIBP password checker service"
git push origin main
```

Make sure the `hibp-service/` directory is in your repository.

## Step 2: Sign Up / Log In to Railway

1. Go to [railway.app](https://railway.app)
2. Click **"Start a New Project"** or **"Login"**
3. Sign up with GitHub (recommended) - this connects your GitHub account

## Step 3: Create a New Project

1. Click **"New Project"** in Railway dashboard
2. Select **"Deploy from GitHub repo"**
3. Authorize Railway to access your GitHub repositories (if first time)
4. Find and select your **FlowState repository**
5. Click **"Deploy Now"**

## Step 4: Configure Root Directory

Railway will start deploying, but we need to tell it to use the `hibp-service/` subdirectory:

1. In your Railway project, click on the service that was created
2. Go to **Settings** tab
3. Scroll down to **"Root Directory"** section
4. Click **"Edit"** or the pencil icon
5. Enter: `hibp-service`
6. Click **"Save"**

**Important**: Railway will now look for `pom.xml` in the `hibp-service/` directory.

## Step 5: Configure Build Settings (Optional)

Railway should auto-detect Spring Boot, but you can verify:

1. In **Settings** → **Build**
2. **Build Command**: Should be `mvn clean package` (auto-detected)
3. **Start Command**: Should be `java -jar target/hibp-service-1.0.0.jar` (auto-detected)

If not auto-detected, set:
- **Build Command**: `mvn clean package -DskipTests`
- **Start Command**: `java -jar target/hibp-service-1.0.0.jar`

## Step 6: Set Environment Variables (If Needed)

Usually not needed, but if you want to customize:

1. Go to **Variables** tab
2. Add any environment variables (e.g., `PORT=8080` - Railway sets this automatically)

## Step 7: Wait for Deployment

1. Go to **Deployments** tab
2. Watch the build logs
3. You should see:
   ```
   Installing dependencies...
   Building application...
   Starting application...
   ```

## Step 8: Get Your Service URL

1. Once deployment completes, go to **Settings** tab
2. Scroll to **"Networking"** section
3. Click **"Generate Domain"** (if not already generated)
4. Copy the URL (e.g., `https://hibp-service-production-xxxx.up.railway.app`)

**Or** use the custom domain option if you have one.

## Step 9: Test Your Service

Test that the service is working:

```bash
# Health check
curl https://your-service-url.railway.app/actuator/health

# Test password check
curl -X POST https://your-service-url.railway.app/hibp/check \
  -H "Content-Type: application/json" \
  -d '{"password":"test"}'
```

Expected response:
```json
{"pwned":false,"count":0}
```

## Step 10: Configure Android App

1. Open `local.properties` in your project root
2. Add your Railway service URL:
   ```properties
   HIBP_SERVICE_URL=https://your-service-url.railway.app
   ```
3. Rebuild your Android app:
   ```bash
   ./gradlew clean assembleDebug
   ```

## Troubleshooting

### Build Fails: "Cannot find pom.xml"

**Solution**: Make sure Root Directory is set to `hibp-service` in Settings.

### Build Fails: "Java version not found"

**Solution**: Railway should auto-detect Java 17. If not:
1. Go to Settings → Build
2. Set **NIXPACKS_BUILD_IMAGE** to `nixpacks/java-17`

### Service Deploys But Returns 502

**Check**:
1. View logs in Railway dashboard
2. Verify the service started successfully
3. Check that port is configured correctly (Railway sets `PORT` env var automatically)

### Service URL Not Working

**Solution**:
1. Make sure domain is generated in Settings → Networking
2. Wait a few minutes for DNS propagation
3. Check that service is running (green status in dashboard)

## Railway Dashboard Overview

After deployment, you'll see:

- **Metrics**: CPU, Memory, Network usage
- **Logs**: Real-time application logs
- **Deployments**: Build history
- **Settings**: Configuration options
- **Variables**: Environment variables

## Updating Your Service

When you make changes:

1. Commit and push to GitHub:
   ```bash
   git add hibp-service/
   git commit -m "Update HIBP service"
   git push origin main
   ```

2. Railway will automatically detect the push and redeploy!

**Or** manually trigger:
- Go to Railway dashboard
- Click **"Redeploy"** button

## Free Tier Limits

Railway's free tier includes:
- $5 credit per month
- 500 hours of usage
- Perfect for this small service

If you exceed limits, you'll be notified and can upgrade.

## Next Steps

1. ✅ Service deployed to Railway
2. ✅ URL copied to `local.properties`
3. ✅ Android app configured
4. ✅ Test password checking in signup flow

Your HIBP service is now live! 🎉

