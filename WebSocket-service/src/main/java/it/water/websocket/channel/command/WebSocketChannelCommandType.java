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

import it.water.websocket.api.WebSocketBasicCommandType;
import it.water.websocket.api.WebSocketCommand;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("java:S1192") // string literals duplicated in lambdas and public constants — forward reference restriction prevents using constants in enum initializers
public enum WebSocketChannelCommandType implements WebSocketCommand {

    //command used to synchronize peer on events happening on each peer of the cluster
    FOLLOW(() -> "FOLLOW"),

    CREATE_CHANNEL(() -> "CREATE_CHANNEL"),

    DELETE_CHANNEL(() -> "DELETE_CHANNEL"),

    JOIN_CHANNEL(() -> "JOIN_CHANNEL"),

    LEAVE_CHANNEL(() -> "LEAVE_CHANNEL"),

    BAN_USER(() -> "BAN_USER"),

    UNBAN_USER(() -> "UNBAN_USER"),

    KICK_USER(() -> "KICK_USER"),

    SEND_PRIVATE_MESSAGE(() -> "SEND_PRIVATE_MESSAGE"),

    SEND_MESSAGE_TO_SERVER(() -> "PROCESS_ON_SERVER");

    public static final Set<WebSocketCommand> ALL_CMDS;

    public static final String FOLLOW_COMMAND = "FOLLOW";
    public static final String BAN_USER_COMMAND = "BAN_USER";
    public static final String UNBAN_USER_COMMAND = "UNBAN_USER";
    public static final String CREATE_CHANNEL_COMMAND = "CREATE_CHANNEL";
    public static final String DELETE_CHANNEL_COMMAND = "DELETE_CHANNEL";
    public static final String JOIN_CHANNEL_COMMAND = "JOIN_CHANNEL";
    public static final String LEAVE_CHANNEL_COMMAND = "LEAVE_CHANNEL";
    public static final String KICK_USER_COMMAND = "KICK_USER";
    public static final String SEND_PRIVATE_MESSAGE_COMMAND = "SEND_PRIVATE_MESSAGE";
    public static final String PROCESS_ON_SERVER_COMMAND = "PROCESS_ON_SERVER";

    static {
        Set<WebSocketCommand> commands = new HashSet<>();
        commands.add(WebSocketChannelCommandType.FOLLOW);
        commands.add(WebSocketChannelCommandType.CREATE_CHANNEL);
        commands.add(WebSocketChannelCommandType.DELETE_CHANNEL);
        commands.add(WebSocketChannelCommandType.JOIN_CHANNEL);
        commands.add(WebSocketChannelCommandType.LEAVE_CHANNEL);
        commands.add(WebSocketChannelCommandType.SEND_PRIVATE_MESSAGE);
        commands.add(WebSocketChannelCommandType.BAN_USER);
        commands.add(WebSocketChannelCommandType.UNBAN_USER);
        commands.add(WebSocketChannelCommandType.KICK_USER);
        commands.add(WebSocketChannelCommandType.SEND_MESSAGE_TO_SERVER);
        commands.add(WebSocketBasicCommandType.SEND_MESSAGE);
        commands.add(WebSocketBasicCommandType.READ_MESSAGE);
        ALL_CMDS = Collections.unmodifiableSet(commands);
    }

    private WebSocketCommand cmd;

    WebSocketChannelCommandType(WebSocketCommand cmd) {
        this.cmd = cmd;
    }

    @Override
    public String getCommandName() {
        return this.cmd.getCommandName();
    }
}
