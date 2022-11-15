package de.dlr.ddomtia.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import lombok.extern.log4j.Log4j2;

import org.bouncycastle.jce.interfaces.ECPrivateKey;
import org.bouncycastle.jce.interfaces.ECPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Log4j2
@Service
public final class EncryptionService {
    private final ECPrivateKey privateKey;
    private final ECPublicKey publicKey;

    @Autowired
    public EncryptionService(@Value("${encryption.privateKey.path}") String privateKeyPath, @Value("${encryption.publicKey.path}") String publicKeyPath) {
        this.privateKey = loadPrivateKey(privateKeyPath);
        this.publicKey = loadPublicKey(publicKeyPath);
        log.info("Keys have been loaded.");
        log.info("DocumentHashService has been created.");
    }

    public byte[] sign(byte[] hashedDataBytes) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        final Signature signature = Signature.getInstance("SHA512withECDSA");
        signature.initSign(this.privateKey);
        signature.update(hashedDataBytes);
        return signature.sign();
    }

    public boolean verify(byte[] signatureBytes) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        final Signature signature = Signature.getInstance("SHA512withECDSA");
        signature.initVerify(this.publicKey);
        return signature.verify(signatureBytes);
    }

    private static ECPrivateKey loadPrivateKey(String keyPath) {
        try {
            Security.addProvider(new BouncyCastleProvider());
            final InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(keyPath);
            assert stream != null;
            final PemObject pemObject = new PemReader(new StringReader(new String(stream.readAllBytes(), StandardCharsets.UTF_8))).readPemObject();
            final PrivateKey key = KeyFactory.getInstance("EC", BouncyCastleProvider.PROVIDER_NAME).generatePrivate(new PKCS8EncodedKeySpec(pemObject.getContent()));
            return (ECPrivateKey) key;
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException | NoSuchProviderException e) {
            log.warn("Load private key failed.");
            throw new RuntimeException(e);
        }
    }

    private static ECPublicKey loadPublicKey(String keyPath) {
        try {
            Security.addProvider(new BouncyCastleProvider());
            final InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(keyPath);
            assert stream != null;
            final PemObject pemObject = new PemReader(new StringReader(new String(stream.readAllBytes(), StandardCharsets.UTF_8))).readPemObject();
            final PublicKey key = KeyFactory.getInstance("EC", BouncyCastleProvider.PROVIDER_NAME).generatePublic(new X509EncodedKeySpec(pemObject.getContent()));
            return (ECPublicKey) key;
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException | NoSuchProviderException e) {
            log.warn("Load public key failed.");
            throw new RuntimeException(e);
        }
    }
}
