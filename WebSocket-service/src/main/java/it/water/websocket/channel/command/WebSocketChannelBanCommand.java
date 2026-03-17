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
import it.water.websocket.api.channel.WebSocketChannel;
import it.water.websocket.api.channel.WebSocketChannelCommand;
import it.water.websocket.api.channel.WebSocketChannelManager;
import it.water.websocket.api.channel.WebSocketChannelRemoteCommand;
import it.water.websocket.channel.util.WebSocketChannelConstants;
import it.water.websocket.model.WebSocketUserInfo;
import it.water.websocket.model.message.WebSocketMessage;

import java.util.Optional;

@FrameworkComponent(services = {WebSocketChannelCommand.class, WebSocketChannelRemoteCommand.class}, properties = {
        WebSocketChannelConstants.COMMAND_FILTER + "=" + WebSocketChannelCommandType.BAN_USER_COMMAND
})
public class WebSocketChannelBanCommand extends WebSocketChannelAbstractCommand implements WebSocketChannelRemoteCommand {

    @Override
    public void execute(WebSocketMessage message, String channelId, WebSocketChannelManager channelManager) {
        checkRequiredParameters(message, WebSocketChannelConstants.CHANNEL_MESSAGE_PARAM_USER_TO_KICK, WebSocketChannelConstants.CHANNEL_MESSAGE_PARAM_KICK_MESSAGE);
        WebSocketChannel channel = findChannelOrDie(channelId, channelManager);
        String senderUsername = message.getParams().get(WebSocketMessage.WS_MESSAGE_SENDER_PARAM_NAME);
        Optional<WebSocketUserInfo> bannerUserInfoOptional = channel.getPartecipantsInfo().stream().filter(userInfo -> userInfo.getUsername().equals(senderUsername)).findAny();
        if (bannerUserInfoOptional.isPresent()) {
            channelManager.banParticipant(channelId, bannerUserInfoOptional.get(), message);
        }
    }
}
