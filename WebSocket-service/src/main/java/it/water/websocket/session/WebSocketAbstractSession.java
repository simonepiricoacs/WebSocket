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

package it.water.websocket.session;

import it.water.core.api.registry.ComponentRegistry;
import it.water.core.api.security.AuthenticationProvider;
import it.water.service.rest.api.security.jwt.JwtTokenService;
import it.water.websocket.api.WebSocketSession;
import it.water.websocket.compression.WebSocketCompression;
import it.water.websocket.encryption.WebSocketEncryption;
import it.water.websocket.model.WebSocketConstants;
import it.water.websocket.model.WebSocketUserInfo;
import it.water.websocket.model.message.WebSocketMessage;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WriteCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.HttpCookie;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Author Generoso Martello
 * This class implements the concept of a Web Socket Session
 */
public abstract class WebSocketAbstractSession implements WebSocketSession {
    private static Logger log = LoggerFactory.getLogger(WebSocketAbstractSession.class);

    private Session session;
    private Set<Principal> principals;
    private String loggedUsername;
    private ComponentRegistry componentRegistry;
    private boolean authenticationRequired;
    private WebSocketMessageBroker messageBroker;
    private WebSocketUserInfo userInfo;

    private JwtTokenService jwtTokenService;

    /**
     * @param session
     * @param authenticationRequired
     * @param componentRegistry
     */
    public WebSocketAbstractSession(Session session, boolean authenticationRequired, ComponentRegistry componentRegistry) {
        log.debug("Creating websocket session....");
        this.session = session;
        this.componentRegistry = componentRegistry;
        this.authenticationRequired = authenticationRequired;
        this.initMessageBroker(session, null, null);
    }

    /**
     * @param session
     * @param authenticated
     * @param encryptionPolicy
     * @param componentRegistry
     */
    public WebSocketAbstractSession(Session session, boolean authenticated, WebSocketEncryption encryptionPolicy, ComponentRegistry componentRegistry) {
        this(session, authenticated, componentRegistry);
        initMessageBroker(session, encryptionPolicy, null);
    }

    /**
     * @param session
     * @param authenticated
     * @param compressionPolicy
     * @param componentRegistry
     */
    public WebSocketAbstractSession(Session session, boolean authenticated, WebSocketCompression compressionPolicy, ComponentRegistry componentRegistry) {
        this(session, authenticated, componentRegistry);
        initMessageBroker(session, null, compressionPolicy);
    }

    /**
     * @param session
     * @param authenticated
     * @param encryptionPolicy
     * @param compressionPolicy
     * @param componentRegistry
     */
    public WebSocketAbstractSession(Session session, boolean authenticated, WebSocketEncryption encryptionPolicy, WebSocketCompression compressionPolicy, ComponentRegistry componentRegistry) {
        this(session, authenticated, componentRegistry);
        initMessageBroker(session, encryptionPolicy, compressionPolicy);
    }

    /**
     * @param s
     * @param encryptionPolicy
     * @param compressionPolicy
     */
    private void initMessageBroker(Session s, WebSocketEncryption encryptionPolicy, WebSocketCompression compressionPolicy) {
        this.messageBroker = new WebSocketMessageBroker(s);
        this.messageBroker.setEncryptionPolicy(encryptionPolicy);
        this.messageBroker.setCompressionPolicy(compressionPolicy);
        this.messageBroker.onOpenSession(s);
    }

    /**
     * @return
     */
    public Session getSession() {
        return session;
    }

    /**
     * @return
     */
    protected WebSocketMessageBroker getMessageBroker() {
        return messageBroker;
    }

    /**
     * @return
     */
    protected ComponentRegistry getComponentRegistry() {
        return componentRegistry;
    }

    /**
     * @return the set of principals from JWT authentication
     */
    protected Set<Principal> getPrincipals() {
        return principals;
    }

    /**
     * @return the logged username extracted from JWT
     */
    protected String getLoggedUsername() {
        return loggedUsername;
    }

    /**
     *
     */
    public void dispose() {
        this.getMessageBroker().onCloseSession(this.getSession());
        try {
            session.close();
        } catch (Throwable t) {
            log.error(t.getMessage(), t);
        }
    }

    /**
     * @return
     */
    public boolean isAuthenticated() {
        return this.authenticationRequired && this.loggedUsername != null && !this.loggedUsername.isEmpty();
    }

    /**
     * @return
     */
    public boolean isAuthenticationRequired() {
        return authenticationRequired;
    }

    /**
     * @param m
     */
    public void sendRemote(WebSocketMessage m) {
        this.sendRemote(m, true, null);
    }

    /**
     * @param m
     * @param callback
     */
    public void sendRemote(WebSocketMessage m, WriteCallback callback) {
        this.sendRemote(m, true, callback);
    }

    /**
     * @param message
     * @param callback
     */
    public void sendRemote(String message, WriteCallback callback) {
        this.sendRemote(message, true, callback);
    }

