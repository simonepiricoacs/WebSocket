package it.water.websocket.channel;

import it.water.core.model.exceptions.WaterRuntimeException;
import it.water.websocket.api.WebSocketBasicCommandType;
import it.water.websocket.api.channel.WebSocketChannelClusterMessageBroker;
import it.water.websocket.api.channel.WebSocketChannelRole;
import it.water.websocket.api.channel.WebSocketChannelSession;
import it.water.websocket.channel.command.WebSocketChannelCommandType;
import it.water.websocket.channel.role.WebSocketChannelOwnerRole;
import it.water.websocket.channel.role.WebSocketChannelParticipantRole;
import it.water.websocket.channel.util.WebSocketChannelConstants;
import it.water.websocket.model.WebSocketConstants;
import it.water.websocket.model.WebSocketUserInfo;
import it.water.websocket.model.message.WebSocketMessage;
import it.water.websocket.model.message.WebSocketMessageType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@SuppressWarnings("java:S5778") // inline mock() calls are intentional for locally scoped test scenarios
class WebSocketBasicChannelTest {

    private WebSocketBasicChannel channel;

    @Mock
    private WebSocketChannelClusterMessageBroker broker;

    @Mock
    private WebSocketChannelSession ownerSession;

    @Mock
    private WebSocketChannelSession participantSession;

    private WebSocketChannelOwnerRole ownerRole;
    private WebSocketChannelParticipantRole participantRole;

    private WebSocketUserInfo ownerInfo;
    private WebSocketUserInfo participantInfo;

    @BeforeEach
    void setUp() {
        channel = new WebSocketBasicChannel("test-channel", "ch-1", 10,
                new HashMap<>(), broker);
        ownerRole = new WebSocketChannelOwnerRole();
        participantRole = new WebSocketChannelParticipantRole();

        ownerInfo = new WebSocketUserInfo("owner", null, "127.0.0.1");
        participantInfo = new WebSocketUserInfo("participant", null, "10.0.0.1");

        when(ownerSession.getUserInfo()).thenReturn(ownerInfo);
        when(participantSession.getUserInfo()).thenReturn(participantInfo);
    }

    // --- Getters ---

    @Test
    void getChannelIdAndNameReturnCorrectValues() {
        assertEquals("ch-1", channel.getChannelId());
        assertEquals("test-channel", channel.getChannelName());
    }

    @Test
    void channelParamsOperationsWork() {
        channel.addChannelParam("key1", "value1");
        assertEquals("value1", channel.getChannelParam("key1"));

        Map<String, Object> params = channel.getChannelParams();
        assertTrue(params.containsKey("key1"));

        channel.removeChannelParam("key1");
        assertNull(channel.getChannelParam("key1"));
    }

    // --- addPartecipantInfo / addPartecipantSession ---

    @Test
    void addPartecipantInfoAddsToChannel() {
        Set<WebSocketChannelRole> roles = Collections.singleton(participantRole);
        channel.addPartecipantInfo(participantInfo, roles);
        assertTrue(channel.getPartecipantsInfo().contains(participantInfo));
    }

    @Test
    void addPartecipantSessionAddsSessionAndBroadcasts() {
        Set<WebSocketChannelRole> roles = Collections.singleton(ownerRole);
        channel.addPartecipantSession(ownerInfo, roles, ownerSession);

        assertTrue(channel.hasPartecipantSession(ownerInfo));
        assertEquals(ownerSession, channel.getPartecipantSession(ownerInfo));
        verify(ownerSession, atLeastOnce()).addJoinedChannels(channel);
        verify(ownerSession, atLeastOnce()).sendRemote(any(WebSocketMessage.class));
    }

