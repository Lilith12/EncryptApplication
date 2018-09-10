package com.example.aleksandra.encryptapplication;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Arrays;
import java.util.Collection;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import io.socket.client.IO;
import io.socket.client.Socket;
import okhttp3.OkHttpClient;
import okio.Buffer;

public class SocketConnect {
    private static final String hostname = "https://encrypt-app.tk:8085";
    private static final String localhostHostname = "http://192.168.1.5:8085";
    //netstat -r 0.0.0.0

    private static Socket mSocket;

    private SocketConnect() {

    }

    public static Socket getSocket() {
        if (mSocket == null) {
            try {
                createSSLConnection();
//                createLocalConnection();
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }
        return mSocket;
    }

    private static void createSSLConnection() throws URISyntaxException {
        IO.Options opts = new IO.Options();

        X509TrustManager trustManager;
        SSLSocketFactory sslSocketFactory;
        try {
            trustManager = trustManagerForCertificates(trustedCertificatesInputStream());
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{trustManager}, null);
            sslSocketFactory = sslContext.getSocketFactory();
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .sslSocketFactory(sslSocketFactory, trustManager)
                .build();
        opts.callFactory = okHttpClient;
        opts.webSocketFactory = okHttpClient;
        mSocket = IO.socket(hostname, opts);
    }

    private static void createLocalConnection() throws URISyntaxException {
        mSocket = IO.socket(localhostHostname);
    }

    private static X509TrustManager trustManagerForCertificates(InputStream in)
            throws GeneralSecurityException {
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509", "BC");
        Collection<? extends Certificate> certificates = certificateFactory.generateCertificates(
                in);
        if (certificates.isEmpty()) {
            throw new IllegalArgumentException("expected non-empty set of trusted certificates");
        }

        // Put the certificates a key store.
        char[] password = "password".toCharArray(); // Any password will work.
        KeyStore keyStore = newEmptyKeyStore(password);
        int index = 0;
        for (Certificate certificate : certificates) {
            String certificateAlias = Integer.toString(index++);
            keyStore.setCertificateEntry(certificateAlias, certificate);
        }

        // Use it to build an X509 trust manager.
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(
                KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, password);
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keyStore);
        TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
        if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
            throw new IllegalStateException("Unexpected default trust managers:"
                    + Arrays.toString(trustManagers));
        }
        return (X509TrustManager) trustManagers[0];
    }

    private static KeyStore newEmptyKeyStore(char[] password) throws GeneralSecurityException {
        try {
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            InputStream in = null; // By convention, 'null' creates an empty key store.
            keyStore.load(in, password);
            return keyStore;
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }


    private static InputStream trustedCertificatesInputStream() {
        // PEM files for root certificates of Comodo and Entrust. These two CAs are sufficient to view
        // https://publicobject.com (Comodo) and https://squareup.com (Entrust). But they aren't
        // sufficient to connect to most HTTPS sites including https://godaddy.com and https://visa.com.
        // Typically developers will need to get a PEM file from their organization's TLS administrator.
        String comodoRsaCertificationAuthority = ""
                + "-----BEGIN CERTIFICATE-----\n"
                + "MIIGHTCCBQWgAwIBAgISA+M66TGl2ATht1TlWN/TpnTWMA0GCSqGSIb3DQEBCwUA\n"
                + "MEoxCzAJBgNVBAYTAlVTMRYwFAYDVQQKEw1MZXQncyBFbmNyeXB0MSMwIQYDVQQD\n"
                + "ExpMZXQncyBFbmNyeXB0IEF1dGhvcml0eSBYMzAeFw0xODA5MDExMDU4NDZaFw0x\n"
                + "ODExMzAxMDU4NDZaMBkxFzAVBgNVBAMTDmVuY3J5cHQtYXBwLnRrMIIBIjANBgkq\n"
                + "hkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA6Dn2Hj4IfrsLp1Eqtb/aWJLWtHmo/Jk8\n"
                + "BK+knsv8OBDJKNmUOPzoZGd9VSlLG4VarH6esZU/HaYsuX/ZtOR80c4e577z1+IM\n"
                + "9/MfIKIyiiAzge9dA17muPkAKU3kOJQTTQlqNk+zuHGbqi2Zvt/Xt3YNp8xfKAuq\n"
                + "vNy7Dn0iRGfJ6hVvz2xQBfKLtsrxyeD9TJ2EBeoJh0aIcZdU/JLlQewfiY392S1U\n"
                + "x2wcsSeQeYTk9fgdCjWt7Fq9B1evLhxjGGwFEU99i92igFKg2hkvrR58E1kB0M2U\n"
                + "YD0v65Xl3zFzYg4RvCvOgQI/zg1oyin+r2Z/nOP/6gZObLDgWDVX9wIDAQABo4ID\n"
                + "LDCCAygwDgYDVR0PAQH/BAQDAgWgMB0GA1UdJQQWMBQGCCsGAQUFBwMBBggrBgEF\n"
                + "BQcDAjAMBgNVHRMBAf8EAjAAMB0GA1UdDgQWBBSukxzYKX6J/qZt/a9BkvhMpu9S\n"
                + "WTAfBgNVHSMEGDAWgBSoSmpjBH3duubRObemRWXv86jsoTBvBggrBgEFBQcBAQRj\n"
                + "MGEwLgYIKwYBBQUHMAGGImh0dHA6Ly9vY3NwLmludC14My5sZXRzZW5jcnlwdC5v\n"
                + "cmcwLwYIKwYBBQUHMAKGI2h0dHA6Ly9jZXJ0LmludC14My5sZXRzZW5jcnlwdC5v\n"
                + "cmcvMC0GA1UdEQQmMCSCDmVuY3J5cHQtYXBwLnRrghJ3d3cuZW5jcnlwdC1hcHAu\n"
                + "dGswgf4GA1UdIASB9jCB8zAIBgZngQwBAgEwgeYGCysGAQQBgt8TAQEBMIHWMCYG\n"
                + "CCsGAQUFBwIBFhpodHRwOi8vY3BzLmxldHNlbmNyeXB0Lm9yZzCBqwYIKwYBBQUH\n"
                + "AgIwgZ4MgZtUaGlzIENlcnRpZmljYXRlIG1heSBvbmx5IGJlIHJlbGllZCB1cG9u\n"
                + "IGJ5IFJlbHlpbmcgUGFydGllcyBhbmQgb25seSBpbiBhY2NvcmRhbmNlIHdpdGgg\n"
                + "dGhlIENlcnRpZmljYXRlIFBvbGljeSBmb3VuZCBhdCBodHRwczovL2xldHNlbmNy\n"
                + "eXB0Lm9yZy9yZXBvc2l0b3J5LzCCAQYGCisGAQQB1nkCBAIEgfcEgfQA8gB3ACk8\n"
                + "UZZUyDlluqpQ/FgH1Ldvv1h6KXLcpMMM9OVFR/R4AAABZZT/84YAAAQDAEgwRgIh\n"
                + "AJSLNf4MLMxdRIxYj2gzGYI2//zhmbDaxbqdHc9QLVvmAiEAjwiMey7Uq6Nf+HnE\n"
                + "OrdB9sLsyxG99Q/8rf6ZgpKKOZQAdwBvU3asMfAxGdiZAKRRFf93FRwR2QLBACkG\n"
                + "jbIImjfZEwAAAWWU//RAAAAEAwBIMEYCIQCsowzR9c2GPziLr87be7uBiQVKmEr3\n"
                + "4SlX+YnBUr7xeQIhALJAG6U/tcWF6Vsuix+8geoWZjRwk/29EXwqOIwH9NRnMA0G\n"
                + "CSqGSIb3DQEBCwUAA4IBAQAEwKxo9+sa27lAoBjOxHDqK3KeUxJ7G8L5q1F91RrB\n"
                + "fBzB4YdjYxznmzv8DzdJ23Pj4Zs+8H0JX4/YLMIH9XEtVsM/OxcaLbr9+HGtI1Mw\n"
                + "LJllvJbYSOzqK1jCmsTN8Jk/iZlOP9QMcY9Ygz1BHUWjlJLxgqkcIvc8BWPVic6M\n"
                + "FLH1EggyV1YnHtGoa4KD8n1aludo27lgFc2SzCoisMrqheEUtVs/1BPGVqoN1vio\n"
                + "kzDEswcANz6CuTyuDCqvlM/b7ZaOJDyOlWmb/MMIA1yzNjumTLKqW7mxT037kuPs\n"
                + "ch4t9qcdkLkQjaG4dr0atMq7LTbp9jpNTe+tXCPG4Jfe\n"
                + "-----END CERTIFICATE-----\n";
        String entrustRootCertificateAuthority = ""
                + "-----BEGIN CERTIFICATE-----\n"
                + "MIIEkjCCA3qgAwIBAgIQCgFBQgAAAVOFc2oLheynCDANBgkqhkiG9w0BAQsFADA/\n"
                + "MSQwIgYDVQQKExtEaWdpdGFsIFNpZ25hdHVyZSBUcnVzdCBDby4xFzAVBgNVBAMT\n"
                + "DkRTVCBSb290IENBIFgzMB4XDTE2MDMxNzE2NDA0NloXDTIxMDMxNzE2NDA0Nlow\n"
                + "SjELMAkGA1UEBhMCVVMxFjAUBgNVBAoTDUxldCdzIEVuY3J5cHQxIzAhBgNVBAMT\n"
                + "GkxldCdzIEVuY3J5cHQgQXV0aG9yaXR5IFgzMIIBIjANBgkqhkiG9w0BAQEFAAOC\n"
                + "AQ8AMIIBCgKCAQEAnNMM8FrlLke3cl03g7NoYzDq1zUmGSXhvb418XCSL7e4S0EF\n"
                + "q6meNQhY7LEqxGiHC6PjdeTm86dicbp5gWAf15Gan/PQeGdxyGkOlZHP/uaZ6WA8\n"
                + "SMx+yk13EiSdRxta67nsHjcAHJyse6cF6s5K671B5TaYucv9bTyWaN8jKkKQDIZ0\n"
                + "Z8h/pZq4UmEUEz9l6YKHy9v6Dlb2honzhT+Xhq+w3Brvaw2VFn3EK6BlspkENnWA\n"
                + "a6xK8xuQSXgvopZPKiAlKQTGdMDQMc2PMTiVFrqoM7hD8bEfwzB/onkxEz0tNvjj\n"
                + "/PIzark5McWvxI0NHWQWM6r6hCm21AvA2H3DkwIDAQABo4IBfTCCAXkwEgYDVR0T\n"
                + "AQH/BAgwBgEB/wIBADAOBgNVHQ8BAf8EBAMCAYYwfwYIKwYBBQUHAQEEczBxMDIG\n"
                + "CCsGAQUFBzABhiZodHRwOi8vaXNyZy50cnVzdGlkLm9jc3AuaWRlbnRydXN0LmNv\n"
                + "bTA7BggrBgEFBQcwAoYvaHR0cDovL2FwcHMuaWRlbnRydXN0LmNvbS9yb290cy9k\n"
                + "c3Ryb290Y2F4My5wN2MwHwYDVR0jBBgwFoAUxKexpHsscfrb4UuQdf/EFWCFiRAw\n"
                + "VAYDVR0gBE0wSzAIBgZngQwBAgEwPwYLKwYBBAGC3xMBAQEwMDAuBggrBgEFBQcC\n"
                + "ARYiaHR0cDovL2Nwcy5yb290LXgxLmxldHNlbmNyeXB0Lm9yZzA8BgNVHR8ENTAz\n"
                + "MDGgL6AthitodHRwOi8vY3JsLmlkZW50cnVzdC5jb20vRFNUUk9PVENBWDNDUkwu\n"
                + "Y3JsMB0GA1UdDgQWBBSoSmpjBH3duubRObemRWXv86jsoTANBgkqhkiG9w0BAQsF\n"
                + "AAOCAQEA3TPXEfNjWDjdGBX7CVW+dla5cEilaUcne8IkCJLxWh9KEik3JHRRHGJo\n"
                + "uM2VcGfl96S8TihRzZvoroed6ti6WqEBmtzw3Wodatg+VyOeph4EYpr/1wXKtx8/\n"
                + "wApIvJSwtmVi4MFU5aMqrSDE6ea73Mj2tcMyo5jMd6jmeWUHK8so/joWUoHOUgwu\n"
                + "X4Po1QYz+3dszkDqMp4fklxBwXRsW10KXzPMTZ+sOPAveyxindmjkW8lGy+QsRlG\n"
                + "PfZ+G6Z6h7mjem0Y+iWlkYcV4PIWL1iwBi8saCbGS5jN2p8M+X+Q7UNKEkROb3N6\n"
                + "KOqkqm57TH2H3eDJAkSnh6/DNFu0Qg==\n"
                + "-----END CERTIFICATE-----\n";
        return new Buffer()
                .writeUtf8(comodoRsaCertificationAuthority)
                .writeUtf8(entrustRootCertificateAuthority)
                .inputStream();
    }

}
