package it.water.websocket.session;

import it.water.core.api.registry.ComponentRegistry;
import it.water.core.api.security.AuthenticationProvider;
import it.water.service.rest.api.security.jwt.JwtTokenService;
import it.water.websocket.api.WebSocketSession;
import it.water.websocket.compression.WebSocketCompression;
import it.water.websocket.encryption.WebSocketEncryption;
import it.water.websocket.model.WebSocketUserInfo;
import it.water.websocket.model.message.WebSocketMessage;
import it.water.websocket.model.message.WebSocketMessageType;
import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.UpgradeRequest;
import org.eclipse.jetty.websocket.api.WriteCallback;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.net.HttpCookie;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WebSocketAbstractSessionTest {

    @Mock
    private Session mockSession;

    @Mock
    private RemoteEndpoint mockRemote;

    @Mock
    private UpgradeRequest mockUpgradeRequest;

    @Mock
    private ComponentRegistry mockRegistry;

    @Mock
    private JwtTokenService mockJwtTokenService;

    @Mock
    private AuthenticationProvider mockAuthProvider;

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
        when(mockUpgradeRequest.getHeader("Authorization")).thenReturn(null);
        doNothing().when(mockRemote).sendString(anyString(), any());
    }

    private WebSocketAbstractSession createSession(boolean authRequired) {
        return new WebSocketAbstractSession(mockSession, authRequired, mockRegistry) {
            @Override
            public void initialize() {
                // no-op
            }

            @Override
            public void onMessage(String message) {
                // no-op
            }

            @Override
            public void close(String message) {
                // no-op
            }
        };
    }

    @Test
    void isAuthenticationRequiredTrue() {
        WebSocketAbstractSession session = createSession(true);
        assertTrue(session.isAuthenticationRequired());
    }

    @Test
    void isAuthenticationRequiredFalse() {
        WebSocketAbstractSession session = createSession(false);
        assertFalse(session.isAuthenticationRequired());
    }

    @Test
    void isAuthenticatedFalseBeforeAuth() {
        WebSocketAbstractSession session = createSession(true);
        assertFalse(session.isAuthenticated());
    }

    @Test
    void isAuthenticatedFalseForNotRequiredSession() {
        WebSocketAbstractSession session = createSession(false);
        assertFalse(session.isAuthenticated());
    }

    @Test
    void authenticateAnonymousSetsUserInfo() {
        WebSocketAbstractSession session = createSession(false);
        session.authenticateAnonymous();
        WebSocketUserInfo userInfo = session.getUserInfo();
        assertNotNull(userInfo);
        assertTrue(userInfo.getUsername().startsWith("anonymous-"));
    }

    @Test
    void getUserInfoNullBeforeAuth() {
        WebSocketAbstractSession session = createSession(false);
        assertNull(session.getUserInfo());
    }

    @Test
    void getSessionReturnsSession() {
        WebSocketAbstractSession session = createSession(false);
        assertEquals(mockSession, session.getSession());
    }

    @Test
    void disposeCallsSessionClose() {
        WebSocketAbstractSession session = createSession(false);
        session.dispose();
        verify(mockSession).close();
    }

    @Test
    void sendRemoteForwardsToMessageBroker() {
        WebSocketAbstractSession session = createSession(false);
        WebSocketMessage msg = WebSocketMessage.createMessage("TEST", "hello".getBytes(), WebSocketMessageType.OK);
        session.sendRemote(msg);
        verify(mockRemote, atLeastOnce()).sendString(anyString(), any());
    }

    @Test
    void sendRemoteStringForwardsToMessageBroker() {
        WebSocketAbstractSession session = createSession(false);
        session.sendRemote("hello world");
        verify(mockRemote, atLeastOnce()).sendString(anyString(), any());
    }

    @Test
    void authenticateWithNoCookiesNoHeaderSetsNullUsername() {
        WebSocketAbstractSession session = createSession(false);
        // No cookies, no headers -> doAuthenticate returns null, no auth
        session.authenticate();
        assertFalse(session.isAuthenticated());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Bearer some.jwt.token", "JWT some.jwt.token", "plaintoken"})
    void authenticateWithVariousHeaderFormatsFailsWithoutJwtService(String headerValue) {
        // jwtTokenService is null (component registry returns null) -> no valid auth regardless of header format
        when(mockUpgradeRequest.getHeader("Authorization")).thenReturn(headerValue);
        WebSocketAbstractSession session = createSession(false);
        session.authenticate();
        assertFalse(session.isAuthenticated());
    }

    @Test
    void getPolicyParamsDefaultNull() {
        WebSocketSession session = createSession(false);
        assertNull(session.getPolicyParams());
    }

    @Test
    void getWebSocketPoliciesDefaultEmpty() {
        WebSocketSession session = createSession(false);
        assertNotNull(session.getWebScoketPolicies());
        assertTrue(session.getWebScoketPolicies().isEmpty());
    }

    // --- Cookie authentication ---

    @Test
    void authenticateWithCookieNoJwtServiceSetsNullUsername() {
        HttpCookie cookie = new HttpCookie("water-auth-token", "some.jwt.token");
        when(mockUpgradeRequest.getCookies()).thenReturn(List.of(cookie));
        // registry.findComponent(JwtTokenService) → null (no JWT service)
        when(mockRegistry.findComponent(eq(JwtTokenService.class), any())).thenReturn(null);

        WebSocketAbstractSession session = createSession(false);
        session.authenticate();
        assertFalse(session.isAuthenticated());
    }

    @Test
    void authenticateWithCookieAndJwtServiceButInvalidToken() {
        HttpCookie cookie = new HttpCookie("water-auth-token", "invalid.token");
        when(mockUpgradeRequest.getCookies()).thenReturn(List.of(cookie));
        when(mockRegistry.findComponent(eq(JwtTokenService.class), any())).thenReturn(mockJwtTokenService);
        when(mockRegistry.findComponents(eq(AuthenticationProvider.class), any())).thenReturn(Collections.emptyList());
        when(mockJwtTokenService.validateToken(any(), anyString())).thenReturn(false);

        WebSocketAbstractSession session = createSession(false);
        session.authenticate();
        assertFalse(session.isAuthenticated());
    }

    @Test
    void authenticateWithCookieAndJwtServiceValidToken() {
        HttpCookie cookie = new HttpCookie("water-auth-token", "valid.jwt.token");
        when(mockUpgradeRequest.getCookies()).thenReturn(List.of(cookie));
        when(mockRegistry.findComponent(eq(JwtTokenService.class), any())).thenReturn(mockJwtTokenService);
        when(mockRegistry.findComponents(eq(AuthenticationProvider.class), any())).thenReturn(Collections.emptyList());
        when(mockJwtTokenService.validateToken(any(), anyString())).thenReturn(true);
        java.security.Principal principal = () -> "authenticatedUser";
        when(mockJwtTokenService.getPrincipals(anyString())).thenReturn(Set.of(principal));

        WebSocketAbstractSession session = createSession(true);
        session.authenticate();
        assertTrue(session.isAuthenticated());
    }

    @Test
    void authenticateWithAuthProviderLoadedIssuers() {
        HttpCookie cookie = new HttpCookie("water-auth-token", "valid.token");
        when(mockUpgradeRequest.getCookies()).thenReturn(List.of(cookie));
        when(mockRegistry.findComponent(eq(JwtTokenService.class), any())).thenReturn(mockJwtTokenService);
        when(mockRegistry.findComponents(eq(AuthenticationProvider.class), any())).thenReturn(List.of(mockAuthProvider));
        when(mockAuthProvider.issuersNames()).thenReturn(List.of("myIssuer"));
        when(mockJwtTokenService.validateToken(any(), anyString())).thenReturn(false);

        WebSocketAbstractSession session = createSession(false);
        session.authenticate();
        verify(mockAuthProvider).issuersNames();
    }

    @Test
    void authenticateAuthRequiredWithNoCredentialsSendsCloseMessage() {
        // No cookies, no headers. Auth required.
        WebSocketAbstractSession session = createSession(true);
        session.authenticate();
        // Should have called sendString to send "Client not authenticated!"
        verify(mockRemote, atLeastOnce()).sendString(anyString(), any());
    }

    // --- sendRemote overloads ---

    @Test
    void sendRemoteWithCallback() {
        WriteCallback callback = mock(WriteCallback.class);
        WebSocketAbstractSession session = createSession(false);
        WebSocketMessage msg = WebSocketMessage.createMessage("CMD", "data".getBytes(), WebSocketMessageType.OK);
        session.sendRemote(msg, callback);
        verify(mockRemote, atLeastOnce()).sendString(anyString(), any());
    }

    @Test
    void sendRemoteStringWithCallback() {
        WriteCallback callback = mock(WriteCallback.class);
        WebSocketAbstractSession session = createSession(false);
        session.sendRemote("hello", callback);
        verify(mockRemote, atLeastOnce()).sendString(anyString(), any());
    }

    @Test
    void sendRemoteWithEncodeBase64Flag() {
        WebSocketAbstractSession session = createSession(false);
        WebSocketMessage msg = WebSocketMessage.createMessage("CMD", "data".getBytes(), WebSocketMessageType.OK);
        session.sendRemote(msg, false);
        verify(mockRemote, atLeastOnce()).sendString(anyString(), any());
    }

    @Test
    void sendRemoteStringWithEncodeBase64Flag() {
        WebSocketAbstractSession session = createSession(false);
        session.sendRemote("hello", false);
        verify(mockRemote, atLeastOnce()).sendString(anyString(), any());
    }

    @Test
    void sendRemoteStringWithEncodeBase64FlagAndCallback() {
        WriteCallback callback = mock(WriteCallback.class);
        WebSocketAbstractSession session = createSession(false);
        session.sendRemote("hello", false, callback);
        verify(mockRemote, atLeastOnce()).sendString(anyString(), any());
    }

    // --- constructors with encryption / compression ---

    @Test
    void constructorWithEncryptionPolicy() {
        doNothing().when(mockEncryption).init(any());
        WebSocketAbstractSession session = new WebSocketAbstractSession(mockSession, false, mockEncryption, mockRegistry) {
            @Override public void initialize() {}
            @Override public void onMessage(String message) {}
            @Override public void close(String message) {}
        };
        assertNotNull(session.getSession());
    }

    @Test
    void constructorWithCompressionPolicy() {
        doNothing().when(mockCompression).init(any());
        WebSocketAbstractSession session = new WebSocketAbstractSession(mockSession, false, mockCompression, mockRegistry) {
            @Override public void initialize() {}
            @Override public void onMessage(String message) {}
            @Override public void close(String message) {}
        };
        assertNotNull(session.getSession());
    }

    @Test
    void constructorWithEncryptionAndCompressionPolicies() {
        doNothing().when(mockEncryption).init(any());
        doNothing().when(mockCompression).init(any());
        WebSocketAbstractSession session = new WebSocketAbstractSession(mockSession, false, mockEncryption, mockCompression, mockRegistry) {
            @Override public void initialize() {}
            @Override public void onMessage(String message) {}
            @Override public void close(String message) {}
        };
        assertNotNull(session.getSession());
    }
}
