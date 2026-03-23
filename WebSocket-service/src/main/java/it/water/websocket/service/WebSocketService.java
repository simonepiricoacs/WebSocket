/*
 * Copyright 2019-2023 HyperIoT
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package it.water.websocket.service;

import it.water.core.api.bundle.ApplicationProperties;
import it.water.core.api.interceptors.OnActivate;
import it.water.core.api.interceptors.OnDeactivate;
import it.water.core.api.registry.ComponentRegistry;
import it.water.core.interceptors.annotations.FrameworkComponent;
import it.water.core.interceptors.annotations.Inject;
import it.water.websocket.api.WebSocketEndPoint;
import it.water.websocket.api.WebSocketPolicy;
import it.water.websocket.api.WebSocketSession;
import it.water.websocket.model.message.WebSocketMessage;
import it.water.websocket.model.message.WebSocketMessageType;
import it.water.websocket.policy.*;
import it.water.websocket.session.WebSocketAbstractSession;
import lombok.Setter;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WriteCallback;
import org.eclipse.jetty.websocket.api.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Component which opens a web socket session and keeps track of all user's sessions.
 * Ported from HyperIoT to Water Framework.
 */
@FrameworkComponent
@WebSocket()
@SuppressWarnings("java:S112") // WebSocket event handlers must catch broad exceptions to prevent connection teardown on any runtime error
public class WebSocketService {
    private static final Logger log = LoggerFactory.getLogger(WebSocketService.class);

    // Default configuration values
    private static final int DEFAULT_OPEN_THREADS = 200;
    private static final int DEFAULT_CLOSE_THREADS = 200;
    private static final int DEFAULT_MESSAGE_THREADS = 500;
    private static final int DEFAULT_ERROR_THREADS = 20;
    @SuppressWarnings("java:S1075") // configurable default; overridable via PROP_WS_BASE_PATH property
    private static final String DEFAULT_WS_BASE_PATH = "/ws";

    // Property keys for ApplicationProperties
    private static final String PROP_WS_BASE_PATH = "water.websocket.base.path";
    private static final String PROP_OPEN_THREADS = "water.websocket.threads.open";
    private static final String PROP_CLOSE_THREADS = "water.websocket.threads.close";
    private static final String PROP_MESSAGE_THREADS = "water.websocket.threads.message";
    private static final String PROP_ERROR_THREADS = "water.websocket.threads.error";

    @Inject
    @Setter
    private ComponentRegistry componentRegistry;

    @Inject
    @Setter
    private ApplicationProperties applicationProperties;

    private String webSocketServiceUrl;

    /**
     * Managing all websocket sessions in terms of:
     * 1) Classical sessions
     * 2) Bridged Sessions
     * 3) Custom Websocket policies for each session
     */
    private static final Map<Session, WebSocketSession> sessions = Collections.synchronizedMap(new HashMap<>());
    private static final Map<Session, List<WebSocketPolicy>> webSocketSessionPolicies = Collections.synchronizedMap(new HashMap<>());

    /**
     * Managing Threads with custom executors in order to avoid greedy clients to monopolize connections
     */
    private Executor onOpenDispatchThreads;
    private Executor onCloseDispatchThreads;
    private Executor onMessageDispatchThreads;
    private Executor onErrorDispatchThreads;

