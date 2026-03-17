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

import it.water.websocket.model.WebSocketConstants;
import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileInputStream;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RSAWithAESEncryptionMode extends WebSocketMixedEncryptionMode {
    private static Logger log = LoggerFactory.getLogger(RSAWithAESEncryptionMode.class.getName());

    public static final String MODE_PARAM_AES_PASSWORD = "aesPassword";
    public static final String MODE_PARAM_AES_IV = "aesIv";

    public static Cipher currRsaCipherEnc;
    public static Cipher currRsaCipherEncWeb;
    public static Cipher currRsaCipherDec;
    public static Cipher currRsaCipherDecWeb;

    static {
        try {
            PrivateKey key = getServerKeyPair().getPrivate();
            currRsaCipherEnc = getCipherRSAOAEPPadding();
            currRsaCipherEncWeb = getCipherRSAPKCS1Padding();
            currRsaCipherDec = getCipherRSAOAEPPadding();
            currRsaCipherDec.init(Cipher.DECRYPT_MODE, key);
            currRsaCipherDecWeb = getCipherRSAPKCS1Padding();
            currRsaCipherDecWeb.init(Cipher.DECRYPT_MODE, key);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private boolean webSession;
    private Cipher currAesCipherEnc;
    private Cipher currAesCipherDec;

    @Override
    public void init(Session session) {
        try {
            String clientPubKeyStrEnc = session.getUpgradeRequest().getHeader(WebSocketConstants.CLIENT_PUB_KEY_HEADER);
            //Using OAEP Padding
            Cipher currCipherRSADec = null;
            if (clientPubKeyStrEnc == null) {
                //trying with query param since javascript API doesn't support custom headers in websocket
                List<String> pubKeyParam = session.getUpgradeRequest().getParameterMap().get(WebSocketConstants.CLIENT_PUB_KEY_QUERY_PARAM);
                if (pubKeyParam.size() == 1) {
                    clientPubKeyStrEnc = pubKeyParam.get(0);
                }
                this.webSession = true;
            } else {
                this.webSession = false;
            }
            if (clientPubKeyStrEnc != null) {
                this.setPrivateKey(getServerKeyPair().getPrivate());
                byte[] decodedPubKey = Base64.getDecoder().decode(clientPubKeyStrEnc.getBytes("UTF8"));
                byte[] decryptedPubKey = decryptAsymmetric(getServerKeyPair().getPrivate(), decodedPubKey);
                String clientPubKeyStr = new String(decryptedPubKey);
                PublicKey clientPubKey = getPublicKeyFromString(clientPubKeyStr);
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
        synchronized (cipher) {
            return encryptText(publicKey, plainText, encodeBase64, cipher);
        }
    }

    @Override
    protected byte[] decryptSymmetric(byte[] symmetricPassword, byte[] symmetricIv, String s) throws Exception {
        return decryptWithAES(symmetricPassword, symmetricIv, s, getCipherAESDec());
    }

    @Override
    protected byte[] decryptAsymmetric(PrivateKey privateKey, byte[] plainText) throws Exception {
        Cipher cipher = getCipherRSADec();
        byte[] decrypted = null;
        synchronized (cipher) {
            decrypted = cipher.doFinal(plainText);
        }
        return decrypted;
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

    private Cipher getCipherRSADec() {
        try {
            if (this.webSession)
                return currRsaCipherDecWeb;
            return currRsaCipherDec;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    private Cipher getCipherRSAEnc() {
        try {
            if (this.webSession)
                return currRsaCipherEncWeb;
            return currRsaCipherEnc;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    private Cipher getCipherAESEnc() throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, NoSuchProviderException {
        if (this.currAesCipherEnc == null) {
            this.currAesCipherEnc = getCipherAES();
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


    private Cipher getCipherAESDec() throws InvalidKeyException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchPaddingException, NoSuchProviderException {
        if (this.currAesCipherDec == null) {
            this.currAesCipherDec = getCipherAES();
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

    // ---- Crypto utility methods (replacing HyperIoTSecurityUtil) ----

    /**
     * Returns the server KeyPair loaded from the JKS keystore.
     * Keystore path, password, key password, and alias are read from system properties.
     *
     * @return the server KeyPair, or null if loading fails
     */
    private static KeyPair getServerKeyPair() {
        try {
            String keystoreFile = System.getProperty("it.water.security.keystore.file", "keystore.jks");
            String keystorePassword = System.getProperty("it.water.security.keystore.password", "changeit");
            String keyPassword = System.getProperty("it.water.security.key.password", "changeit");
            String alias = System.getProperty("it.water.security.keystore.alias", "water");

            KeyStore keystore = KeyStore.getInstance("JKS");
            keystore.load(new FileInputStream(new File(keystoreFile)), keystorePassword.toCharArray());
            Key key = keystore.getKey(alias, keyPassword.toCharArray());
            if (key instanceof PrivateKey) {
                java.security.cert.Certificate cert = keystore.getCertificate(alias);
                PublicKey publicKey = cert.getPublicKey();
                return new KeyPair(publicKey, (PrivateKey) key);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * Creates RSA Cipher with OAEP Padding using BouncyCastle provider.
     *
     * @return Cipher instance
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchProviderException
     */
    private static Cipher getCipherRSAOAEPPadding() throws NoSuchPaddingException, NoSuchAlgorithmException, NoSuchProviderException {
        return Cipher.getInstance("RSA/NONE/OAEPPadding", "BC");
    }

    /**
     * Creates RSA Cipher with PKCS1 Padding (ECB mode) using BouncyCastle provider.
     *
     * @return Cipher instance
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchProviderException
     */
    private static Cipher getCipherRSAPKCS1Padding() throws NoSuchPaddingException, NoSuchAlgorithmException, NoSuchProviderException {
        return Cipher.getInstance("RSA/ECB/PKCS1PADDING", "BC");
    }

    /**
     * Creates AES Cipher with CBC/PKCS5PADDING using BouncyCastle provider.
     *
     * @return Cipher instance
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchProviderException
     */
    private static Cipher getCipherAES() throws NoSuchPaddingException, NoSuchAlgorithmException, NoSuchProviderException {
        return Cipher.getInstance("AES/CBC/PKCS5PADDING", "BC");
    }

    /**
     * Parses a PEM-encoded public key string (PKCS8/X509 format) into a PublicKey object.
     *
     * @param key PEM-encoded public key string
     * @return the PublicKey, or null if parsing fails
     */
    private static PublicKey getPublicKeyFromString(String key) {
        try {
            String publicKeyPEM = key;
            publicKeyPEM = publicKeyPEM.replace("-----BEGIN PUBLIC KEY-----", "");
            publicKeyPEM = publicKeyPEM.replace("-----END PUBLIC KEY-----", "");
            publicKeyPEM = publicKeyPEM.replace("\r", "");
            publicKeyPEM = publicKeyPEM.replace("\n", "");
            byte[] byteKey = Base64.getDecoder().decode(publicKeyPEM.getBytes("UTF8"));
            X509EncodedKeySpec X509publicKey = new X509EncodedKeySpec(byteKey);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePublic(X509publicKey);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * Encrypts plain text using the given public key and RSA cipher.
     *
     * @param pk             Public key
     * @param text           Plain text bytes
     * @param encodeInBase64 Whether to Base64-encode the result
     * @param asymmetricCipher The RSA cipher to use
     * @return encrypted bytes, optionally Base64-encoded
     */
    private static byte[] encryptText(PublicKey pk, byte[] text, boolean encodeInBase64, Cipher asymmetricCipher) {
        try {
            asymmetricCipher.init(Cipher.ENCRYPT_MODE, pk);
            if (encodeInBase64) {
                return Base64.getEncoder().encode(asymmetricCipher.doFinal(text));
            }
            return asymmetricCipher.doFinal(text);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * Encrypts content with AES using the provided password, IV, and cipher.
     *
     * @param aesPassword AES secret key bytes
     * @param initVector  Initialization vector bytes
     * @param content     Content string to encrypt
     * @param aesCipher   The AES cipher to use
     * @return Base64-encoded encrypted bytes, or null on failure
     */
    private static byte[] encryptWithAES(byte[] aesPassword, byte[] initVector, String content, Cipher aesCipher) {
        try {
            IvParameterSpec iv = new IvParameterSpec(initVector);
            SecretKeySpec skeySpec = new SecretKeySpec(aesPassword, "AES");
            aesCipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);
            byte[] encrypted = aesCipher.doFinal(content.getBytes("UTF8"));
            return Base64.getEncoder().encode(encrypted);
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
        return null;
    }

    /**
     * Decrypts AES-encrypted content (Base64-encoded) using the provided password, IV, and cipher.
     *
     * @param aesPassword AES secret key bytes
     * @param initVector  Initialization vector bytes
     * @param content     Base64-encoded encrypted content string
     * @param aesCipher   The AES cipher to use
     * @return Decrypted bytes
     * @throws Exception on decryption failure
     */
    private static byte[] decryptWithAES(byte[] aesPassword, byte[] initVector, String content, Cipher aesCipher) throws Exception {
        SecretKeySpec skeySpec = new SecretKeySpec(aesPassword, "AES");
        IvParameterSpec iv = new IvParameterSpec(initVector);
        aesCipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);
        return aesCipher.doFinal(Base64.getDecoder().decode(content));
    }

}
