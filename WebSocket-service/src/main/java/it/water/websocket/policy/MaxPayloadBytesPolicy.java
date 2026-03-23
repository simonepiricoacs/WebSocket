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

package it.water.websocket.policy;

import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;

public class MaxPayloadBytesPolicy extends WebSocketAbstractPolicy {
    private static final Logger log = LoggerFactory.getLogger(MaxPayloadBytesPolicy.class);
    private int maxPayloadBytes;


    public MaxPayloadBytesPolicy(Session s, int maxPayloadBytes) {
        super(s);
        this.maxPayloadBytes = maxPayloadBytes;
    }

    @Override
    public boolean closeWebSocketOnFail() {
        return false;
    }

    @Override
    public boolean printWarningOnFail() {
        return true;
    }

    @Override
    public boolean sendWarningBackToClientOnFail() {
        return true;
    }

    @Override
    public boolean ignoreMessageOnFail() {
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MaxPayloadBytesPolicy that)) return false;
        if (!super.equals(o)) return false;
        return maxPayloadBytes == that.maxPayloadBytes;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), maxPayloadBytes);
    }

    @Override
    public boolean isSatisfied(Map<String, Object> params, byte[] payload) {
        log.debug("Policy Max Payload bytes, current payload is: {}, max is {}", payload.length, maxPayloadBytes);
        return payload.length <= maxPayloadBytes;
    }
}
