package com.example.aleksandra.encryptapplication.encrypt;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;

import lombok.SneakyThrows;

public class RSA {
    private BigInteger N;
    private BigInteger e;
    private BigInteger d;
    private int bitlen = 2048;
    private PublicKey publicKey;
    private static RSA rsa;


    public static RSA getRSAInstance() {
        if (rsa == null) {
            rsa = new RSA();
        }
        return rsa;
    }

    private RSA() {
        SecureRandom r = new SecureRandom();
        BigInteger p = new BigInteger(bitlen / 2, 100, r);
        BigInteger q = new BigInteger(bitlen / 2, 100, r);
        N = p.multiply(q);
        BigInteger m = (p.subtract(BigInteger.ONE)).multiply(q
                .subtract(BigInteger.ONE));
        e = new BigInteger("3");
        while (m.gcd(e).intValue() > 1) {
            e = e.add(new BigInteger("2"));
        }
        d = e.modInverse(m);
    }

    @SneakyThrows
    public synchronized String encrypt(String message, BigInteger exponent, BigInteger modulus) {
        if (message.getBytes("UTF-8").length < 245) {
            return (new BigInteger(message.getBytes())).modPow(exponent, modulus).toString();
        } else {
            return message;
        }
    }

    public synchronized String decrypt(String message) {
        return new String(new BigInteger(message).modPow(d, N).toByteArray());
    }

    public synchronized BigInteger getN() {
        return N;
    }

    public synchronized BigInteger getE() {
        return e;
    }

    @SneakyThrows
    public PublicKey getPublicKey() {
        if (publicKey == null) {
            createPublicKeyFromPrimes();
        }
        return publicKey;
    }

    private void createPublicKeyFromPrimes()
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        RSAPublicKeySpec spec = new RSAPublicKeySpec(N, e);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        publicKey = factory.generatePublic(spec);
    }
}
