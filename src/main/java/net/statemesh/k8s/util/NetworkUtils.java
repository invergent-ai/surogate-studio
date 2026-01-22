package net.statemesh.k8s.util;

import io.kubernetes.client.util.KubeConfig;
import net.statemesh.service.dto.ClusterDTO;
import okhttp3.OkHttpClient;
import org.apache.commons.codec.binary.Base64;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.InetAddress;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class NetworkUtils {
    public static boolean ping(String ipAddress, int timeout) {
        try {
            InetAddress inet = InetAddress.getByName(ipAddress);
            return inet.isReachable(timeout);
        } catch (IOException e) {
            return false;
        }
    }

    public static OkHttpClient kubeletClient(ClusterDTO clusterDTO) throws Exception {
        KubeConfig kubeConfig = KubeConfig.loadKubeConfig(new StringReader(clusterDTO.getKubeConfig()));
        var clientCert = Base64.decodeBase64(kubeConfig.getClientCertificateData());
        var clientKey = Base64.decodeBase64(kubeConfig.getClientKeyData());

        ByteArrayInputStream certInputStream = new ByteArrayInputStream(clientCert);
        ByteArrayInputStream keyInputStream = new ByteArrayInputStream(clientKey);

        // Load the client certificate
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate cert = (X509Certificate) cf.generateCertificate(certInputStream);

        // Load the client private key
        PEMParser pemParser = new PEMParser(new InputStreamReader(keyInputStream));
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");
        PEMKeyPair keyPair = (PEMKeyPair) pemParser.readObject();
        PrivateKey privateKey = converter.getPrivateKey(keyPair.getPrivateKeyInfo());
        pemParser.close();

        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, new char[0]);
        keyStore.setKeyEntry("key", privateKey, new char[0], new X509Certificate[]{cert});

        // Create a KeyManagerFactory and initialize it with the key store
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, "".toCharArray());

        // Create an SSLContext with the key managers
        final TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[]{};
                }
            }
        };
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), trustAllCerts, null);

        // Create an OkHttpClient with the SSLContext
        return new OkHttpClient.Builder()
            .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustAllCerts[0])
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
            .hostnameVerifier((hostname, session) -> true)
            .build();
    }
}
