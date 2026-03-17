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

package it.water.websocket.api.channel;

import it.water.websocket.model.WebSocketUserInfo;
import it.water.websocket.model.message.WebSocketMessage;

import java.util.Collection;
import java.util.Map;
import java.util.Set;


public interface WebSocketChannelManager {
    WebSocketChannelClusterMessageBroker getClusterBroker();

    WebSocketChannel findChannel(String channelId);

    boolean channelExists(String channelId);

    Collection<WebSocketChannel> getAvailableChannels();

    void createChannel(String channelType, String channelName, String newChannelId, int maxPartecipants, Map<String, Object> params, WebSocketChannelSession ownerSession, Set<WebSocketChannelRole> roles);

    void joinChannel(String channelId, WebSocketChannelSession partecipantSession, Set<WebSocketChannelRole> roles);

    void leaveChannel(String channelId, WebSocketChannelSession partecipantSession);

    void kickParticipant(String channelId, WebSocketUserInfo kickerInfo, WebSocketMessage kickMessageCommand);

    void banParticipant(String channelId, WebSocketUserInfo bannerInfo, WebSocketMessage banMessageCommand);

    void unbanParticipant(String channelId, WebSocketUserInfo bannerInfo, WebSocketMessage unbanMessageCommand);

    void deleteChannel(WebSocketUserInfo userInfo, WebSocketChannel newChannel);

    void deliverMessage(WebSocketMessage message);

    void forwardMessage(String channelId, WebSocketMessage message);

    void onChannelAdded(WebSocketChannel channel);

    void onChannelRemoved(String channelId);

    void onPartecipantAdded(String channelId, WebSocketUserInfo partecipantInfo, Set<WebSocketChannelRole> roles);

    void onPartecipantGone(String channelId, WebSocketUserInfo partecipantInfo);

    void onPartecipantDisconnected(String channelId, WebSocketUserInfo partecipantInfo);
}
