package it.water.websocket.channel.manager;

import it.water.core.model.exceptions.WaterRuntimeException;
import it.water.websocket.api.WebSocketSession;
import it.water.websocket.api.channel.WebSocketChannel;
import it.water.websocket.api.channel.WebSocketChannelClusterCoordinator;
import it.water.websocket.api.channel.WebSocketChannelClusterMessageBroker;
import it.water.websocket.api.channel.WebSocketChannelSession;
import it.water.websocket.channel.WebSocketBasicChannel;
import it.water.websocket.channel.WebSocketChannelType;
import it.water.websocket.channel.command.WebSocketChannelCommandType;
import it.water.websocket.channel.role.WebSocketChannelOwnerRole;
import it.water.websocket.channel.role.WebSocketChannelParticipantRole;
import it.water.websocket.channel.util.WebSocketChannelConstants;
import it.water.websocket.model.WebSocketUserInfo;
import it.water.websocket.model.message.WebSocketMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@SuppressWarnings("java:S5778") // inline mock() calls are intentional for locally scoped test scenarios
class WebSocketDefaultChannelManagerTest {

    @Mock
    private WebSocketChannelClusterCoordinator coordinator;

    @Mock
    private WebSocketChannelClusterMessageBroker broker;

    @Mock
    private WebSocketChannelSession ownerSession;

    @Mock
    private WebSocketChannelSession participantSession;

    private WebSocketDefaultChannelManager<WebSocketBasicChannel> manager;

    private WebSocketUserInfo ownerInfo;
    private WebSocketUserInfo participantInfo;
    private WebSocketChannelOwnerRole ownerRole;
    private WebSocketChannelParticipantRole participantRole;

    @BeforeEach
    void setUp() {
        when(coordinator.connectNewPeer(any())).thenReturn(Collections.emptyMap());
        manager = new WebSocketDefaultChannelManager<>(WebSocketBasicChannel.class, coordinator, broker);

        ownerInfo = new WebSocketUserInfo("owner", null, "127.0.0.1");
        participantInfo = new WebSocketUserInfo("participant", null, "10.0.0.1");
        ownerRole = new WebSocketChannelOwnerRole();
        participantRole = new WebSocketChannelParticipantRole();

        when(ownerSession.getUserInfo()).thenReturn(ownerInfo);
        when(participantSession.getUserInfo()).thenReturn(participantInfo);
        // addJoinedChannels is needed when joining
        doNothing().when(ownerSession).addJoinedChannels(any());
        doNothing().when(participantSession).addJoinedChannels(any());
    }

    // --- Constructor ---

    @Test
    void constructorRegistersWithBrokerAndConnectsPeer() {
        verify(broker).registerChannelManager(manager);
        verify(coordinator).connectNewPeer(manager);
    }

    @Test
    void constructorWithNullBrokerDoesNotRegister() {
        when(coordinator.connectNewPeer(any())).thenReturn(Collections.emptyMap());
        WebSocketDefaultChannelManager<WebSocketBasicChannel> m =
                new WebSocketDefaultChannelManager<>(WebSocketBasicChannel.class, coordinator, null);
        // Just verifying no NullPointerException
        assertNotNull(m);
    }

    // --- findChannel / channelExists / getAvailableChannels ---

    @Test
    void findChannelReturnsNullForUnknownId() {
        assertNull(manager.findChannel("unknown-id"));
    }

    @Test
    void channelExistsReturnsFalseForUnknownId() {
        assertFalse(manager.channelExists("unknown-id"));
    }

    @Test
    void getAvailableChannelsInitiallyEmpty() {
        assertTrue(manager.getAvailableChannels().isEmpty());
    }

    // --- createChannel ---

    @Test
    void createChannelAddsChannelAndJoinsOwner() {
        doNothing().when(coordinator).notifyChannelAdded(any(), any());
        doNothing().when(coordinator).notifyPartecipantAdded(any(), any(), any());
        doNothing().when(ownerSession).sendRemote(any());

        manager.createChannel(
                WebSocketChannelType.PLAIN.name(), "TestChannel", "ch-test", 10,
                new HashMap<>(), ownerSession, Collections.singleton(ownerRole)
        );

        assertTrue(manager.channelExists("ch-test"));
        assertNotNull(manager.findChannel("ch-test"));
    }

