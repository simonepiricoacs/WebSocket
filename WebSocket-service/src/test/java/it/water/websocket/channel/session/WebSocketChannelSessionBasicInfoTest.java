package it.water.websocket.channel.session;

import it.water.websocket.api.channel.WebSocketChannel;
import it.water.websocket.api.channel.WebSocketChannelRole;
import it.water.websocket.model.WebSocketUserInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WebSocketChannelSessionBasicInfoTest {

    @Mock
    private WebSocketChannel channel;

    @Mock
    private WebSocketChannelRole role;

    private WebSocketUserInfo userInfo;
    private Set<WebSocketChannelRole> roles;
    private WebSocketChannelSessionBasicInfo info;

    @BeforeEach
    void setUp() {
        userInfo = new WebSocketUserInfo("testUser", null, "127.0.0.1");
        roles = Collections.emptySet();
        info = new WebSocketChannelSessionBasicInfo(userInfo, channel, roles);
    }

    @Test
    void constructorAndGetters() {
        assertEquals(userInfo, info.getUserInfo());
        assertEquals(channel, info.getChannel());
        assertEquals(roles, info.getRoles());
    }

    @Test
    void equalsAndHashCode() {
        WebSocketChannelSessionBasicInfo info2 = new WebSocketChannelSessionBasicInfo(userInfo, channel, roles);
        assertEquals(info, info2);
        assertEquals(info.hashCode(), info2.hashCode());
    }

    @Test
    void equalsNotEqualWhenDifferentUser() {
        WebSocketUserInfo otherUser = new WebSocketUserInfo("otherUser", null, "10.0.0.1");
        WebSocketChannelSessionBasicInfo info2 = new WebSocketChannelSessionBasicInfo(otherUser, channel, roles);
        assertNotEquals(info, info2);
    }

    @Test
    void equalsSameInstance() {
        assertEquals(info, info);
    }

    @Test
    void equalsNullReturnsFalse() {
        assertNotEquals(null, info);
    }

    @Test
    void equalsWrongTypeFalse() {
        assertNotEquals("not-an-info", info);
    }

    @Test
    void toJsonDoesNotThrow() {
        // The mock channel may cause Jackson serialization issues, but toJson should never throw
        String json = info.toJson();
        assertNotNull(json);
        assertFalse(json.isEmpty());
    }

    @Test
    void fromStringNullReturnsNull() {
        assertNull(WebSocketChannelSessionBasicInfo.fromString(null));
    }

    @Test
    void fromStringInvalidJsonReturnsNull() {
        assertNull(WebSocketChannelSessionBasicInfo.fromString("INVALID{{{"));
    }

    @Test
    void fromStringEmptyObjectReturnsNull() {
        // WebSocketChannelSessionBasicInfo has no no-arg constructor, so Jackson cannot deserialize {}
        WebSocketChannelSessionBasicInfo result = WebSocketChannelSessionBasicInfo.fromString("{}");
        assertNull(result);
    }
}
