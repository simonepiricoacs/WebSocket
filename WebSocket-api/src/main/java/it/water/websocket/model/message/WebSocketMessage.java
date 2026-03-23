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

package it.water.websocket.model.message;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WebSocketMessage {
    public static final String WS_MESSAGE_SENDER_PARAM_NAME = "sender";

    @JsonIgnore
    private static final Logger log = LoggerFactory.getLogger(WebSocketMessage.class);
    @JsonIgnore
    private static final ObjectMapper mapper = new ObjectMapper();

    private String cmd;
    private byte[] payload;
    private String contentType;
    private Date timestamp;
    private WebSocketMessageType type;
    private Map<String, String> params;

    static {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public WebSocketMessage() {
        this.params = new HashMap<>();
        this.contentType = "text/plain";
    }

    public Map<String, String> getParams() {
        return params;
    }

    public void setParams(Map<String, String> params) {
        this.params = params;
    }

    public String getCmd() {
        return cmd;
    }

    public void setCmd(String cmd) {
        this.cmd = cmd;
    }

    public byte[] getPayload() {
        return payload;
    }

    public void setPayload(byte[] payload) {
        this.payload = payload;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public WebSocketMessageType getType() {
        return type;
    }

    public void setType(WebSocketMessageType type) {
        this.type = type;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public static WebSocketMessage createMessage(String cmd, byte[] payload, WebSocketMessageType type) {
        WebSocketMessage m = new WebSocketMessage();
        m.setTimestamp(new Date());
        m.setCmd(cmd);
        m.setPayload(payload);
        m.setType(type);
        return m;
    }

    public static WebSocketMessage fromString(String message) {
        if (message == null) return null;
        try {
            return mapper.readValue(message, WebSocketMessage.class);
        } catch (JsonProcessingException t) {
            log.debug("Error while parsing websocket message: {}", t.getMessage(), t);
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