    @Test
    void addPartecipantSessionThrowsWhenBannedByIp() {
        // Ban participant's IP
        WebSocketUserInfo bannerInfo = new WebSocketUserInfo("owner", null, "127.0.0.1");
        Set<WebSocketChannelRole> ownerRoles = Collections.singleton(ownerRole);
        channel.addPartecipantSession(bannerInfo, ownerRoles, ownerSession);

        // Setup unban message to actually add to ban list
        WebSocketMessage banMsg = new WebSocketMessage();
        HashMap<String, String> params = new HashMap<>();
        params.put(WebSocketChannelConstants.CHANNEL_MESSAGE_PARAM_USER_TO_KICK, participantInfo.getUsername());
        params.put(WebSocketChannelConstants.CHANNEL_MESSAGE_PARAM_BANNED_IP, participantInfo.getIpAddress());
        params.put(WebSocketChannelConstants.CHANNEL_MESSAGE_PARAM_BANNED_USERNAME, participantInfo.getUsername());
        banMsg.setParams(params);

        // First add participant to be banned
        channel.addPartecipantSession(participantInfo, Collections.singleton(participantRole), participantSession);
        // Ban them
        channel.banPartecipant(bannerInfo, banMsg);

        // Try to join again — should fail
        WebSocketUserInfo bannedUser = new WebSocketUserInfo("participant", null, "10.0.0.1");
        assertThrows(WaterRuntimeException.class, () ->
                channel.addPartecipantSession(bannedUser, Collections.singleton(participantRole), participantSession)
        );
    }

    @Test
    void addPartecipantSessionThrowsWhenMaxReached() {
        WebSocketBasicChannel smallChannel = new WebSocketBasicChannel("small", "ch-small", 1,
                new HashMap<>(), broker);

        WebSocketUserInfo user1 = new WebSocketUserInfo("user1", null, "1.1.1.1");
        WebSocketChannelSession session1 = mock(WebSocketChannelSession.class);
        when(session1.getUserInfo()).thenReturn(user1);

        smallChannel.addPartecipantSession(user1, Collections.singleton(participantRole), session1);

        WebSocketUserInfo user2 = new WebSocketUserInfo("user2", null, "2.2.2.2");
        WebSocketChannelSession session2 = mock(WebSocketChannelSession.class);
        when(session2.getUserInfo()).thenReturn(user2);

        assertThrows(WaterRuntimeException.class, () ->
                smallChannel.addPartecipantSession(user2, Collections.singleton(participantRole), session2)
        );
    }

    // --- leaveChannel ---

    @Test
    void leaveChannelRemovesParticipantAndBroadcasts() {
        channel.addPartecipantSession(ownerInfo, Collections.singleton(ownerRole), ownerSession);
        channel.addPartecipantSession(participantInfo, Collections.singleton(participantRole), participantSession);

        channel.leaveChannel(participantInfo);

        assertFalse(channel.hasPartecipantSession(participantInfo));
        assertFalse(channel.getPartecipantsInfo().contains(participantInfo));
        verify(participantSession, atLeastOnce()).sendRemote(any(WebSocketMessage.class));
    }

    // --- removePartecipant ---

    @Test
    void removePartecipantRemovesFromBothMaps() {
        channel.addPartecipantSession(participantInfo, Collections.singleton(participantRole), participantSession);
        channel.removePartecipant(participantInfo);
        assertFalse(channel.hasPartecipantSession(participantInfo));
        assertFalse(channel.getPartecipantsInfo().contains(participantInfo));
    }

    // --- findUserInfo / findPartecipantInfoFromUserId ---

    @Test
    void findUserInfoReturnsCorrectInfo() {
        channel.addPartecipantInfo(participantInfo, Collections.singleton(participantRole));
        Optional<WebSocketUserInfo> found = channel.findUserInfo("participant");
        assertTrue(found.isPresent());
        assertEquals("participant", found.get().getUsername());
    }

    @Test
    void findUserInfoReturnsEmptyForUnknownUser() {
        Optional<WebSocketUserInfo> found = channel.findUserInfo("nobody");
        assertFalse(found.isPresent());
    }

    @Test
    void findPartecipantInfoFromUserIdWorks() {
        channel.addPartecipantInfo(ownerInfo, Collections.singleton(ownerRole));
        Optional<WebSocketUserInfo> found = channel.findPartecipantInfoFromUserId("OWNER");
        assertTrue(found.isPresent());
    }

    // --- userHasPermission ---

    @Test
    void userHasPermissionReturnsTrueForAllowedCmd() {
        channel.addPartecipantSession(ownerInfo, Collections.singleton(ownerRole), ownerSession);
        assertTrue(channel.userHasPermission(ownerInfo, WebSocketChannelCommandType.KICK_USER));
    }

    @Test
    void userHasPermissionReturnsFalseForDisallowedCmd() {
        channel.addPartecipantSession(participantInfo, Collections.singleton(participantRole), participantSession);
        assertFalse(channel.userHasPermission(participantInfo, WebSocketChannelCommandType.KICK_USER));
    }

