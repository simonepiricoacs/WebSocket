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

package it.water.websocket.channel.command;

import it.water.core.api.registry.ComponentRegistry;
import it.water.core.api.registry.filter.ComponentFilter;
import it.water.core.api.registry.filter.ComponentFilterBuilder;
import it.water.core.model.exceptions.WaterRuntimeException;
import it.water.websocket.api.channel.WebSocketChannelCommand;
import it.water.websocket.api.channel.WebSocketChannelRemoteCommand;
import it.water.websocket.channel.util.WebSocketChannelConstants;

import java.util.List;

public class WebSocketChannelCommandFactory {

    private final ComponentRegistry componentRegistry;

    public WebSocketChannelCommandFactory(ComponentRegistry componentRegistry) {
        this.componentRegistry = componentRegistry;
    }

    private <T extends WebSocketChannelCommand> WebSocketChannelCommand createCommandInternal(String commandStr, Class<T> commandInterface) {
        ComponentFilterBuilder filterBuilder = componentRegistry.getComponentFilterBuilder();
        ComponentFilter filter = filterBuilder.createFilter(WebSocketChannelConstants.COMMAND_FILTER, commandStr);
        List<T> refs = componentRegistry.findComponents(commandInterface, filter);
        if (refs != null && !refs.isEmpty()) {
            if (refs.size() > 1)
                throw new WaterRuntimeException("Commands conflict! two or more command are associated with " + commandStr + " please check it!");
            return refs.get(0);
        }
        throw new WaterRuntimeException("No web socket channel command found for string: " + commandStr);
    }

    public WebSocketChannelCommand createCommand(String commandStr) {
        return createCommandInternal(commandStr, WebSocketChannelCommand.class);
    }

    public WebSocketChannelRemoteCommand createRemoteCommand(String commandStr) {
        return (WebSocketChannelRemoteCommand) createCommandInternal(commandStr, WebSocketChannelRemoteCommand.class);
    }
}
