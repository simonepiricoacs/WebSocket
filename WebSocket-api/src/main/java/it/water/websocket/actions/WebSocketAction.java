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

package it.water.websocket.actions;

import it.water.core.api.action.Action;

public enum WebSocketAction implements Action {

	CREATE_CHANNEL("CREATE_CHANNEL", 1);

	private String name;
	private long actionId;

     /**
	 * Role Action with the specified name.
	 *
	 * @param name parameter that represent the WebSocket  action
	 * @param actionId parameter that represent the action id (power of 2)
	 */
	private WebSocketAction(String name, long actionId) {
		this.name = name;
		this.actionId = actionId;
	}

	/**
	 * Gets the name of WebSocket action
	 */
	@Override
	public String getActionName() {
		return name;
	}

	/**
	 * Gets the type of WebSocket action
	 */
	@Override
	public String getActionType() {
		return WebSocketAction.class.getName();
	}

	/**
	 * Gets the action id of WebSocket action
	 */
	@Override
	public long getActionId() {
		return actionId;
	}

}
