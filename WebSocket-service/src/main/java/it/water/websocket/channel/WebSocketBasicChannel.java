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

package it.water.websocket.channel;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.water.core.api.service.cluster.ClusterNodeInfo;
import it.water.core.model.exceptions.WaterRuntimeException;
import it.water.websocket.api.WebSocketBasicCommandType;
import it.water.websocket.api.WebSocketCommand;
import it.water.websocket.api.WebSocketSession;
import it.water.websocket.api.channel.WebSocketChannel;
import it.water.websocket.api.channel.WebSocketChannelClusterMessageBroker;
import it.water.websocket.api.channel.WebSocketChannelRole;
import it.water.websocket.api.channel.WebSocketChannelSession;
import it.water.websocket.channel.command.WebSocketChannelCommandType;
import it.water.websocket.channel.util.WebSocketChannelConstants;
import it.water.websocket.model.WebSocketConstants;
import it.water.websocket.model.WebSocketUserInfo;
import it.water.websocket.model.message.WebSocketMessage;
import it.water.websocket.model.message.WebSocketMessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.*;


@JsonIgnoreProperties(ignoreUnknown = true)
@SuppressWarnings("java:S1948") // channel collaborators are restored at runtime after deserialization
public class WebSocketBasicChannel implements WebSocketChannel, Serializable {
    private static final long serialVersionUID = 1L;

    @JsonIgnore
    private static final Logger log = LoggerFactory.getLogger(WebSocketBasicChannel.class);

    private String channelId;
    private String channelName;
    private int maxPartecipants;

    private Map<String, Object> channelParams;

    private static final ObjectMapper mapper = new ObjectMapper();

    //All session across the eventual cluster
    @JsonIgnore
    private Map<WebSocketUserInfo, Set<WebSocketChannelRole>> partecipantsInfo;

    //All session on the specific node
    @JsonIgnore
    private transient Map<WebSocketUserInfo, WebSocketChannelSession> partecipantsSessions;

    private WebSocketChannelClusterMessageBroker clusterMessageBroker;

    private List<String> bennedIps;

    private List<String> bannedUsernames;

    public WebSocketBasicChannel(String channelName, String channelId, int maxPartecipants, Map<String, Object> channelParams, WebSocketChannelClusterMessageBroker clusterMessageBroker) {
        this.channelName = channelName;
        this.channelId = channelId;
        this.maxPartecipants = maxPartecipants;
        this.partecipantsInfo = Collections.synchronizedMap(new HashMap<>());
        this.partecipantsSessions = new HashMap<>();
        this.bennedIps = Collections.synchronizedList(new ArrayList<>());
        this.bannedUsernames = Collections.synchronizedList(new ArrayList<>());
        this.channelParams = channelParams;
        defineClusterMessageBroker(clusterMessageBroker);
    }

    protected WebSocketBasicChannel() {
        this(null, null, 0, null, null);
    }

    public void defineClusterMessageBroker(WebSocketChannelClusterMessageBroker clusterMessageBroker) {
        this.clusterMessageBroker = clusterMessageBroker;
    }

    @Override
    public String getChannelId() {
        return channelId;
    }

    @Override
    public String getChannelName() {
        return channelName;
    }

    @Override
    public void addChannelParam(String name, Object value) {
        this.channelParams.put(name, value);
    }

    @Override
    public void removeChannelParam(String name) {
        this.channelParams.remove(name);
    }

    @Override
    public Map<String, Object> getChannelParams() {
        return Collections.unmodifiableMap(this.channelParams);
    }

    @Override
    public Object getChannelParam(String name) {
        return this.channelParams.get(name);
    }

    @Override
    public Optional<WebSocketUserInfo> findUserInfo(String usedId) {
        return partecipantsInfo.keySet().stream().filter(info -> info.getUsername().equalsIgnoreCase(usedId)).findAny();
    }

    public Set<WebSocketUserInfo> getPartecipantsInfo() {
        return Collections.unmodifiableSet(partecipantsInfo.keySet());
    }

    public Optional<WebSocketUserInfo> findPartecipantInfoFromUserId(String userId) {
        return partecipantsInfo.keySet().stream().filter(partecipantInfo -> partecipantInfo.getUsername().equalsIgnoreCase(userId)).findAny();
    }

    public void addPartecipantInfo(WebSocketUserInfo partecipantInfo, Set<WebSocketChannelRole> roles) {
        this.checkPartecipantsLimits();
        this.partecipantsInfo.put(partecipantInfo, Collections.unmodifiableSet(roles));
    }

