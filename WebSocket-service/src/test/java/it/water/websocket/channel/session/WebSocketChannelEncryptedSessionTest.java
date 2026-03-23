package it.water.websocket.channel.session;

import it.water.core.api.registry.ComponentRegistry;
import it.water.core.api.registry.filter.ComponentFilter;
import it.water.core.api.registry.filter.ComponentFilterBuilder;
import it.water.websocket.api.channel.WebSocketChannelManager;
import it.water.websocket.api.channel.WebSocketChannelRole;
import it.water.websocket.channel.role.WebSocketChannelRoleManager;
import it.water.websocket.compression.WebSocketCompression;
import it.water.websocket.encryption.WebSocketEncryption;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WebSocketChannelEncryptedSessionTest {

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
    private WebSocketChannelRole mockRole;

    @Mock
    private WebSocketEncryption mockEncryption;

    @Mock
    private WebSocketCompression mockCompression;

    @BeforeEach
    void setUp() throws Exception {
        when(mockSession.getRemote()).thenReturn(mockRemote);
        when(mockRemote.getInetSocketAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 12345));
        when(mockSession.getUpgradeRequest()).thenReturn(mockUpgradeRequest);
        when(mockUpgradeRequest.getCookies()).thenReturn(Collections.emptyList());
        when(mockUpgradeRequest.getHeader(anyString())).thenReturn(null);
        doNothing().when(mockRemote).sendString(anyString(), any());
        doNothing().when(mockEncryption).init(any());
        doNothing().when(mockEncryption).dispose(any());
        doNothing().when(mockCompression).init(any());
        try {
            when(mockEncryption.encrypt(any(), anyBoolean())).thenAnswer(inv -> inv.getArgument(0));
            when(mockEncryption.decrypt(any(), anyBoolean())).thenAnswer(inv -> inv.getArgument(0));
        } catch (Exception ignored) {}

        WebSocketChannelRoleManager.setComponentRegistry(mockRegistry);
        when(mockRegistry.getComponentFilterBuilder()).thenReturn(filterBuilder);
        when(filterBuilder.createFilter(anyString(), anyString())).thenReturn(filter);
        when(mockRegistry.findComponents(eq(WebSocketChannelRole.class), any())).thenReturn(List.of(mockRole));
    }

    @AfterEach
    void tearDown() {
        WebSocketChannelRoleManager.setComponentRegistry(null);
    }

    private WebSocketChannelEncryptedSession createConcreteEncryptedSession(Session s, boolean authRequired, WebSocketChannelManager mgr, ComponentRegistry reg) {
        return new WebSocketChannelEncryptedSession(s, authRequired, mgr, reg) {
            @Override
            protected String defineEncryptionMessage() {
                return "aesKey:aesIv";
            }

            @Override
            protected Map<String, Object> defineEncryptionPolicyParams() {
                return new HashMap<>();
            }
        };
    }

    // --- Basic constructor and getSession ---

    @Test
    void constructorBasicCreatesSession() {
        WebSocketChannelEncryptedSession session = createConcreteEncryptedSession(
                mockSession, false, channelManager, mockRegistry);
        assertNotNull(session.getSession());
    }

    @Test
    void constructorWithEncryptionPolicy() {
        WebSocketChannelEncryptedSession session = new WebSocketChannelEncryptedSession(
                mockSession, false, mockEncryption, channelManager, mockRegistry) {
            @Override protected String defineEncryptionMessage() { return "key:iv"; }
            @Override protected Map<String, Object> defineEncryptionPolicyParams() { return new HashMap<>(); }
        };
        assertNotNull(session.getSession());
    }

    @Test
    void constructorWithCompressionPolicy() {
        WebSocketChannelEncryptedSession session = new WebSocketChannelEncryptedSession(
                mockSession, false, mockCompression, channelManager, mockRegistry) {
            @Override protected String defineEncryptionMessage() { return "key:iv"; }
            @Override protected Map<String, Object> defineEncryptionPolicyParams() { return new HashMap<>(); }
        };
        assertNotNull(session.getSession());
    }

    @Test
    void constructorWithEncryptionAndCompressionPolicies() {
        WebSocketChannelEncryptedSession session = new WebSocketChannelEncryptedSession(
                mockSession, false, mockEncryption, mockCompression, channelManager, mockRegistry) {
            @Override protected String defineEncryptionMessage() { return "key:iv"; }
            @Override protected Map<String, Object> defineEncryptionPolicyParams() { return new HashMap<>(); }
        };
        assertNotNull(session.getSession());
    }

    // --- initialize triggers onConnect → sends SET_ENCRYPTION_KEY message ---

    @Test
    void initializeSendsEncryptionKeyMessage() {
        WebSocketChannelEncryptedSession session = createConcreteEncryptedSession(
                mockSession, false, channelManager, mockRegistry);
        session.authenticateAnonymous();
        session.initialize();
        // onConnect() sends CONNECTION_OK + SET_ENCRYPTION_KEY messages
        verify(mockRemote, atLeastOnce()).sendString(anyString(), any());
    }

    @Test
    void initializeWithEncryptionPolicySendsMessages() {
        WebSocketChannelEncryptedSession session = new WebSocketChannelEncryptedSession(
                mockSession, false, mockEncryption, channelManager, mockRegistry) {
            @Override protected String defineEncryptionMessage() { return "key:iv"; }
            @Override protected Map<String, Object> defineEncryptionPolicyParams() { return new HashMap<>(); }
        };
        session.authenticateAnonymous();
        session.initialize();
        verify(mockRemote, atLeastOnce()).sendString(anyString(), any());
    }
}
