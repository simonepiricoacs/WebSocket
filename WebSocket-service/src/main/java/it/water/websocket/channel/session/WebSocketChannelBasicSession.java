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
import it.water.websocket.api.WebSocketBasicCommandType;
import it.water.websocket.api.channel.WebSocketChannel;
import it.water.websocket.api.channel.WebSocketChannelCommand;
import it.water.websocket.api.channel.WebSocketChannelManager;
import it.water.websocket.api.channel.WebSocketChannelSession;
import it.water.websocket.channel.command.WebSocketChannelCommandFactory;
import it.water.websocket.channel.util.WebSocketChannelConstants;
import it.water.websocket.compression.WebSocketCompression;
import it.water.websocket.encryption.WebSocketEncryption;
import it.water.websocket.model.WebSocketConstants;
import it.water.websocket.model.message.WebSocketMessage;
import it.water.websocket.model.message.WebSocketMessageType;
import it.water.websocket.session.WebSocketAbstractSession;
import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * @Author Aristide Cittadino
 */
public class WebSocketChannelBasicSession extends WebSocketAbstractSession implements WebSocketChannelSession {
    private static Logger log = LoggerFactory.getLogger(WebSocketChannelBasicSession.class);

    private WebSocketChannelManager channelManager;
    private WebSocketChannelCommandFactory commandFactory;
    private Map<String, Object> sessionParams;
    private Set<WebSocketChannel> joinedChannels;

    public WebSocketChannelBasicSession(Session session, boolean authenticationRequired, WebSocketChannelManager channelManager, ComponentRegistry componentRegistry) {
        super(session, authenticationRequired, componentRegistry);
        initUserSession(channelManager, componentRegistry);
    }

    public WebSocketChannelBasicSession(Session session, boolean authenticated, WebSocketEncryption encryptionPolicy, WebSocketChannelManager channelManager, ComponentRegistry componentRegistry) {
        super(session, authenticated, encryptionPolicy, componentRegistry);
        initUserSession(channelManager, componentRegistry);
    }

    public WebSocketChannelBasicSession(Session session, boolean authenticated, WebSocketCompression compressionPolicy, WebSocketChannelManager channelManager, ComponentRegistry componentRegistry) {
        super(session, authenticated, compressionPolicy, componentRegistry);
        initUserSession(channelManager, componentRegistry);
    }

    public WebSocketChannelBasicSession(Session session, boolean authenticated, WebSocketEncryption encryptionPolicy, WebSocketCompression compressionPolicy, WebSocketChannelManager channelManager, ComponentRegistry componentRegistry) {
        super(session, authenticated, encryptionPolicy, compressionPolicy, componentRegistry);
        initUserSession(channelManager, componentRegistry);
    }

    private void initUserSession(WebSocketChannelManager channelManager, ComponentRegistry componentRegistry) {
        this.channelManager = channelManager;
        this.commandFactory = new WebSocketChannelCommandFactory(componentRegistry);
        this.sessionParams = new HashMap<>();
        this.joinedChannels = new HashSet<>();
    }

    @Override
    public void initialize() {
        boolean closeSession = false;
        String closeMessage = null;

        if (this.isAuthenticationRequired() && !this.isAuthenticated()) {
            closeSession = true;
            closeMessage = "Not authenticated,closing session";
        }

        if (closeSession) {
            this.close(closeMessage);
            return;
        }

        this.onConnect();
    }

    @Override
    public void addSessionParam(String name, Object value) {
        this.sessionParams.put(name, value);
    }

    @Override
    public Object getSessionParam(String name) {
        return this.sessionParams.get(name);
    }

    @Override
    public void removeSessionParam(String name) {
        this.sessionParams.remove(name);
    }

    @Override
    public void onMessage(String message) {
        byte[] rawMessage = this.getMessageBroker().readRaw(message);
        this.processMessage(rawMessage);
    }

