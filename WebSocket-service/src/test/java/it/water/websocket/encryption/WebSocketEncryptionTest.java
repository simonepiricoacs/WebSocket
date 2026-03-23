package it.water.websocket.encryption;

import it.water.websocket.encryption.mode.WebSocketEncryptionMode;
import org.eclipse.jetty.websocket.api.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WebSocketEncryptionTest {

    @Mock
    private WebSocketEncryptionMode mockMode;

    @Mock
    private Session mockSession;

    private WebSocketEncryption encryption;

    @BeforeEach
    void setUp() {
        encryption = new WebSocketEncryption(mockMode);
    }

    @Test
    void encryptDelegatesToMode() throws Exception {
        byte[] input = "plaintext".getBytes();
        byte[] expected = "encrypted".getBytes();
        when(mockMode.encrypt(input, true)).thenReturn(expected);

        byte[] result = encryption.encrypt(input, true);

        assertArrayEquals(expected, result);
        verify(mockMode).encrypt(input, true);
    }

    @Test
    void decryptDelegatesToMode() throws Exception {
        byte[] input = "ciphertext".getBytes();
        byte[] expected = "plaintext".getBytes();
        when(mockMode.decrypt(input)).thenReturn(expected);

        byte[] result = encryption.decrypt(input, true);

        assertArrayEquals(expected, result);
        verify(mockMode).decrypt(input);
    }

    @Test
    void initDelegatesToMode() {
        encryption.init(mockSession);
        verify(mockMode).init(mockSession);
    }

    @Test
    void disposeDelegatesToMode() {
        encryption.dispose(mockSession);
        verify(mockMode).dispose(mockSession);
    }

    @Test
    void updateModeDelegatesToMode() {
        Map<String, Object> params = new HashMap<>();
        params.put("key", "value");
        encryption.updateMode(params);
        verify(mockMode).update(params);
    }

    @Test
    void getModeParamsDelegatesToMode() {
        Map<String, Object> params = new HashMap<>();
        params.put("aes", "secret");
        when(mockMode.getParams()).thenReturn(params);

        Map<String, Object> result = encryption.getModeParams();

        assertEquals(params, result);
        verify(mockMode).getParams();
    }

    // --- WebSocketEncryptionFactory ---

    @Test
    void factoryCreatesRSAAndAESPolicy() {
        // RSAWithAESEncryptionMode static block may fail gracefully (no keystore in test env)
        // but the factory method itself should still create a non-null WebSocketEncryption
        WebSocketEncryption policy = WebSocketEncryptionFactory.createRSAAndAESEncryptionPolicy();
        assertNotNull(policy);
    }
}