    public synchronized void addPartecipantSession(WebSocketUserInfo partecipantInfo, Set<WebSocketChannelRole> roles, WebSocketChannelSession session) {
        this.checkPartecipantsLimits();
        if (!this.bennedIps.contains(partecipantInfo.getIpAddress()) && !this.bannedUsernames.contains(partecipantInfo.getUsername())) {
            addPartecipantInfo(partecipantInfo, roles);
            this.partecipantsSessions.put(partecipantInfo, session);
            //used to send a feedback message to the connecting user
            this.partecipantJoined(session);
        } else {
            throw new WaterRuntimeException("Cannot join channel " + this.channelName + ", you have been banned!");
        }
    }

    @Override
    public WebSocketSession getPartecipantSession(WebSocketUserInfo partecipantInfo) {
        return this.partecipantsSessions.get(partecipantInfo);
    }

    @Override
    public boolean hasPartecipantSession(WebSocketUserInfo partecipantInfo) {
        return this.partecipantsSessions.containsKey(partecipantInfo);
    }

    public void kickPartecipant(WebSocketUserInfo kickerInfo, WebSocketMessage kickMessageCommand) {
        boolean kickerIsOnThisNode = kickerInfo.isOnLocalNode(null);
        String usernameToKick = kickMessageCommand.getParams().get(WebSocketChannelConstants.CHANNEL_MESSAGE_PARAM_USER_TO_KICK);
        Optional<WebSocketUserInfo> toKick = this.partecipantsInfo.keySet().stream().filter(info -> info.getUsername().equalsIgnoreCase(usernameToKick)).findAny();
        //broadcast the message only if it is on the kicker node
        //done on the kicker node
        if (kickerIsOnThisNode) {
            //send command to other nodes in order to allign their state
            this.clusterMessageBroker.sendMessage(channelId, kickMessageCommand);
        }
        if (toKick.isEmpty()) {
            sendKOMessage(this.partecipantsSessions.get(kickerInfo), "User not found in channel!");
            return;
        }
        //might be null if the kicked user session is on another node
        WebSocketSession kickedUserSession = this.partecipantsSessions.get(toKick.get());
        //each node will remove the info about the kicked user
        if (userHasPermission(kickerInfo, WebSocketChannelCommandType.KICK_USER)) {
            removePartecipant(toKick.get());
            //notify all local channel participant user has been kicked
            String kickMessageStr = (kickMessageCommand.getParams() != null && kickMessageCommand.getParams().containsKey(WebSocketChannelConstants.CHANNEL_MESSAGE_PARAM_KICK_MESSAGE)) ? kickMessageCommand.getParams().get(WebSocketChannelConstants.CHANNEL_MESSAGE_PARAM_KICK_MESSAGE) : "";
            WebSocketMessage kickMessageNotification = WebSocketMessage.createMessage(WebSocketBasicCommandType.READ_MESSAGE_COMMAND, kickMessageStr.getBytes(StandardCharsets.UTF_8), WebSocketMessageType.PARTICIPANT_KICKED);
            //copying params from original message sender,kicker,channel
            kickMessageNotification.setParams(kickMessageCommand.getParams());
            //deliver kick message notification on the local node session
            //each node will notify its own users inside the channel since every node will receive the kick notification
            deliverMessage(null, kickMessageNotification);
            //send kick message to kicked user, since it is outside from the channel
            if (kickedUserSession != null) {
                kickedUserSession.sendRemote(kickMessageNotification);
            }
        } else {
            //it is sent only if the session is on the current node
            sendKOMessage(this.partecipantsSessions.get(kickerInfo), "You do not have permissions to perform kick!");
        }
    }

    public void banPartecipant(WebSocketUserInfo banner, WebSocketMessage banMessageCommand) {
        String usernameToBan = banMessageCommand.getParams().get(WebSocketChannelConstants.CHANNEL_MESSAGE_PARAM_USER_TO_KICK);
        Optional<WebSocketUserInfo> toBan = this.partecipantsInfo.keySet().stream().filter(info -> info.getUsername().equalsIgnoreCase(usernameToBan)).findAny();
        boolean bannerIsOnThisNode = banner.isOnLocalNode(null);
        //broadcast the message only if it is on the kicker node
        //done on the kicker node
        if (bannerIsOnThisNode) {
            //send command to other nodes in order to allign their state
            this.clusterMessageBroker.sendMessage(channelId, banMessageCommand);
        }
        if (toBan.isPresent()) {
            if (userHasPermission(banner, WebSocketChannelCommandType.BAN_USER)) {
                this.kickPartecipant(banner, banMessageCommand);
                this.bennedIps.add(toBan.get().getIpAddress());
                this.bannedUsernames.add(toBan.get().getUsername());
            } else {
                sendKOMessage(this.partecipantsSessions.get(banner), "You do not have permissions to ban!");
            }
        }
    }

