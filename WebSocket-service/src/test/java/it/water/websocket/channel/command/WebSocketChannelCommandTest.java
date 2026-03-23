package it.water.websocket.channel.command;

import it.water.core.api.registry.ComponentRegistry;
import it.water.core.api.registry.filter.ComponentFilter;
import it.water.core.api.registry.filter.ComponentFilterBuilder;
import it.water.core.model.exceptions.WaterRuntimeException;
import it.water.websocket.api.channel.WebSocketChannel;
import it.water.websocket.api.channel.WebSocketChannelManager;
import it.water.websocket.api.channel.WebSocketChannelRole;
import it.water.websocket.api.channel.WebSocketChannelSession;
import it.water.websocket.channel.role.WebSocketChannelRoleManager;
import it.water.websocket.channel.util.WebSocketChannelConstants;
import it.water.websocket.model.WebSocketUserInfo;
import it.water.websocket.model.message.WebSocketMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WebSocketChannelCommandTest {

    @Mock
    private ComponentRegistry mockRegistry;

    @Mock
    private ComponentFilterBuilder filterBuilder;

    @Mock
    private ComponentFilter filter;

    @Mock
    private WebSocketChannelSession userSession;

    @Mock
    private WebSocketChannelManager channelManager;

    @Mock
    private WebSocketChannel channel;

    @Mock
    private WebSocketChannelRole ownerRole;

    @Mock
    private WebSocketChannelRole participantRole;

    private WebSocketUserInfo userInfo;

    @BeforeEach
    void setUp() {
        WebSocketChannelRoleManager.setComponentRegistry(mockRegistry);
        when(mockRegistry.getComponentFilterBuilder()).thenReturn(filterBuilder);
        when(filterBuilder.createFilter(anyString(), anyString())).thenReturn(filter);
        when(mockRegistry.findComponents(eq(WebSocketChannelRole.class), any())).thenReturn(List.of(ownerRole));
        when(channelManager.findChannel(anyString())).thenReturn(channel);
        userInfo = new WebSocketUserInfo("testuser", null, "127.0.0.1");
        when(userSession.getUserInfo()).thenReturn(userInfo);
        when(channel.getPartecipantsInfo()).thenReturn(Collections.singleton(userInfo));
    }

    @AfterEach
    void tearDown() {
        WebSocketChannelRoleManager.setComponentRegistry(null);
    }

    // --- WebSocketChannelAbstractCommand helpers ---

    @Test
    void findChannelOrDieThrowsWhenNotFound() {
        when(channelManager.findChannel("unknown")).thenReturn(null);
        WebSocketChannelLeaveCommand cmd = new WebSocketChannelLeaveCommand();
        WebSocketMessage msg = createMessageWithParams(
                WebSocketChannelConstants.CHANNEL_ID_PARAM, "unknown");
        assertThrows(WaterRuntimeException.class, () ->
                cmd.execute(userSession, msg, "unknown", channelManager));
    }

    @Test
    void checkRequiredParametersThrowsWhenMissing() {
        WebSocketChannelSendMessageCommand cmd = new WebSocketChannelSendMessageCommand();
        WebSocketMessage msgMissingParam = new WebSocketMessage();
        msgMissingParam.setParams(new HashMap<>());
        assertThrows(WaterRuntimeException.class, () ->
                cmd.execute(userSession, msgMissingParam, "ch-1", channelManager));
    }

    // --- Create command ---

    @Test
    void createCommandExecuteCreatesChannel() {
        WebSocketChannelCreateCommand cmd = new WebSocketChannelCreateCommand();
        WebSocketMessage msg = new WebSocketMessage();
        HashMap<String, String> params = new HashMap<>();
        params.put(WebSocketChannelConstants.CHANNEL_ID_PARAM, "new-ch");
        params.put(WebSocketChannelConstants.CHANNEL_NAME_PARAM, "My Channel");
        params.put(WebSocketChannelConstants.CHANNEL_MAX_PARTECIPANTS_PARAM, "10");
        params.put(WebSocketChannelConstants.CHANNEL_TYPE_PARAM, "PLAIN");
        msg.setParams(params);

        cmd.execute(userSession, msg, null, channelManager);

        verify(channelManager).createChannel(eq("PLAIN"), eq("My Channel"), eq("new-ch"),
                eq(10), any(), eq(userSession), any());
    }

    @Test
    void createCommandMissingParamThrows() {
        WebSocketChannelCreateCommand cmd = new WebSocketChannelCreateCommand();
        WebSocketMessage msg = new WebSocketMessage();
        msg.setParams(new HashMap<>());
        assertThrows(WaterRuntimeException.class, () ->
                cmd.execute(userSession, msg, null, channelManager));
    }

    @Test
    void createCommandInvalidMaxPartecipants() {
        WebSocketChannelCreateCommand cmd = new WebSocketChannelCreateCommand();
        WebSocketMessage msg = new WebSocketMessage();
        HashMap<String, String> params = new HashMap<>();
        params.put(WebSocketChannelConstants.CHANNEL_ID_PARAM, "new-ch");
        params.put(WebSocketChannelConstants.CHANNEL_NAME_PARAM, "My Channel");
        params.put(WebSocketChannelConstants.CHANNEL_MAX_PARTECIPANTS_PARAM, "notanumber");
        params.put(WebSocketChannelConstants.CHANNEL_TYPE_PARAM, "PLAIN");
        msg.setParams(params);

        // Should not throw — invalid max partecipants defaults to -1
        cmd.execute(userSession, msg, null, channelManager);
        verify(channelManager).createChannel(any(), any(), any(), eq(-1), any(), any(), any());
    }

    // --- Join command ---

    @Test
    void joinCommandExecuteJoinsChannel() {
        WebSocketChannelJoinCommand cmd = new WebSocketChannelJoinCommand();
        WebSocketMessage msg = createMessageWithParams(
                WebSocketChannelConstants.CHANNEL_ID_PARAM, "ch-1");
        when(mockRegistry.findComponents(eq(WebSocketChannelRole.class), any())).thenReturn(List.of(participantRole));
        when(channel.getChannelId()).thenReturn("ch-1");

        cmd.execute(userSession, msg, "ch-1", channelManager);

        verify(channelManager).joinChannel(eq("ch-1"), eq(userSession), any());
    }

    // --- Leave command ---

    @Test
    void leaveCommandExecuteLeavesChannel() {
        WebSocketChannelLeaveCommand cmd = new WebSocketChannelLeaveCommand();
        WebSocketMessage msg = createMessageWithParams(
                WebSocketChannelConstants.CHANNEL_ID_PARAM, "ch-1");

        cmd.execute(userSession, msg, "ch-1", channelManager);

        verify(channelManager).leaveChannel("ch-1", userSession);
    }

    // --- Delete command ---

    @Test
    void deleteCommandExecuteDeletesChannel() {
        WebSocketChannelDeleteCommand cmd = new WebSocketChannelDeleteCommand();
        WebSocketMessage msg = new WebSocketMessage();
        msg.setParams(new HashMap<>());

        cmd.execute(userSession, msg, "ch-1", channelManager);

        verify(channelManager).deleteChannel(userInfo, channel);
    }

    // --- Kick command (remote) ---

    @Test
    void kickCommandExecutesKickUser() {
        WebSocketChannelKickCommand cmd = new WebSocketChannelKickCommand();
        WebSocketMessage msg = new WebSocketMessage();
        HashMap<String, String> params = new HashMap<>();
        params.put(WebSocketChannelConstants.CHANNEL_MESSAGE_PARAM_USER_TO_KICK, "victim");
        params.put(WebSocketChannelConstants.CHANNEL_MESSAGE_PARAM_KICK_MESSAGE, "bye");
        params.put(WebSocketMessage.WS_MESSAGE_SENDER_PARAM_NAME, "testuser");
        msg.setParams(params);

        cmd.execute(msg, "ch-1", channelManager);

        verify(channelManager).kickParticipant("ch-1", userInfo, msg);
    }

    @Test
    void kickCommandMissingParamThrows() {
        WebSocketChannelKickCommand cmd = new WebSocketChannelKickCommand();
        WebSocketMessage msg = new WebSocketMessage();
        msg.setParams(new HashMap<>());
        assertThrows(WaterRuntimeException.class, () ->
                cmd.execute(msg, "ch-1", channelManager));
    }

    // --- Ban command (remote) ---

    @Test
    void banCommandExecutesBanUser() {
        WebSocketChannelBanCommand cmd = new WebSocketChannelBanCommand();
        WebSocketMessage msg = new WebSocketMessage();
        HashMap<String, String> params = new HashMap<>();
        params.put(WebSocketChannelConstants.CHANNEL_MESSAGE_PARAM_USER_TO_KICK, "victim");
        params.put(WebSocketChannelConstants.CHANNEL_MESSAGE_PARAM_KICK_MESSAGE, "banned");
        params.put(WebSocketMessage.WS_MESSAGE_SENDER_PARAM_NAME, "testuser");
        msg.setParams(params);

        cmd.execute(msg, "ch-1", channelManager);

        verify(channelManager).banParticipant("ch-1", userInfo, msg);
    }

    // --- Unban command (remote) ---

    @Test
    void unbanCommandExecutesUnbanUser() {
        WebSocketChannelUnbanCommand cmd = new WebSocketChannelUnbanCommand();
        WebSocketMessage msg = new WebSocketMessage();
        HashMap<String, String> params = new HashMap<>();
        params.put(WebSocketChannelConstants.CHANNEL_MESSAGE_PARAM_BANNED_IP, "10.0.0.1");
        params.put(WebSocketChannelConstants.CHANNEL_MESSAGE_PARAM_BANNED_USERNAME, "victim");
        params.put(WebSocketMessage.WS_MESSAGE_SENDER_PARAM_NAME, "testuser");
        msg.setParams(params);

        cmd.execute(msg, "ch-1", channelManager);

        verify(channelManager).unbanParticipant("ch-1", userInfo, msg);
    }

    // --- SendMessage command ---

    @Test
    void sendMessageCommandForwards() {
        WebSocketChannelSendMessageCommand cmd = new WebSocketChannelSendMessageCommand();
        WebSocketMessage msg = createMessageWithParams(
                WebSocketChannelConstants.CHANNEL_ID_PARAM, "ch-1");

        cmd.execute(userSession, msg, "ch-1", channelManager);

        verify(channel).exchangeMessage(userSession, msg);
    }

    // --- ReadMessage command (remote) ---

    @Test
    void readMessageCommandForwards() {
        WebSocketChannelReadMessageCommand cmd = new WebSocketChannelReadMessageCommand();
        WebSocketMessage msg = createMessageWithParams(
                WebSocketChannelConstants.CHANNEL_ID_PARAM, "ch-1");

        cmd.execute(msg, "ch-1", channelManager);

        verify(channelManager).deliverMessage(msg);
    }

    @Test
    void readMessageCommandMissingParamThrows() {
        WebSocketChannelReadMessageCommand cmd = new WebSocketChannelReadMessageCommand();
        WebSocketMessage msg = new WebSocketMessage();
        msg.setParams(new HashMap<>());
        assertThrows(WaterRuntimeException.class, () ->
                cmd.execute(msg, "ch-1", channelManager));
    }

    // --- SendPrivateMessage command ---

    @Test
    void sendPrivateMessageCommandForwards() {
        WebSocketChannelSendPrivateMessageCommand cmd = new WebSocketChannelSendPrivateMessageCommand();
        WebSocketMessage msg = createMessageWithParams(
                WebSocketChannelConstants.CHANNEL_MESSAGE_PARAM_PVT_MESSAGE_RECIPIENT, "recipient");

        cmd.execute(userSession, msg, "ch-1", channelManager);

        verify(channel).exchangeMessage(userSession, msg);
    }

    // Helper

    private WebSocketMessage createMessageWithParams(String key, String value) {
        WebSocketMessage msg = new WebSocketMessage();
        HashMap<String, String> params = new HashMap<>();
        params.put(key, value);
        msg.setParams(params);
        return msg;
    }
}
