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

import it.water.core.interceptors.annotations.FrameworkComponent;
import it.water.websocket.api.channel.WebSocketChannelCommand;
import it.water.websocket.api.channel.WebSocketChannelManager;
import it.water.websocket.api.channel.WebSocketChannelRole;
import it.water.websocket.api.channel.WebSocketChannelSession;
import it.water.websocket.channel.role.WebSocketChannelRoleManager;
import it.water.websocket.channel.util.WebSocketChannelConstants;
import it.water.websocket.model.message.WebSocketMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@FrameworkComponent(services = WebSocketChannelCommand.class, properties = {
        WebSocketChannelConstants.COMMAND_FILTER + "=" + WebSocketChannelCommandType.CREATE_CHANNEL_COMMAND
})
public class WebSocketChannelCreateCommand extends WebSocketChannelAbstractCommand implements WebSocketChannelCommand {
    private static Logger log = LoggerFactory.getLogger(WebSocketChannelCreateCommand.class);

    @Override
    public void execute(WebSocketChannelSession userSession, WebSocketMessage message, String channelId, WebSocketChannelManager channelManager) {
        checkRequiredParameters(message, WebSocketChannelConstants.CHANNEL_ID_PARAM, WebSocketChannelConstants.CHANNEL_NAME_PARAM, WebSocketChannelConstants.CHANNEL_MAX_PARTECIPANTS_PARAM, WebSocketChannelConstants.CHANNEL_TYPE_PARAM);
        WebSocketChannelRole ownerRole = WebSocketChannelRoleManager.getWebSocketChannelRole(WebSocketChannelConstants.CHANNEL_ROLE_OWNER);
        String channelName = message.getParams().get(WebSocketChannelConstants.CHANNEL_NAME_PARAM);
        String newChannelId = message.getParams().get(WebSocketChannelConstants.CHANNEL_ID_PARAM);
        Map<String, Object> params = new HashMap<>();
        params.putAll(message.getParams());
        int maxPartecipants = -1;
        try {
            maxPartecipants = Integer.parseInt(message.getParams().get(WebSocketChannelConstants.CHANNEL_MAX_PARTECIPANTS_PARAM));
        } catch (NumberFormatException e) {
            log.error("Impossible to parse number of max partecipants. Channel will have unlimited number of partecipants");
        }
        String channelType = message.getParams().get(WebSocketChannelConstants.CHANNEL_TYPE_PARAM);
        channelManager.createChannel(channelType, channelName, newChannelId, maxPartecipants, params, userSession, WebSocketChannelRoleManager.newRoleSet(ownerRole));
    }
}
