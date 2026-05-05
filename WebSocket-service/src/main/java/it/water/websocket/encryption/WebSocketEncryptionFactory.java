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

package it.water.websocket.encryption;

import it.water.core.api.security.EncryptionUtil;
import it.water.websocket.encryption.mode.RSAWithAESEncryptionMode;

/**
 * Factory for creating available Encryption Policies for websockets.
 */
public class WebSocketEncryptionFactory {
    private WebSocketEncryptionFactory() {
        throw new UnsupportedOperationException();
    }

    /**
     * Creates an RSA+AES encryption policy backed by the given {@link EncryptionUtil}.
     *
     * @param encryptionUtil the Water crypto service (reads keystore from ApplicationProperties)
     * @return a new WebSocketEncryption wrapping the RSA+AES mode
     */
    public static WebSocketEncryption createRSAAndAESEncryptionPolicy(EncryptionUtil encryptionUtil) {
        RSAWithAESEncryptionMode rsaAndAesMode = new RSAWithAESEncryptionMode(encryptionUtil);
        return new WebSocketEncryption(rsaAndAesMode);
    }
}
