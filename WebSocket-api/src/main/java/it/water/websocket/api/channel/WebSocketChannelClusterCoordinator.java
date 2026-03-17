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
import it.water.websocket.model.WebSocketUserInfo;

import java.util.Map;
import java.util.Set;

public interface WebSocketChannelClusterCoordinator {
    /**
     * Connecting new peer means receive current channel list
     * @param channelManager
     * @return
     */
    Map<String, WebSocketChannel> connectNewPeer(WebSocketChannelManager channelManager);

    /**
     * Disconnects
     */
    void disconnectPeer();

    /**
     * @param channel
     */
    void notifyChannelAdded(ClusterNodeInfo sourceNode, WebSocketChannel channel);

    /**
     * @param channelId
     */
    void notifyChannelDeleted(String channelId);

    /**
     * @param channelId
     * @param partecipantInfo
     * @param roles
     */
    void notifyPartecipantAdded(String channelId, WebSocketUserInfo partecipantInfo, Set<WebSocketChannelRole> roles);

    /**
     * @param channelId
     * @param partecipantInfo
     */
    void notifyPartecipantGone(String channelId, WebSocketUserInfo partecipantInfo);

    /**
     * @param channelId
     * @param partecipantInfo
     */
    void notifyPartecipantDisconnected(String channelId, WebSocketUserInfo partecipantInfo);

    /**
     *
     * @return the current channel manager
     */
    WebSocketChannelManager getRegisteredWebSocketChannelManager();

}
