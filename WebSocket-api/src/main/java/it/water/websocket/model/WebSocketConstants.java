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

public class WebSocketConstants {
    private WebSocketConstants() {
        throw new UnsupportedOperationException();
    }

    public static final String WEB_SOCKET_USERNAME_PARAM = "username";
    public static final String WEB_SOCKET_RECIPIENT_USER_PARAM = "recipient";

    public static final String CHANNEL_CLUSTER_COORDINATOR_OSGI_FILTER_NAME = "it.water.websocket.channel.cluster.coordinator.type";
    public static final String CHANNEL_CLUSTER_MESSAGE_BROKER_OSGI_FILTER_NAME = "it.water.websocket.channel.cluster.message.broker.type";

    /**
     * Header name for client public key in WebSocket encryption handshake.
     */
    public static final String CLIENT_PUB_KEY_HEADER = "X-WATER-CLIENT-PUB-KEY";

    /**
     * Query parameter name for client public key (fallback for JavaScript WebSocket API
     * which does not support custom headers).
     */
    public static final String CLIENT_PUB_KEY_QUERY_PARAM = "water-client-pub-key";

    /**
     * Cookie name for authorization token in WebSocket upgrade requests.
     */
    public static final String AUTHORIZATION_COOKIE_NAME = "water-auth-token";
}