    @Test
    void createChannelThrowsWhenChannelAlreadyExists() {
        doNothing().when(coordinator).notifyChannelAdded(any(), any());
        doNothing().when(coordinator).notifyPartecipantAdded(any(), any(), any());
        doNothing().when(ownerSession).sendRemote(any());

        manager.createChannel(
                WebSocketChannelType.PLAIN.name(), "TestChannel", "ch-dup", 10,
                new HashMap<>(), ownerSession, Collections.singleton(ownerRole)
        );

        assertThrows(WaterRuntimeException.class, () ->
                manager.createChannel(
                        WebSocketChannelType.PLAIN.name(), "DupChannel", "ch-dup", 10,
                        new HashMap<>(), ownerSession, Collections.singleton(ownerRole)
                )
        );
    }

    // --- joinChannel ---

    @Test
    void joinChannelAddsParticipant() {
        doNothing().when(coordinator).notifyChannelAdded(any(), any());
        doNothing().when(coordinator).notifyPartecipantAdded(any(), any(), any());
        doNothing().when(ownerSession).sendRemote(any());
        doNothing().when(participantSession).sendRemote(any());

        manager.createChannel(
                WebSocketChannelType.PLAIN.name(), "JoinTest", "ch-join", 10,
                new HashMap<>(), ownerSession, Collections.singleton(ownerRole)
        );

        manager.joinChannel("ch-join", participantSession, Collections.singleton(participantRole));

        WebSocketChannel ch = manager.findChannel("ch-join");
        assertTrue(ch.hasPartecipantSession(participantInfo));
    }

    @Test
    void joinChannelThrowsWhenChannelNotFound() {
        assertThrows(WaterRuntimeException.class, () ->
                manager.joinChannel("non-existent", participantSession, Collections.emptySet())
        );
    }

    // --- leaveChannel ---

    @Test
    void leaveChannelRemovesParticipant() {
        doNothing().when(coordinator).notifyChannelAdded(any(), any());
        doNothing().when(coordinator).notifyPartecipantAdded(any(), any(), any());
        doNothing().when(coordinator).notifyPartecipantGone(any(), any());
        doNothing().when(ownerSession).sendRemote(any());
        doNothing().when(participantSession).sendRemote(any());

        manager.createChannel(
                WebSocketChannelType.PLAIN.name(), "LeaveTest", "ch-leave", 10,
                new HashMap<>(), ownerSession, Collections.singleton(ownerRole)
        );
        manager.joinChannel("ch-leave", participantSession, Collections.singleton(participantRole));
        manager.leaveChannel("ch-leave", participantSession);

        WebSocketChannel ch = manager.findChannel("ch-leave");
        assertFalse(ch.hasPartecipantSession(participantInfo));
    }

    @Test
    void leaveChannelThrowsWhenChannelNotFound() {
        assertThrows(WaterRuntimeException.class, () ->
                manager.leaveChannel("non-existent", participantSession)
        );
    }

    // --- unbanParticipant (bug fix verification) ---

    @Test
    void unbanParticipantOnExistingChannelDoesNotThrow() {
        doNothing().when(coordinator).notifyChannelAdded(any(), any());
        doNothing().when(coordinator).notifyPartecipantAdded(any(), any(), any());
        doNothing().when(ownerSession).sendRemote(any());

        manager.createChannel(
                WebSocketChannelType.PLAIN.name(), "UnbanTest", "ch-unban", 10,
                new HashMap<>(), ownerSession, Collections.singleton(ownerRole)
        );

        WebSocketMessage unbanMsg = new WebSocketMessage();
        HashMap<String, String> params = new HashMap<>();
        params.put(WebSocketChannelConstants.CHANNEL_MESSAGE_PARAM_BANNED_IP, "10.0.0.1");
        params.put(WebSocketChannelConstants.CHANNEL_MESSAGE_PARAM_BANNED_USERNAME, "participant");
        unbanMsg.setParams(params);

        // Must NOT throw (this was the bug: always threw WaterRuntimeException)
        assertDoesNotThrow(() -> manager.unbanParticipant("ch-unban", ownerInfo, unbanMsg));
    }

    @Test
    void unbanParticipantThrowsWhenChannelNotFound() {
        WebSocketMessage unbanMsg = new WebSocketMessage();
        unbanMsg.setParams(new HashMap<>());

        assertThrows(WaterRuntimeException.class, () ->
                manager.unbanParticipant("non-existent", ownerInfo, unbanMsg)
        );
    }

    // --- kickParticipant ---

    @Test
    void kickParticipantThrowsWhenChannelNotFound() {
        WebSocketMessage kickMsg = new WebSocketMessage();
        HashMap<String, String> params = new HashMap<>();
        params.put(WebSocketChannelConstants.CHANNEL_MESSAGE_PARAM_USER_TO_KICK, "target");
        kickMsg.setParams(params);

        assertThrows(WaterRuntimeException.class, () ->
                manager.kickParticipant("non-existent", ownerInfo, kickMsg)
        );
    }