    @OnActivate
    public void activate() {
        try {
            // Load configuration
            int openThreads = getPropertyOrDefault(PROP_OPEN_THREADS, DEFAULT_OPEN_THREADS);
            int closeThreads = getPropertyOrDefault(PROP_CLOSE_THREADS, DEFAULT_CLOSE_THREADS);
            int messageThreads = getPropertyOrDefault(PROP_MESSAGE_THREADS, DEFAULT_MESSAGE_THREADS);
            int errorThreads = getPropertyOrDefault(PROP_ERROR_THREADS, DEFAULT_ERROR_THREADS);

            webSocketServiceUrl = getPropertyOrDefault(PROP_WS_BASE_PATH, DEFAULT_WS_BASE_PATH);

            // Create thread pools
            onOpenDispatchThreads = Executors.newFixedThreadPool(openThreads, buildThreadFactory("water-ws-open-thread-%d"));
            onCloseDispatchThreads = Executors.newFixedThreadPool(closeThreads, buildThreadFactory("water-ws-close-thread-%d"));
            onMessageDispatchThreads = Executors.newFixedThreadPool(messageThreads, buildThreadFactory("water-ws-message-thread-%d"));
            onErrorDispatchThreads = Executors.newFixedThreadPool(errorThreads, buildThreadFactory("water-ws-error-thread-%d"));

            log.info("WebSocket service activated with URL: {}, threads: open={}, close={}, message={}, error={}",
                    webSocketServiceUrl, openThreads, closeThreads, messageThreads, errorThreads);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    @OnDeactivate
    public void deactivate() {
        if (!sessions.values().isEmpty()) {
            for (WebSocketSession webSocketSession : sessions.values()) {
                try {
                    webSocketSession.dispose();
                } catch (Exception e) {
                    log.warn("Error disposing session: {}", e.getMessage(), e);
                }
            }
        }
        sessions.clear();
        webSocketSessionPolicies.clear();
        log.info("WebSocket service deactivated");
    }

    @OnWebSocketConnect
    public void onOpen(Session session) {
        try {
            log.debug("Opening web socket...");
            HashMap<String, WebSocketEndPoint> endPointHashMap = findWebSocketEndPoints();
            String requestPath = session.getUpgradeRequest().getRequestURI().getPath();
            if (webSocketServiceUrl != null) {
                requestPath = requestPath.replace(webSocketServiceUrl, "");
            }
            if (requestPath.startsWith("/")) requestPath = requestPath.substring(1);
            if (endPointHashMap.containsKey(requestPath)) {
                WebSocketEndPoint webSocketEndPoint = endPointHashMap.get(requestPath);
                Runnable r = () -> setupSessionForEndPoint(webSocketEndPoint, session);
                Executor onOpenCustomExecutor = webSocketEndPoint.getExecutorForOpenConnections(session);
                Executor runner = (onOpenCustomExecutor != null) ? onOpenCustomExecutor : onOpenDispatchThreads;
                runner.execute(r);
            } else {
                WebSocketMessage m = WebSocketMessage.createMessage(null, "Unknown service requested.".getBytes(StandardCharsets.UTF_8), WebSocketMessageType.ERROR);
                session.close(1010, m.toJson());
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private void setupSessionForEndPoint(WebSocketEndPoint webSocketEndPoint, Session session) {
        try {
            WebSocketSession webSocketSession = webSocketEndPoint.getHandler(session);
            if (webSocketSession.isAuthenticationRequired())
                webSocketSession.authenticate();
            else
                webSocketSession.authenticateAnonymous();
            //2 minutes idle timeout
            session.setIdleTimeout(120000);
            sessions.put(webSocketSession.getSession(), webSocketSession);
            List<WebSocketPolicy> policies = webSocketSession.getWebScoketPolicies();
            if (policies != null && !policies.isEmpty()) {
                webSocketSessionPolicies.put(session, policies);
                applyCustomPolicies(session, policies);
            }
            webSocketSession.initialize();
        } catch (Exception t) {
            log.error(t.getMessage(), t);
        }
    }

    @OnWebSocketClose
    @SuppressWarnings("java:S1172") // Jetty callback signature provides the status code even if this implementation does not use it
    public void onClose(Session session, int statusCode, String reason) {
        Runnable r = () -> {
            try {
                final WebSocketSession webSocketSession = sessions.get(session);
                sessions.remove(session);
                webSocketSessionPolicies.remove(session);
                log.debug("On Close websocket : {}", reason);
                sendClosingMessage(session, webSocketSession, reason);
                disposeSession(webSocketSession);
            } catch (Exception t) {
                log.error("Error while closing websocket: {}", t.getMessage(), t);
            }
        };
        onCloseDispatchThreads.execute(r);
    }

    private void sendClosingMessage(Session session, WebSocketSession webSocketSession, String reason) {
        if (!session.isOpen()) return;
        try {
            WebSocketMessage m = WebSocketMessage.createMessage(null, ("Closing websocket: " + reason).getBytes(StandardCharsets.UTF_8), WebSocketMessageType.DISCONNECTING);
            WriteCallback wc = new WriteCallback() {
                @Override
                public void writeFailed(Throwable x) {
                    log.warn("Error while sending message: {}", x.getMessage(), x);
                }

                @Override
                public void writeSuccess() {
                    log.debug("Close message sent!");
                }
            };
            //if websocket is a WebSocketAbstractSession it will use its own method for sending messages
            if (webSocketSession instanceof WebSocketAbstractSession abstractSession) {
                abstractSession.sendRemote(m, wc);
            } else {
                session.getRemote().sendString(m.toJson(), wc);
            }
        } catch (Exception e) {
            log.warn("Cannot send closing reason on websocket: {}", e.getMessage(), e);
        }
    }

    private void disposeSession(WebSocketSession webSocketSession) {
        if (webSocketSession == null) return;
        try {
            webSocketSession.dispose();
        } catch (Exception e) {
            log.warn("Error closing connection: {}", e.getMessage(), e);
        }
    }


    @OnWebSocketMessage
    @SuppressWarnings("java:S2259") // session lifecycle is managed asynchronously and the lookup may be cleared between validation and policy handling
    public void onMessage(Session session, String message) {
        Runnable r = () -> {
            try {
                if (session == null || !session.isOpen()) return;
                log.debug("On Message websocket getting session...");
                WebSocketSession webSocketSession = sessions.get(session);
                if (!isPolicySatisfied(webSocketSession, message)) return;
                long sessionFoundTime = System.nanoTime();
                if (webSocketSession != null) {
                    webSocketSession.onMessage(message);
                    log.debug("Message forwarded to session in {} seconds", ((double) System.nanoTime() - sessionFoundTime) / 1_000_000_000);
                }
            } catch (Exception e) {
                log.error("Error while forwarding message to websocket session: {}", e.getMessage(), e);
            }
        };
        onMessageDispatchThreads.execute(r);
    }

    private boolean isPolicySatisfied(WebSocketSession webSocketSession, String message) {
        if (!hasPoliciesForSession(webSocketSession)) return true;
        List<WebSocketPolicy> policies = webSocketSessionPolicies.get(webSocketSession.getSession());
        for (WebSocketPolicy policy : policies) {
            if (!policy.isSatisfied(webSocketSession.getPolicyParams(), message.getBytes(StandardCharsets.UTF_8))
                    && shouldStopOnPolicyViolation(policy, webSocketSession)) {
                return false;
            }
        }
        return true;
    }

    private boolean hasPoliciesForSession(WebSocketSession webSocketSession) {
        return webSocketSessionPolicies != null
                && !webSocketSessionPolicies.isEmpty()
                && webSocketSessionPolicies.containsKey(webSocketSession.getSession());
    }

    private boolean shouldStopOnPolicyViolation(WebSocketPolicy policy, WebSocketSession webSocketSession) {
        if (policy.printWarningOnFail()) {
            log.error("Policy {} not satisfied!", policy.getClass().getSimpleName());
        }
        if (policy.sendWarningBackToClientOnFail()) {
            sendPolicyWarning(policy, webSocketSession);
        }
        if (policy.closeWebSocketOnFail()) {
            webSocketSession.dispose();
            return true;
        }
        return policy.ignoreMessageOnFail();
    }

    private void sendPolicyWarning(WebSocketPolicy policy, WebSocketSession webSocketSession) {
        try {
            String policyWarning = "Policy " + policy.getClass().getSimpleName() + " Not satisfied!";
            WebSocketMessage m = WebSocketMessage.createMessage(null, policyWarning.getBytes(StandardCharsets.UTF_8), WebSocketMessageType.WEBSOCKET_POLICY_WARNING);
            webSocketSession.getSession().getRemote().sendString(m.toJson());
        } catch (Exception e) {
            log.warn("Could not send policy warning to client: {}", e.getMessage(), e);
        }
    }

    @OnWebSocketError
    public void onError(Session session, Throwable cause) {
        Runnable r = () -> {
            if (session == null)
                return;
            log.warn("On Web Socket Error: {}", cause.getMessage(), cause);
            try {
                log.debug("Trying close websocket on error: {}", cause.getMessage(), cause);
                WebSocketService.this.onClose(session, 500, cause.getCause() != null ? cause.getCause().getMessage() : cause.getMessage());
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        };
        onErrorDispatchThreads.execute(r);
    }

    /**
     * Find all registered WebSocketEndPoint components via Water ComponentRegistry.
     */
    private HashMap<String, WebSocketEndPoint> findWebSocketEndPoints() {
        HashMap<String, WebSocketEndPoint> endPointHashMap = new HashMap<>();
        try {
            List<WebSocketEndPoint> endPoints = componentRegistry.findComponents(WebSocketEndPoint.class, null);
            if (endPoints != null) {
                for (WebSocketEndPoint ep : endPoints) {
                    String endPointPath = (ep.getPath().startsWith("/")) ? ep.getPath().substring(1) : ep.getPath();
                    endPointHashMap.put(endPointPath, ep);
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return endPointHashMap;
    }

    /**
     * Apply custom policies to session
     *
     * @param s
     * @param policies
     */
    private void applyCustomPolicies(Session s, List<WebSocketPolicy> policies) {
        for (WebSocketPolicy policy : policies) {
            if (policy instanceof InputBufferSizePolicy ibsPolicy) {
                s.getPolicy().setInputBufferSize(ibsPolicy.getInputBufferSize());
            } else if (policy instanceof MaxBinaryMessageBufferSizePolicy mbmbsPolicy) {
                s.getPolicy().setMaxBinaryMessageBufferSize(mbmbsPolicy.getMaxBinaryMessageBufferSize());
            } else if (policy instanceof MaxBinaryMessageSizePolicy mbmsPolicy) {
                s.getPolicy().setMaxBinaryMessageSize(mbmsPolicy.getMaxBinaryMessageSize());
            } else if (policy instanceof MaxTextMessageBufferSizePolicy mtmbsPolicy) {
                s.getPolicy().setMaxTextMessageBufferSize(mtmbsPolicy.getMaxTextMessageBufferSize());
            } else if (policy instanceof MaxTextMessageSizePolicy mtmsPolicy) {
                s.getPolicy().setMaxTextMessageSize(mtmsPolicy.getMaxTextMessageSizePolicy());
            }
        }
    }

    public static Map<Session, WebSocketSession> getSessions() {
        return sessions;
    }

    public String getWebSocketServiceUrl() {
        return webSocketServiceUrl;
    }

    /**
     * Build a named thread factory for thread pools.
     */
    private ThreadFactory buildThreadFactory(String namePattern) {
        AtomicInteger counter = new AtomicInteger(0);
        return r -> {
            Thread t = new Thread(r);
            t.setName(String.format(namePattern, counter.getAndIncrement()));
            t.setDaemon(false);
            return t;
        };
    }

    /**
     * Get an integer property or default value from ApplicationProperties.
     */
    private int getPropertyOrDefault(String key, int defaultValue) {
        if (applicationProperties != null) {
            try {
                Object value = applicationProperties.getProperty(key);
                if (value != null) {
                    return Integer.parseInt(value.toString());
                }
            } catch (Exception e) {
                log.debug("Property {} not found or invalid, using default: {}", key, defaultValue);
            }
        }
        return defaultValue;
    }

    /**
     * Get a string property or default value from ApplicationProperties.
     */
    private String getPropertyOrDefault(String key, String defaultValue) {
        if (applicationProperties != null) {
            try {
                Object value = applicationProperties.getProperty(key);
                if (value != null) {
                    return value.toString();
                }
            } catch (Exception e) {
                log.debug("Property {} not found, using default: {}", key, defaultValue);
            }
        }
        return defaultValue;
    }
}
