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

import it.water.websocket.api.channel.WebSocketChannelClusterMessageBroker;
import it.water.websocket.api.channel.WebSocketChannelSession;
import it.water.websocket.channel.WebSocketBasicChannel;

import java.util.Map;


public abstract class WebSocketEncryptedBasicChannel extends WebSocketBasicChannel {

    protected WebSocketEncryptedBasicChannel(String channelName, String channelId, int maxPartecipants, Map<String, Object> channelParams, WebSocketChannelClusterMessageBroker clusterMessageBroker) {
        super(channelName, channelId, maxPartecipants, channelParams, clusterMessageBroker);
        initChannelEncryption();
    }

    protected WebSocketEncryptedBasicChannel() {
        super();
    }

    @Override
    protected void partecipantJoined(WebSocketChannelSession userSession) {
        super.partecipantJoined(userSession);
        this.setupPartecipantEncryptedSession(userSession);
    }

    protected abstract void initChannelEncryption();

    protected abstract void setupPartecipantEncryptedSession(WebSocketChannelSession session);

}
