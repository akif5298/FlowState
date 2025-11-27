# HIBP Password Checker Service

A minimal Spring Boot service that checks passwords against the HaveIBeenPwned (HIBP) API to prevent users from using compromised passwords.

## Features

- Accepts JSON POST requests with either plain-text password or SHA-1 hash
- Queries HIBP API using the range API (k-anonymity model)
- Returns JSON response with `pwned` boolean and breach `count`
- No logging of sensitive data (passwords or full hashes)
- Proper HTTP status codes (200 OK, 400 Bad Request, 502 Bad Gateway)

## API Endpoint

### POST `/hibp/check`

**Request Body:**
```json
{
  "password": "plain-text-password"
}
```
OR
```json
{
  "sha1": "UPPERCASE_SHA1_HASH"
}
```

**Response (200 OK):**
```json
{
  "pwned": true,
  "count": 12345
}
```
OR
```json
{
  "pwned": false,
  "count": 0
}
```

**Error Responses:**
- `400 Bad Request`: Invalid request (missing password/sha1, invalid SHA-1 format)
- `502 Bad Gateway`: HIBP API unavailable or returned error

## Requirements

- Java 17+
- Maven 3.6+

## Building

```bash
cd hibp-service
mvn clean package
```

## Running

### Local Development
```bash
mvn spring-boot:run
```

### Using JAR
```bash
java -jar target/hibp-service-1.0.0.jar
```

### Using Docker
```bash
docker build -t hibp-service .
docker run -p 8080:8080 hibp-service
```

### Using Docker Compose
```bash
docker-compose up
```

## Deployment Options

### Heroku
```bash
heroku create your-app-name
git push heroku main
```

### Railway
1. Connect your GitHub repository
2. Railway will auto-detect Spring Boot
3. Deploy automatically

### Fly.io
```bash
fly launch
fly deploy
```

### AWS Elastic Beanstalk
1. Package the JAR
2. Upload via AWS Console or EB CLI

### Google Cloud Run
```bash
gcloud run deploy hibp-service --source .
```

## Integration with Supabase

### Client-Side (Android App)

Before creating a user with Supabase Auth:

```java
// Check password against HIBP
OkHttpClient client = new OkHttpClient();
RequestBody body = RequestBody.create(
    "{\"password\":\"" + password + "\"}",
    MediaType.parse("application/json")
);
Request request = new Request.Builder()
    .url("https://your-hibp-service.com/hibp/check")
    .post(body)
    .build();

Response response = client.newCall(request).execute();
if (response.isSuccessful()) {
    JSONObject json = new JSONObject(response.body().string());
    if (json.getBoolean("pwned")) {
        // Show error: "This password has been found in data breaches"
        return;
    }
}
// Proceed with Supabase signup
```

### Server-Side (Supabase Edge Function)

Create a Supabase Edge Function that calls this service:

```typescript
// supabase/functions/check-password/index.ts
import { serve } from "https://deno.land/std@0.168.0/http/server.ts"

serve(async (req) => {
  const { password } = await req.json()
  
  const response = await fetch('https://your-hibp-service.com/hibp/check', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ password })
  })
  
  const result = await response.json()
  return new Response(JSON.stringify(result), {
    headers: { 'Content-Type': 'application/json' }
  })
})
```

## Security Best Practices

1. **No Logging**: The service does not log passwords or full SHA-1 hashes
2. **HTTPS Only**: Always deploy with TLS/HTTPS enabled
3. **Rate Limiting**: Consider adding rate limiting per IP to prevent abuse
4. **CORS**: Configure CORS appropriately for your client origins
5. **User-Agent**: Includes proper User-Agent header as recommended by HIBP

## CORS Configuration

If you need to call this from a web browser, add CORS configuration:

```java
@Configuration
public class CorsConfig {
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/hibp/**")
                        .allowedOrigins("https://your-app.com")
                        .allowedMethods("POST", "OPTIONS")
                        .allowedHeaders("*");
            }
        };
    }
}
```

## Health Check

The service includes Spring Boot Actuator for health checks:

```
GET /actuator/health
```

## License

MIT

