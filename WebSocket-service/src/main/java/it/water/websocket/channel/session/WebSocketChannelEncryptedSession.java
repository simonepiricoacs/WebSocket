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
import it.water.websocket.api.channel.WebSocketChannelManager;
import it.water.websocket.compression.WebSocketCompression;
import it.water.websocket.encryption.WebSocketEncryption;
import it.water.websocket.model.message.WebSocketMessage;
import it.water.websocket.model.message.WebSocketMessageType;
import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public abstract class WebSocketChannelEncryptedSession extends WebSocketChannelBasicSession {
    private static Logger log = LoggerFactory.getLogger(WebSocketChannelEncryptedSession.class);

    public WebSocketChannelEncryptedSession(Session session, boolean authenticationRequired, WebSocketChannelManager channelManager, ComponentRegistry componentRegistry) {
        super(session, authenticationRequired, channelManager, componentRegistry);
    }

    public WebSocketChannelEncryptedSession(Session session, boolean authenticated, WebSocketEncryption encryptionPolicy, WebSocketChannelManager channelManager, ComponentRegistry componentRegistry) {
        super(session, authenticated, encryptionPolicy, channelManager, componentRegistry);
    }

    public WebSocketChannelEncryptedSession(Session session, boolean authenticated, WebSocketCompression compressionPolicy, WebSocketChannelManager channelManager, ComponentRegistry componentRegistry) {
        super(session, authenticated, compressionPolicy, channelManager, componentRegistry);
    }

    public WebSocketChannelEncryptedSession(Session session, boolean authenticated, WebSocketEncryption encryptionPolicy, WebSocketCompression compressionPolicy, WebSocketChannelManager channelManager, ComponentRegistry componentRegistry) {
        super(session, authenticated, encryptionPolicy, compressionPolicy, channelManager, componentRegistry);
    }

    @Override
    protected void onConnect() {
        super.onConnect();
        try {
            String encryptionKeyMessageString = defineEncryptionMessage();
            WebSocketMessage m = WebSocketMessage.createMessage(null, encryptionKeyMessageString.getBytes("UTF8"), WebSocketMessageType.SET_ENCRYPTION_KEY);
            sendRemote(m);
            Map<String, Object> params = defineEncryptionPolicyParams();
            this.updateEncryptionPolicyParams(params);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    protected abstract String defineEncryptionMessage();

    protected abstract Map<String, Object> defineEncryptionPolicyParams();
}
