package it.water.websocket.channel.role;

import it.water.core.api.registry.ComponentRegistry;
import it.water.core.api.registry.filter.ComponentFilter;
import it.water.core.api.registry.filter.ComponentFilterBuilder;
import it.water.core.model.exceptions.WaterRuntimeException;
import it.water.websocket.api.WebSocketBasicCommandType;
import it.water.websocket.api.WebSocketCommand;
import it.water.websocket.api.channel.WebSocketChannelRole;
import it.water.websocket.channel.command.WebSocketChannelCommandType;
import it.water.websocket.channel.util.WebSocketChannelConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WebSocketChannelRoleTest {

    @Mock
    private ComponentRegistry mockRegistry;

    @Mock
    private ComponentFilterBuilder filterBuilder;

    @Mock
    private ComponentFilter filter;

    @AfterEach
    void tearDown() {
        WebSocketChannelRoleManager.setComponentRegistry(null);
    }

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
        assertTrue(cmds.contains(WebSocketChannelCommandType.DELETE_CHANNEL));
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
        assertTrue(cmds.contains(WebSocketChannelCommandType.CREATE_CHANNEL));
        assertTrue(cmds.contains(WebSocketChannelCommandType.SEND_PRIVATE_MESSAGE));
    }

    @Test
    void participantRoleDoesNotAllowModerationCommands() {
        WebSocketChannelParticipantRole role = new WebSocketChannelParticipantRole();
        Set<WebSocketCommand> cmds = role.getAllowedCmds();

        assertFalse(cmds.contains(WebSocketChannelCommandType.KICK_USER));
        assertFalse(cmds.contains(WebSocketChannelCommandType.BAN_USER));
        assertFalse(cmds.contains(WebSocketChannelCommandType.DELETE_CHANNEL));
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
    void roleManagerConstructorThrowsUnsupportedOperationException() throws Exception {
        var constructor = WebSocketChannelRoleManager.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        var invocation = assertThrows(java.lang.reflect.InvocationTargetException.class, constructor::newInstance);
        assertInstanceOf(UnsupportedOperationException.class, invocation.getCause());
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

    @Test
    void getWebSocketChannelRoleNullRegistryThrows() {
        WebSocketChannelRoleManager.setComponentRegistry(null);
        assertThrows(WaterRuntimeException.class, () ->
                WebSocketChannelRoleManager.getWebSocketChannelRole("owner"));
    }

    @Test
    void getWebSocketChannelRoleNotFoundThrows() {
        WebSocketChannelRoleManager.setComponentRegistry(mockRegistry);
        when(mockRegistry.getComponentFilterBuilder()).thenReturn(filterBuilder);
        when(filterBuilder.createFilter(anyString(), anyString())).thenReturn(filter);
        when(mockRegistry.findComponents(eq(WebSocketChannelRole.class), any())).thenReturn(Collections.emptyList());

        assertThrows(WaterRuntimeException.class, () ->
                WebSocketChannelRoleManager.getWebSocketChannelRole("nonexistent"));
    }

    @Test
    void getWebSocketChannelRoleMultipleFoundThrows() {
        WebSocketChannelOwnerRole r1 = new WebSocketChannelOwnerRole();
        WebSocketChannelOwnerRole r2 = new WebSocketChannelOwnerRole();
        WebSocketChannelRoleManager.setComponentRegistry(mockRegistry);
        when(mockRegistry.getComponentFilterBuilder()).thenReturn(filterBuilder);
        when(filterBuilder.createFilter(anyString(), anyString())).thenReturn(filter);
        when(mockRegistry.findComponents(eq(WebSocketChannelRole.class), any())).thenReturn(List.of(r1, r2));

        assertThrows(WaterRuntimeException.class, () ->
                WebSocketChannelRoleManager.getWebSocketChannelRole("owner"));
    }

    @Test
    void getWebSocketChannelRoleSingleFoundReturnsRole() {
        WebSocketChannelOwnerRole owner = new WebSocketChannelOwnerRole();
        WebSocketChannelRoleManager.setComponentRegistry(mockRegistry);
        when(mockRegistry.getComponentFilterBuilder()).thenReturn(filterBuilder);
        when(filterBuilder.createFilter(anyString(), anyString())).thenReturn(filter);
        when(mockRegistry.findComponents(eq(WebSocketChannelRole.class), any())).thenReturn(List.of(owner));

        WebSocketChannelRole role = WebSocketChannelRoleManager.getWebSocketChannelRole("owner");

        assertSame(owner, role);
    }

    @Test
    void fromCommaSeparatedListReturnsRoleSet() {
        WebSocketChannelOwnerRole owner = new WebSocketChannelOwnerRole();
        WebSocketChannelRoleManager.setComponentRegistry(mockRegistry);
        when(mockRegistry.getComponentFilterBuilder()).thenReturn(filterBuilder);
        when(filterBuilder.createFilter(anyString(), anyString())).thenReturn(filter);
        when(mockRegistry.findComponents(eq(WebSocketChannelRole.class), any())).thenReturn(List.of(owner));

        Set<WebSocketChannelRole> roles = WebSocketChannelRoleManager.fromCommaSeparatedList(WebSocketChannelConstants.CHANNEL_ROLE_OWNER);
        assertNotNull(roles);
        assertTrue(roles.contains(owner));
    }

    @Test
    void newRoleSetVarargs() {
        WebSocketChannelOwnerRole owner = new WebSocketChannelOwnerRole();
        WebSocketChannelParticipantRole participant = new WebSocketChannelParticipantRole();
        Set<WebSocketChannelRole> roles = WebSocketChannelRoleManager.newRoleSet(owner, participant);
        assertEquals(2, roles.size());
        assertTrue(roles.contains(owner));
        assertTrue(roles.contains(participant));
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
