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

package it.water.websocket.channel.manager;

import it.water.core.api.registry.ComponentRegistry;
import it.water.core.api.registry.filter.ComponentFilter;
import it.water.core.api.registry.filter.ComponentFilterBuilder;
import it.water.core.model.exceptions.WaterRuntimeException;
import it.water.websocket.api.channel.WebSocketChannel;
import it.water.websocket.api.channel.WebSocketChannelClusterCoordinator;
import it.water.websocket.api.channel.WebSocketChannelClusterMessageBroker;
import it.water.websocket.api.channel.WebSocketChannelManager;
import it.water.websocket.model.WebSocketConstants;

import java.util.List;

public class WebSocketChannelManagerFactory {
    private WebSocketChannelClusterCoordinator coordinator;
    private WebSocketChannelClusterMessageBroker messageBroker;

    public WebSocketChannelManagerFactory withClusterCoordinator(WebSocketChannelClusterCoordinator coordinator) {
        this.coordinator = coordinator;
        return this;
    }

    public WebSocketChannelManagerFactory withClusterMessageBroker(WebSocketChannelClusterMessageBroker broker) {
        this.messageBroker = broker;
        return this;
    }

    public WebSocketChannelManagerFactory loadFromComponentRegistry(ComponentRegistry componentRegistry, String clusterMessageBrokerName, String clusterCoordinatorName) {
        ComponentFilterBuilder filterBuilder = componentRegistry.getComponentFilterBuilder();

        ComponentFilter brokerFilter = filterBuilder.createFilter(WebSocketConstants.CHANNEL_CLUSTER_MESSAGE_BROKER_OSGI_FILTER_NAME, clusterMessageBrokerName);
        ComponentFilter coordFilter = filterBuilder.createFilter(WebSocketConstants.CHANNEL_CLUSTER_COORDINATOR_OSGI_FILTER_NAME, clusterCoordinatorName);

        List<WebSocketChannelClusterMessageBroker> messageBrokerRefs = componentRegistry.findComponents(WebSocketChannelClusterMessageBroker.class, brokerFilter);
        List<WebSocketChannelClusterCoordinator> clusterCoordinatorRefs = componentRegistry.findComponents(WebSocketChannelClusterCoordinator.class, coordFilter);

        //loading cluster message broker from registered components
        if (messageBrokerRefs != null && !messageBrokerRefs.isEmpty()) {
            this.withClusterMessageBroker(messageBrokerRefs.get(0));
        }

        if (clusterCoordinatorRefs != null && !clusterCoordinatorRefs.isEmpty()) {
            this.withClusterCoordinator(clusterCoordinatorRefs.get(0));
        }

        return this;
    }

    public <T extends WebSocketChannel> WebSocketChannelManager build(Class<T> channelType) {
        if (channelType == null)
            throw new WaterRuntimeException("Channel Manager must be set with a channel type, please use withChannelType method");
        return new WebSocketDefaultChannelManager<>(channelType, this.coordinator, this.messageBroker);
    }

    public void reset() {
        this.coordinator = null;
        this.messageBroker = null;
    }

    public static WebSocketChannelManagerFactory newDefaultChannelManagerFactory() {
        return new WebSocketChannelManagerFactory();
    }

}