    @Test
    void userHasPermissionReturnsFalseForUnknownUser() {
        WebSocketUserInfo unknown = new WebSocketUserInfo("unknown", null, "9.9.9.9");
        assertFalse(channel.userHasPermission(unknown, WebSocketChannelCommandType.KICK_USER));
    }

    // --- kickPartecipant ---

    @Test
    void kickPartecipantWithPermissionRemovesUser() {
        channel.addPartecipantSession(ownerInfo, Collections.singleton(ownerRole), ownerSession);
        channel.addPartecipantSession(participantInfo, Collections.singleton(participantRole), participantSession);

        WebSocketMessage kickMsg = new WebSocketMessage();
        HashMap<String, String> params = new HashMap<>();
        params.put(WebSocketChannelConstants.CHANNEL_MESSAGE_PARAM_USER_TO_KICK, "participant");
        params.put(WebSocketChannelConstants.CHANNEL_MESSAGE_PARAM_KICK_MESSAGE, "Kicked by owner");
        kickMsg.setParams(params);

        channel.kickPartecipant(ownerInfo, kickMsg);

        assertFalse(channel.hasPartecipantSession(participantInfo));
    }

    @Test
    void kickPartecipantWithoutPermissionSendsError() {
        channel.addPartecipantSession(ownerInfo, Collections.singleton(ownerRole), ownerSession);
        channel.addPartecipantSession(participantInfo, Collections.singleton(participantRole), participantSession);

        WebSocketMessage kickMsg = new WebSocketMessage();
        HashMap<String, String> params = new HashMap<>();
        params.put(WebSocketChannelConstants.CHANNEL_MESSAGE_PARAM_USER_TO_KICK, "owner");
        params.put(WebSocketChannelConstants.CHANNEL_MESSAGE_PARAM_KICK_MESSAGE, "");
        kickMsg.setParams(params);

        channel.kickPartecipant(participantInfo, kickMsg);

        // participant has no kick permission — gets error message
        assertTrue(channel.hasPartecipantSession(ownerInfo));
    }

    // --- exchangeMessage ---

    @Test
    void exchangeMessageWithPermissionDeliversToOthers() {
        channel.addPartecipantSession(ownerInfo, Collections.singleton(ownerRole), ownerSession);
        channel.addPartecipantSession(participantInfo, Collections.singleton(participantRole), participantSession);

        WebSocketMessage msg = new WebSocketMessage();
        msg.setCmd(WebSocketBasicCommandType.SEND_MESSAGE.toString());
        msg.setPayload("hello".getBytes());
        msg.setParams(new HashMap<>());

        channel.exchangeMessage(ownerSession, msg);

        // participant should receive the message (not the sender)
        verify(participantSession, atLeastOnce()).sendRemote(any(WebSocketMessage.class));
    }

    @Test
    void exchangeMessageWithoutPermissionSendsErrorToSender() {
        channel.addPartecipantSession(ownerInfo, Collections.singleton(ownerRole), ownerSession);

        // Create a user with NO SEND_MESSAGE permission
        WebSocketChannelRole noSendRole = mock(WebSocketChannelRole.class);
        when(noSendRole.getAllowedCmds()).thenReturn(Collections.emptySet());
        WebSocketUserInfo restrictedUser = new WebSocketUserInfo("restricted", null, "5.5.5.5");
        WebSocketChannelSession restrictedSession = mock(WebSocketChannelSession.class);
        when(restrictedSession.getUserInfo()).thenReturn(restrictedUser);
        channel.addPartecipantSession(restrictedUser, Collections.singleton(noSendRole), restrictedSession);

        WebSocketMessage msg = new WebSocketMessage();
        msg.setCmd(WebSocketBasicCommandType.SEND_MESSAGE.toString());
        msg.setParams(new HashMap<>());

        channel.exchangeMessage(restrictedSession, msg);

        verify(restrictedSession, atLeastOnce()).sendRemote(argThat(m ->
                m.getType() == WebSocketMessageType.ERROR
        ));
    }

    // --- deliverMessage ---

