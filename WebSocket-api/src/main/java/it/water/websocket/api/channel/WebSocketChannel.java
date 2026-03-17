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

import it.water.core.api.service.cluster.ClusterNodeInfo;
import it.water.websocket.api.WebSocketCommand;
import it.water.websocket.api.WebSocketSession;
import it.water.websocket.model.WebSocketUserInfo;
import it.water.websocket.model.message.WebSocketMessage;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface WebSocketChannel {

    String getChannelId();

    String getChannelName();

    void defineClusterMessageBroker(WebSocketChannelClusterMessageBroker clusterMessageBroker);

    Map<String, Object> getChannelParams();

    Object getChannelParam(String name);

    void addChannelParam(String name, Object value);

    void removeChannelParam(String name);

    Optional<WebSocketUserInfo> findUserInfo(String usedId);

    Set<WebSocketUserInfo> getPartecipantsInfo();

    Optional<WebSocketUserInfo> findPartecipantInfoFromUserId(String userId);

    void addPartecipantInfo(WebSocketUserInfo partecipantInfo, Set<WebSocketChannelRole> roles);

    void addPartecipantSession(WebSocketUserInfo partecipantInfo, Set<WebSocketChannelRole> roles, WebSocketChannelSession session);

    void leaveChannel(WebSocketUserInfo participantInfo);

    WebSocketSession getPartecipantSession(WebSocketUserInfo partecipantInfo);

    boolean hasPartecipantSession(WebSocketUserInfo partecipantInfo);

    void kickPartecipant(WebSocketUserInfo kickerInfo, WebSocketMessage kickMessageCommand);

    void banPartecipant(WebSocketUserInfo banner, WebSocketMessage banMessageCommand);

    void unbanPartecipant(WebSocketUserInfo banner, WebSocketMessage unbanMessageCommand);

    void removePartecipant(WebSocketUserInfo partecipantInfo);

    boolean userHasPermission(WebSocketUserInfo user, WebSocketCommand commandType);

    Set<ClusterNodeInfo> getPeers();

    /**
     * Send the message through multiple session even across the cluster on different nodes
     *
     * @param senderSession
     * @param message
     */
    void exchangeMessage(WebSocketChannelSession senderSession, WebSocketMessage message);

    /**
     * Deliver current message on local sessions on the current node on which message has been received
     *
     * @param sender
     * @param message
     */
    void deliverMessage(WebSocketUserInfo sender, WebSocketMessage message);

    /**
     * @param senderSession
     * @param message
     * @return
     */
    void receiveMessageForServer(WebSocketChannelSession senderSession, WebSocketMessage message);

    String toJson();
}
