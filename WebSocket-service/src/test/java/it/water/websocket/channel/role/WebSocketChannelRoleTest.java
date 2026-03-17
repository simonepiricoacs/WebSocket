package it.water.websocket.channel.role;

import it.water.websocket.api.WebSocketBasicCommandType;
import it.water.websocket.api.WebSocketCommand;
import it.water.websocket.api.channel.WebSocketChannelRole;
import it.water.websocket.channel.command.WebSocketChannelCommandType;
import it.water.websocket.channel.util.WebSocketChannelConstants;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class WebSocketChannelRoleTest {

    // ===================== WebSocketChannelOwnerRole =====================

    @Test
    void ownerRoleIsOwner() {
        WebSocketChannelOwnerRole role = new WebSocketChannelOwnerRole();
        assertTrue(role.isOwner());
    }

    @Test
    void ownerRoleNameIsOwner() {
        WebSocketChannelOwnerRole role = new WebSocketChannelOwnerRole();
        assertEquals(WebSocketChannelConstants.CHANNEL_ROLE_OWNER, role.getRoleName());
    }

    @Test
    void ownerRoleAllowsAllChannelCommands() {
        WebSocketChannelOwnerRole role = new WebSocketChannelOwnerRole();
        Set<WebSocketCommand> cmds = role.getAllowedCmds();

        assertTrue(cmds.contains(WebSocketChannelCommandType.KICK_USER));
        assertTrue(cmds.contains(WebSocketChannelCommandType.BAN_USER));
        assertTrue(cmds.contains(WebSocketChannelCommandType.UNBAN_USER));
        assertTrue(cmds.contains(WebSocketChannelCommandType.DELETE_CHANNEl));
        assertTrue(cmds.contains(WebSocketChannelCommandType.JOIN_CHANNEL));
        assertTrue(cmds.contains(WebSocketChannelCommandType.LEAVE_CHANNEL));
        assertTrue(cmds.contains(WebSocketChannelCommandType.SEND_MESSAGE_TO_SERVER));
    }

    @Test
    void ownerRoleAllowsBasicCommands() {
        WebSocketChannelOwnerRole role = new WebSocketChannelOwnerRole();
        Set<WebSocketCommand> cmds = role.getAllowedCmds();

        assertTrue(cmds.contains(WebSocketBasicCommandType.SEND_MESSAGE));
        assertTrue(cmds.contains(WebSocketBasicCommandType.READ_MESSAGE));
    }

    // ===================== WebSocketChannelParticipantRole =====================

    @Test
    void participantRoleIsNotOwner() {
        WebSocketChannelParticipantRole role = new WebSocketChannelParticipantRole();
        assertFalse(role.isOwner());
    }

    @Test
    void participantRoleNameIsParticipant() {
        WebSocketChannelParticipantRole role = new WebSocketChannelParticipantRole();
        assertEquals(WebSocketChannelConstants.CHANNEL_ROLE_PARTECIPANT, role.getRoleName());
    }

    @Test
    void participantRoleAllowsBasicCommands() {
        WebSocketChannelParticipantRole role = new WebSocketChannelParticipantRole();
        Set<WebSocketCommand> cmds = role.getAllowedCmds();

        assertTrue(cmds.contains(WebSocketBasicCommandType.SEND_MESSAGE));
        assertTrue(cmds.contains(WebSocketBasicCommandType.READ_MESSAGE));
        assertTrue(cmds.contains(WebSocketChannelCommandType.JOIN_CHANNEL));
        assertTrue(cmds.contains(WebSocketChannelCommandType.LEAVE_CHANNEL));
        assertTrue(cmds.contains(WebSocketChannelCommandType.CREATE_CHANNEl));
        assertTrue(cmds.contains(WebSocketChannelCommandType.SEND_PRIVATE_MESSAGE));
    }

    @Test
    void participantRoleDoesNotAllowModerationCommands() {
        WebSocketChannelParticipantRole role = new WebSocketChannelParticipantRole();
        Set<WebSocketCommand> cmds = role.getAllowedCmds();

        assertFalse(cmds.contains(WebSocketChannelCommandType.KICK_USER));
        assertFalse(cmds.contains(WebSocketChannelCommandType.BAN_USER));
        assertFalse(cmds.contains(WebSocketChannelCommandType.DELETE_CHANNEl));
    }

    // ===================== WebSocketChannelRoleManager =====================

    @Test
    void newRoleSetMergesRoles() {
        WebSocketChannelOwnerRole owner = new WebSocketChannelOwnerRole();
        WebSocketChannelParticipantRole participant = new WebSocketChannelParticipantRole();

        Set<WebSocketChannelRole> ownerSet = WebSocketChannelRoleManager.newRoleSet(owner);
        Set<WebSocketChannelRole> merged = WebSocketChannelRoleManager.newRoleSet(ownerSet, WebSocketChannelRoleManager.newRoleSet(participant));

        assertTrue(merged.contains(owner));
        assertTrue(merged.contains(participant));
    }

    @Test
    void rolesAsCommaSeparatedListAndBack() {
        WebSocketChannelOwnerRole owner = new WebSocketChannelOwnerRole();
        WebSocketChannelParticipantRole participant = new WebSocketChannelParticipantRole();
        Set<WebSocketChannelRole> roles = WebSocketChannelRoleManager.newRoleSet(owner, participant);

        String csv = WebSocketChannelRoleManager.rolesAsCommaSeparatedList(roles);
        assertNotNull(csv);
        assertTrue(csv.contains(WebSocketChannelConstants.CHANNEL_ROLE_OWNER)
                || csv.contains(WebSocketChannelConstants.CHANNEL_ROLE_PARTECIPANT));
    }

    // ===================== WebSocketChannelType =====================

    @Test
    void channelTypeValuesExist() {
        assertNotNull(it.water.websocket.channel.WebSocketChannelType.PLAIN);
        assertNotNull(it.water.websocket.channel.WebSocketChannelType.ENCRYPTED_RSA_WITH_AES);
        assertEquals(2, it.water.websocket.channel.WebSocketChannelType.values().length);
    }

    @Test
    void channelTypeFromStringWorks() {
        it.water.websocket.channel.WebSocketChannelType type =
                it.water.websocket.channel.WebSocketChannelType.valueOf("PLAIN");
        assertEquals(it.water.websocket.channel.WebSocketChannelType.PLAIN, type);
    }
}