    /**
     * @param message
     */
    public void sendRemote(String message) {
        this.sendRemote(message, true);
    }

    /**
     * @param m
     */
    public void sendRemote(WebSocketMessage m, boolean encodeBase64) {
        this.messageBroker.sendAsync(m, encodeBase64, null);
    }

    /**
     * @param m
     * @param callback
     */
    public void sendRemote(WebSocketMessage m, boolean encodeBase64, WriteCallback callback) {
        this.messageBroker.sendAsync(m, encodeBase64, callback);
    }

    /**
     * @param message
     * @param callback
     */
    public void sendRemote(String message, boolean encodeBase64, WriteCallback callback) {
        this.messageBroker.sendAsync(message.getBytes(), encodeBase64, callback);
    }

    /**
     * @param message
     */
    public void sendRemote(String message, boolean encodeBase64) {
        this.messageBroker.sendAsync(message.getBytes(), encodeBase64, null);
    }

    public final void authenticate() {
        preAuthenticate(session);
        String username = doAuthenticate();
        this.userInfo = WebSocketUserInfo.fromSession(username, session);
        postAuthenticate(session);
    }

    @Override
    public void authenticateAnonymous() {
        this.userInfo = WebSocketUserInfo.anonymous(session);
    }

    /**
     * Performs JWT authentication using Water's JwtTokenService.
     * Extracts JWT from cookies or Authorization header, validates it,
     * and extracts principals.
     */
    protected String doAuthenticate() {
        try {
            if (this.jwtTokenService == null && this.componentRegistry != null) {
                this.jwtTokenService = componentRegistry.findComponent(JwtTokenService.class, null);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        String jwtToken = null;

        log.debug("Checking Auth token in cookies or headers");
        if (session.getUpgradeRequest()
                .getCookies() != null && session.getUpgradeRequest()
                .getCookies().size() > 0) {
            log.debug("Token found in cookies");
            HttpCookie cookie = session.getUpgradeRequest()
                    .getCookies()
                    .stream()
                    .filter((c) -> c.getName().equals(WebSocketConstants.AUTHORIZATION_COOKIE_NAME))
                    .findAny().orElse(null);

            if (cookie != null) {
                log.debug("Cookie found, checking authentication...");
                jwtToken = cookie.getValue();
            }
        } else if (session.getUpgradeRequest().getHeader("Authorization") != null) {
            log.debug("token found in header");
            String authHeader = session.getUpgradeRequest().getHeader("Authorization");
            // Support both "Bearer <token>" and "JWT <token>" formats
            if (authHeader.startsWith("Bearer ")) {
                jwtToken = authHeader.substring(7);
            } else if (authHeader.startsWith("JWT ")) {
                jwtToken = authHeader.substring(4);
            } else {
                jwtToken = authHeader;
            }
        }

        if (jwtToken != null && jwtTokenService != null) {
            // Load valid issuers from all registered AuthenticationProviders
            List<String> issuers = loadValidIssuers();
            if (jwtTokenService.validateToken(issuers, jwtToken)) {
                this.principals = jwtTokenService.getPrincipals(jwtToken);
                if (this.principals != null && !this.principals.isEmpty()) {
                    // Extract username from the first principal
                    this.loggedUsername = this.principals.iterator().next().getName();
                }
            }
        }

        if (this.loggedUsername == null || this.loggedUsername.isEmpty()) {
            log.debug("User not authorized to connect to websocket");
            //Closes the connection if the client is not authenticated and authentication is required
            if (this.isAuthenticationRequired()) {
                try {
                    session.getRemote().sendString("Client not authenticated!", new WriteCallback() {
                        @Override
                        public void writeFailed(Throwable x) {
                            log.warn("Error while sending message: {}", new Object[]{x});
                        }

                        @Override
                        public void writeSuccess() {
                            log.debug("Send message success!");
                        }
                    });
                    session.close(1008, "Client not authenticated!");
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
            return null;
        }
        return this.loggedUsername;
    }

    @Override
    public WebSocketUserInfo getUserInfo() {
        return this.userInfo;
    }

    private List<String> loadValidIssuers() {
        List<String> issuers = new ArrayList<>();
        try {
            if (this.componentRegistry != null) {
                List<AuthenticationProvider> providers = componentRegistry.findComponents(AuthenticationProvider.class, null);
                if (providers != null) {
                    issuers = providers.stream()
                            .flatMap(p -> p.issuersNames().stream())
                            .collect(Collectors.toList());
                }
            }
        } catch (Exception e) {
            log.warn("Could not load authentication providers for JWT issuer validation: {}", e.getMessage());
        }
        return issuers;
    }

    protected void preAuthenticate(Session s) {
        //Do nothing
    }

    protected void postAuthenticate(Session s) {
        //Do nothing
    }


    @Override
    public abstract void initialize();

    /**
     * @param message
     */
    @Override
    public abstract void onMessage(String message);
}
