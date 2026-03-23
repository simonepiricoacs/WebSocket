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

package it.water.websocket.channel.role;

import it.water.core.api.registry.ComponentRegistry;
import it.water.core.api.registry.filter.ComponentFilter;
import it.water.core.api.registry.filter.ComponentFilterBuilder;
import it.water.core.model.exceptions.WaterRuntimeException;
import it.water.websocket.api.channel.WebSocketChannelRole;
import it.water.websocket.channel.util.WebSocketChannelConstants;

import java.util.*;

public class WebSocketChannelRoleManager {
    private WebSocketChannelRoleManager() {
        throw new UnsupportedOperationException();
    }

    private static ComponentRegistry componentRegistry;

    public static void setComponentRegistry(ComponentRegistry registry) {
        componentRegistry = registry;
    }

    public static Set<WebSocketChannelRole> newRoleSet(WebSocketChannelRole... roleList) {
        return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(roleList)));
    }

    public static Set<WebSocketChannelRole> newRoleSet(Set<WebSocketChannelRole>... roleList) {
        Set<WebSocketChannelRole> mergedSet = new HashSet<>();
        Arrays.stream(roleList).forEach(mergedSet::addAll);
        return Collections.unmodifiableSet(mergedSet);
    }

    public static WebSocketChannelRole getWebSocketChannelRole(String roleName) {
        if (componentRegistry == null) {
            throw new WaterRuntimeException("ComponentRegistry not set in WebSocketChannelRoleManager");
        }
        ComponentFilterBuilder filterBuilder = componentRegistry.getComponentFilterBuilder();
        ComponentFilter filter = filterBuilder.createFilter(WebSocketChannelConstants.WEBSOCKET_CHANNEL_ROLE_NAME, roleName);
        List<WebSocketChannelRole> refs = componentRegistry.findComponents(WebSocketChannelRole.class, filter);
        //returns the first instance
        if (refs != null && refs.size() == 1) {
            return refs.get(0);
        } else if (refs != null && refs.size() > 1) {
            throw new WaterRuntimeException("Multiple Web Socket Channel role found with name " + roleName);
        } else {
            throw new WaterRuntimeException("No Web Socket Channel role found with name " + roleName);
        }
    }

    public static String rolesAsCommaSeparatedList(Set<WebSocketChannelRole> roles) {
        StringBuilder sb = new StringBuilder();
        roles.forEach(role -> sb.append(role.getRoleName()).append(","));
        return sb.substring(0, sb.length() - 1);
    }

    public static Set<WebSocketChannelRole> fromCommaSeparatedList(String rolesCommaSeparatedList) {
        String[] roles = rolesCommaSeparatedList.split(",");
        Set<WebSocketChannelRole> rolesSet = new HashSet<>();
        Arrays.stream(roles).forEach(roleName -> {
            WebSocketChannelRole r = getWebSocketChannelRole(roleName);
            if (r != null)
                rolesSet.add(r);
        });
        return rolesSet;
    }


}
