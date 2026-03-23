package it.water.websocket;

import it.water.websocket.channel.util.WebSocketChannelConstants;
import it.water.websocket.compression.WebSocketCompressionFactory;
import it.water.websocket.encryption.mode.WebSocketSymmetricKeyEncryptionMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Coverage tests for stub/empty/constants classes.
 */
class WebSocketStubClassesTest {

    @Test
    void webSocketCompressionFactoryInstantiation() {
        assertNotNull(new WebSocketCompressionFactory());
    }

    @Test
    void webSocketSymmetricKeyEncryptionModeInstantiation() {
        assertNotNull(new WebSocketSymmetricKeyEncryptionMode());
    }

    @Test
    void webSocketChannelConstantsValues() {
        assertEquals("channelId", WebSocketChannelConstants.CHANNEL_ID_PARAM);
        assertEquals("channelType", WebSocketChannelConstants.CHANNEL_TYPE_PARAM);
        assertEquals("owner", WebSocketChannelConstants.CHANNEL_ROLE_OWNER);
        assertEquals("partecipant", WebSocketChannelConstants.CHANNEL_ROLE_PARTECIPANT);
        assertEquals(":", WebSocketChannelConstants.WS_MESSAGE_CHANNEL_AES_DATA_SEPARATOR);
    }
}
