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

package it.water.websocket.channel.util;

public class WebSocketChannelConstants {
    private WebSocketChannelConstants() {
        throw new UnsupportedOperationException();
    }

    public static final String COMMAND_FILTER = "it.water.websocket.channel.model.command";
    public static final String WS_MESSAGE_CHANNEL_AES_DATA_SEPARATOR = ":";

    public static final String CHANNEL_ID_PARAM = "channelId";
    public static final String CHANNEL_TYPE_PARAM = "channelType";
    public static final String CHANNEL_MAX_PARTECIPANTS_PARAM = "maxPartecipants";
    public static final String CHANNEL_NAME_PARAM = "channelName";

    public static final String CHANNEL_MAX_OWNERS_PARAM = "maxOwners";
    public static final String CHANNEL_MESSAGE_PARAM_USER_TO_KICK = "userIdToKick";
    public static final String CHANNEL_MESSAGE_PARAM_KICK_MESSAGE = "kickMessage";

    public static final String CHANNEL_MESSAGE_PARAM_BANNED_IP = "bannedIp";
    public static final String CHANNEL_MESSAGE_PARAM_BANNED_USERNAME = "bannedUsername";
    public static final String CHANNEL_MESSAGE_PARAM_PVT_MESSAGE_RECIPIENT = "recipient";
    public static final String CHANNEL_MESSAGE_PARAM_PVT_MESSAGE_SENDER = "sender";

    public static final String CHANNEL_ROLE_PARTECIPANT = "partecipant";
    public static final String CHANNEL_ROLE_OWNER = "owner";

    public static final String WEBSOCKET_CHANNEL_ROLE_NAME = "it.water.websocket.channel.model.role.name";
}
