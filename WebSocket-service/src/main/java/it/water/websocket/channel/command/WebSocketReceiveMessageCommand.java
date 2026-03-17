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
import it.water.core.model.exceptions.WaterRuntimeException;
import it.water.websocket.api.WebSocketBasicCommandType;
import it.water.websocket.api.channel.WebSocketChannel;
import it.water.websocket.api.channel.WebSocketChannelCommand;
import it.water.websocket.api.channel.WebSocketChannelManager;
import it.water.websocket.api.channel.WebSocketChannelSession;
import it.water.websocket.channel.util.WebSocketChannelConstants;
import it.water.websocket.model.WebSocketUserInfo;
import it.water.websocket.model.message.WebSocketMessage;

import java.util.Optional;

@FrameworkComponent(services = WebSocketChannelCommand.class, properties = {
        WebSocketChannelConstants.COMMAND_FILTER + "=" + WebSocketBasicCommandType.READ_MESSAGE_COMMAND
})
public class WebSocketReceiveMessageCommand extends WebSocketChannelAbstractCommand implements WebSocketChannelCommand {

    @Override
    public void execute(WebSocketChannelSession userSession, WebSocketMessage message, String channelId, WebSocketChannelManager channelManager) {
        //here user session is null since this message may arrive from different cluster node
        checkRequiredParameters(message, WebSocketChannelConstants.CHANNEL_MESSAGE_PARAM_PVT_MESSAGE_SENDER);
        WebSocketChannel channel = findChannelOrDie(channelId, channelManager);
        String messageSenderUserId = message.getParams().get(WebSocketChannelConstants.CHANNEL_MESSAGE_PARAM_PVT_MESSAGE_SENDER);
        Optional<WebSocketUserInfo> senderInfo = channel.findPartecipantInfoFromUserId(messageSenderUserId);
        if (senderInfo.isPresent()) {
            channel.deliverMessage(senderInfo.get(), message);
        } else {
            throw new WaterRuntimeException("Sender not found, impossible to dispatch message to channel");
        }
    }

}
