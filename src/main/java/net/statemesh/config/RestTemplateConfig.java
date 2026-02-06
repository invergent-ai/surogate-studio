package net.statemesh.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.*;
import java.net.Socket;
import java.net.http.HttpClient;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.function.Function;

@Configuration
public class RestTemplateConfig {
    @Bean
    @Primary
    public RestTemplate restTemplate() {
        return buildRestTemplate(
            (builder) -> builder
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .connectTimeout(Duration.of(10, ChronoUnit.SECONDS))
                .build()
        );
    }


    @Bean(name = "vllmRestTemplate")
    public RestTemplate vllmRestTemplate() {
        return buildRestTemplate(
            (builder) -> builder
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(Duration.of(5, ChronoUnit.MINUTES))
                .build()
        );
    }

    private RestTemplate buildRestTemplate(Function<HttpClient.Builder, HttpClient> httpClientFunction) {
        TrustManager[] trustAllCerts = new TrustManager[] {
            new X509ExtendedTrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {}

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) {}

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) {}

                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) {}

                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {}

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {}

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            }
        };

        final SSLContext sslContext;
        try {
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException(e);
        }
        SSLParameters sslParameters = new SSLParameters();
        sslParameters.setEndpointIdentificationAlgorithm(null);

        return new RestTemplate(new JdkClientHttpRequestFactory(
            httpClientFunction.apply(
                defaultHttpClientBuilder(sslContext, sslParameters)
            )
        ));
    }

    private HttpClient.Builder defaultHttpClientBuilder(SSLContext sslContext, SSLParameters sslParameters) {
        return HttpClient.newBuilder()
            .sslContext(sslContext)
            .sslParameters(sslParameters)
            .executor(Executors.newVirtualThreadPerTaskExecutor());
    }
}