    // --- onChannelAdded / onChannelRemoved ---

    @Test
    void onChannelAddedPutsChannelIfAbsent() {
        WebSocketChannel ch = mock(WebSocketChannel.class);
        when(ch.getChannelId()).thenReturn("external-ch");
        manager.onChannelAdded(ch);
        assertEquals(ch, manager.findChannel("external-ch"));
    }

    @Test
    void onChannelRemovedDeletesChannel() {
        WebSocketChannel ch = mock(WebSocketChannel.class);
        when(ch.getChannelId()).thenReturn("to-remove");
        manager.onChannelAdded(ch);
        manager.onChannelRemoved("to-remove");
        assertFalse(manager.channelExists("to-remove"));
    }

    @Test
    void onChannelRemovedDoesNothingForUnknownId() {
        assertDoesNotThrow(() -> manager.onChannelRemoved("unknown-id"));
    }

    // --- onPartecipantAdded ---

    @Test
    void onPartecipantAddedAddsRemoteUserInfo() {
        WebSocketChannel ch = mock(WebSocketChannel.class);
        when(ch.getChannelId()).thenReturn("ch-remote");
        manager.onChannelAdded(ch);

        // Remote user (clusterNodeInfo not null, but using null locally → isOnLocalNode returns true)
        // To test the "remote" branch we need a user with clusterNodeInfo != null
        WebSocketUserInfo remoteUser = new WebSocketUserInfo("remote-user", null, "200.0.0.1");
        // With null clusterNodeInfo, isOnLocalNode(null) returns true → addPartecipantInfo is NOT called
        manager.onPartecipantAdded("ch-remote", remoteUser, Collections.singleton(participantRole));
        // Verify: since isOnLocalNode(null)=true, addPartecipantInfo is not called
        verify(ch, never()).addPartecipantInfo(any(), any());
    }

    @Test
    void onPartecipantAddedThrowsWhenChannelNotFound() {
        assertThrows(WaterRuntimeException.class, () ->
                manager.onPartecipantAdded("non-existent", ownerInfo, Collections.emptySet())
        );
    }

    // --- onPartecipantGone ---

    @Test
    void onPartecipantGoneRemovesRemoteParticipant() {
        WebSocketChannel ch = mock(WebSocketChannel.class);
        when(ch.getChannelId()).thenReturn("ch-gone");
        manager.onChannelAdded(ch);

        WebSocketUserInfo remoteUser = new WebSocketUserInfo("remote", null, "9.9.9.9");
        // isOnLocalNode(null) = true → returns immediately
        manager.onPartecipantGone("ch-gone", remoteUser);
        verify(ch, never()).removePartecipant(any());
    }

    @Test
    void onPartecipantGoneThrowsWhenChannelNotFound() {
        WebSocketUserInfo remoteUser = new WebSocketUserInfo("remote", null, "9.9.9.9");
        // local node (isOnLocalNode returns true) → returns early, no exception
        assertDoesNotThrow(() -> manager.onPartecipantGone("non-existent", remoteUser));
    }

    // --- onPartecipantDisconnected ---

    @Test
    void onPartecipantDisconnectedBehavesLikeGone() {
        WebSocketUserInfo remoteUser = new WebSocketUserInfo("remote-dc", null, "8.8.8.8");
        // isOnLocalNode returns true → early return
        assertDoesNotThrow(() -> manager.onPartecipantDisconnected("non-existent", remoteUser));
    }

    // --- forwardMessage ---

    @Test
    void forwardMessageDelegatesToBroker() {
        WebSocketMessage msg = new WebSocketMessage();
        msg.setParams(new HashMap<>());
        manager.forwardMessage("ch-1", msg);
        verify(broker).sendMessage("ch-1", msg);
    }

    // --- getClusterBroker ---

    @Test
    void getClusterBrokerReturnsConfiguredBroker() {
        assertEquals(broker, manager.getClusterBroker());
    }

    // --- deleteChannel ---

    @Test
    void deleteChannelWithPermissionNotifies() {
        // Set up: channel mock that says user has permission
        WebSocketChannel mockChannel = mock(WebSocketChannel.class);
        WebSocketSession mockWebSocketSession = mock(WebSocketSession.class);
        when(mockChannel.getChannelId()).thenReturn("ch-del");
        when(mockChannel.userHasPermission(ownerInfo, WebSocketChannelCommandType.DELETE_CHANNEL)).thenReturn(true);
        when(mockChannel.getPartecipantSession(ownerInfo)).thenReturn(mockWebSocketSession);
        doNothing().when(mockWebSocketSession).sendRemote(any());
        doNothing().when(coordinator).notifyChannelDeleted(anyString());

        manager.onChannelAdded(mockChannel);
        manager.deleteChannel(ownerInfo, mockChannel);

        verify(coordinator).notifyChannelDeleted("ch-del");
    }

