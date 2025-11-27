package com.flowstate.hibp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

@SpringBootApplication
@RestController
@RequestMapping("/hibp")
public class PasswordPwnedApplication {

    private static final HttpClient httpClient = HttpClient.newHttpClient();

    public static void main(String[] args) {
        SpringApplication.run(PasswordPwnedApplication.class, args);
    }

    record CheckRequest(
            @JsonProperty("password") String password,
            @JsonProperty("sha1") String sha1
    ) {}

    record CheckResponse(
            @JsonProperty("pwned") boolean pwned,
            @JsonProperty("count") int count
    ) {}

    @PostMapping(
            value = "/check",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<CheckResponse> checkPassword(@RequestBody CheckRequest req) throws Exception {
        String sha1;

        if (req.password() != null && !req.password().isBlank()) {
            sha1 = sha1Hex(req.password());
        } else if (req.sha1() != null && !req.sha1().isBlank()) {
            sha1 = req.sha1().toUpperCase(Locale.ROOT);
            if (!sha1.matches("^[0-9A-F]{40}$")) {
                return ResponseEntity.badRequest().build();
            }
        } else {
            return ResponseEntity.badRequest().build();
        }

        String prefix = sha1.substring(0, 5);
        String suffix = sha1.substring(5);

        HttpRequest httpReq = HttpRequest.newBuilder()
                .uri(URI.create("https://api.pwnedpasswords.com/range/" + prefix))
                .header("User-Agent", "FlowState-HIBP-Check/1.0")
                .GET()
                .build();

        HttpResponse<String> httpResp = httpClient.send(
                httpReq,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );

        if (httpResp.statusCode() != 200) {
            // Propagate as 502 Bad Gateway (or choose other handling)
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(new CheckResponse(false, 0));
        }

        String body = httpResp.body();
        String[] lines = body.split("\\r?\\n");

        for (String line : lines) {
            String[] parts = line.split(":");
            if (parts.length != 2) continue;

            String returnedSuffix = parts[0].trim().toUpperCase(Locale.ROOT);
            int count = Integer.parseInt(parts[1].trim());

            if (returnedSuffix.equalsIgnoreCase(suffix)) {
                return ResponseEntity.ok(new CheckResponse(true, count));
            }
        }

        return ResponseEntity.ok(new CheckResponse(false, 0));
    }

    private static String sha1Hex(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));

        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02X", b));
        }

        return sb.toString();
    }
}

