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

import java.util.Base64;
import java.util.Map;

public class WebSocketRSAWithAESEncryptedBasicChannel extends WebSocketEncryptedBasicChannel {
    private static Logger log = LoggerFactory.getLogger(WebSocketRSAWithAESEncryptedBasicChannel.class);

    private byte[] aesPwd;
    private byte[] aesIv;
    private String aesPwdStr;
    private String aesIvStr;
    private String aesInfoPayload;
    private WebSocketMessage aesInfoMessage;

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
    protected void initChannelEncryption() {
        {
            try {
                EncryptionUtil encryptionUtil = (EncryptionUtil) ((ComponentRegistry) getChannelParam("componentRegistry")).findComponent(EncryptionUtil.class, null);
                aesPwd = encryptionUtil.generateRandomAESPassword();
                aesIv = encryptionUtil.generateRandomAESInitVector().getIV();
                aesPwdStr = new String(Base64.getEncoder().encode(aesPwd));
                aesIvStr = new String(Base64.getEncoder().encode(aesIv));
                aesInfoPayload = aesPwdStr + WebSocketChannelConstants.WS_MESSAGE_CHANNEL_AES_DATA_SEPARATOR + aesIvStr;
                aesInfoMessage = WebSocketMessage.createMessage(null, aesInfoPayload.getBytes("UTF8"), WebSocketMessageType.SET_CHANNEL_ENCRYPTION_KEY);
            } catch (Throwable t) {
                log.error(t.getMessage(), t);
                throw new WaterRuntimeException("Impossible to create channel:" + t.getMessage());
            }
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
