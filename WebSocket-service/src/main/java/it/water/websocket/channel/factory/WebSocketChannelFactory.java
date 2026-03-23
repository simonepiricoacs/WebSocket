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

package it.water.websocket.channel.factory;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.water.core.model.exceptions.WaterRuntimeException;
import it.water.websocket.api.channel.WebSocketChannel;
import it.water.websocket.api.channel.WebSocketChannelClusterMessageBroker;
import it.water.websocket.channel.WebSocketBasicChannel;
import it.water.websocket.channel.WebSocketChannelType;
import it.water.websocket.channel.encryption.WebSocketRSAWithAESEncryptedBasicChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

public class WebSocketChannelFactory {
    private WebSocketChannelFactory() {
        throw new UnsupportedOperationException();
    }

    private static final Logger log = LoggerFactory.getLogger(WebSocketChannelFactory.class);

    private static final ObjectMapper mapper = new ObjectMapper();

    public static WebSocketChannel createChannelFromChannelType(String channelType, String channelId, String channelName, int maxPartecipants, Map<String, Object> params, WebSocketChannelClusterMessageBroker clusterMessageBroker) {
        WebSocketChannelType type = WebSocketChannelType.valueOf(channelType);
        switch (type) {
            case PLAIN:
                return new WebSocketBasicChannel(channelName, channelId, maxPartecipants, params, clusterMessageBroker);
            case ENCRYPTED_RSA_WITH_AES:
                return new WebSocketRSAWithAESEncryptedBasicChannel(channelName, channelId, maxPartecipants, params, clusterMessageBroker);
            default:
                throw new WaterRuntimeException("Invalid channel type");
        }
    }

    public static WebSocketChannel createChannelFromClass(Class<? extends WebSocketChannel> classType, String channelId, String channelName, int maxPartecipants, Map<String, Object> params, WebSocketChannelClusterMessageBroker clusterMessageBroker) throws InvocationTargetException, InstantiationException, IllegalAccessException {
        return (WebSocketChannel) classType.getDeclaredConstructors()[0].newInstance(channelId, channelName, maxPartecipants, params, clusterMessageBroker);
    }

    public static WebSocketChannel createFromString(String channelJson, String classNameStr, WebSocketChannelClusterMessageBroker clusterMessageBroker) {
        try {
            Class<? extends WebSocketChannel> channelClassType = (Class<? extends WebSocketChannel>) Class.forName(classNameStr);
            WebSocketChannel channel = mapper.readValue(channelJson, channelClassType);
            channel.defineClusterMessageBroker(clusterMessageBroker);
            return channel;
        } catch (Exception t) {
            log.debug("Error while parsing websocket channel: {}", t.getMessage(), t);
        }
        return null;
    }
}
