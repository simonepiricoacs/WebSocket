package it.water.websocket.channel.encryption;

import it.water.core.api.registry.ComponentRegistry;
import it.water.core.api.security.EncryptionUtil;
import it.water.websocket.api.channel.WebSocketChannelClusterMessageBroker;
import it.water.websocket.api.channel.WebSocketChannelSession;
import it.water.websocket.channel.role.WebSocketChannelOwnerRole;
import it.water.websocket.model.WebSocketUserInfo;
import it.water.websocket.model.message.WebSocketMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import javax.crypto.spec.IvParameterSpec;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WebSocketEncryptedBasicChannelTest {

    @Mock
    private WebSocketChannelClusterMessageBroker broker;

    @Mock
    private WebSocketChannelSession ownerSession;

    @Mock
    private ComponentRegistry mockRegistry;

    @Mock
    private EncryptionUtil mockEncryptionUtil;

    private WebSocketUserInfo ownerInfo;

    @BeforeEach
    void setUp() throws Exception {
        ownerInfo = new WebSocketUserInfo("owner", null, "127.0.0.1");
        when(ownerSession.getUserInfo()).thenReturn(ownerInfo);
        doNothing().when(ownerSession).sendRemote(any(WebSocketMessage.class));
        doNothing().when(ownerSession).addJoinedChannels(any());

        byte[] aesKey = "0123456789abcdef".getBytes();
        byte[] aesIv = "fedcba9876543210".getBytes();
        when(mockEncryptionUtil.generateRandomAESPassword()).thenReturn(aesKey);
        when(mockEncryptionUtil.generateRandomAESInitVector()).thenReturn(new IvParameterSpec(aesIv));
        when(mockRegistry.findComponent(eq(EncryptionUtil.class), any())).thenReturn(mockEncryptionUtil);
    }

    // Anonymous concrete subclass that does nothing in initChannelEncryption
    private WebSocketEncryptedBasicChannel createConcreteChannel() {
        return new WebSocketEncryptedBasicChannel("test-channel", "ch-enc-1", 10,
                new HashMap<>(), broker) {
            @Override
            protected void initChannelEncryption() {
                // no-op
            }

            @Override
            protected void setupPartecipantEncryptedSession(WebSocketChannelSession session) {
                // no-op — tested separately via RSAWithAES subclass
            }
        };
    }

    @Test
    void constructorCreatesChannel() {
        WebSocketEncryptedBasicChannel channel = createConcreteChannel();
        assertEquals("ch-enc-1", channel.getChannelId());
        assertEquals("test-channel", channel.getChannelName());
    }

    @Test
    void partecipantJoinedCalledOnAddSession() {
        WebSocketEncryptedBasicChannel channel = createConcreteChannel();
        Set<it.water.websocket.api.channel.WebSocketChannelRole> roles =
                Set.of(new WebSocketChannelOwnerRole());

        channel.addPartecipantSession(ownerInfo, roles, ownerSession);

        verify(ownerSession, atLeastOnce()).addJoinedChannels(channel);
        verify(ownerSession, atLeastOnce()).sendRemote(any(WebSocketMessage.class));
    }

    @Test
    void rsaWithAesChannelCreatedWithMockedRegistry() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("componentRegistry", mockRegistry);

        WebSocketRSAWithAESEncryptedBasicChannel channel = new WebSocketRSAWithAESEncryptedBasicChannel(
                "rsa-channel", "ch-rsa-1", 5, params, broker);

        assertEquals("ch-rsa-1", channel.getChannelId());
        verify(mockEncryptionUtil).generateRandomAESPassword();
        verify(mockEncryptionUtil).generateRandomAESInitVector();
    }

    @Test
    void rsaWithAesChannelSetupPartecipantEncryptedSession() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("componentRegistry", mockRegistry);

        WebSocketRSAWithAESEncryptedBasicChannel channel = new WebSocketRSAWithAESEncryptedBasicChannel(
                "rsa-channel", "ch-rsa-2", 5, params, broker);

        Set<it.water.websocket.api.channel.WebSocketChannelRole> roles =
                Set.of(new WebSocketChannelOwnerRole());

        channel.addPartecipantSession(ownerInfo, roles, ownerSession);

        // setupPartecipantEncryptedSession sends the aesInfoMessage to the session
        verify(ownerSession, atLeastOnce()).sendRemote(any(WebSocketMessage.class));
    }
}
