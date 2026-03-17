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
import it.water.websocket.api.WebSocketBasicCommandType;
import it.water.websocket.api.channel.WebSocketChannelCommand;
import it.water.websocket.api.channel.WebSocketChannelManager;
import it.water.websocket.api.channel.WebSocketChannelRemoteCommand;
import it.water.websocket.channel.util.WebSocketChannelConstants;
import it.water.websocket.model.message.WebSocketMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@FrameworkComponent(services = {WebSocketChannelCommand.class, WebSocketChannelRemoteCommand.class}, properties = {
        WebSocketChannelConstants.COMMAND_FILTER + "=" + WebSocketBasicCommandType.READ_MESSAGE_COMMAND
})
public class WebSocketChannelReadMessageCommand extends WebSocketChannelAbstractCommand implements WebSocketChannelRemoteCommand {
    private static Logger log = LoggerFactory.getLogger(WebSocketChannelReadMessageCommand.class);

    @Override
    public void execute(WebSocketMessage message, String channelId, WebSocketChannelManager channelManager) {
        checkRequiredParameters(message, WebSocketChannelConstants.CHANNEL_ID_PARAM);
        channelManager.deliverMessage(message);
    }
}
