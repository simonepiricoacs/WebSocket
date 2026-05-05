/*
 * Copyright 2019-2023 HyperIoT
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package it.water.websocket.encryption.mode;

import it.water.core.api.security.EncryptionUtil;
import it.water.websocket.model.WebSocketConstants;
import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RSA+AES encryption mode for WebSocket channels.
 * <p>
 * All crypto operations (keystore access, cipher creation, key parsing) are
 * delegated to {@link EncryptionUtil} — the Water Framework's standard crypto
 * service that reads keystore configuration from {@code ApplicationProperties}
 * (keys {@code water.keystore.*}).
 * <p>
 * RSA ciphers are initialized lazily on first use rather than in a
 * {@code static { }} block, so the framework has time to inject
 * {@code EncryptionUtil} before any crypto operation is attempted.
 */
@SuppressWarnings("java:S112") // encryption SPI intentionally propagates generic crypto exceptions
public class RSAWithAESEncryptionMode extends WebSocketMixedEncryptionMode {
    private static final Logger log = LoggerFactory.getLogger(RSAWithAESEncryptionMode.class);

    public static final String MODE_PARAM_AES_PASSWORD = "aesPassword";
    public static final String MODE_PARAM_AES_IV = "aesIv";

    private final EncryptionUtil encryptionUtil;

    // Lazy-initialized RSA ciphers (guarded by synchronized on this)
    private Cipher rsaCipherEnc;
    private Cipher rsaCipherEncWeb;
    private Cipher rsaCipherDec;
    private Cipher rsaCipherDecWeb;
    private boolean rsaCiphersInitialized;

    private boolean webSession;
    private Cipher currAesCipherEnc;
    private Cipher currAesCipherDec;

    /**
     * Creates an encryption mode backed by the given {@link EncryptionUtil}.
     *
     * @param encryptionUtil the Water encryption service (never null in production;
     *                       may be null in tests that only exercise symmetric paths)
     */
    public RSAWithAESEncryptionMode(EncryptionUtil encryptionUtil) {
        this.encryptionUtil = encryptionUtil;
    }

    /**
     * No-arg constructor for backward compatibility in tests.
     * RSA operations will fail with a clear error if attempted without
     * an {@code EncryptionUtil}.
     */
    public RSAWithAESEncryptionMode() {
        this(null);
    }

