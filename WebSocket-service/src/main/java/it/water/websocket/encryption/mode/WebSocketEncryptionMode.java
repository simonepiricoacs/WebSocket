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

import org.eclipse.jetty.websocket.api.Session;

import java.util.Map;

/**
 * Interface identifying an Encryption Mode.
 */
@SuppressWarnings("java:S112") // encryption SPI intentionally propagates generic crypto exceptions
public interface WebSocketEncryptionMode {
    /**
     * Init method for intializing mode
     *
     * @param s
     */
    void init(Session s);

    /**
     * Called on close
     *
     * @param s
     */
    void dispose(Session s);

    /**
     * Method used to update mode (new keys received or change algorithm)
     *
     * @param params
     */
    void update(Map<String, Object> params);

    /**
     * @return Current params of the encryption mode
     */
    Map<String, Object> getParams();


    /**
     * Encrypts content
     *
     * @param plainText
     * @param encodeBase64
     * @return
     * @throws Exception
     */
    byte[] encrypt(byte[] plainText, boolean encodeBase64) throws Exception;

    /**
     * Decrypts content
     *
     * @param cipherText
     * @return
     * @throws Exception
     */
    byte[] decrypt(byte[] cipherText) throws Exception;
}
