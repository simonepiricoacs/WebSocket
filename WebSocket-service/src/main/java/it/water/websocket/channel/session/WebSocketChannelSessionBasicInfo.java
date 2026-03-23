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

package it.water.websocket.channel.session;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.water.websocket.api.channel.WebSocketChannel;
import it.water.websocket.api.channel.WebSocketChannelRole;
import it.water.websocket.api.channel.WebSocketChannelSessionInfo;
import it.water.websocket.model.WebSocketUserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Set;

public class WebSocketChannelSessionBasicInfo implements WebSocketChannelSessionInfo {
    @JsonIgnore
    private static final Logger log = LoggerFactory.getLogger(WebSocketChannelSessionBasicInfo.class);

    @JsonIgnore
    private static final ObjectMapper mapper = new ObjectMapper();

    private WebSocketUserInfo userInfo;
    private WebSocketChannel channel;
    private Set<WebSocketChannelRole> roles;

    public WebSocketChannelSessionBasicInfo(WebSocketUserInfo userInfo, WebSocketChannel channel, Set<WebSocketChannelRole> roles) {
        this.userInfo = userInfo;
        this.channel = channel;
        this.roles = roles;
    }

    public WebSocketUserInfo getUserInfo() {
        return userInfo;
    }

    public WebSocketChannel getChannel() {
        return channel;
    }

    public Set<WebSocketChannelRole> getRoles() {
        return roles;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WebSocketChannelSessionBasicInfo that = (WebSocketChannelSessionBasicInfo) o;
        return userInfo.equals(that.userInfo) && channel.equals(that.channel) && roles.equals(that.roles);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userInfo, channel, roles);
    }

    public static WebSocketChannelSessionBasicInfo fromString(String message) {
        if (message == null) return null;
        try {
            return mapper.readValue(message, WebSocketChannelSessionBasicInfo.class);
        } catch (Exception t) {
            log.debug("Error while parsing websocket channel user info: {}", t.getMessage(), t);
        }
        return null;
    }

    public String toJson() {
        try {
            return mapper.writeValueAsString(this);
        } catch (Exception t) {
            log.error(t.getMessage(), t);
        }
        return "{}";
    }
}
