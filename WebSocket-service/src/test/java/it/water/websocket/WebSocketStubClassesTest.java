package it.water.websocket;

import it.water.websocket.channel.util.WebSocketChannelConstants;
import it.water.websocket.compression.WebSocketCompressionFactory;
import it.water.websocket.encryption.WebSocketEncryption;
import it.water.websocket.encryption.WebSocketEncryptionFactory;
import it.water.websocket.encryption.mode.WebSocketSymmetricKeyEncryptionMode;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
    void webSocketEncryptionFactoryCreatesEncryptionWrapper() {
        WebSocketEncryption encryption = WebSocketEncryptionFactory.createRSAAndAESEncryptionPolicy(Mockito.mock(it.water.core.api.security.EncryptionUtil.class));
        assertNotNull(encryption);
    }

    @Test
    void webSocketEncryptionFactoryConstructorThrows() throws Exception {
        var constructor = WebSocketEncryptionFactory.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        var invocation = assertThrows(java.lang.reflect.InvocationTargetException.class, constructor::newInstance);
        assertNotNull(invocation.getCause());
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
