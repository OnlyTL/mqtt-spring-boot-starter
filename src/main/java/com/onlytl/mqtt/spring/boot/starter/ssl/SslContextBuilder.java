package com.onlytl.mqtt.spring.boot.starter.ssl;


import com.onlytl.mqtt.spring.boot.starter.config.SslProperties;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/**
 * <p>
 * SslContextBuilder
 * </p >
 *
 * @author TL
 * @version 1.0.0
 */
public class SslContextBuilder {
    static {
        // 添加BouncyCastle安全提供者
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private final SslProperties sslProperties;

    public SslContextBuilder(SslProperties sslProperties) {
        this.sslProperties = sslProperties;
    }

    public SSLContext build() throws Exception {
        if (!sslProperties.isEnabled()) {
            return null;
        }

        SSLContext sslContext = SSLContext.getInstance(sslProperties.getProtocol());

        if (sslProperties.getCertType() == SslProperties.CertType.JKS) {
            // 使用JKS方式配置
            configureWithJks(sslContext);
        } else {
            // 使用PEM方式配置
            configureWithPem(sslContext);
        }

        return sslContext;
    }

    private void configureWithJks(SSLContext sslContext) throws Exception {
        // 信任库配置
        TrustManagerFactory tmf = null;
        if (StringUtils.hasText(sslProperties.getTrustStore())) {
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            try (InputStream is = getResource(sslProperties.getTrustStore()).getInputStream()) {
                trustStore.load(is, sslProperties.getTrustStorePassword() != null ?
                        sslProperties.getTrustStorePassword().toCharArray() : null);
            }
            tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStore);
        }

        // 密钥库配置
        KeyManagerFactory kmf = null;
        if (StringUtils.hasText(sslProperties.getKeyStore())) {
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            try (InputStream is = getResource(sslProperties.getKeyStore()).getInputStream()) {
                keyStore.load(is, sslProperties.getKeyStorePassword() != null ?
                        sslProperties.getKeyStorePassword().toCharArray() : null);
            }
            kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, sslProperties.getKeyStorePassword() != null ?
                    sslProperties.getKeyStorePassword().toCharArray() : null);
        }

        sslContext.init(
                kmf != null ? kmf.getKeyManagers() : null,
                tmf != null ? tmf.getTrustManagers() : null,
                null
        );
    }

    private void configureWithPem(SSLContext sslContext) throws Exception {
        // 加载CA证书
        X509Certificate caCert = null;
        if (StringUtils.hasText(sslProperties.getCaFile())) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            try (InputStream is = getResource(sslProperties.getCaFile()).getInputStream()) {
                caCert = (X509Certificate) cf.generateCertificate(is);
            }
        }

        // 加载客户端证书
        X509Certificate clientCert = null;
        if (StringUtils.hasText(sslProperties.getClientCertFile())) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            try (InputStream is = getResource(sslProperties.getClientCertFile()).getInputStream()) {
                clientCert = (X509Certificate) cf.generateCertificate(is);
            }
        }

        // 加载客户端私钥
        PrivateKey privateKey = null;
        if (StringUtils.hasText(sslProperties.getClientKeyFile())) {
            privateKey = loadPrivateKey(sslProperties.getClientKeyFile());
        }

        // 创建信任管理器
        TrustManagerFactory tmf = null;
        if (caCert != null) {
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, null);
            trustStore.setCertificateEntry("ca-certificate", caCert);

            tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStore);
        }

        // 创建密钥管理器
        KeyManagerFactory kmf = null;
        if (clientCert != null && privateKey != null) {
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);
            keyStore.setCertificateEntry("client-cert", clientCert);
            keyStore.setKeyEntry("client-key", privateKey,
                    sslProperties.getClientKeyPassword() != null ?
                            sslProperties.getClientKeyPassword().toCharArray() : "".toCharArray(),
                    new Certificate[]{clientCert});

            kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, sslProperties.getClientKeyPassword() != null ?
                    sslProperties.getClientKeyPassword().toCharArray() : "".toCharArray());
        }

        // 初始化SSL上下文
        sslContext.init(
                kmf != null ? kmf.getKeyManagers() : null,
                tmf != null ? tmf.getTrustManagers() : null,
                null
        );
    }

    private PrivateKey loadPrivateKey(String keyPath) throws Exception {
        try (
                InputStreamReader keyReader = new InputStreamReader(getResource(keyPath).getInputStream());
                PEMParser pemParser = new PEMParser(keyReader)
        ) {
            Object pemObject = pemParser.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
            converter.setProvider(BouncyCastleProvider.PROVIDER_NAME);

            if (pemObject instanceof PEMKeyPair) {
                PEMKeyPair keyPair = (PEMKeyPair) pemObject;
                return converter.getPrivateKey(keyPair.getPrivateKeyInfo());
            } else if (pemObject instanceof org.bouncycastle.asn1.pkcs.PrivateKeyInfo) {
                return converter.getPrivateKey((org.bouncycastle.asn1.pkcs.PrivateKeyInfo) pemObject);
            } else {
                throw new Exception("Unsupported key format: " +
                        (pemObject == null ? "null" : pemObject.getClass().getName()));
            }
        }
    }

    private Resource getResource(String location) {
        if (location.startsWith("classpath:")) {
            return new ClassPathResource(location.substring("classpath:".length()));
        } else {
            File file = new File(location);
            if (file.exists()) {
                return new FileSystemResource(file);
            } else {
                // 尝试从类路径加载
                return new ClassPathResource(location);
            }
        }
    }
}