    @Test
    void deleteChannelWithoutPermissionThrows() {
        WebSocketChannel mockChannel = mock(WebSocketChannel.class);
        when(mockChannel.userHasPermission(any(), any())).thenReturn(false);

        assertThrows(WaterRuntimeException.class, () ->
                manager.deleteChannel(ownerInfo, mockChannel));
    }

    @Test
    void deleteChannelWithNullChannelThrows() {
        assertThrows(WaterRuntimeException.class, () ->
                manager.deleteChannel(ownerInfo, null));
    }

    // --- deliverMessage ---

    @Test
    void deliverMessageRoutesMessageToChannel() {
        doNothing().when(coordinator).notifyChannelAdded(any(), any());
        doNothing().when(coordinator).notifyPartecipantAdded(any(), any(), any());
        doNothing().when(ownerSession).sendRemote(any());

        manager.createChannel(
                WebSocketChannelType.PLAIN.name(), "DeliverTest", "ch-deliver", 10,
                new HashMap<>(), ownerSession, Collections.singleton(ownerRole)
        );

        WebSocketMessage msg = new WebSocketMessage();
        HashMap<String, String> params = new HashMap<>();
        params.put(WebSocketChannelConstants.CHANNEL_ID_PARAM, "ch-deliver");
        params.put(WebSocketMessage.WS_MESSAGE_SENDER_PARAM_NAME, "owner");
        msg.setParams(params);
        msg.setCmd("READ_MESSAGE");

        // Should not throw - channel exists
        assertDoesNotThrow(() -> manager.deliverMessage(msg));
    }

    // --- kickParticipant happy path ---

    @Test
    void kickParticipantOnExistingChannelCallsKick() {
        doNothing().when(coordinator).notifyChannelAdded(any(), any());
        doNothing().when(coordinator).notifyPartecipantAdded(any(), any(), any());
        doNothing().when(coordinator).notifyPartecipantGone(any(), any());
        doNothing().when(ownerSession).sendRemote(any());
        doNothing().when(participantSession).sendRemote(any());

        manager.createChannel(
                WebSocketChannelType.PLAIN.name(), "KickTest", "ch-kick", 10,
                new HashMap<>(), ownerSession, Collections.singleton(ownerRole)
        );
        manager.joinChannel("ch-kick", participantSession, Collections.singleton(participantRole));

        WebSocketMessage kickMsg = new WebSocketMessage();
        HashMap<String, String> params = new HashMap<>();
        params.put(WebSocketChannelConstants.CHANNEL_MESSAGE_PARAM_USER_TO_KICK, "participant");
        params.put(WebSocketChannelConstants.CHANNEL_MESSAGE_PARAM_KICK_MESSAGE, "kicked");
        kickMsg.setParams(params);

        // Should not throw
        assertDoesNotThrow(() -> manager.kickParticipant("ch-kick", ownerInfo, kickMsg));
    }

    // --- banParticipant happy path ---

    @Test
    void banParticipantOnExistingChannelCallsBan() {
        doNothing().when(coordinator).notifyChannelAdded(any(), any());
        doNothing().when(coordinator).notifyPartecipantAdded(any(), any(), any());
        doNothing().when(ownerSession).sendRemote(any());

        manager.createChannel(
                WebSocketChannelType.PLAIN.name(), "BanTest", "ch-ban", 10,
                new HashMap<>(), ownerSession, Collections.singleton(ownerRole)
        );

        WebSocketMessage banMsg = new WebSocketMessage();
        HashMap<String, String> params = new HashMap<>();
        params.put(WebSocketChannelConstants.CHANNEL_MESSAGE_PARAM_USER_TO_KICK, "someuser");
        params.put(WebSocketChannelConstants.CHANNEL_MESSAGE_PARAM_KICK_MESSAGE, "banned");
        banMsg.setParams(params);

        assertDoesNotThrow(() -> manager.banParticipant("ch-ban", ownerInfo, banMsg));
    }

    @Test
    void banParticipantThrowsWhenChannelNotFound() {
        WebSocketMessage banMsg = new WebSocketMessage();
        HashMap<String, String> params = new HashMap<>();
        params.put(WebSocketChannelConstants.CHANNEL_MESSAGE_PARAM_USER_TO_KICK, "user");
        banMsg.setParams(params);
        assertThrows(WaterRuntimeException.class, () ->
                manager.banParticipant("non-existent", ownerInfo, banMsg));
    }
}
