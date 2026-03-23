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

package it.water.websocket.api;

import it.water.websocket.model.WebSocketUserInfo;
import it.water.websocket.model.message.WebSocketMessage;
import org.eclipse.jetty.websocket.api.Session;

import java.util.Collections;
import java.util.List;
import java.util.Map;


public interface WebSocketSession {

    /**
     * @return true if this websocket requires authentication
     */
    boolean isAuthenticationRequired();

    /**
     * Method which implements authentication for websocket
     */
    void authenticate();

    /**
     * Method which defines how anonymous users should be tracked
     */
    void authenticateAnonymous();

    /**
     * @return WebSocket Session
     */
    Session getSession();

    /**
     * Use this method to insert custom initialization code, on websocket open event
     */
    void initialize();

    /**
     * @param message
     */
    void onMessage(String message);

    /**
     *
     */
    void dispose();

    /**
     *
     */
    void sendRemote(WebSocketMessage m);

    /**
     * @param message
     */
    void close(String message);

    /**
     * @return
     */
    WebSocketUserInfo getUserInfo();

    /**
     * @return Web Socket Policy Params to check, or null if no custom params are defined
     */
    @SuppressWarnings("java:S1168") // null intentionally indicates no custom policy params; implementations may override to return an empty map
    default Map<String, Object> getPolicyParams() {
        return null;
    }

    /**
     * @return The Policy List for the created websocket instance
     */
    default List<WebSocketPolicy> getWebScoketPolicies() {
        return Collections.emptyList();
    }
}
