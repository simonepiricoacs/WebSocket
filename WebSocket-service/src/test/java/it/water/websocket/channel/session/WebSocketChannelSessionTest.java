package it.water.websocket.channel.session;

import it.water.core.api.registry.ComponentRegistry;
import it.water.core.api.registry.filter.ComponentFilter;
import it.water.core.api.registry.filter.ComponentFilterBuilder;
import it.water.websocket.api.channel.WebSocketChannel;
import it.water.websocket.api.channel.WebSocketChannelCommand;
import it.water.websocket.api.channel.WebSocketChannelManager;
import it.water.websocket.api.channel.WebSocketChannelRole;
import it.water.websocket.channel.role.WebSocketChannelRoleManager;
import it.water.websocket.compression.WebSocketCompression;
import it.water.websocket.encryption.WebSocketEncryption;
import it.water.websocket.model.WebSocketUserInfo;
import it.water.websocket.model.message.WebSocketMessage;
import it.water.websocket.model.message.WebSocketMessageType;
import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.UpgradeRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WebSocketChannelSessionTest {

    @Mock
    private Session mockSession;

    @Mock
    private RemoteEndpoint mockRemote;

    @Mock
    private UpgradeRequest mockUpgradeRequest;

    @Mock
    private ComponentRegistry mockRegistry;

    @Mock
    private ComponentFilterBuilder filterBuilder;

    @Mock
    private ComponentFilter filter;

    @Mock
    private WebSocketChannelManager channelManager;

    @Mock
    private WebSocketChannelCommand mockCommand;

    @Mock
    private WebSocketChannelRole mockRole;

    @Mock
    private WebSocketEncryption mockEncryption;

    @Mock
    private WebSocketCompression mockCompression;

    private WebSocketChannelBasicSession channelSession;
    private WebSocketUserInfo userInfo;

    @BeforeEach
    void setUp() throws Exception {
        when(mockSession.getRemote()).thenReturn(mockRemote);
        when(mockRemote.getInetSocketAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 12345));
        when(mockSession.getUpgradeRequest()).thenReturn(mockUpgradeRequest);
        when(mockUpgradeRequest.getCookies()).thenReturn(Collections.emptyList());
        when(mockUpgradeRequest.getHeader(anyString())).thenReturn(null);
        doNothing().when(mockRemote).sendString(anyString(), any());

        WebSocketChannelRoleManager.setComponentRegistry(mockRegistry);
        when(mockRegistry.getComponentFilterBuilder()).thenReturn(filterBuilder);
        when(filterBuilder.createFilter(anyString(), anyString())).thenReturn(filter);
        when(mockRegistry.findComponents(eq(WebSocketChannelRole.class), any())).thenReturn(List.of(mockRole));

        // auth not required so initialize() won't close the session
        channelSession = new WebSocketChannelBasicSession(mockSession, false, channelManager, mockRegistry);
        // Manually set user info via authenticateAnonymous (which needs remote address)
        channelSession.authenticateAnonymous();
        userInfo = channelSession.getUserInfo();
    }

    @AfterEach
    void tearDown() {
        WebSocketChannelRoleManager.setComponentRegistry(null);
    }

    // --- Session params ---

    @Test
    void addAndGetSessionParam() {
        channelSession.addSessionParam("key", "value");
        assertEquals("value", channelSession.getSessionParam("key"));
    }

    @Test
    void removeSessionParam() {
        channelSession.addSessionParam("key", "value");
        channelSession.removeSessionParam("key");
        assertNull(channelSession.getSessionParam("key"));
    }

    // --- Joined channels ---

    @Test
    void addAndGetJoinedChannels() {
        WebSocketChannel ch = mock(WebSocketChannel.class);
        channelSession.addJoinedChannels(ch);
        assertTrue(channelSession.getJoinedChannels().contains(ch));
    }

    @Test
    void removeJoinedChannels() {
        WebSocketChannel ch = mock(WebSocketChannel.class);
        channelSession.addJoinedChannels(ch);
        channelSession.removeJoinedChannels(ch);
        assertFalse(channelSession.getJoinedChannels().contains(ch));
    }

    @Test
    void emptyJoinedChannels() {
        WebSocketChannel ch1 = mock(WebSocketChannel.class);
        WebSocketChannel ch2 = mock(WebSocketChannel.class);
        channelSession.addJoinedChannels(ch1);
        channelSession.addJoinedChannels(ch2);
        channelSession.emptyJoinedChannels(ch1);
        assertTrue(channelSession.getJoinedChannels().isEmpty());
    }

    @Test
    void getJoinedChannelsReturnsUnmodifiable() {
        Set<WebSocketChannel> channels = channelSession.getJoinedChannels();
        assertNotNull(channels);
        assertThrows(UnsupportedOperationException.class, () ->
                channels.add(mock(WebSocketChannel.class)));
    }

    // --- initialize ---

    @Test
    void initializeWhenAuthNotRequired() {
        // auth not required -> goes to onConnect -> sends CONNECTION_OK
        channelSession.initialize();
        verify(mockRemote, atLeastOnce()).sendString(anyString(), any());
    }

    @Test
    void initializeWhenAuthRequiredAndNotAuthenticated() {
        // Create a session with auth required but not authenticated
        WebSocketChannelBasicSession authRequired = new WebSocketChannelBasicSession(
                mockSession, true, channelManager, mockRegistry);
        // Not authenticated (no login) -> initialize() should call close()
        authRequired.initialize();
        // close() -> dispose() -> session.close() is called
        verify(mockSession, atLeastOnce()).close();
    }

    // --- processMessage ---

    @Test
    void processMessageInvalidJsonSendsError() {
        byte[] invalid = "NOT_JSON".getBytes();
        channelSession.processMessage(invalid);
        // Should send ERROR message
        verify(mockRemote, atLeastOnce()).sendString(anyString(), any());
    }

    @Test
    void processMessageValidJsonExecutesCommand() {
        // Set up the command factory to return mockCommand
        when(mockRegistry.findComponents(eq(WebSocketChannelCommand.class), any()))
                .thenReturn(List.of(mockCommand));

        WebSocketMessage msg = WebSocketMessage.createMessage("SEND_MESSAGE", "test".getBytes(), WebSocketMessageType.OK);
        HashMap<String, String> params = new HashMap<>();
        params.put("channelId", "ch-1");
        msg.setParams(params);
        String json = msg.toJson();

        channelSession.processMessage(json.getBytes());

        verify(mockCommand).execute(eq(channelSession), any(), any(), eq(channelManager));
    }

    // --- dispose ---

    @Test
    void disposeCallsLeaveChannelForJoinedChannels() {
        WebSocketChannel mockCh = mock(WebSocketChannel.class);
        when(mockCh.getChannelId()).thenReturn("ch-1");
        channelSession.addJoinedChannels(mockCh);

        channelSession.dispose();

        verify(channelManager).leaveChannel("ch-1", channelSession);
    }

    // --- equals / hashCode ---

    @Test
    void equalsWithSameSessionAndUserInfo() {
        WebSocketChannelBasicSession other = new WebSocketChannelBasicSession(
                mockSession, false, channelManager, mockRegistry);
        other.authenticateAnonymous();
        // Different userInfo (different UUID) so they're not equal
        assertNotEquals(channelSession, other);
    }

    @Test
    void equalsWithItself() {
        assertEquals(channelSession, channelSession);
    }

    @Test
    void equalsNullReturnsFalse() {
        assertNotEquals(null, channelSession);
    }

    @Test
    void hashCodeIsDetermined() {
        int h1 = channelSession.hashCode();
        int h2 = channelSession.hashCode();
        assertEquals(h1, h2);
    }

    // --- alternate constructors ---

    @Test
    void constructorWithEncryptionPolicy() {
        doNothing().when(mockEncryption).init(any());
        WebSocketChannelBasicSession session = new WebSocketChannelBasicSession(
                mockSession, false, mockEncryption, channelManager, mockRegistry);
        assertNotNull(session.getSession());
    }

    @Test
    void constructorWithCompressionPolicy() {
        doNothing().when(mockCompression).init(any());
        WebSocketChannelBasicSession session = new WebSocketChannelBasicSession(
                mockSession, false, mockCompression, channelManager, mockRegistry);
        assertNotNull(session.getSession());
    }

    @Test
    void constructorWithEncryptionAndCompressionPolicies() {
        doNothing().when(mockEncryption).init(any());
        doNothing().when(mockCompression).init(any());
        WebSocketChannelBasicSession session = new WebSocketChannelBasicSession(
                mockSession, false, mockEncryption, mockCompression, channelManager, mockRegistry);
        assertNotNull(session.getSession());
    }

    // --- updateEncryptionPolicyParams ---

    @Test
    void updateEncryptionPolicyParamsDoesNotThrow() {
        assertDoesNotThrow(() -> channelSession.updateEncryptionPolicyParams(new java.util.HashMap<>()));
    }

    // --- processMessage with command that throws ---

    @Test
    void processMessageCommandThrowsSendsError() {
        when(mockRegistry.findComponents(eq(WebSocketChannelCommand.class), any()))
                .thenReturn(List.of(mockCommand));
        doThrow(new RuntimeException("command failed")).when(mockCommand)
                .execute(any(), any(), any(), any());

        WebSocketMessage msg = WebSocketMessage.createMessage("SEND_MESSAGE", "test".getBytes(), WebSocketMessageType.OK);
        java.util.HashMap<String, String> params = new java.util.HashMap<>();
        params.put("channelId", "ch-1");
        msg.setParams(params);

        channelSession.processMessage(msg.toJson().getBytes());
        // Should send error message back
        verify(mockRemote, atLeastOnce()).sendString(anyString(), any());
    }

    // --- close with null message ---

    @Test
    void closeWithNullMessageDoesNotThrow() {
        // close() is called with null → uses empty string
        // It calls dispose() which calls session.close()
        assertDoesNotThrow(() -> channelSession.close(null));
    }
}
