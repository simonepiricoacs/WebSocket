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

public enum WebSocketChannelCommandType implements WebSocketCommand {

    //command used to synchronize peer on events happening on each peer of the cluster
    FOLLOW(new WebSocketCommand() {
        @Override
        public String getCommandName() {
            return FOLLOW_COMMAND;
        }
    }),

    CREATE_CHANNEl(new WebSocketCommand() {
        @Override
        public String getCommandName() {
            return CREATE_CHANNEL_COMMAND;
        }
    }),

    DELETE_CHANNEl(new WebSocketCommand() {
        @Override
        public String getCommandName() {
            return DELETE_CHANNEL_COMMAND;
        }
    }),

    JOIN_CHANNEL(new WebSocketCommand() {
        @Override
        public String getCommandName() {
            return JOIN_CHANNEL_COMMAND;
        }
    }),

    LEAVE_CHANNEL(new WebSocketCommand() {
        @Override
        public String getCommandName() {
            return LEAVE_CHANNEL_COMMAND;
        }
    }),

    BAN_USER(new WebSocketCommand() {
        @Override
        public String getCommandName() {
            return BAN_USER_COMMAND;
        }
    }),

    UNBAN_USER(new WebSocketCommand() {
        @Override
        public String getCommandName() {
            return UNBAN_USER_COMMAND;
        }
    }),

    KICK_USER(new WebSocketCommand() {
        @Override
        public String getCommandName() {
            return KICK_USER_COMMAND;
        }
    }),

    SEND_PRIVATE_MESSAGE(new WebSocketCommand() {
        @Override
        public String getCommandName() {
            return SEND_PRIVATE_MESSAGE_COMMAND;
        }
    }),

    SEND_MESSAGE_TO_SERVER(new WebSocketCommand() {
        @Override
        public String getCommandName() {
            return PROCESS_ON_SERVER_COMMAND;
        }
    });

    public static Set<WebSocketCommand> allCmds;

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
        commands.add(WebSocketChannelCommandType.CREATE_CHANNEl);
        commands.add(WebSocketChannelCommandType.DELETE_CHANNEl);
        commands.add(WebSocketChannelCommandType.JOIN_CHANNEL);
        commands.add(WebSocketChannelCommandType.LEAVE_CHANNEL);
        commands.add(WebSocketChannelCommandType.SEND_PRIVATE_MESSAGE);
        commands.add(WebSocketChannelCommandType.BAN_USER);
        commands.add(WebSocketChannelCommandType.UNBAN_USER);
        commands.add(WebSocketChannelCommandType.KICK_USER);
        commands.add(WebSocketChannelCommandType.SEND_MESSAGE_TO_SERVER);
        commands.add(WebSocketBasicCommandType.SEND_MESSAGE);
        commands.add(WebSocketBasicCommandType.READ_MESSAGE);
        allCmds = Collections.unmodifiableSet(commands);
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