    public void unbanPartecipant(WebSocketUserInfo banner, WebSocketMessage unbanMessageCommand) {
        boolean bannerIsOnThisNode = banner.isOnLocalNode(null);
        //broadcast the message only if it is on the kicker node
        //done on the kicker node
        if (bannerIsOnThisNode) {
            //send command to other nodes in order to allign their state
            this.clusterMessageBroker.sendMessage(channelId, unbanMessageCommand);
        }

        if (userHasPermission(banner, WebSocketChannelCommandType.BAN_USER)) {
            String ipAddress = unbanMessageCommand.getParams().get(WebSocketChannelConstants.CHANNEL_MESSAGE_PARAM_BANNED_IP);
            String bannedUsername = unbanMessageCommand.getParams().get(WebSocketChannelConstants.CHANNEL_MESSAGE_PARAM_BANNED_USERNAME);
            this.bennedIps.remove(ipAddress);
            this.bannedUsernames.remove(bannedUsername);
            if (bannerIsOnThisNode)
                sendOKMessage(this.partecipantsSessions.get(banner), "UNBANNED");
        }
    }

    public void removePartecipant(WebSocketUserInfo participantInfo) {
        this.partecipantsInfo.remove(participantInfo);
        if (this.partecipantsSessions.containsKey(participantInfo)) {
            WebSocketChannelSession session = this.partecipantsSessions.get(participantInfo);
            session.removeJoinedChannels(this);
            this.partecipantsSessions.remove(participantInfo);
        }
    }

    public void leaveChannel(WebSocketUserInfo participantInfo) {
        WebSocketSession leavingUserSession = this.partecipantsSessions.get(participantInfo);
        this.removePartecipant(participantInfo);
        this.sendOKMessage(leavingUserSession, "CHANNEL_LEAVED");
        WebSocketMessage m = WebSocketMessage.createMessage(WebSocketBasicCommandType.READ_MESSAGE_COMMAND, "".getBytes(StandardCharsets.UTF_8), WebSocketMessageType.PARTICIPANT_GONE);
        m.getParams().put(WebSocketChannelConstants.CHANNEL_ID_PARAM, this.channelId);
        m.getParams().put(WebSocketConstants.WEB_SOCKET_USERNAME_PARAM, participantInfo.getUsername());
        this.broadcastSystemMessage(participantInfo, m);
    }

    public Set<ClusterNodeInfo> getPeers() {
        Set<ClusterNodeInfo> nodeList = new HashSet<>();
        partecipantsInfo.keySet().forEach(userInfo -> {
            if (!userInfo.isOnLocalNode(null)) {
                nodeList.add(userInfo.getClusterNodeInfo());
            }
        });
        return nodeList;
    }

