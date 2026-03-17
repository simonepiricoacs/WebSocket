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
import it.water.websocket.api.channel.*;
import it.water.websocket.channel.role.WebSocketChannelRoleManager;
import it.water.websocket.channel.util.WebSocketChannelConstants;
import it.water.websocket.model.message.WebSocketMessage;

@FrameworkComponent(services = WebSocketChannelCommand.class, properties = {
        WebSocketChannelConstants.COMMAND_FILTER + "=" + WebSocketChannelCommandType.LEAVE_CHANNEL_COMMAND
})
public class WebSocketChannelLeaveCommand extends WebSocketChannelAbstractCommand implements WebSocketChannelCommand {

    @Override
    public void execute(WebSocketChannelSession userSession, WebSocketMessage message, String channelId, WebSocketChannelManager channelManager) {
        findChannelOrDie(channelId, channelManager);
        channelManager.leaveChannel(channelId, userSession);
    }

}
