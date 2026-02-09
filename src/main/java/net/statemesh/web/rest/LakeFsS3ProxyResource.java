package net.statemesh.web.rest;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.statemesh.config.ApplicationProperties;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.*;
import java.net.URI;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/lakefs-s3")
@RequiredArgsConstructor
public class LakeFsS3ProxyResource {

    private final ApplicationProperties properties;

    @RequestMapping("/**")
    public ResponseEntity<byte[]> proxy(HttpServletRequest request) {
        try {
            String path = request.getRequestURI().substring("/api/lakefs-s3".length());
            String query = request.getQueryString();

            String s3Endpoint = Optional.ofNullable(properties.getK8sAccessMode()).orElse(false)
                ? properties.getLakeFs().getS3EndpointInternal()
                : properties.getLakeFs().getS3Endpoint();

            String targetUrl = s3Endpoint + path;
            if (query != null) {
                targetUrl += "?" + query;
            }

            HttpHeaders headers = new HttpHeaders();
            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                String lower = headerName.toLowerCase();
                if (lower.equals("host") || lower.equals("connection") ||
                    lower.equals("origin") || lower.equals("referer")) {
                    continue;
                }
                Enumeration<String> values = request.getHeaders(headerName);
                while (values.hasMoreElements()) {
                    headers.add(headerName, values.nextElement());
                }
            }

            // Inject LakeFS S3 basic auth
            String auth = properties.getLakeFs().getKey() + ":" + properties.getLakeFs().getSecret();
            String encodedAuth = java.util.Base64.getEncoder().encodeToString(auth.getBytes());
            headers.set("Authorization", "Basic " + encodedAuth);

            HttpMethod method = HttpMethod.valueOf(request.getMethod());
            byte[] body = request.getInputStream().readAllBytes();
            HttpEntity<byte[]> entity = new HttpEntity<>(body, headers);

            RestTemplate restTemplate = createTrustAllRestTemplate();
            ResponseEntity<byte[]> response = restTemplate.exchange(
                URI.create(targetUrl), method, entity, byte[].class);

            HttpHeaders responseHeaders = new HttpHeaders();
            response.getHeaders().forEach((name, values) -> {
                if (!name.equalsIgnoreCase("transfer-encoding")) {
                    responseHeaders.addAll(name, values);
                }
            });

            return new ResponseEntity<>(response.getBody(), responseHeaders, response.getStatusCode());
        } catch (HttpClientErrorException e) {
            return new ResponseEntity<>(e.getResponseBodyAsByteArray(), e.getResponseHeaders(), e.getStatusCode());
        } catch (Exception e) {
            log.error("S3 proxy error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }

    private RestTemplate createTrustAllRestTemplate() {
        try {
            TrustManager[] trustAll = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return null; }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                }
            };
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAll, new java.security.SecureRandom());

            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);

            return new RestTemplate();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create trust-all RestTemplate", e);
        }
    }
}
