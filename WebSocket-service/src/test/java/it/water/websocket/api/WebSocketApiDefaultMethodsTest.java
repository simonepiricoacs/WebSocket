package it.water.websocket.api;

import it.water.websocket.api.channel.WebSocketChannelManager;
import it.water.websocket.api.channel.WebSocketChannelRemoteCommand;
import it.water.websocket.api.channel.WebSocketChannelSession;
import it.water.websocket.model.WebSocketConstants;
import it.water.websocket.model.WebSocketUserInfo;
import it.water.websocket.model.message.WebSocketMessage;
import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.Session;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for default methods on api interfaces and utility class constructors
 * that are not covered by concrete implementation tests.
 */
@ExtendWith(MockitoExtension.class)
class WebSocketApiDefaultMethodsTest {

    @Mock
    private Session mockSession;

    @Mock
    private RemoteEndpoint mockRemote;

    // ===================== WebSocketPolicy default methods =====================

    @Test
    void webSocketPolicyDefaultCloseOnFail() {
        WebSocketPolicy policy = (params, payload) -> true;
        assertTrue(policy.closeWebSocketOnFail());
    }

    @Test
    void webSocketPolicyDefaultPrintWarning() {
        WebSocketPolicy policy = (params, payload) -> true;
        assertTrue(policy.printWarningOnFail());
    }

    @Test
    void webSocketPolicyDefaultSendWarningBack() {
        WebSocketPolicy policy = (params, payload) -> true;
        assertFalse(policy.sendWarningBackToClientOnFail());
    }

    @Test
    void webSocketPolicyDefaultIgnoreMessage() {
        WebSocketPolicy policy = (params, payload) -> true;
        assertFalse(policy.ignoreMessageOnFail());
    }

    // ===================== WebSocketEndPoint default method =====================

    @Test
    void webSocketEndPointDefaultExecutorReturnsNull() {
        WebSocketEndPoint endPoint = new WebSocketEndPoint() {
            @Override
            public String getPath() { return "/test"; }
            @Override
            public WebSocketSession getHandler(Session session) { return null; }
        };
        assertNull(endPoint.getExecutorForOpenConnections(mockSession));
    }

    // ===================== WebSocketSession default methods =====================

    @Test
    void webSocketSessionDefaultGetPolicyParamsReturnsNull() {
        WebSocketSession session = createMinimalSession();
        assertNull(session.getPolicyParams());
    }

    @Test
    void webSocketSessionDefaultGetWebSocketPoliciesReturnsEmptyList() {
        WebSocketSession session = createMinimalSession();
        assertNotNull(session.getWebScoketPolicies());
        assertTrue(session.getWebScoketPolicies().isEmpty());
    }

    // ===================== WebSocketChannelRemoteCommand default method =====================

    @Test
    void remoteCommandDefaultExecuteDelegatesToRemoteExecute() {
        WebSocketChannelSession mockChannelSession = mock(WebSocketChannelSession.class);
        WebSocketChannelManager mockManager = mock(WebSocketChannelManager.class);
        WebSocketMessage msg = new WebSocketMessage();

        // Track whether the remote execute method is called
        boolean[] remoteCalled = {false};
        WebSocketChannelRemoteCommand cmd = (message, channelId, manager) -> remoteCalled[0] = true;

        cmd.execute(mockChannelSession, msg, "ch-1", mockManager);
        assertTrue(remoteCalled[0]);
    }

    // ===================== WebSocketConstants private constructor =====================

    @Test
    void webSocketConstantsPrivateConstructorThrows() {
        assertThrows(InvocationTargetException.class, () -> {
            var ctor = WebSocketConstants.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            ctor.newInstance();
        });
    }

    @Test
    void webSocketConstantsFieldsAreDefined() {
        assertNotNull(WebSocketConstants.WEB_SOCKET_USERNAME_PARAM);
        assertNotNull(WebSocketConstants.WEB_SOCKET_RECIPIENT_USER_PARAM);
        assertNotNull(WebSocketConstants.CLIENT_PUB_KEY_HEADER);
        assertNotNull(WebSocketConstants.CLIENT_PUB_KEY_QUERY_PARAM);
        assertNotNull(WebSocketConstants.AUTHORIZATION_COOKIE_NAME);
    }

    // ===================== WebSocketUserInfo factory methods with Session =====================

    @Test
    void fromSessionWithUsernameAndClusterInfo() {
        when(mockSession.getRemote()).thenReturn(mockRemote);
        when(mockRemote.getInetSocketAddress()).thenReturn(new InetSocketAddress("192.168.1.1", 8080));

        WebSocketUserInfo info = WebSocketUserInfo.fromSession("testUser", null, mockSession);
        assertEquals("testUser", info.getUsername());
        assertEquals("192.168.1.1", info.getIpAddress());
    }

    @Test
    void fromSessionWithUsernameOnly() {
        when(mockSession.getRemote()).thenReturn(mockRemote);
        when(mockRemote.getInetSocketAddress()).thenReturn(new InetSocketAddress("10.0.0.1", 9999));

        WebSocketUserInfo info = WebSocketUserInfo.fromSession("bob", mockSession);
        assertEquals("bob", info.getUsername());
        assertNull(info.getClusterNodeInfo());
    }

    @Test
    void anonymousWithoutClusterInfo() {
        when(mockSession.getRemote()).thenReturn(mockRemote);
        when(mockRemote.getInetSocketAddress()).thenReturn(new InetSocketAddress("10.0.0.2", 9999));

        WebSocketUserInfo info = WebSocketUserInfo.anonymous(mockSession);
        assertTrue(info.getUsername().startsWith("anonymous-"));
        assertNull(info.getClusterNodeInfo());
    }

    // ===================== Helper =====================

    private WebSocketSession createMinimalSession() {
        return new WebSocketSession() {
            @Override public boolean isAuthenticationRequired() { return false; }
            @Override public void authenticate() {}
            @Override public void authenticateAnonymous() {}
            @Override public Session getSession() { return mockSession; }
            @Override public void initialize() {}
            @Override public void onMessage(String message) {}
            @Override public void dispose() {}
            @Override public void sendRemote(WebSocketMessage m) {}
            @Override public void close(String message) {}
            @Override public WebSocketUserInfo getUserInfo() { return null; }
        };
    }
}
