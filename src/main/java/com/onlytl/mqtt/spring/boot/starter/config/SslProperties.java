package com.onlytl.mqtt.spring.boot.starter.config;


import lombok.Data;

/**
 * <p>
 * SslProperties
 * </p >
 *
 * @author TL
 * @version 1.0.0
 */
@Data
public class SslProperties {

    /**
     * 是否启用SSL
     */
    private boolean enabled = false;

    /**
     * 协议，如TLSv1.2
     */
    private String protocol = "TLSv1.2";

    /**
     * 证书类型，支持：PEM, JKS
     */
    private CertType certType = CertType.PEM;

    /**
     * CA证书路径
     */
    private String caFile;

    /**
     * 客户端证书路径
     */
    private String clientCertFile;

    /**
     * 客户端私钥路径
     */
    private String clientKeyFile;

    /**
     * 客户端私钥密码
     */
    private String clientKeyPassword;

    /**
     * 信任库路径（JKS类型）
     */
    private String trustStore;

    /**
     * 信任库密码
     */
    private String trustStorePassword;

    /**
     * 密钥库路径（JKS类型）
     */
    private String keyStore;

    /**
     * 密钥库密码
     */
    private String keyStorePassword;

    /**
     * 是否验证主机名
     */
    private boolean verifyHostname = true;

    public enum CertType {
        /**
         * PEM格式证书
         */
        PEM,

        /**
         * Java KeyStore
         */
        JKS
    }
}
