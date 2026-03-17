package it.water.websocket.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WebSocketUserInfoTest {

    @Test
    void constructorSetsFields() {
        WebSocketUserInfo info = new WebSocketUserInfo("alice", null, "127.0.0.1");
        assertEquals("alice", info.getUsername());
        assertNull(info.getClusterNodeInfo());
        assertEquals("127.0.0.1", info.getIpAddress());
    }

    @Test
    void equalsByUsernameOnly() {
        WebSocketUserInfo a = new WebSocketUserInfo("alice", null, "1.2.3.4");
        WebSocketUserInfo b = new WebSocketUserInfo("alice", null, "9.9.9.9");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void notEqualForDifferentUsernames() {
        WebSocketUserInfo a = new WebSocketUserInfo("alice", null, "1.2.3.4");
        WebSocketUserInfo b = new WebSocketUserInfo("bob", null, "1.2.3.4");
        assertNotEquals(a, b);
    }

    @Test
    void notEqualToNull() {
        WebSocketUserInfo a = new WebSocketUserInfo("alice", null, "1.2.3.4");
        assertNotEquals(a, null);
    }

    @Test
    void notEqualToDifferentType() {
        WebSocketUserInfo a = new WebSocketUserInfo("alice", null, "1.2.3.4");
        assertNotEquals(a, "alice");
    }

    @Test
    void isOnLocalNodeReturnsTrueWhenClusterInfoIsNull() {
        WebSocketUserInfo info = new WebSocketUserInfo("alice", null, "127.0.0.1");
        assertTrue(info.isOnLocalNode(null));
    }

    @Test
    void isOnLocalNodeReturnsTrueWhenLocalNodeInfoIsNull() {
        WebSocketUserInfo info = new WebSocketUserInfo("alice", null, "127.0.0.1");
        assertTrue(info.isOnLocalNode(null));
    }

    @Test
    void toJsonSerializesUsername() {
        WebSocketUserInfo info = new WebSocketUserInfo("alice", null, "127.0.0.1");
        String json = info.toJson();
        assertNotNull(json);
        assertTrue(json.contains("alice"));
    }

    @Test
    void fromStringDeserializesValidJson() {
        String json = "{\"username\":\"bob\",\"ipAddress\":\"10.0.0.1\"}";
        WebSocketUserInfo info = WebSocketUserInfo.fromString(json);
        assertNotNull(info);
        assertEquals("bob", info.getUsername());
    }

    @Test
    void fromStringReturnsNullOnInvalidJson() {
        WebSocketUserInfo info = WebSocketUserInfo.fromString("NOT_JSON{{{{");
        assertNull(info);
    }

    @Test
    void fromStringReturnsNullOnNull() {
        WebSocketUserInfo info = WebSocketUserInfo.fromString(null);
        assertNull(info);
    }

    @Test
    void sameObjectEqualsItself() {
        WebSocketUserInfo a = new WebSocketUserInfo("alice", null, "1.2.3.4");
        assertEquals(a, a);
    }
}
