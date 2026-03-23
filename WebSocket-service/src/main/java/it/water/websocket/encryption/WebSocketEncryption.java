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

import it.water.websocket.encryption.mode.WebSocketEncryptionMode;
import org.eclipse.jetty.websocket.api.Session;

import java.util.Map;

/**
 * Author Aristide Cittadino
 * This class maps the concept of encryption policy, It owns an encryption mode which is responsable
 * of how messages are encrypted or decrypted.
 */
@SuppressWarnings("java:S112") // encryption SPI intentionally propagates generic crypto exceptions
public class WebSocketEncryption {

    private WebSocketEncryptionMode mode;

    public WebSocketEncryption(WebSocketEncryptionMode mode) {
        this.mode = mode;
    }

    public byte[] encrypt(byte[] message, boolean encodeBase64) throws Exception {
        return mode.encrypt(message, encodeBase64);
    }

    @SuppressWarnings("java:S1172") // decodeBase64 is part of the public API contract; not forwarded to the current mode but reserved for future modes
    public byte[] decrypt(byte[] message, boolean decodeBase64) throws Exception {
        return mode.decrypt(message);
    }

    public void updateMode(Map<String, Object> params) {
        mode.update(params);
    }

    public Map<String, Object> getModeParams() {
        return mode.getParams();
    }

    public void init(Session s) {
        mode.init(s);
    }

    public void dispose(Session s) {
        mode.dispose(s);
    }

}
