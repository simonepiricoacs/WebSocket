package it.water.websocket.service;

import it.water.core.api.bundle.ApplicationProperties;
import it.water.core.api.registry.ComponentRegistry;
import it.water.websocket.api.WebSocketEndPoint;
import it.water.websocket.api.WebSocketPolicy;
import it.water.websocket.api.WebSocketSession;
import it.water.websocket.policy.InputBufferSizePolicy;
import it.water.websocket.policy.MaxBinaryMessageSizePolicy;
import it.water.websocket.policy.MaxBinaryMessageBufferSizePolicy;
import it.water.websocket.policy.MaxTextMessageBufferSizePolicy;
import it.water.websocket.policy.MaxTextMessageSizePolicy;
import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.UpgradeRequest;
import org.eclipse.jetty.websocket.api.WriteCallback;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WebSocketServiceTest {

    @Mock
    private ComponentRegistry componentRegistry;

    @Mock
    private ApplicationProperties applicationProperties;

    @Mock
    private Session mockSession;

    @Mock
    private RemoteEndpoint mockRemote;

    @Mock
    private UpgradeRequest mockUpgradeRequest;

    @Mock
    private WebSocketEndPoint mockEndPoint;

    @Mock
    private WebSocketSession mockWebSocketSession;

    @Mock
    private org.eclipse.jetty.websocket.api.WebSocketPolicy jettyPolicy;

    private WebSocketService service;

    @BeforeEach
    void setUp() throws Exception {
        // Clear the static sessions map before each test
        WebSocketService.getSessions().clear();

        service = new WebSocketService();
        // Inject dependencies via setters
        service.setComponentRegistry(componentRegistry);
        service.setApplicationProperties(applicationProperties);

        // Mock session setup
        when(mockSession.getPolicy()).thenReturn(jettyPolicy);
        when(mockSession.getRemote()).thenReturn(mockRemote);
        when(mockSession.getUpgradeRequest()).thenReturn(mockUpgradeRequest);
        when(mockUpgradeRequest.getRequestURI()).thenReturn(new URI("ws://localhost/ws/test"));
        doNothing().when(mockRemote).sendString(anyString(), any());
        when(mockSession.isOpen()).thenReturn(true);

        // Mock endpoint
        when(mockEndPoint.getPath()).thenReturn("/test");
        when(mockEndPoint.getHandler(any())).thenReturn(mockWebSocketSession);
        when(mockEndPoint.getExecutorForOpenConnections(any())).thenReturn(null);

        // Mock WebSocketSession
        when(mockWebSocketSession.getSession()).thenReturn(mockSession);
        when(mockWebSocketSession.isAuthenticationRequired()).thenReturn(false);
        when(mockWebSocketSession.getWebScoketPolicies()).thenReturn(Collections.emptyList());
        when(mockWebSocketSession.getPolicyParams()).thenReturn(Collections.emptyMap());

        // Mock ApplicationProperties to return /ws as base path; other props return null -> use defaults
        when(applicationProperties.getProperty("water.websocket.base.path")).thenReturn("/ws");

        // Activate to set webSocketServiceUrl, then re-inject sync executors
        service.activate();
        injectSynchronousExecutors(service);
    }

    @AfterEach
    void tearDown() {
        WebSocketService.getSessions().clear();
    }

    private void injectSynchronousExecutors(WebSocketService svc) throws Exception {
        Executor syncExecutor = Runnable::run;
        setField(svc, "onOpenDispatchThreads", syncExecutor);
        setField(svc, "onCloseDispatchThreads", syncExecutor);
        setField(svc, "onMessageDispatchThreads", syncExecutor);
        setField(svc, "onErrorDispatchThreads", syncExecutor);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field f = WebSocketService.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    @Test
    void getSessionsReturnsNonNullEmptyMap() {
        assertNotNull(WebSocketService.getSessions());
        assertTrue(WebSocketService.getSessions().isEmpty());
    }

    @Test
    void activateWithDefaultProperties() {
        // setUp already called activate() with mocked properties returning /ws
        assertNotNull(service.getWebSocketServiceUrl());
        assertEquals("/ws", service.getWebSocketServiceUrl());
    }

    @Test
    void activateWithCustomUrl() throws Exception {
        WebSocketService customService = new WebSocketService();
        customService.setComponentRegistry(componentRegistry);
        when(applicationProperties.getProperty("water.websocket.base.path")).thenReturn("/custom-ws");
        when(applicationProperties.getProperty("water.websocket.threads.open")).thenReturn("5");
        when(applicationProperties.getProperty("water.websocket.threads.close")).thenReturn("5");
        when(applicationProperties.getProperty("water.websocket.threads.message")).thenReturn("10");
        when(applicationProperties.getProperty("water.websocket.threads.error")).thenReturn("2");
        customService.setApplicationProperties(applicationProperties);
        customService.activate();
        assertEquals("/custom-ws", customService.getWebSocketServiceUrl());
    }

    @Test
    void activateWithNullProperties() throws Exception {
        WebSocketService customService = new WebSocketService();
        customService.setComponentRegistry(componentRegistry);
        customService.setApplicationProperties(null);
        customService.activate();
        // Should use default base path
        assertEquals("/ws", customService.getWebSocketServiceUrl());
    }

    @Test
    void activateWithInvalidThreadPropertiesFallsBackToDefaults() {
        when(applicationProperties.getProperty("water.websocket.threads.open")).thenReturn("invalid");
        when(applicationProperties.getProperty("water.websocket.threads.close")).thenReturn("invalid");
        when(applicationProperties.getProperty("water.websocket.threads.message")).thenReturn("invalid");
        when(applicationProperties.getProperty("water.websocket.threads.error")).thenReturn("invalid");

        assertDoesNotThrow(() -> service.activate());
        assertEquals("/ws", service.getWebSocketServiceUrl());
    }

    @Test
    void deactivateDisposesAllSessions() throws Exception {
        // Manually add a session to the static map
        WebSocketService.getSessions().put(mockSession, mockWebSocketSession);

        service.deactivate();

        verify(mockWebSocketSession).dispose();
        assertTrue(WebSocketService.getSessions().isEmpty());
    }

    @Test
    void onOpenWithKnownPathAddsSession() {
        when(componentRegistry.findComponents(eq(WebSocketEndPoint.class), any()))
                .thenReturn(List.of(mockEndPoint));

        service.onOpen(mockSession);

        assertFalse(WebSocketService.getSessions().isEmpty());
        verify(mockWebSocketSession).initialize();
    }

    @Test
    void onOpenMatchesEndpointWithoutLeadingSlash() {
        when(mockEndPoint.getPath()).thenReturn("test");
        when(componentRegistry.findComponents(eq(WebSocketEndPoint.class), any()))
                .thenReturn(List.of(mockEndPoint));

        service.onOpen(mockSession);

        verify(mockWebSocketSession).initialize();
    }

    @Test
    void onOpenWithKnownPathAuthenticatesAnonymous() {
        when(componentRegistry.findComponents(eq(WebSocketEndPoint.class), any()))
                .thenReturn(List.of(mockEndPoint));
        when(mockWebSocketSession.isAuthenticationRequired()).thenReturn(false);

        service.onOpen(mockSession);

        verify(mockWebSocketSession).authenticateAnonymous();
    }

    @Test
    void onOpenWithKnownPathAuthenticates() {
        when(componentRegistry.findComponents(eq(WebSocketEndPoint.class), any()))
                .thenReturn(List.of(mockEndPoint));
        when(mockWebSocketSession.isAuthenticationRequired()).thenReturn(true);

        service.onOpen(mockSession);

        verify(mockWebSocketSession).authenticate();
    }

    @Test
    void onOpenWithUnknownPathClosesSession() {
        when(componentRegistry.findComponents(eq(WebSocketEndPoint.class), any()))
                .thenReturn(Collections.emptyList());

        service.onOpen(mockSession);

        verify(mockSession).close(eq(1010), anyString());
    }

    @Test
    void onCloseRemovesSession() {
        WebSocketService.getSessions().put(mockSession, mockWebSocketSession);

        service.onClose(mockSession, 1000, "Normal");

        assertFalse(WebSocketService.getSessions().containsKey(mockSession));
    }

    @Test
    void onCloseCallsDispose() {
        WebSocketService.getSessions().put(mockSession, mockWebSocketSession);

        service.onClose(mockSession, 1000, "Normal");

        verify(mockWebSocketSession).dispose();
    }

    @Test
    void onMessageForwardsToSession() {
        WebSocketService.getSessions().put(mockSession, mockWebSocketSession);
        when(mockWebSocketSession.getPolicyParams()).thenReturn(null);

        service.onMessage(mockSession, "{\"cmd\":\"TEST\",\"type\":\"OK\"}");

        verify(mockWebSocketSession).onMessage(anyString());
    }

    @Test
    void onMessagePolicyFailClose() {
        WebSocketPolicy failPolicy = mock(WebSocketPolicy.class);
        when(failPolicy.isSatisfied(any(), any())).thenReturn(false);
        when(failPolicy.closeWebSocketOnFail()).thenReturn(true);
        when(failPolicy.printWarningOnFail()).thenReturn(false);
        when(failPolicy.sendWarningBackToClientOnFail()).thenReturn(false);
        when(failPolicy.ignoreMessageOnFail()).thenReturn(false);

        WebSocketService.getSessions().put(mockSession, mockWebSocketSession);
        // Set up policies in the session policies map via reflection
        try {
            Field policiesField = WebSocketService.class.getDeclaredField("webSocketSessionPolicies");
            policiesField.setAccessible(true);
            java.util.Map<Session, java.util.List<WebSocketPolicy>> policiesMap =
                    (java.util.Map<Session, java.util.List<WebSocketPolicy>>) policiesField.get(null);
            policiesMap.put(mockSession, List.of(failPolicy));
        } catch (Exception e) {
            fail("Could not set up policies: " + e.getMessage());
        }

        service.onMessage(mockSession, "any message");

        verify(mockWebSocketSession).dispose();
    }

    @Test
    void onMessagePolicyIgnore() {
        WebSocketPolicy ignorePolicy = mock(WebSocketPolicy.class);
        when(ignorePolicy.isSatisfied(any(), any())).thenReturn(false);
        when(ignorePolicy.closeWebSocketOnFail()).thenReturn(false);
        when(ignorePolicy.printWarningOnFail()).thenReturn(false);
        when(ignorePolicy.sendWarningBackToClientOnFail()).thenReturn(false);
        when(ignorePolicy.ignoreMessageOnFail()).thenReturn(true);

        WebSocketService.getSessions().put(mockSession, mockWebSocketSession);
        try {
            Field policiesField = WebSocketService.class.getDeclaredField("webSocketSessionPolicies");
            policiesField.setAccessible(true);
            java.util.Map<Session, java.util.List<WebSocketPolicy>> policiesMap =
                    (java.util.Map<Session, java.util.List<WebSocketPolicy>>) policiesField.get(null);
            policiesMap.put(mockSession, List.of(ignorePolicy));
        } catch (Exception e) {
            fail("Could not set up policies: " + e.getMessage());
        }

        service.onMessage(mockSession, "any message");

        verify(mockWebSocketSession, never()).onMessage(anyString());
    }

    @Test
    void onErrorDelegatesToOnClose() {
        WebSocketService.getSessions().put(mockSession, mockWebSocketSession);

        service.onError(mockSession, new RuntimeException("test error"));

        verify(mockWebSocketSession).dispose();
    }

    @Test
    void onErrorWithNullSession() {
        // Should not throw
        assertDoesNotThrow(() -> service.onError(null, new RuntimeException("error")));
    }

    @Test
    void onMessageNullSession() {
        // Should not throw
        assertDoesNotThrow(() -> service.onMessage(null, "message"));
    }

    @Test
    void onMessageSessionNotOpen() {
        when(mockSession.isOpen()).thenReturn(false);
        WebSocketService.getSessions().put(mockSession, mockWebSocketSession);

        service.onMessage(mockSession, "message");

        verify(mockWebSocketSession, never()).onMessage(anyString());
    }

    @Test
    void onCloseWriteCallbackWriteFailed() {
        WebSocketService.getSessions().put(mockSession, mockWebSocketSession);
        ArgumentCaptor<WriteCallback> cbCaptor = ArgumentCaptor.forClass(WriteCallback.class);

        service.onClose(mockSession, 1000, "Normal");

        verify(mockRemote).sendString(anyString(), cbCaptor.capture());
        assertDoesNotThrow(() -> cbCaptor.getValue().writeFailed(new RuntimeException("send failed")));
    }

    @Test
    void onCloseWriteCallbackWriteSuccess() {
        WebSocketService.getSessions().put(mockSession, mockWebSocketSession);
        ArgumentCaptor<WriteCallback> cbCaptor = ArgumentCaptor.forClass(WriteCallback.class);

        service.onClose(mockSession, 1000, "Normal");

        verify(mockRemote).sendString(anyString(), cbCaptor.capture());
        assertDoesNotThrow(() -> cbCaptor.getValue().writeSuccess());
    }

    @Test
    void onOpenWithCustomExecutor() {
        Executor customExecutor = Runnable::run;
        when(mockEndPoint.getExecutorForOpenConnections(any())).thenReturn(customExecutor);
        when(componentRegistry.findComponents(eq(WebSocketEndPoint.class), any()))
                .thenReturn(List.of(mockEndPoint));

        service.onOpen(mockSession);

        assertFalse(WebSocketService.getSessions().isEmpty());
        verify(mockWebSocketSession).initialize();
    }

    @Test
    void onOpenAppliesInputBufferSizePolicy() {
        InputBufferSizePolicy inputPolicy = new InputBufferSizePolicy(mockSession, 8192);
        when(mockWebSocketSession.getWebScoketPolicies()).thenReturn(List.of(inputPolicy));
        when(componentRegistry.findComponents(eq(WebSocketEndPoint.class), any()))
                .thenReturn(List.of(mockEndPoint));

        service.onOpen(mockSession);

        verify(jettyPolicy).setInputBufferSize(8192);
    }

    @Test
    void onOpenAppliesMaxBinaryMessageBufferSizePolicy() {
        MaxBinaryMessageBufferSizePolicy mbPolicy = new MaxBinaryMessageBufferSizePolicy(mockSession, 65536);
        when(mockWebSocketSession.getWebScoketPolicies()).thenReturn(List.of(mbPolicy));
        when(componentRegistry.findComponents(eq(WebSocketEndPoint.class), any()))
                .thenReturn(List.of(mockEndPoint));

        service.onOpen(mockSession);

        verify(jettyPolicy).setMaxBinaryMessageBufferSize(65536);
    }

    @Test
    void onOpenAppliesMaxTextMessageSizePolicy() {
        MaxTextMessageSizePolicy mtPolicy = new MaxTextMessageSizePolicy(mockSession, 32768);
        when(mockWebSocketSession.getWebScoketPolicies()).thenReturn(List.of(mtPolicy));
        when(componentRegistry.findComponents(eq(WebSocketEndPoint.class), any()))
                .thenReturn(List.of(mockEndPoint));

        service.onOpen(mockSession);

        verify(jettyPolicy).setMaxTextMessageSize(32768);
    }

    @Test
    void onOpenAppliesMaxBinaryMessageSizePolicy() {
        MaxBinaryMessageSizePolicy policy = new MaxBinaryMessageSizePolicy(mockSession, 4096);
        when(mockWebSocketSession.getWebScoketPolicies()).thenReturn(List.of(policy));
        when(componentRegistry.findComponents(eq(WebSocketEndPoint.class), any()))
                .thenReturn(List.of(mockEndPoint));

        service.onOpen(mockSession);

        verify(jettyPolicy).setMaxBinaryMessageSize(4096);
    }

    @Test
    void onOpenAppliesMaxTextMessageBufferSizePolicy() {
        MaxTextMessageBufferSizePolicy policy = new MaxTextMessageBufferSizePolicy(mockSession, 4096);
        when(mockWebSocketSession.getWebScoketPolicies()).thenReturn(List.of(policy));
        when(componentRegistry.findComponents(eq(WebSocketEndPoint.class), any()))
                .thenReturn(List.of(mockEndPoint));

        service.onOpen(mockSession);

        verify(jettyPolicy).setMaxTextMessageBufferSize(4096);
    }

    @Test
    void onOpenHandlesRegistryErrorsByNotThrowing() {
        when(componentRegistry.findComponents(eq(WebSocketEndPoint.class), any()))
                .thenThrow(new IllegalStateException("registry error"));

        assertDoesNotThrow(() -> service.onOpen(mockSession));
    }

    @Test
    void onMessagePolicySendWarningBackToClient() throws Exception {
        WebSocketPolicy warnPolicy = mock(WebSocketPolicy.class);
        when(warnPolicy.isSatisfied(any(), any())).thenReturn(false);
        when(warnPolicy.closeWebSocketOnFail()).thenReturn(false);
        when(warnPolicy.printWarningOnFail()).thenReturn(false);
        when(warnPolicy.sendWarningBackToClientOnFail()).thenReturn(true);
        when(warnPolicy.ignoreMessageOnFail()).thenReturn(false);

        WebSocketService.getSessions().put(mockSession, mockWebSocketSession);
        try {
            Field policiesField = WebSocketService.class.getDeclaredField("webSocketSessionPolicies");
            policiesField.setAccessible(true);
            java.util.Map<Session, java.util.List<WebSocketPolicy>> policiesMap =
                    (java.util.Map<Session, java.util.List<WebSocketPolicy>>) policiesField.get(null);
            policiesMap.put(mockSession, List.of(warnPolicy));
        } catch (Exception e) {
            fail("Could not set up policies: " + e.getMessage());
        }

        service.onMessage(mockSession, "any message");

        verify(mockRemote).sendString(anyString());
    }
}
