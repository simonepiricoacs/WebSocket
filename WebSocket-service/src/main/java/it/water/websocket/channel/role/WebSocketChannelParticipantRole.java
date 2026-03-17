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

package it.water.websocket.channel.role;

import it.water.core.interceptors.annotations.FrameworkComponent;
import it.water.websocket.api.WebSocketBasicCommandType;
import it.water.websocket.api.WebSocketCommand;
import it.water.websocket.api.channel.WebSocketChannelRole;
import it.water.websocket.channel.command.WebSocketChannelCommandType;
import it.water.websocket.channel.util.WebSocketChannelConstants;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@FrameworkComponent(services = WebSocketChannelRole.class, properties = {
        WebSocketChannelConstants.WEBSOCKET_CHANNEL_ROLE_NAME + "="+ WebSocketChannelConstants.CHANNEL_ROLE_PARTECIPANT
})
public class WebSocketChannelParticipantRole implements WebSocketChannelRole {

    private static Set<WebSocketCommand> allowedCmds;

    static {
        Set<WebSocketCommand> commands = new HashSet<>();
        commands.add(WebSocketChannelCommandType.CREATE_CHANNEl);
        commands.add(WebSocketChannelCommandType.LEAVE_CHANNEL);
        commands.add(WebSocketChannelCommandType.JOIN_CHANNEL);
        commands.add(WebSocketChannelCommandType.SEND_PRIVATE_MESSAGE);
        commands.add(WebSocketBasicCommandType.READ_MESSAGE);
        commands.add(WebSocketBasicCommandType.SEND_MESSAGE);
        allowedCmds = Collections.unmodifiableSet(commands);
    }

    @Override
    public Set<WebSocketCommand> getAllowedCmds() {
        return allowedCmds;
    }

    @Override
    public String getRoleName() {
        return WebSocketChannelConstants.CHANNEL_ROLE_PARTECIPANT;
    }
}
