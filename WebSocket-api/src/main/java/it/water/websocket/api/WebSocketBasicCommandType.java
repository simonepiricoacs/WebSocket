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

package it.water.websocket.api;

@SuppressWarnings("java:S1192") // string literals duplicated in lambdas and public constants — forward reference restriction prevents using constants in enum initializers
public enum WebSocketBasicCommandType implements WebSocketCommand {

    READ_MESSAGE(() -> "READ_MESSAGE"),

    SEND_MESSAGE(() -> "SEND_MESSAGE");

    private WebSocketCommand cmd;

    public static final String READ_MESSAGE_COMMAND = "READ_MESSAGE";
    public static final String SEND_MESSAGE_COMMAND = "SEND_MESSAGE";

    WebSocketBasicCommandType(WebSocketCommand cmd) {
        this.cmd = cmd;
    }

    @Override
    public String getCommandName() {
        return this.cmd.getCommandName();
    }
}
