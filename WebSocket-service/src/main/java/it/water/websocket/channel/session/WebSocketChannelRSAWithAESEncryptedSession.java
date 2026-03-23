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

package it.water.websocket.channel.session;

import it.water.core.api.registry.ComponentRegistry;
import it.water.core.api.security.EncryptionUtil;
import it.water.websocket.api.channel.WebSocketChannelManager;
import it.water.websocket.channel.util.WebSocketChannelConstants;
import it.water.websocket.compression.WebSocketCompression;
import it.water.websocket.encryption.WebSocketEncryption;
import it.water.websocket.encryption.mode.RSAWithAESEncryptionMode;
import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("java:S2160") // params and componentRegistry are internal state
public class WebSocketChannelRSAWithAESEncryptedSession extends WebSocketChannelEncryptedSession {

    private static final Logger log = LoggerFactory.getLogger(WebSocketChannelRSAWithAESEncryptedSession.class);
    @SuppressWarnings("java:S1450") // params bridges defineEncryptionMessage() and defineEncryptionPolicyParams() called in sequence
    private Map<String, Object> params;
    private ComponentRegistry componentRegistry;

    public WebSocketChannelRSAWithAESEncryptedSession(Session session, boolean authenticationRequired, WebSocketChannelManager channelManager, ComponentRegistry componentRegistry) {
        super(session, authenticationRequired, channelManager, componentRegistry);
        this.componentRegistry = componentRegistry;
    }

    public WebSocketChannelRSAWithAESEncryptedSession(Session session, boolean authenticated, WebSocketEncryption encryptionPolicy, WebSocketChannelManager channelManager, ComponentRegistry componentRegistry) {
        super(session, authenticated, encryptionPolicy, channelManager, componentRegistry);
        this.componentRegistry = componentRegistry;
    }

    public WebSocketChannelRSAWithAESEncryptedSession(Session session, boolean authenticated, WebSocketCompression compressionPolicy, WebSocketChannelManager channelManager, ComponentRegistry componentRegistry) {
        super(session, authenticated, compressionPolicy, channelManager, componentRegistry);
        this.componentRegistry = componentRegistry;
    }

    public WebSocketChannelRSAWithAESEncryptedSession(Session session, boolean authenticated, WebSocketEncryption encryptionPolicy, WebSocketCompression compressionPolicy, WebSocketChannelManager channelManager, ComponentRegistry componentRegistry) {
        super(session, authenticated, encryptionPolicy, compressionPolicy, channelManager, componentRegistry);
        this.componentRegistry = componentRegistry;
    }

    @Override
    protected String defineEncryptionMessage() {
        try {
            EncryptionUtil encryptionUtil = componentRegistry.findComponent(EncryptionUtil.class, null);
            byte[] aesPwd = encryptionUtil.generateRandomAESPassword();
            byte[] aesIv = encryptionUtil.generateRandomAESInitVector().getIV();
            //sending <password:iv>
            String aesPwdStr = new String(Base64.getEncoder().encode(aesPwd), StandardCharsets.UTF_8);
            String aesIvStr = new String(Base64.getEncoder().encode(aesIv), StandardCharsets.UTF_8);
            String aesInfoPayload = aesPwdStr + WebSocketChannelConstants.WS_MESSAGE_CHANNEL_AES_DATA_SEPARATOR + aesIvStr;
            params = new HashMap<>();
            params.put(RSAWithAESEncryptionMode.MODE_PARAM_AES_PASSWORD, aesPwd);
            params.put(RSAWithAESEncryptionMode.MODE_PARAM_AES_IV, aesIv);
            return aesInfoPayload;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    protected Map<String, Object> defineEncryptionPolicyParams() {
        return params;
    }
}
