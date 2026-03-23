package it.water.websocket.channel.util;

import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.*;

class WebSocketChannelConstantsTest {

    @Test
    void privateConstructorThrows() {
        assertThrows(InvocationTargetException.class, () -> {
            var ctor = WebSocketChannelConstants.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            ctor.newInstance();
        });
    }

    @Test
    void constantsAreDefined() {
        assertNotNull(WebSocketChannelConstants.CHANNEL_ID_PARAM);
        assertNotNull(WebSocketChannelConstants.CHANNEL_TYPE_PARAM);
        assertNotNull(WebSocketChannelConstants.CHANNEL_ROLE_OWNER);
        assertNotNull(WebSocketChannelConstants.CHANNEL_ROLE_PARTECIPANT);
        assertNotNull(WebSocketChannelConstants.WS_MESSAGE_CHANNEL_AES_DATA_SEPARATOR);
    }
}
