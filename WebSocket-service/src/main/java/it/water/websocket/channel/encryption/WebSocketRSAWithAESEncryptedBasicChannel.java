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

package it.water.websocket.channel.encryption;

import it.water.core.api.registry.ComponentRegistry;
import it.water.core.api.security.EncryptionUtil;
import it.water.core.model.exceptions.WaterRuntimeException;
import it.water.websocket.api.channel.WebSocketChannelClusterMessageBroker;
import it.water.websocket.api.channel.WebSocketChannelSession;
import it.water.websocket.channel.util.WebSocketChannelConstants;
import it.water.websocket.model.message.WebSocketMessage;
import it.water.websocket.model.message.WebSocketMessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

public class WebSocketRSAWithAESEncryptedBasicChannel extends WebSocketEncryptedBasicChannel {
    private static final Logger log = LoggerFactory.getLogger(WebSocketRSAWithAESEncryptedBasicChannel.class);

    @SuppressWarnings("java:S1948") // aesInfoMessage is transient-equivalent: regenerated on channel creation and not needed after deserialization
    private transient WebSocketMessage aesInfoMessage;

    public WebSocketRSAWithAESEncryptedBasicChannel(String channelName, String channelId, int maxPartecipants, Map<String, Object> channelParams, WebSocketChannelClusterMessageBroker clusterMessageBroker) {
        super(channelName, channelId, maxPartecipants, channelParams, clusterMessageBroker);
    }

    //used for deserialization
    private WebSocketRSAWithAESEncryptedBasicChannel(){
        super();
    }

    /**
     * This is invoked at channel creation.
     * We create specific key for each channel
     */
    @Override
    @SuppressWarnings("java:S2139") // exception is logged here and rethrown as WaterRuntimeException for caller context
    protected void initChannelEncryption() {
        try {
            EncryptionUtil encryptionUtil = ((ComponentRegistry) getChannelParam("componentRegistry")).findComponent(EncryptionUtil.class, null);
            byte[] aesPwd = encryptionUtil.generateRandomAESPassword();
            byte[] aesIv = encryptionUtil.generateRandomAESInitVector().getIV();
            String aesPwdStr = new String(Base64.getEncoder().encode(aesPwd), StandardCharsets.UTF_8);
            String aesIvStr = new String(Base64.getEncoder().encode(aesIv), StandardCharsets.UTF_8);
            String aesInfoPayload = aesPwdStr + WebSocketChannelConstants.WS_MESSAGE_CHANNEL_AES_DATA_SEPARATOR + aesIvStr;
            aesInfoMessage = WebSocketMessage.createMessage(null, aesInfoPayload.getBytes(StandardCharsets.UTF_8), WebSocketMessageType.SET_CHANNEL_ENCRYPTION_KEY);
        } catch (Exception t) {
            log.error(t.getMessage(), t);
            throw new WaterRuntimeException("Impossible to create channel:" + t.getMessage(), t);
        }
    }

    /**
     * This is invoked every time a partecipant joins into a specific channel
     *
     * @param session
     */
    protected void setupPartecipantEncryptedSession(WebSocketChannelSession session) {
        try {
            session.sendRemote(aesInfoMessage);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

}
