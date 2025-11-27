# HIBP Password Checker Integration Guide

This guide explains how to deploy the HIBP password checker service and integrate it with your FlowState Android app.

## Quick Start

### 1. Deploy the Service

**You can keep the service in the same repository!** Most platforms support monorepo deployments (deploying from a subdirectory).

Choose one of these deployment options:

#### Option A: Railway (Recommended - Easiest, Monorepo Support)
1. Push your entire project (including `hibp-service/`) to GitHub
2. Go to [railway.app](https://railway.app)
3. Click "New Project" → "Deploy from GitHub repo"
4. Select your repository
5. In the deployment settings, set **Root Directory** to `hibp-service`
6. Railway will auto-detect Spring Boot and deploy
7. Copy the deployment URL (e.g., `https://your-app.railway.app`)

**Note**: Railway automatically detects the subdirectory if it contains a `pom.xml` or `build.gradle`.

#### Option B: Heroku (Monorepo Support)
```bash
# From project root
cd hibp-service
heroku create your-app-name
# Heroku will detect pom.xml in current directory
git subtree push --prefix hibp-service heroku main
# OR use Heroku CLI with subdirectory:
heroku git:remote -a your-app-name
git push heroku `git subtree split --prefix hibp-service main`:main --force
```

**Alternative**: Use Heroku's monorepo buildpack or deploy via GitHub integration with root directory set to `hibp-service`.

#### Option C: Fly.io (Monorepo Support)
```bash
cd hibp-service
fly launch
# When prompted, Fly.io will detect the Spring Boot app
# Edit fly.toml if needed to set correct paths
fly deploy
fly open
```

**Note**: Fly.io will create a `fly.toml` in the `hibp-service/` directory. This is fine - it's scoped to that service.

#### Option D: Docker (Any Platform - Works from Same Repo)
```bash
# From project root
cd hibp-service
docker build -t hibp-service .
docker run -p 8080:8080 hibp-service
```

#### Option E: Separate Repository (Optional)
If you prefer a separate repo:
1. Create a new GitHub repository
2. Copy only the `hibp-service/` directory contents
3. Push to the new repo
4. Deploy from that repo

**Pros of separate repo:**
- Cleaner separation
- Independent versioning
- Easier to set different access controls

**Pros of same repo (monorepo):**
- Everything in one place
- Easier to keep in sync
- Single source of truth
- Less overhead for small projects

**Recommendation**: For this project size, keep it in the same repo (monorepo approach).

### 2. Configure Android App

1. Add the HIBP service URL to `local.properties`:
   ```properties
   HIBP_SERVICE_URL=https://your-hibp-service.com
   ```

2. Rebuild the app:
   ```bash
   ./gradlew clean assembleDebug
   ```

### 3. Test the Integration

1. Open the FlowState app
2. Go to Sign Up
3. Try entering a known compromised password (e.g., "password123")
4. You should see an error: "This password has been found in X data breaches"

## How It Works

### Service Endpoint

**POST** `/hibp/check`

**Request:**
```json
{
  "password": "user-password"
}
```

**Response (200 OK):**
```json
{
  "pwned": true,
  "count": 12345
}
```

### Android Integration

The `SignUpActivity` automatically checks passwords before signup:

1. User enters password and clicks "Sign Up"
2. App calls HIBP service to check password
3. If password is pwned → Show error, block signup
4. If password is safe → Proceed with Supabase signup
5. If service unavailable → Log warning but allow signup (fail open)

### Code Flow

```
SignUpActivity.signUpUser()
    ↓
HibpPasswordChecker.checkPassword()
    ↓
POST /hibp/check → HIBP Service
    ↓
HIBP Service → HaveIBeenPwned API
    ↓
Response: { pwned: true/false, count: N }
    ↓
SignUpActivity → Block or Proceed
```

## Security Considerations

### Fail-Open Strategy

The Android app uses a **fail-open** strategy:
- If HIBP service is unavailable, signup proceeds
- This prevents service outages from blocking legitimate users
- Errors are logged but don't block signup

To change to **fail-closed** (block signup if service unavailable), modify `SignUpActivity.java`:

```java
@Override
public void onError(Exception error) {
    btnSignUp.setEnabled(true);
    btnSignUp.setText("Sign Up");
    Snackbar.make(rootView, "Password check service unavailable. Please try again later.", Snackbar.LENGTH_LONG).show();
    // Don't call performSignUp() - block signup
}
```

### Rate Limiting

Consider adding rate limiting to prevent abuse:

1. **Service-side**: Add Spring Boot rate limiting
2. **Client-side**: Cache results for a short period
3. **IP-based**: Limit requests per IP address

### HTTPS Only

Always deploy the service with HTTPS enabled. Most platforms (Railway, Heroku, Fly.io) provide HTTPS automatically.

## Troubleshooting

### Service Returns 502 Bad Gateway

- HIBP API might be down
- Check service logs
- Service will return `{ pwned: false, count: 0 }` on error

### Android App Can't Connect

1. Check `HIBP_SERVICE_URL` in `local.properties`
2. Verify service is running: `curl https://your-service.com/actuator/health`
3. Check Android network permissions in `AndroidManifest.xml`
4. Verify CORS is configured if calling from web

### Password Check Always Fails

1. Verify service URL is correct
2. Check service logs for errors
3. Test service directly: `curl -X POST https://your-service.com/hibp/check -H "Content-Type: application/json" -d '{"password":"test"}'`

## Production Checklist

- [ ] Service deployed with HTTPS
- [ ] `HIBP_SERVICE_URL` configured in `local.properties`
- [ ] Service health check endpoint working
- [ ] Tested with known compromised password
- [ ] Tested with safe password
- [ ] Tested service unavailable scenario
- [ ] CORS configured (if needed for web)
- [ ] Rate limiting configured (optional)
- [ ] Monitoring/logging set up

## Support

For issues or questions:
1. Check service logs
2. Verify service is accessible
3. Test endpoint directly with `curl` or Postman
4. Check Android logcat for errors

