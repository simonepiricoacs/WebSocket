package it.water.websocket.session;

import it.water.websocket.compression.WebSocketCompression;
import it.water.websocket.encryption.WebSocketEncryption;
import it.water.websocket.model.message.WebSocketMessage;
import it.water.websocket.model.message.WebSocketMessageType;
import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WriteCallback;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WebSocketMessageBrokerTest {

    @Mock
    private Session mockSession;

    @Mock
    private RemoteEndpoint mockRemote;

    @Mock
    private WebSocketEncryption mockEncryption;

    @Mock
    private WebSocketCompression mockCompression;

    private WebSocketMessageBroker broker;

    @BeforeEach
    void setUp() {
        when(mockSession.getRemote()).thenReturn(mockRemote);
        doNothing().when(mockRemote).sendString(anyString(), any());
        broker = new WebSocketMessageBroker(mockSession);
    }

    // --- hasEncryption / hasCompression ---

    @Test
    void hasEncryptionFalseByDefault() {
        assertFalse(broker.hasEncryption());
    }

    @Test
    void hasCompressionFalseByDefault() {
        assertFalse(broker.hasCompression());
    }

    @Test
    void setEncryptionPolicyEnablesEncryption() {
        broker.setEncryptionPolicy(mockEncryption);
        assertTrue(broker.hasEncryption());
    }

    @Test
    void setCompressionPolicyEnablesCompression() {
        broker.setCompressionPolicy(mockCompression);
        assertTrue(broker.hasCompression());
    }

    // --- onOpenSession / onCloseSession ---

    @Test
    void onOpenSessionCallsEncryptionInit() {
        broker.setEncryptionPolicy(mockEncryption);
        broker.onOpenSession(mockSession);
        verify(mockEncryption, atLeastOnce()).init(mockSession);
    }

    @Test
    void onOpenSessionCallsCompressionInit() {
        broker.setCompressionPolicy(mockCompression);
        broker.onOpenSession(mockSession);
        verify(mockCompression, atLeastOnce()).init(mockSession);
    }

    @Test
    void onOpenSessionWithNullPoliciesDoesNotThrow() {
        assertDoesNotThrow(() -> broker.onOpenSession(mockSession));
    }

    @Test
    void onCloseSessionCallsEncryptionDispose() {
        broker.setEncryptionPolicy(mockEncryption);
        broker.onCloseSession(mockSession);
        verify(mockEncryption).dispose(mockSession);
    }

    @Test
    void onCloseSessionCallsCompressionDispose() {
        broker.setCompressionPolicy(mockCompression);
        broker.onCloseSession(mockSession);
        verify(mockCompression).dispose(mockSession);
    }

    @Test
    void onCloseSessionWithNullPoliciesDoesNotThrow() {
        assertDoesNotThrow(() -> broker.onCloseSession(mockSession));
    }

    // --- read ---

    @Test
    void readWithValidJsonReturnsMessage() {
        WebSocketMessage msg = WebSocketMessage.createMessage("TEST", "payload".getBytes(), WebSocketMessageType.OK);
        String json = msg.toJson();
        WebSocketMessage result = broker.read(json);
        assertNotNull(result);
        assertEquals("TEST", result.getCmd());
    }

    @Test
    void readWithInvalidJsonReturnsNull() {
        WebSocketMessage result = broker.read("NOT_VALID_JSON{{{");
        assertNull(result);
    }

    // --- readRaw overloads ---

    @Test
    void readRawBytesNoEncryptionReturnsOriginalBytes() {
        byte[] input = "hello".getBytes(StandardCharsets.UTF_8);
        // decodeBase64BeforeDecrypt=false, no policies → returns input unchanged
        byte[] result = broker.readRaw(input, false);
        assertNotNull(result);
        assertArrayEquals(input, result);
    }

    @Test
    void readRawBytesBase64NoEncryptionReturnsOriginalBytes() {
        // decodeBase64=true but no encryption policy → bytes are passed through unchanged
        byte[] input = "hello".getBytes(StandardCharsets.UTF_8);
        byte[] result = broker.readRaw(input, true);
        assertNotNull(result);
        // without encryption policy, processMessageBeforeRead returns the bytes as-is
        assertArrayEquals(input, result);
    }

    @Test
    void readRawStringNoEncryptionReturnsBytes() {
        String message = "testcontent";
        byte[] result = broker.readRaw(message);
        assertNotNull(result);
    }

    @Test
    void readRawStringWithFlagNoEncryptionReturnsBytes() {
        String message = "testcontent";
        byte[] result = broker.readRaw(message, false);
        assertNotNull(result);
    }

    @Test
    void readRawBytesWithEncryptionCallsDecrypt() throws Exception {
        byte[] input = "hello".getBytes(StandardCharsets.UTF_8);
        when(mockEncryption.decrypt(any(), anyBoolean())).thenReturn("decrypted".getBytes());
        broker.setEncryptionPolicy(mockEncryption);
        byte[] result = broker.readRaw(input, false);
        assertNotNull(result);
        verify(mockEncryption).decrypt(input, false);
    }

    @Test
    void readRawWithCompressionCallsDecompress() throws Exception {
        byte[] input = "compressed".getBytes(StandardCharsets.UTF_8);
        when(mockCompression.decompress(any())).thenReturn("decompressed".getBytes());
        broker.setCompressionPolicy(mockCompression);
        byte[] result = broker.readRaw(input, false);
        assertNotNull(result);
        verify(mockCompression).decompress(input);
    }

    // --- sendAsync ---

    @Test
    void sendAsyncMessageCallsRemote() {
        WebSocketMessage msg = WebSocketMessage.createMessage("CMD", "data".getBytes(), WebSocketMessageType.OK);
        broker.sendAsync(msg);
        verify(mockRemote, atLeastOnce()).sendString(anyString(), any());
    }

    @Test
    void sendAsyncMessageWithCallbackCallsRemote() {
        WriteCallback callback = mock(WriteCallback.class);
        WebSocketMessage msg = WebSocketMessage.createMessage("CMD", "data".getBytes(), WebSocketMessageType.OK);
        broker.sendAsync(msg, callback);
        verify(mockRemote, atLeastOnce()).sendString(anyString(), any());
    }

    @Test
    void sendAsyncMessageWithEncodeBase64FalseCallsRemote() {
        WebSocketMessage msg = WebSocketMessage.createMessage("CMD", "data".getBytes(), WebSocketMessageType.OK);
        broker.sendAsync(msg, false, null);
        verify(mockRemote, atLeastOnce()).sendString(anyString(), any());
    }

    @Test
    void sendAsyncBytesWithCallbackCallsRemote() {
        WriteCallback callback = mock(WriteCallback.class);
        broker.sendAsync("hello".getBytes(), true, callback);
        verify(mockRemote).sendString(anyString(), eq(callback));
    }

    @Test
    void sendAsyncBytesWithNullCallbackCallsRemote() {
        broker.sendAsync("hello".getBytes(), true, null);
        verify(mockRemote).sendString(anyString(), isNull());
    }

    @Test
    void sendAsyncStringCallsRemote() {
        broker.sendAsync("plain message");
        verify(mockRemote, atLeastOnce()).sendString(anyString(), any());
    }

    @Test
    void sendAsyncStringWithCallbackCallsRemote() {
        WriteCallback callback = mock(WriteCallback.class);
        broker.sendAsync("plain message", callback);
        verify(mockRemote, atLeastOnce()).sendString(anyString(), any());
    }

    @Test
    void sendAsyncWithEncryptionCallsEncrypt() throws Exception {
        byte[] encrypted = Base64.getEncoder().encode("encrypted".getBytes());
        when(mockEncryption.encrypt(any(), anyBoolean())).thenReturn(encrypted);
        broker.setEncryptionPolicy(mockEncryption);
        broker.sendAsync("hello", null);
        verify(mockEncryption, atLeastOnce()).encrypt(any(), anyBoolean());
    }

    @Test
    void sendAsyncWithCompressionCallsCompress() throws Exception {
        byte[] compressed = "compressed".getBytes();
        when(mockCompression.compress(any())).thenReturn(compressed);
        broker.setCompressionPolicy(mockCompression);
        broker.sendAsync("hello".getBytes(), true, null);
        verify(mockCompression).compress(any());
    }

    @Test
    void sendAsyncMessageSerializationFailureTriggersErrorMessageFallback() throws Exception {
        WebSocketMessage msg = mock(WebSocketMessage.class);
        when(msg.getTimestamp()).thenThrow(new IllegalStateException("serialize error"));

        broker.sendAsync(msg);

        verify(mockRemote, atLeastOnce()).sendString(anyString(), any());
    }

    @Test
    void sendAsyncBytesSwallowsRemoteSendErrors() {
        doThrow(new RuntimeException("send failure")).when(mockRemote).sendString(anyString(), isNull());

        assertDoesNotThrow(() -> broker.sendAsync("hello".getBytes(StandardCharsets.UTF_8), true, null));
    }

    // --- closeSessionWithMessage ---

    @Test
    void closeSessionWithMessageCallsSessionClose() {
        WebSocketMessage msg = WebSocketMessage.createMessage("CLOSE", "bye".getBytes(), WebSocketMessageType.OK);
        broker.closeSessionWithMessage(msg);
        verify(mockSession).close(eq(500), anyString());
    }

    @Test
    void closeSessionWithMessageFallsBackToCloseWithoutReason() throws Exception {
        WebSocketMessage msg = mock(WebSocketMessage.class);
        when(msg.getTimestamp()).thenThrow(new IllegalStateException("cannot serialize"));

        broker.closeSessionWithMessage(msg);

        verify(mockSession).close();
    }

    @Test
    void readRawReturnsNullWhenDecryptionFails() throws Exception {
        when(mockEncryption.decrypt(any(), anyBoolean())).thenThrow(new IllegalStateException("decrypt failure"));
        broker.setEncryptionPolicy(mockEncryption);

        assertNull(broker.readRaw("hello".getBytes(StandardCharsets.UTF_8), false));
    }

    // --- updateEncryptionPolicyParams / getEncryptionPolicyParams ---

    @Test
    void updateEncryptionPolicyParamsCallsUpdate() {
        broker.setEncryptionPolicy(mockEncryption);
        Map<String, Object> params = new HashMap<>();
        params.put("key", "value");
        broker.updateEncryptionPolicyParams(params);
        verify(mockEncryption).updateMode(params);
    }

    @Test
    void updateEncryptionPolicyParamsWithNullPolicyDoesNothing() {
        assertDoesNotThrow(() -> broker.updateEncryptionPolicyParams(new HashMap<>()));
    }

    @Test
    void getEncryptionPolicyParamsReturnsParamsFromPolicy() {
        Map<String, Object> params = new HashMap<>();
        params.put("aesKey", "val");
        when(mockEncryption.getModeParams()).thenReturn(params);
        broker.setEncryptionPolicy(mockEncryption);
        assertEquals(params, broker.getEncryptionPolicyParams());
    }

    @Test
    void getEncryptionPolicyParamsWithNullPolicyReturnsNull() {
        assertNull(broker.getEncryptionPolicyParams());
    }
}
