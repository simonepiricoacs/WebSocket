# WebSocket Module — Real-time WebSocket Communication

## Purpose
Provides a generic WebSocket infrastructure for Water Framework applications. Supports endpoint registration, authentication-aware sessions, multi-user channels with role-based permissions, message policies, and optional end-to-end encryption (RSA+AES, symmetric, or mixed modes). Cluster-aware: messages are routed across nodes via `WebSocketChannelClusterMessageBroker`. Does NOT use JPA entities — no database persistence. Does NOT define application-specific commands — those are implemented by consumers of this module.

## Sub-modules

| Sub-module | Runtime | Key Classes |
|---|---|---|
| `WebSocket-api` | All | `WebSocketEndPoint`, `WebSocketSession`, `WebSocketChannel`, `WebSocketChannelManager`, `WebSocketMessage`, `WebSocketUserInfo`, `WebSocketPolicy`, `WebSocketCommand`, `WebSocketChannelRole`, `WebSocketAction` |
| `WebSocket-service` | Water/OSGi | Channel manager impl, encryption modes, role managers, command factories, session handlers |

## Core Models

### WebSocketMessage
```java
public class WebSocketMessage {
    private String cmd;                         // Command name
    private byte[] payload;                     // Binary payload
    private String contentType;                 // Default: "text/plain"
    private Date timestamp;
    private WebSocketMessageType type;
    private HashMap<String, String> params;

    static WebSocketMessage createMessage(String cmd, byte[] payload, WebSocketMessageType type);
    static WebSocketMessage fromString(String message);  // JSON deserialization
}
```

### WebSocketUserInfo
```java
public class WebSocketUserInfo implements Serializable {
    private String username;
    private ClusterNodeInfo clusterNodeInfo;     // null for single-node deployments
    private String ipAddress;

    // Factory methods
    static WebSocketUserInfo fromSession(String username, ClusterNodeInfo clusterNodeInfo, Session session);
    static WebSocketUserInfo fromSession(String username, Session session);
    static WebSocketUserInfo anonymous(Session session);

    boolean isOnLocalNode(ClusterNodeInfo localNodeInfo);
}
```

### WebSocket Constants
```java
WebSocketConstants.WEB_SOCKET_USERNAME_PARAM     = "username"
WebSocketConstants.WEB_SOCKET_RECIPIENT_USER_PARAM = "recipient"
WebSocketConstants.CLIENT_PUB_KEY_HEADER         = "X-WATER-CLIENT-PUB-KEY"
WebSocketConstants.CLIENT_PUB_KEY_QUERY_PARAM    = "water-client-pub-key"
WebSocketConstants.AUTHORIZATION_COOKIE_NAME     = "water-auth-token"
```

## Key Interfaces

### WebSocketEndPoint
```java
public interface WebSocketEndPoint {
    String getPath();                                // e.g., "/ws/chat"
    WebSocketSession getHandler(Session session);   // Factory: new session handler per connection
    Executor getExecutorForOpenConnections(Session s); // Optional custom executor
}
```

### WebSocketSession
```java
public interface WebSocketSession {
    boolean isAuthenticationRequired();
    void authenticate();                    // Extract JWT from cookie or header
    void authenticateAnonymous();          // Track anonymous connections
    Session getSession();                  // Jetty WebSocket session
    void initialize();                     // Custom setup on connection open
    void onMessage(String message);        // Handle incoming message
    void dispose();                        // Cleanup on connection close
    void sendRemote(WebSocketMessage msg); // Send message to this client
    void close(String message);
    WebSocketUserInfo getUserInfo();
    Map<String, Object> getPolicyParams();      // Parameters for policy evaluation
    List<WebSocketPolicy> getWebSocketPolicies(); // Policies to enforce on messages
}
```

### WebSocketPolicy
```java
public interface WebSocketPolicy {
    boolean isSatisfied(Map<String, Object> params, byte[] payload);
    default boolean closeWebSocketOnFail()          { return true; }
    default boolean printWarningOnFail()            { return true; }
    default boolean sendWarningBackToClientOnFail() { return false; }
    default boolean ignoreMessageOnFail()           { return false; }
}
```

### WebSocketChannel
```java
public interface WebSocketChannel {
    String getChannelId();
    String getChannelName();

    // Participant lifecycle
    void addPartecipantInfo(WebSocketUserInfo info, Set<WebSocketChannelRole> roles);
    void addPartecipantSession(WebSocketUserInfo info, Set<WebSocketChannelRole> roles, WebSocketChannelSession session);
    void leaveChannel(WebSocketUserInfo participantInfo);
    WebSocketSession getPartecipantSession(WebSocketUserInfo info);

    // Moderation
    void kickPartecipant(WebSocketUserInfo kickerInfo, WebSocketMessage kickCommand);
    void banPartecipant(WebSocketUserInfo banner, WebSocketMessage banCommand);
    void unbanPartecipant(WebSocketUserInfo banner, WebSocketMessage unbanCommand);

    // Messaging
    boolean userHasPermission(WebSocketUserInfo user, WebSocketCommand command);
    void exchangeMessage(WebSocketChannelSession senderSession, WebSocketMessage message);
    void deliverMessage(WebSocketUserInfo sender, WebSocketMessage message);

    // Cluster
    Set<ClusterNodeInfo> getPeers();
    void defineClusterMessageBroker(WebSocketChannelClusterMessageBroker broker);
}
```