    @Test
    void deliverMessageBroadcastsToAllExceptSender() {
        channel.addPartecipantSession(ownerInfo, Collections.singleton(ownerRole), ownerSession);
        channel.addPartecipantSession(participantInfo, Collections.singleton(participantRole), participantSession);
        // Reset invocations from setup (partecipantJoined sends messages during addPartecipantSession)
        clearInvocations(ownerSession, participantSession);

        WebSocketMessage msg = WebSocketMessage.createMessage("READ_MESSAGE", "test".getBytes(), WebSocketMessageType.OK);
        msg.setParams(new HashMap<>());

        channel.deliverMessage(ownerInfo, msg);

        // participant gets message, owner does not (is the sender)
        verify(participantSession, atLeastOnce()).sendRemote(any(WebSocketMessage.class));
        verify(ownerSession, never()).sendRemote(any(WebSocketMessage.class));
    }

    @Test
    void deliverMessageToSpecificRecipient() {
        channel.addPartecipantSession(ownerInfo, Collections.singleton(ownerRole), ownerSession);
        channel.addPartecipantSession(participantInfo, Collections.singleton(participantRole), participantSession);
        // Reset invocations from setup (partecipantJoined sends messages during addPartecipantSession)
        clearInvocations(ownerSession, participantSession);

        WebSocketMessage msg = WebSocketMessage.createMessage("READ_MESSAGE", "private".getBytes(), WebSocketMessageType.OK);
        HashMap<String, String> params = new HashMap<>();
        params.put(WebSocketConstants.WEB_SOCKET_RECIPIENT_USER_PARAM, "participant");
        msg.setParams(params);

        channel.deliverMessage(ownerInfo, msg);

        verify(participantSession, atLeastOnce()).sendRemote(any(WebSocketMessage.class));
        verify(ownerSession, never()).sendRemote(any(WebSocketMessage.class));
    }

    // --- getPeers ---

    @Test
    void getPeersReturnsOnlyRemoteNodeUsers() {
        // Single-node mode: all users have null clusterNodeInfo → all local
        channel.addPartecipantInfo(ownerInfo, Collections.singleton(ownerRole));
        Set<?> peers = channel.getPeers();
        assertTrue(peers.isEmpty()); // all on local node
    }

    // --- toJson ---

    @Test
    void toJsonReturnsValidJson() {
        String json = channel.toJson();
        assertNotNull(json);
        assertFalse(json.isEmpty());
        assertTrue(json.contains("ch-1") || json.contains("channelId"));
    }

    // --- unbanPartecipant ---

    @Test
    void unbanPartecipantRemovesFromBanLists() {
        channel.addPartecipantSession(ownerInfo, Collections.singleton(ownerRole), ownerSession);

        WebSocketMessage unbanMsg = new WebSocketMessage();
        HashMap<String, String> params = new HashMap<>();
        params.put(WebSocketChannelConstants.CHANNEL_MESSAGE_PARAM_BANNED_IP, "10.0.0.1");
        params.put(WebSocketChannelConstants.CHANNEL_MESSAGE_PARAM_BANNED_USERNAME, "participant");
        unbanMsg.setParams(params);

        // Should not throw even if user is not in ban list
        assertDoesNotThrow(() -> channel.unbanPartecipant(ownerInfo, unbanMsg));
    }

    // --- receiveMessageForServer ---

    @Test
    void receiveMessageForServerSendsEmptyResultBack() {
        channel.addPartecipantSession(ownerInfo, Collections.singleton(ownerRole), ownerSession);
        WebSocketMessage msg = new WebSocketMessage();
        msg.setParams(new HashMap<>());

        // Default processMessageOnServer returns "" — null check prevents sending
        channel.receiveMessageForServer(ownerSession, msg);
        // Default returns "" which is not null, but the check is `if (serverResponse != null)` so we expect a RESULT message
        verify(ownerSession, atLeastOnce()).sendRemote(argThat(m ->
                m.getType() == WebSocketMessageType.RESULT
        ));
    }

    // --- defineClusterMessageBroker ---

    @Test
    void defineClusterMessageBrokerUpdatesField() {
        WebSocketChannelClusterMessageBroker newBroker = mock(WebSocketChannelClusterMessageBroker.class);
        channel.defineClusterMessageBroker(newBroker);
        // Simply verifying no exception is thrown and broker can be changed
        assertDoesNotThrow(() -> channel.defineClusterMessageBroker(null));
    }
}
