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

package it.water.websocket.api.channel;

import it.water.websocket.api.WebSocketSession;
import it.water.websocket.model.WebSocketUserInfo;

import java.util.Map;
import java.util.Set;

public interface WebSocketChannelSession extends WebSocketSession {

    void addSessionParam(String name, Object value);

    Object getSessionParam(String name);

    void removeSessionParam(String name);

    void updateEncryptionPolicyParams(Map<String, Object> params);

    void close(String message);

    WebSocketUserInfo getUserInfo();

    void processMessage(byte[] rawMessage);

    void addJoinedChannels(WebSocketChannel channel);

    void removeJoinedChannels(WebSocketChannel channel);

    void emptyJoinedChannels(WebSocketChannel channel);

    Set<WebSocketChannel> getJoinedChannels();
}
