package it.water.websocket.channel.factory;

import it.water.websocket.api.channel.WebSocketChannel;
import it.water.websocket.api.channel.WebSocketChannelClusterMessageBroker;
import it.water.websocket.channel.WebSocketBasicChannel;
import it.water.websocket.channel.WebSocketChannelType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@SuppressWarnings("java:S5778") // inline mock() calls are intentional for locally scoped test scenarios
class WebSocketChannelFactoryTest {

    @Mock
    private WebSocketChannelClusterMessageBroker broker;

    @Test
    void constructorThrowsUnsupportedOperationException() throws Exception {
        var constructor = WebSocketChannelFactory.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        var invocation = assertThrows(java.lang.reflect.InvocationTargetException.class, constructor::newInstance);
        assertInstanceOf(UnsupportedOperationException.class, invocation.getCause());
    }

    @Test
    void createChannelFromTypePlain() {
        WebSocketChannel channel = WebSocketChannelFactory.createChannelFromChannelType(
                WebSocketChannelType.PLAIN.name(), "ch-1", "test", 10, new HashMap<>(), broker);
        assertNotNull(channel);
        assertInstanceOf(WebSocketBasicChannel.class, channel);
        assertEquals("ch-1", channel.getChannelId());
        assertEquals("test", channel.getChannelName());
    }

    @Test
    void createChannelFromTypeEncryptedRsaWithAesRequiresRegistry() {
        // The encrypted channel requires a ComponentRegistry in params to initialize;
        // without it the constructor throws WaterRuntimeException.
        assertThrows(it.water.core.model.exceptions.WaterRuntimeException.class, () ->
                WebSocketChannelFactory.createChannelFromChannelType(
                        WebSocketChannelType.ENCRYPTED_RSA_WITH_AES.name(), "ch-2", "encrypted", 5, new HashMap<>(), broker));
    }

    @Test
    void createChannelFromTypeInvalidThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                WebSocketChannelFactory.createChannelFromChannelType(
                        "UNKNOWN_TYPE", "ch-3", "name", 10, new HashMap<>(), broker));
    }

    @Test
    void createFromStringWithValidJsonPlainChannel() {
        // Create a plain channel and serialize it, then restore via createFromString
        WebSocketBasicChannel original = new WebSocketBasicChannel("byclass", "ch-4", 20, new HashMap<>(), broker);
        String json = original.toJson();
        WebSocketChannel result = WebSocketChannelFactory.createFromString(
                json, WebSocketBasicChannel.class.getName(), broker);
        assertNotNull(result);
        assertInstanceOf(WebSocketBasicChannel.class, result);
        assertEquals("ch-4", result.getChannelId());
    }

    @Test
    void createChannelFromClassPlainChannelWorks() throws Exception {
        Map<String, Object> params = new HashMap<>();
        WebSocketChannel result = WebSocketChannelFactory.createChannelFromClass(
                WebSocketBasicChannel.class, "ch-5", "plain-class", 7, params, broker);
        assertNotNull(result);
        assertInstanceOf(WebSocketBasicChannel.class, result);
    }

    @Test
    void createFromStringWithInvalidClassReturnsNull() {
        WebSocketChannel result = WebSocketChannelFactory.createFromString(
                "{}", "com.nonexistent.FakeClass", broker);
        assertNull(result);
    }

    @Test
    void createFromStringWithInvalidJsonReturnsNull() {
        WebSocketChannel result = WebSocketChannelFactory.createFromString(
                "NOT_VALID_JSON{{{", WebSocketBasicChannel.class.getName(), broker);
        assertNull(result);
    }

    @Test
    void createFromStringNullJsonReturnsNull() {
        WebSocketChannel result = WebSocketChannelFactory.createFromString(
                null, WebSocketBasicChannel.class.getName(), broker);
        assertNull(result);
    }
}
