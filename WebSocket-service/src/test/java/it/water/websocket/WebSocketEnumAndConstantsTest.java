package it.water.websocket;

import it.water.websocket.actions.WebSocketAction;
import it.water.websocket.api.WebSocketBasicCommandType;
import it.water.websocket.channel.WebSocketChannelType;
import it.water.websocket.channel.command.WebSocketChannelCommandType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebSocketEnumAndConstantsTest {

    @Test
    void webSocketActionCreateChannel() {
        assertEquals("CREATE_CHANNEL", WebSocketAction.CREATE_CHANNEL.getActionName());
        assertEquals(1L, WebSocketAction.CREATE_CHANNEL.getActionId());
        assertEquals(WebSocketAction.class.getName(), WebSocketAction.CREATE_CHANNEL.getActionType());
    }

    @Test
    void webSocketActionValues() {
        assertEquals(1, WebSocketAction.values().length);
    }

    @Test
    void webSocketBasicCommandTypeRead() {
        assertEquals(WebSocketBasicCommandType.READ_MESSAGE_COMMAND, WebSocketBasicCommandType.READ_MESSAGE.getCommandName());
    }

    @Test
    void webSocketBasicCommandTypeSend() {
        assertEquals(WebSocketBasicCommandType.SEND_MESSAGE_COMMAND, WebSocketBasicCommandType.SEND_MESSAGE.getCommandName());
    }

    @Test
    void webSocketBasicCommandTypeValues() {
        assertEquals(2, WebSocketBasicCommandType.values().length);
    }

    @Test
    void webSocketChannelCommandTypeAllCmdsSize() {
        // 10 channel commands + 2 basic commands = 12
        assertEquals(12, WebSocketChannelCommandType.ALL_CMDS.size());
    }

    @Test
    void webSocketChannelCommandTypeAllCmdsContainsExpected() {
        assertTrue(WebSocketChannelCommandType.ALL_CMDS.contains(WebSocketChannelCommandType.FOLLOW));
        assertTrue(WebSocketChannelCommandType.ALL_CMDS.contains(WebSocketChannelCommandType.CREATE_CHANNEL));
        assertTrue(WebSocketChannelCommandType.ALL_CMDS.contains(WebSocketChannelCommandType.DELETE_CHANNEL));
        assertTrue(WebSocketChannelCommandType.ALL_CMDS.contains(WebSocketChannelCommandType.JOIN_CHANNEL));
        assertTrue(WebSocketChannelCommandType.ALL_CMDS.contains(WebSocketChannelCommandType.LEAVE_CHANNEL));
        assertTrue(WebSocketChannelCommandType.ALL_CMDS.contains(WebSocketChannelCommandType.KICK_USER));
        assertTrue(WebSocketChannelCommandType.ALL_CMDS.contains(WebSocketChannelCommandType.BAN_USER));
        assertTrue(WebSocketChannelCommandType.ALL_CMDS.contains(WebSocketChannelCommandType.UNBAN_USER));
        assertTrue(WebSocketChannelCommandType.ALL_CMDS.contains(WebSocketChannelCommandType.SEND_PRIVATE_MESSAGE));
        assertTrue(WebSocketChannelCommandType.ALL_CMDS.contains(WebSocketChannelCommandType.SEND_MESSAGE_TO_SERVER));
        assertTrue(WebSocketChannelCommandType.ALL_CMDS.contains(WebSocketBasicCommandType.SEND_MESSAGE));
        assertTrue(WebSocketChannelCommandType.ALL_CMDS.contains(WebSocketBasicCommandType.READ_MESSAGE));
    }

    @Test
    void webSocketChannelCommandTypeGetCommandName() {
        assertEquals(WebSocketChannelCommandType.FOLLOW_COMMAND, WebSocketChannelCommandType.FOLLOW.getCommandName());
        assertEquals(WebSocketChannelCommandType.CREATE_CHANNEL_COMMAND, WebSocketChannelCommandType.CREATE_CHANNEL.getCommandName());
        assertEquals(WebSocketChannelCommandType.DELETE_CHANNEL_COMMAND, WebSocketChannelCommandType.DELETE_CHANNEL.getCommandName());
        assertEquals(WebSocketChannelCommandType.JOIN_CHANNEL_COMMAND, WebSocketChannelCommandType.JOIN_CHANNEL.getCommandName());
        assertEquals(WebSocketChannelCommandType.LEAVE_CHANNEL_COMMAND, WebSocketChannelCommandType.LEAVE_CHANNEL.getCommandName());
        assertEquals(WebSocketChannelCommandType.BAN_USER_COMMAND, WebSocketChannelCommandType.BAN_USER.getCommandName());
        assertEquals(WebSocketChannelCommandType.UNBAN_USER_COMMAND, WebSocketChannelCommandType.UNBAN_USER.getCommandName());
        assertEquals(WebSocketChannelCommandType.KICK_USER_COMMAND, WebSocketChannelCommandType.KICK_USER.getCommandName());
        assertEquals(WebSocketChannelCommandType.SEND_PRIVATE_MESSAGE_COMMAND, WebSocketChannelCommandType.SEND_PRIVATE_MESSAGE.getCommandName());
        assertEquals(WebSocketChannelCommandType.PROCESS_ON_SERVER_COMMAND, WebSocketChannelCommandType.SEND_MESSAGE_TO_SERVER.getCommandName());
    }

    @Test
    void webSocketChannelTypeEnumValues() {
        WebSocketChannelType[] values = WebSocketChannelType.values();
        assertEquals(2, values.length);
        assertEquals("PLAIN", WebSocketChannelType.PLAIN.getTypeStr());
        assertEquals("ENCRYPTED_RSA_WITH_AES", WebSocketChannelType.ENCRYPTED_RSA_WITH_AES.getTypeStr());
    }
}
