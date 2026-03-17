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

package it.water.websocket.channel.manager;

import it.water.core.model.exceptions.WaterRuntimeException;
import it.water.websocket.api.WebSocketBasicCommandType;
import it.water.websocket.api.WebSocketSession;
import it.water.websocket.api.channel.*;
import it.water.websocket.channel.command.WebSocketChannelCommandType;
import it.water.websocket.channel.factory.WebSocketChannelFactory;
import it.water.websocket.channel.role.WebSocketChannelRoleManager;
import it.water.websocket.channel.util.WebSocketChannelConstants;
import it.water.websocket.model.WebSocketUserInfo;
import it.water.websocket.model.message.WebSocketMessage;
import it.water.websocket.model.message.WebSocketMessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class WebSocketDefaultChannelManager<T extends WebSocketChannel> implements WebSocketChannelManager {
    private static Logger log = LoggerFactory.getLogger(WebSocketDefaultChannelManager.class);

    private ConcurrentHashMap<String, WebSocketChannel> channels;
    private WebSocketChannelClusterCoordinator coordinator;
    private WebSocketChannelClusterMessageBroker clusterBroker;
    private Class<T> channelClass;

    public WebSocketDefaultChannelManager(Class<T> channelClass, WebSocketChannelClusterCoordinator coordinator, WebSocketChannelClusterMessageBroker clusterBroker) {
        this.channels = new ConcurrentHashMap<>(1);
        //loading already create channels eventually on other cluster instances
        this.coordinator = coordinator;
        this.clusterBroker = clusterBroker;
        if (this.clusterBroker != null)
            this.clusterBroker.registerChannelManager(this);
        this.channels.putAll(coordinator.connectNewPeer(this));
        this.channelClass = channelClass;
    }

    public WebSocketChannelClusterMessageBroker getClusterBroker() {
        return clusterBroker;
    }

    @Override
    public WebSocketChannel findChannel(String channelId) {
        return channels.get(channelId);
    }

    @Override
    public boolean channelExists(String channelId) {
        return channels.containsKey(channelId);
    }

    @Override
    public Collection<WebSocketChannel> getAvailableChannels() {
        return Collections.unmodifiableCollection(channels.values());
    }

    @Override
    public void createChannel(String channelType, String channelName, String newChannelId, int maxPartecipants, Map<String, Object> params, WebSocketChannelSession ownerSession, Set<WebSocketChannelRole> roles) {
        try {
            WebSocketChannel newChannel = WebSocketChannelFactory.createChannelFromChannelType(channelType, newChannelId, channelName, maxPartecipants, params, this.clusterBroker);
            if(!this.channels.containsKey(newChannelId)) {
                this.channels.put(newChannelId, newChannel);
                //automatically join channel after creation
                notifyChannelCreated(newChannel, ownerSession, roles);
                joinChannel(newChannelId, ownerSession, roles);
            } else {
                throw new WaterRuntimeException("Channel Already exists!");
            }
        } catch (Throwable t) {
            throw new WaterRuntimeException(t.getMessage());
        }
    }

    @Override
    public void joinChannel(String channelId, WebSocketChannelSession partecipantSession, Set<WebSocketChannelRole> roles) {
        WebSocketChannel channel = findChannel(channelId);
        if (channel != null) {
            Set<WebSocketChannelRole> joinChannelRoles = WebSocketChannelRoleManager.newRoleSet(roles, defineJoinChannelRoles());
            channel.addPartecipantSession(partecipantSession.getUserInfo(), joinChannelRoles, partecipantSession);
            notifyPartecipantAdded(channel, partecipantSession, roles);
            return;
        }
        throw new WaterRuntimeException("Channel not found!");
    }

    @Override
    public void leaveChannel(String channelId, WebSocketChannelSession partecipantSession) {
        WebSocketChannel channel = findChannel(channelId);
        if (channel != null) {
            channel.leaveChannel(partecipantSession.getUserInfo());
            notifyPartecipantGone(channel, partecipantSession);
            return;
        }
        throw new WaterRuntimeException("Channel not found!");
    }

    @Override
    public void kickParticipant(String channelId, WebSocketUserInfo kickerInfo, WebSocketMessage kickMessageCommand) {
        WebSocketChannel channel = findChannel(channelId);
        if (channel != null) {
            channel.kickPartecipant(kickerInfo, kickMessageCommand);
            String usernameToKick = kickMessageCommand.getParams().get(WebSocketChannelConstants.CHANNEL_MESSAGE_PARAM_USER_TO_KICK);
            Optional<WebSocketUserInfo> toKick = channel.getPartecipantsInfo().stream().filter(info -> info.getUsername().equalsIgnoreCase(usernameToKick)).findAny();
            //send coordination message only from the node which owns the kicked user websocket session
            if (toKick.isPresent() && channel.hasPartecipantSession(toKick.get()))
                this.coordinator.notifyPartecipantGone(channelId, toKick.get());
            return;
        }
        throw new WaterRuntimeException("Channel not found!");
    }

    @Override
    public void banParticipant(String channelId, WebSocketUserInfo bannerInfo, WebSocketMessage banMessageCommand) {
        WebSocketChannel channel = findChannel(channelId);
        if (channel != null) {
            channel.banPartecipant(bannerInfo, banMessageCommand);
            String usernameToKick = banMessageCommand.getParams().get(WebSocketChannelConstants.CHANNEL_MESSAGE_PARAM_USER_TO_KICK);
            Optional<WebSocketUserInfo> toBan = channel.getPartecipantsInfo().stream().filter(info -> info.getUsername().equalsIgnoreCase(usernameToKick)).findAny();
            //send coordination message only from the node which owns the kicked user websocket session
            if (toBan.isPresent() && channel.hasPartecipantSession(toBan.get()))
                this.coordinator.notifyPartecipantGone(channelId, toBan.get());
            return;
        }
        throw new WaterRuntimeException("Channel not found!");
    }

    @Override
    public void unbanParticipant(String channelId, WebSocketUserInfo bannerInfo, WebSocketMessage unbanMessageCommand) {
        WebSocketChannel channel = findChannel(channelId);
        if (channel != null) {
            channel.unbanPartecipant(bannerInfo, unbanMessageCommand);
            return;
        }
        throw new WaterRuntimeException("Channel not found!");
    }

    @Override
    public void deleteChannel(WebSocketUserInfo userInfo, WebSocketChannel toDeleteChannel) {
        if (toDeleteChannel != null && toDeleteChannel.userHasPermission(userInfo, WebSocketChannelCommandType.DELETE_CHANNEl)) {
            WebSocketSession s = toDeleteChannel.getPartecipantSession(userInfo);
            channels.remove(toDeleteChannel);
            coordinator.notifyChannelDeleted(toDeleteChannel.getChannelId());
            //only in this case channel manager sends message over sessions
            s.sendRemote(WebSocketMessage.createMessage(WebSocketBasicCommandType.READ_MESSAGE_COMMAND, "CHANNEL_DELETED".getBytes(StandardCharsets.UTF_8), WebSocketMessageType.OK));
        } else {
            throw new WaterRuntimeException("Channel not found or you don't have permissions to delete it");
        }
    }

    @Override
    public void deliverMessage(WebSocketMessage message) {
        final String channelId = message.getParams().get(WebSocketChannelConstants.CHANNEL_ID_PARAM);
        final String sender = message.getParams().get(WebSocketMessage.WS_MESSAGE_SENDER_PARAM_NAME);
        WebSocketChannel channel = findChannel(channelId);
        Optional<WebSocketUserInfo> senderInfo = channel.findUserInfo(sender);
        if (channel != null) {
            if (message.getCmd().equals(WebSocketBasicCommandType.READ_MESSAGE_COMMAND))
                channel.deliverMessage(senderInfo.orElse(null), message);
        } else {
            log.error("Impossible to forward message to user from sender :" + sender + " and channel :" + channelId);
        }
    }

    @Override
    public void forwardMessage(String channelId, WebSocketMessage message) {
        this.clusterBroker.sendMessage(channelId, message);
    }

    //callbacks from cluster coordinator
    @Override
    public void onChannelAdded(WebSocketChannel channel) {
        channels.putIfAbsent(channel.getChannelId(), channel);
    }

    @Override
    public void onChannelRemoved(String channelId) {
        if (channels.containsKey(channelId)) {
            channels.remove(channelId);
        }
    }

    @Override
    public void onPartecipantAdded(String channelId, WebSocketUserInfo partecipantInfo, Set<WebSocketChannelRole> roles) {
        WebSocketChannel channel = findChannel(channelId);
        if (channel != null) {
            if (!partecipantInfo.isOnLocalNode(null))
                channel.addPartecipantInfo(partecipantInfo, roles);
            return;
        }
        throw new WaterRuntimeException("Channel cannot be null");
    }

    @Override
    public void onPartecipantGone(String channelId, WebSocketUserInfo partecipantInfo) {
        if (partecipantInfo.isOnLocalNode(null))
            return;
        WebSocketChannel channel = findChannel(channelId);
        if (channel != null) {
            channel.removePartecipant(partecipantInfo);
            return;
        }
        throw new WaterRuntimeException("Channel cannot be null");
    }

    @Override
    public void onPartecipantDisconnected(String channelId, WebSocketUserInfo partecipantInfo) {
        if (partecipantInfo.isOnLocalNode(null))
            return;
        WebSocketChannel channel = findChannel(channelId);
        if (channel != null) {
            channel.removePartecipant(partecipantInfo);
            return;
        }
        throw new WaterRuntimeException("Channel cannot be null");
    }

    /**
     * Roles to add to partecipants when they are joining a channel
     *
     * @return
     */
    protected Set<WebSocketChannelRole> defineJoinChannelRoles() {
        return Collections.emptySet();
    }

    /**
     * Roles to give to the channel owners when they create channel or they promote other users to be owner
     *
     * @return
     */
    protected Set<WebSocketChannelRole> defineChannelOwnerRoles() {
        return Collections.emptySet();
    }

    /**
     * @param channel
     * @param partecipantSession
     * @param roles
     */
    private void notifyChannelCreated(WebSocketChannel channel, WebSocketChannelSession partecipantSession, Set<WebSocketChannelRole> roles) {
        //notify inside the cluster
        coordinator.notifyChannelAdded(partecipantSession.getUserInfo().getClusterNodeInfo(),channel);
    }

    /**
     * @param channel
     * @param partecipantSession
     * @param roles
     */
    private void notifyPartecipantAdded(WebSocketChannel channel, WebSocketChannelSession partecipantSession, Set<WebSocketChannelRole> roles) {
        //notify inside the cluster
        coordinator.notifyPartecipantAdded(channel.getChannelId(), partecipantSession.getUserInfo(), roles);
    }

    /**
     * @param channel
     * @param partecipantSession
     */
    private void notifyPartecipantGone(WebSocketChannel channel, WebSocketChannelSession partecipantSession) {
        coordinator.notifyPartecipantGone(channel.getChannelId(), partecipantSession.getUserInfo());
    }
}
