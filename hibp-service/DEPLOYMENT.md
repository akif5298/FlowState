# HIBP Service Deployment Guide

## Monorepo vs Separate Repository

### ✅ Recommended: Same Repository (Monorepo)

Keep the `hibp-service/` directory in your main FlowState repository. Most platforms support deploying from a subdirectory.

**Advantages:**
- Single source of truth
- Easier to keep code in sync
- Less repository management overhead
- All project code in one place

### Alternative: Separate Repository

Only create a separate repo if you need:
- Different access controls
- Independent versioning/releases
- Separate CI/CD pipelines

## Platform-Specific Monorepo Instructions

### Railway (Easiest)

1. **Via Dashboard:**
   - Connect your GitHub repo
   - In deployment settings, set **Root Directory** to `hibp-service`
   - Railway auto-detects Spring Boot from `pom.xml`

2. **Via CLI:**
   ```bash
   railway init
   railway link
   # Set root directory in Railway dashboard to hibp-service
   railway up
   ```

### Heroku

**Method 1: Git Subtree (Recommended)**
```bash
cd hibp-service
heroku create your-app-name
git subtree push --prefix hibp-service heroku main
```

**Method 2: GitHub Integration**
1. Connect repo in Heroku dashboard
2. Set **App to Deploy** to your repo
3. Set **Root Directory** to `hibp-service` (if supported)
4. Or use a `project.toml` file (see below)

**Method 3: project.toml** (Create in `hibp-service/`)
```toml
[build]
  root = "."
```

### Fly.io

1. **From `hibp-service/` directory:**
   ```bash
   cd hibp-service
   fly launch
   # Answer prompts - Fly.io will detect Spring Boot
   fly deploy
   ```

2. **Fly.io will create `fly.toml`** in `hibp-service/` - this is correct!

### Google Cloud Run

**Using Cloud Build:**
```bash
# From project root
gcloud run deploy hibp-service \
  --source hibp-service \
  --platform managed \
  --region us-central1
```

### AWS Elastic Beanstalk

1. Build JAR from `hibp-service/`:
   ```bash
   cd hibp-service
   mvn clean package
   ```

2. Deploy JAR:
   ```bash
   eb init -p java-17
   eb create hibp-service-env
   eb deploy
   ```

### Docker + Any Platform

Works perfectly from monorepo:
```bash
cd hibp-service
docker build -t hibp-service .
# Push to registry and deploy
```

## Verifying Monorepo Deployment

After deployment, verify:

1. **Health Check:**
   ```bash
   curl https://your-service.com/actuator/health
   ```

2. **Test Endpoint:**
   ```bash
   curl -X POST https://your-service.com/hibp/check \
     -H "Content-Type: application/json" \
     -d '{"password":"test"}'
   ```

3. **Expected Response:**
   ```json
   {"pwned":false,"count":0}
   ```

## Troubleshooting Monorepo Deployments

### Platform Can't Find pom.xml

**Solution**: Explicitly set root directory to `hibp-service/` in platform settings.

### Build Fails

**Check:**
1. Root directory is set to `hibp-service/`
2. `pom.xml` exists in `hibp-service/`
3. Java 17+ is available in build environment

### Service Works Locally But Not Deployed

**Check:**
1. Port configuration (should be `8080` or use `PORT` env var)
2. Environment variables
3. Platform logs for errors

## Recommended Setup for This Project

**Keep everything in one repo:**
```
your-repo/
├── app/                    # Android app
├── hibp-service/          # Spring Boot service
├── supabase/              # Database schema
└── README.md
```

**Deploy service from subdirectory** - most platforms handle this easily!