    @Override
    public void init(Session session) {
        try {
            String clientPubKeyStrEnc = session.getUpgradeRequest().getHeader(WebSocketConstants.CLIENT_PUB_KEY_HEADER);
            if (clientPubKeyStrEnc == null) {
                //trying with query param since javascript API doesn't support custom headers in websocket
                List<String> pubKeyParam = session.getUpgradeRequest().getParameterMap().get(WebSocketConstants.CLIENT_PUB_KEY_QUERY_PARAM);
                if (pubKeyParam != null && pubKeyParam.size() == 1) {
                    clientPubKeyStrEnc = pubKeyParam.get(0);
                }
                this.webSession = true;
            } else {
                this.webSession = false;
            }
            if (clientPubKeyStrEnc != null) {
                ensureEncryptionUtil();
                KeyPair serverKeyPair = encryptionUtil.getServerKeyPair();
                PrivateKey serverPrivateKey = serverKeyPair.getPrivate();
                this.setPrivateKey(serverPrivateKey);
                byte[] decodedPubKey = Base64.getDecoder().decode(clientPubKeyStrEnc.getBytes(StandardCharsets.UTF_8));
                byte[] decryptedPubKey = decryptAsymmetric(serverPrivateKey, decodedPubKey);
                String clientPubKeyStr = new String(decryptedPubKey, StandardCharsets.UTF_8);
                PublicKey clientPubKey = encryptionUtil.getPublicKeyFromString(clientPubKeyStr);
                this.setPublicKey(clientPubKey);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public void dispose(Session s) {
        //do nothing
    }

    @Override
    protected byte[] encryptSymmetric(byte[] symmetricPassword, byte[] symmetricIv, String s) throws Exception {
        return encryptWithAES(symmetricPassword, symmetricIv, s, getCipherAESEnc());
    }

    @Override
    protected byte[] encryptAsymmetric(PublicKey publicKey, byte[] plainText, boolean encodeBase64) throws Exception {
        Cipher cipher = getCipherRSAEnc();
        if (cipher == null) throw new IllegalStateException("RSA encryption cipher not initialized");
        synchronized (cipher) {
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            if (encodeBase64) {
                return Base64.getEncoder().encode(cipher.doFinal(plainText));
            }
            return cipher.doFinal(plainText);
        }
    }

    @Override
    protected byte[] decryptSymmetric(byte[] symmetricPassword, byte[] symmetricIv, String s) throws Exception {
        return decryptWithAES(symmetricPassword, symmetricIv, s, getCipherAESDec());
    }

    @Override
    protected byte[] decryptAsymmetric(PrivateKey privateKey, byte[] plainText) throws Exception {
        Cipher cipher = getCipherRSADec();
        if (cipher == null) throw new IllegalStateException("RSA decryption cipher not initialized");
        synchronized (cipher) {
            return cipher.doFinal(plainText);
        }
    }

    @Override
    public void update(Map<String, Object> params) {
        byte[] aesPassword = (byte[]) params.get(MODE_PARAM_AES_PASSWORD);
        byte[] iv = (byte[]) params.get(MODE_PARAM_AES_IV);
        this.setSymmetricPassword(aesPassword);
        this.setSymmetricIv(iv);
    }

    @Override
    public Map<String, Object> getParams() {
        HashMap<String, Object> params = new HashMap<>();
        params.put(MODE_PARAM_AES_PASSWORD, this.getSymmetricPassword());
        params.put(MODE_PARAM_AES_IV, this.getSymmetricIv());
        return params;
    }

    // ---- RSA cipher access (lazy-initialized via EncryptionUtil) ----

    private synchronized void ensureRSACiphersInitialized() {
        if (rsaCiphersInitialized) {
            return;
        }
        ensureEncryptionUtil();
        try {
            KeyPair serverKeyPair = encryptionUtil.getServerKeyPair();
            PrivateKey key = serverKeyPair.getPrivate();
            rsaCipherEnc = encryptionUtil.getCipherRSAOAEPPAdding();
            rsaCipherEncWeb = encryptionUtil.getCipherRSAPKCS1Padding(true);
            rsaCipherDec = encryptionUtil.getCipherRSAOAEPPAdding();
            rsaCipherDec.init(Cipher.DECRYPT_MODE, key);
            rsaCipherDecWeb = encryptionUtil.getCipherRSAPKCS1Padding(true);
            rsaCipherDecWeb.init(Cipher.DECRYPT_MODE, key);
            rsaCiphersInitialized = true;
        } catch (Exception e) {
            log.error("Failed to initialize RSA ciphers via EncryptionUtil: {}", e.getMessage(), e);
        }
    }

    private Cipher getCipherRSADec() {
        ensureRSACiphersInitialized();
        return this.webSession ? rsaCipherDecWeb : rsaCipherDec;
    }

    private Cipher getCipherRSAEnc() {
        ensureRSACiphersInitialized();
        return this.webSession ? rsaCipherEncWeb : rsaCipherEnc;
    }

    // ---- AES cipher access (via EncryptionUtil) ----

    @SuppressWarnings("java:S3329") // IV is randomly generated per session during channel setup
    private Cipher getCipherAESEnc() throws InvalidKeyException, InvalidAlgorithmParameterException {
        if (this.currAesCipherEnc == null) {
            this.currAesCipherEnc = getAESCipherInstance();
            SecretKeySpec skeySpec = new SecretKeySpec(this.getSymmetricPassword(), "AES");
            IvParameterSpec iv = null;
            if (this.getSymmetricIv() != null)
                iv = new IvParameterSpec(this.getSymmetricIv());

            if (iv != null)
                this.currAesCipherEnc.init(Cipher.ENCRYPT_MODE, skeySpec, iv);
            else
                this.currAesCipherEnc.init(Cipher.ENCRYPT_MODE, skeySpec);
        }
        return currAesCipherEnc;
    }

    @SuppressWarnings("java:S3329") // IV is randomly generated per session during channel setup
    private Cipher getCipherAESDec() throws InvalidKeyException, InvalidAlgorithmParameterException {
        if (this.currAesCipherDec == null) {
            this.currAesCipherDec = getAESCipherInstance();
            SecretKeySpec skeySpec = new SecretKeySpec(this.getSymmetricPassword(), "AES");
            IvParameterSpec iv = null;
            if (this.getSymmetricIv() != null)
                iv = new IvParameterSpec(this.getSymmetricIv());

            if (iv != null)
                this.currAesCipherDec.init(Cipher.DECRYPT_MODE, skeySpec, iv);
            else
                this.currAesCipherDec.init(Cipher.DECRYPT_MODE, skeySpec);
        }
        return currAesCipherDec;
    }

    private Cipher getAESCipherInstance() {
        if (encryptionUtil != null) {
            return encryptionUtil.getCipherAES();
        }
        // Fallback for tests without EncryptionUtil: create directly via BouncyCastle
        try {
            return Cipher.getInstance("AES/CBC/PKCS5PADDING", "BC");
        } catch (Exception e) {
            throw new IllegalStateException("Cannot create AES cipher: EncryptionUtil not available and BC fallback failed", e);
        }
    }

    private void ensureEncryptionUtil() {
        if (encryptionUtil == null) {
            throw new IllegalStateException(
                    "EncryptionUtil not available. RSAWithAESEncryptionMode must be created with an " +
                    "EncryptionUtil instance (via WebSocketEncryptionFactory) for RSA operations.");
        }
    }

    // ---- AES encrypt/decrypt (static helpers, no EncryptionUtil dependency) ----

    @SuppressWarnings({"java:S3329", "java:S5542"}) // IV is provided and managed by the session encryption protocol
    private static byte[] encryptWithAES(byte[] aesPassword, byte[] initVector, String content, Cipher aesCipher) throws Exception {
        IvParameterSpec iv = new IvParameterSpec(initVector);
        SecretKeySpec skeySpec = new SecretKeySpec(aesPassword, "AES");
        aesCipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);
        byte[] encrypted = aesCipher.doFinal(content.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encode(encrypted);
    }

    private static byte[] decryptWithAES(byte[] aesPassword, byte[] initVector, String content, Cipher aesCipher) throws Exception {
        SecretKeySpec skeySpec = new SecretKeySpec(aesPassword, "AES");
        IvParameterSpec iv = new IvParameterSpec(initVector);
        aesCipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);
        return aesCipher.doFinal(Base64.getDecoder().decode(content));
    }
}