    public void processMessage(byte[] rawMessage) {
        String rawMessageStr = new String(rawMessage);
        WebSocketMessage wsMessage = WebSocketMessage.fromString(rawMessageStr);

        if (wsMessage == null) {
            String errorMessageStr = "Impossible to deserialize message:\n " + rawMessageStr;
            WebSocketMessage errorMessage = WebSocketMessage.createMessage(WebSocketBasicCommandType.READ_MESSAGE_COMMAND, errorMessageStr.getBytes(StandardCharsets.UTF_8), WebSocketMessageType.ERROR);
            this.sendRemote(errorMessage);
            return;
        }

        String channelId = null;
        if (wsMessage.getParams() != null && wsMessage.getParams().containsKey(WebSocketChannelConstants.CHANNEL_ID_PARAM))
            channelId = wsMessage.getParams().get(WebSocketChannelConstants.CHANNEL_ID_PARAM);

        //forcing sender to be the user associated with the current session
        if (wsMessage.getParams() == null)
            wsMessage.setParams(new HashMap<>());
        wsMessage.getParams().put(WebSocketMessage.WS_MESSAGE_SENDER_PARAM_NAME, this.getUserInfo().getUsername());
        WebSocketChannelCommand command = commandFactory.createCommand(wsMessage.getCmd());
        try {
            command.execute(this, wsMessage, channelId, this.channelManager);
        } catch (Throwable t) {
            log.error(t.getMessage(), t);
            String errorMessageStr = (t.getMessage() != null) ? t.getMessage() : "";
            WebSocketMessage errorMessage = WebSocketMessage.createMessage(WebSocketBasicCommandType.READ_MESSAGE_COMMAND, errorMessageStr.getBytes(StandardCharsets.UTF_8), WebSocketMessageType.ERROR);
            this.sendRemote(errorMessage);
        }
    }

    @Override
    public void dispose() {
        WebSocketMessage m = WebSocketMessage.createMessage(WebSocketBasicCommandType.READ_MESSAGE.toString(), "Disconnecting...".getBytes(), WebSocketMessageType.DISCONNECTING);
        this.sendRemote(m);
        this.getMessageBroker().onCloseSession(this.getSession());
        super.dispose();
        this.onClose();
    }

    public void close(String closeMessage) {
        if (closeMessage == null)
            closeMessage = "";
        WebSocketMessage m = WebSocketMessage.createMessage(WebSocketBasicCommandType.READ_MESSAGE.toString(), closeMessage.getBytes(), WebSocketMessageType.ERROR);
        this.sendRemote(m);
        //forcing to close connection
        log.info("Closing session because: {}", closeMessage);
        this.dispose();
    }

    public void addJoinedChannels(WebSocketChannel channel) {
        this.joinedChannels.add(channel);
    }

    public void removeJoinedChannels(WebSocketChannel channel) {
        this.joinedChannels.remove(channel);
    }

    public void emptyJoinedChannels(WebSocketChannel channel) {
        this.joinedChannels.clear();
    }

    public Set<WebSocketChannel> getJoinedChannels() {
        return Collections.unmodifiableSet(this.joinedChannels);
    }

    protected void sendConnectionOkMessage(String message) {
        if (message == null)
            message = "";
        WebSocketMessage m = WebSocketMessage.createMessage(WebSocketBasicCommandType.READ_MESSAGE.toString(), message.getBytes(), WebSocketMessageType.CONNECTION_OK);
        HashMap<String, String> params = new HashMap<>();
        params.put(WebSocketConstants.WEB_SOCKET_USERNAME_PARAM, this.getUserInfo().getUsername());
        m.setParams(params);
        this.sendRemote(m);
    }

    protected void onConnect() {
        this.sendConnectionOkMessage("Connected!");
    }

    protected void onClose() {
        this.joinedChannels.parallelStream().forEach(channel -> {
            this.channelManager.leaveChannel(channel.getChannelId(), this);
        });
        WebSocketMessage m = WebSocketMessage.createMessage(null, "partecipant is disconnecting...".getBytes(), WebSocketMessageType.PARTICIPANT_GONE);
        this.sendRemote(m);
    }

    @Override
    public void updateEncryptionPolicyParams(Map<String, Object> params) {
        this.getMessageBroker().updateEncryptionPolicyParams(params);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WebSocketChannelBasicSession that = (WebSocketChannelBasicSession) o;
        return Objects.equals(getSession(), that.getSession()) && Objects.equals(this.getUserInfo(), that.getUserInfo());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getSession(), this.getUserInfo());
    }

}