### WebSocketChannelManager
```java
public interface WebSocketChannelManager {
    WebSocketChannel findChannel(String channelId);
    boolean channelExists(String channelId);
    Collection<WebSocketChannel> getAvailableChannels();

    void createChannel(String channelType, String channelName, String newChannelId,
                       int maxPartecipants, Map<String, Object> params,
                       WebSocketChannelSession ownerSession, Set<WebSocketChannelRole> roles);

    void joinChannel(String channelId, WebSocketChannelSession participantSession, Set<WebSocketChannelRole> roles);
    void leaveChannel(String channelId, WebSocketChannelSession participantSession);
    void deleteChannel(WebSocketUserInfo userInfo, WebSocketChannel channel);

    void deliverMessage(WebSocketMessage message);
    void forwardMessage(String channelId, WebSocketMessage message);

    // Lifecycle callbacks
    void onChannelAdded(WebSocketChannel channel);
    void onChannelRemoved(String channelId);
    void onPartecipantAdded(String channelId, WebSocketUserInfo info, Set<WebSocketChannelRole> roles);
    void onPartecipantGone(String channelId, WebSocketUserInfo info);
}
```

## Permission — WebSocketAction

```java
public enum WebSocketAction implements Action {
    CREATE_CHANNEL("CREATE_CHANNEL", 1);
}
```

Only `CREATE_CHANNEL` is defined. Channel-level command permissions use `WebSocketChannelRole.getAllowedCmds()`.

## Encryption Modes (in service module)

| Mode | Class | Description |
|---|---|---|
| RSA + AES | `RSAWithAESEncryptionMode` | Client sends RSA public key; server uses it to encrypt AES session key |
| Symmetric | `WebSocketSymmetricKeyEncryptionMode` | Shared symmetric key between client and server |
| Mixed | `WebSocketMixedEncryptionMode` | Hybrid of RSA and symmetric |

Client public key delivered via:
- HTTP header: `X-WATER-CLIENT-PUB-KEY`
- Query parameter: `water-client-pub-key`

## Typical Usage — Implementing a Custom WebSocket Endpoint

```java
@FrameworkComponent
public class MyEndPoint implements WebSocketEndPoint {
    @Override
    public String getPath() { return "/ws/myapp"; }

    @Override
    public WebSocketSession getHandler(Session session) {
        return new MySession(session, componentRegistry);
    }
}

public class MySession implements WebSocketSession {
    @Override
    public boolean isAuthenticationRequired() { return true; }

    @Override
    public void authenticate() {
        // Read JWT from AUTHORIZATION_COOKIE_NAME cookie or Bearer header
        // Set user info via getUserInfo()
    }

    @Override
    public void onMessage(String message) {
        WebSocketMessage msg = WebSocketMessage.fromString(message);
        // Dispatch by msg.getCmd()
    }
}
```

## Dependencies
- `it.water.core:Core-api` — `@FrameworkComponent`, `@Inject`, `ComponentRegistry`
- `it.water.core:Core-permission` — `@AccessControl`, `WebSocketAction`
- `it.water.core:Core-security` — `SecurityContext`, `EncryptionUtil`
- `org.eclipse.jetty.websocket:websocket-server` — Jetty WebSocket server API
- `org.eclipse.jetty.websocket:websocket-api` — `Session`, `RemoteEndpoint`

## Testing
- Unit tests: mock `WebSocketSession` and test policy evaluation, channel operations
- For integration tests: use Jetty `WebSocketClient` (embedded) to connect to a test server
- REST tests: not applicable — WebSocket is not HTTP
- Channel tests: verify `userHasPermission()` before `exchangeMessage()`

## Code Generation Rules
- `WebSocketEndPoint` implementations must be `@FrameworkComponent` — the framework registers them with the Jetty container automatically
- NEVER store user credentials in `WebSocketUserInfo` — only username and cluster node info
- `WebSocketSession.authenticate()` must handle missing/expired JWT gracefully — close connection with appropriate code
- Policy failures: respect `closeWebSocketOnFail()` / `ignoreMessageOnFail()` return values — don't hardcode behavior
- For cluster deployments: set `clusterNodeInfo` in `WebSocketUserInfo` and define a `WebSocketChannelClusterMessageBroker` to route messages
- RSA encryption: client sends public key as base64 in `CLIENT_PUB_KEY_HEADER` header on WebSocket upgrade request
- All channel command authorization: check `userHasPermission()` before processing any channel command
