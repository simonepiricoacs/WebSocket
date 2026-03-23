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

package it.water.websocket.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.water.core.api.service.cluster.ClusterNodeInfo;
import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class WebSocketUserInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    @JsonIgnore
    private static final Logger log = LoggerFactory.getLogger(WebSocketUserInfo.class);

    @JsonIgnore
    private static final ObjectMapper mapper = new ObjectMapper();

    private String username;
    @SuppressWarnings("java:S1948") // cluster metadata is carried by runtime implementations behind the interface
    private ClusterNodeInfo clusterNodeInfo;

    private String ipAddress;

    public WebSocketUserInfo(String username, ClusterNodeInfo clusterNodeInfo, String ipAddress) {
        this.username = username;
        this.clusterNodeInfo = clusterNodeInfo;
        this.ipAddress = ipAddress;
    }

    private WebSocketUserInfo() {
    }

    public String getUsername() {
        return username;
    }


    public ClusterNodeInfo getClusterNodeInfo() {
        return clusterNodeInfo;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WebSocketUserInfo that = (WebSocketUserInfo) o;
        return username.equals(that.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username);
    }

    public static WebSocketUserInfo fromSession(String username, ClusterNodeInfo clusterNodeInfo, Session session) {
        String ipAddress = session.getRemote().getInetSocketAddress().getAddress().getHostAddress();
        return new WebSocketUserInfo(username, clusterNodeInfo, ipAddress);
    }

    public static WebSocketUserInfo fromSession(ClusterNodeInfo clusterNodeInfo, Session session) {
        return fromSession(session.getUpgradeRequest().getUserPrincipal().getName(), clusterNodeInfo, session);
    }

    public static WebSocketUserInfo anonymous(ClusterNodeInfo clusterNodeInfo, Session session) {
        return fromSession("anonymous-" + UUID.randomUUID().toString(), clusterNodeInfo, session);
    }

    /**
     * Creates a WebSocketUserInfo from a username and session, without cluster info.
     * Use this when cluster mode is not active.
     */
    public static WebSocketUserInfo fromSession(String username, Session session) {
        return fromSession(username, null, session);
    }

    /**
     * Creates an anonymous WebSocketUserInfo from a session, without cluster info.
     * Use this when cluster mode is not active.
     */
    public static WebSocketUserInfo anonymous(Session session) {
        return fromSession("anonymous-" + UUID.randomUUID().toString(), null, session);
    }

    /**
     * Checks if the user's cluster node is the local node.
     * If clusterNodeInfo is null (single-node mode), returns true.
     *
     * @param localNodeInfo the local node's ClusterNodeInfo (can be null)
     * @return true if the user is on the local node or cluster is not active
     */
    public boolean isOnLocalNode(ClusterNodeInfo localNodeInfo) {
        if (this.clusterNodeInfo == null || localNodeInfo == null) {
            return true; // no cluster mode, everything is local
        }
        return Objects.equals(this.clusterNodeInfo.getNodeId(), localNodeInfo.getNodeId());
    }

    public static WebSocketUserInfo fromString(String message) {
        if (message == null) return null;
        try {
            return mapper.readValue(message, WebSocketUserInfo.class);
        } catch (JsonProcessingException t) {
            log.debug("Error while parsing websocket user info: {}", t.getMessage(), t);
        }
        return null;
    }

    public String toJson() {
        try {
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException t) {
            log.error(t.getMessage(), t);
        }
        return "{}";
    }
}
