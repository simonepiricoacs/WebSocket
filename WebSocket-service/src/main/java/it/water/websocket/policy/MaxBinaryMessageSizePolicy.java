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

public class MaxBinaryMessageSizePolicy extends WebSocketAbstractPolicy {
    private int maxBinaryMessageSize;

    public MaxBinaryMessageSizePolicy(Session s, int maxBinaryMessageSize) {
        super(s);
        this.maxBinaryMessageSize = maxBinaryMessageSize;
    }

    public int getMaxBinaryMessageSize() {
        return maxBinaryMessageSize;
    }

    @Override
    public boolean equals(Object o) {
        if (!super.equals(o)) return false;
        MaxBinaryMessageSizePolicy that = (MaxBinaryMessageSizePolicy) o;
        return maxBinaryMessageSize == that.maxBinaryMessageSize;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), maxBinaryMessageSize);
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
