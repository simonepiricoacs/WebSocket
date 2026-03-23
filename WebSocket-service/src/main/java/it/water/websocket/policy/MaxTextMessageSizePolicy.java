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

import java.util.Map;
import java.util.Objects;

public class MaxTextMessageSizePolicy extends WebSocketAbstractPolicy {
    private int maxTextMessageSize;

    public MaxTextMessageSizePolicy(Session s, int maxTextMessageSize) {
        super(s);
        this.maxTextMessageSize = maxTextMessageSize;
    }

    public int getMaxTextMessageSizePolicy() {
        return maxTextMessageSize;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MaxTextMessageSizePolicy that)) return false;
        if (!super.equals(o)) return false;
        return maxTextMessageSize == that.maxTextMessageSize;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), maxTextMessageSize);
    }

    @Override
    public boolean isSatisfied(Map<String, Object> params, byte[] payload) {
        return true;
    }

    @Override
    public boolean closeWebSocketOnFail() {
        return false;
    }

    @Override
    public boolean printWarningOnFail() {
        return false;
    }

    @Override
    public boolean sendWarningBackToClientOnFail() {
        return false;
    }

    @Override
    public boolean ignoreMessageOnFail() {
        return false;
    }
}