    /**
     * Broadcasts a message inside the channel
     *
     * @param senderInfo
     * @param message
     */
    private void broadcastSystemMessage(WebSocketUserInfo senderInfo, WebSocketMessage message) {
        message.setCmd(WebSocketBasicCommandType.READ_MESSAGE.toString());
        deliverMessage(senderInfo, message);
        //deliver message to other session in other cluster nodes
        try {
            this.clusterMessageBroker.sendMessage(channelId, message);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Send the user message through multiple session even across the cluster
     *
     * @param senderSession
     * @param message
     */
    public void exchangeMessage(WebSocketChannelSession senderSession, WebSocketMessage message) {
        //Change command from send to read
        WebSocketUserInfo sender = senderSession.getUserInfo();
        if (userHasPermission(sender, WebSocketBasicCommandType.SEND_MESSAGE)) {
            message.setCmd(WebSocketBasicCommandType.READ_MESSAGE.toString());
            message.getParams().put(WebSocketChannelConstants.CHANNEL_MESSAGE_PARAM_PVT_MESSAGE_SENDER, sender.getUsername());
            //deliver message to session that are on current node
            deliverMessage(sender, message);
            //deliver message to other session in other cluster nodes
            try {
                //automatically converts the message to read and spread to the cluster
                this.clusterMessageBroker.sendMessage(channelId, message);
            } catch (Exception e) {
                String messageStr = "Error while sending message:" + e.getMessage();
                WebSocketMessage errorMess = WebSocketMessage.createMessage(WebSocketBasicCommandType.READ_MESSAGE_COMMAND, messageStr.getBytes(StandardCharsets.UTF_8), WebSocketMessageType.ERROR);
                senderSession.sendRemote(errorMess);
            }
        } else {
            WebSocketMessage errorMess = WebSocketMessage.createMessage(WebSocketBasicCommandType.READ_MESSAGE_COMMAND, "Unauthorized to send message".getBytes(StandardCharsets.UTF_8), WebSocketMessageType.ERROR);
            senderSession.sendRemote(errorMess);
        }
    }

    /**
     * Deliver current user message on local sessions on the current node on which message has been received
     *
     * @param sender
     * @param message
     */
    public void deliverMessage(WebSocketUserInfo sender, WebSocketMessage message) {
        //avoiding concurrent modificaiton expcetion with unmodifiable set
        Collections.unmodifiableSet(partecipantsSessions.keySet()).parallelStream().forEach(userInfo -> {
            try {
                //not delivering to the sender eventually
                boolean isMessageSender = sender != null && userInfo.getUsername().equalsIgnoreCase(sender.getUsername());
                boolean hasSingleRecipient = message.getParams() != null && message.getParams().containsKey(WebSocketConstants.WEB_SOCKET_RECIPIENT_USER_PARAM);
                String recipient = hasSingleRecipient ? message.getParams().get(WebSocketConstants.WEB_SOCKET_RECIPIENT_USER_PARAM) : null;
                if ((!hasSingleRecipient && !isMessageSender) || (hasSingleRecipient && userInfo.getUsername().equals(recipient))) {
                    WebSocketChannelSession session = this.partecipantsSessions.get(userInfo);
                    session.sendRemote(message);
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        });
    }

    @Override
    public void receiveMessageForServer(WebSocketChannelSession senderSession, WebSocketMessage message) {
        try {
            String serverResponse = processMessageOnServer(senderSession, message);
            //getting server response, send back to the client
            if (serverResponse != null) {
                WebSocketMessage responseMessage = WebSocketMessage.createMessage(null, serverResponse.getBytes(StandardCharsets.UTF_8), WebSocketMessageType.RESULT);
                senderSession.sendRemote(responseMessage);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            //sending back to the client the error message
            WebSocketMessage errMessage = WebSocketMessage.createMessage(null, e.getMessage().getBytes(StandardCharsets.UTF_8), WebSocketMessageType.ERROR);
            senderSession.sendRemote(errMessage);
        }
    }

    @SuppressWarnings({"java:S1172", "java:S3400"}) // parameters and return value are part of the overridable extension point
    protected String processMessageOnServer(WebSocketChannelSession senderSession, WebSocketMessage message) {
        //Method can be overridden in order to develop custom server logic
        return "";
    }

    public String toJson() {
        try {
            return mapper.writeValueAsString(this);
        } catch (Exception t) {
            log.error(t.getMessage(), t);
        }
        return "{}";
    }

    public boolean userHasPermission(WebSocketUserInfo user, WebSocketCommand commandType) {
        Set<WebSocketChannelRole> roles = partecipantsInfo.get(user);
        return roles != null && roles.stream().anyMatch(role -> role.getAllowedCmds().contains(commandType));
    }

    private void checkPartecipantsLimits() {
        //limit is enabled if maxPartecipants is greater than 0
        if (this.maxPartecipants > 0 && this.partecipantsInfo.keySet().size() >= this.maxPartecipants)
            throw new WaterRuntimeException("Cannot add partecipants to current channel:Maximum number of partecipants reached!");
    }

    /**
     * Can be customized with custom messages as an user get connected to a channel
     *
     * @param userSession
     */
    protected void partecipantJoined(WebSocketChannelSession userSession) {
        userSession.addJoinedChannels(this);
        sendOKMessage(userSession, "SUCCESFULLY_JOINED");
        WebSocketMessage participantJoined = WebSocketMessage.createMessage(WebSocketBasicCommandType.READ_MESSAGE_COMMAND, "".getBytes(StandardCharsets.UTF_8), WebSocketMessageType.PARTICIPANT_ADDED);
        participantJoined.getParams().put(WebSocketChannelConstants.CHANNEL_ID_PARAM, this.channelId);
        participantJoined.getParams().put(WebSocketConstants.WEB_SOCKET_USERNAME_PARAM, userSession.getUserInfo().getUsername());
        //broadcast to other participants
        broadcastSystemMessage(userSession.getUserInfo(), participantJoined);
    }

    /**
     * Sends ok message as response only if the current user has a session on the current node
     */
    protected void sendOKMessage(WebSocketSession session, String payload) {
        if (session != null) {
            WebSocketMessage message = WebSocketMessage.createMessage(WebSocketBasicCommandType.READ_MESSAGE_COMMAND, payload.getBytes(StandardCharsets.UTF_8), WebSocketMessageType.OK);
            session.sendRemote(message);
        }
    }

    /**
     * Sends ok message as response only if the current user has a session on the current node
     */
    protected void sendKOMessage(WebSocketSession session, String koMessage) {
        if (session != null) {
            WebSocketMessage message = WebSocketMessage.createMessage(WebSocketBasicCommandType.READ_MESSAGE_COMMAND, koMessage.getBytes(StandardCharsets.UTF_8), WebSocketMessageType.ERROR);
            session.sendRemote(message);
        }
    }
}
