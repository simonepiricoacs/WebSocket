package it.water.websocket.model.message;

import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class WebSocketMessageTest {

    @Test
    void defaultConstructorInitializesParamsAndContentType() {
        WebSocketMessage msg = new WebSocketMessage();
        assertNotNull(msg.getParams());
        assertEquals("text/plain", msg.getContentType());
        assertNull(msg.getCmd());
        assertNull(msg.getPayload());
        assertNull(msg.getType());
        assertNull(msg.getTimestamp());
    }

    @Test
    void createMessageSetsAllFields() {
        byte[] payload = "hello".getBytes();
        WebSocketMessage msg = WebSocketMessage.createMessage("myCmd", payload, WebSocketMessageType.OK);

        assertEquals("myCmd", msg.getCmd());
        assertArrayEquals(payload, msg.getPayload());
        assertEquals(WebSocketMessageType.OK, msg.getType());
        assertNotNull(msg.getTimestamp());
    }

    @Test
    void createMessageWithNullCmdAndNullPayload() {
        WebSocketMessage msg = WebSocketMessage.createMessage(null, null, WebSocketMessageType.ERROR);
        assertNull(msg.getCmd());
        assertNull(msg.getPayload());
        assertEquals(WebSocketMessageType.ERROR, msg.getType());
        assertNotNull(msg.getTimestamp());
    }

    @Test
    void fromStringDeserializesValidJson() {
        String json = "{\"cmd\":\"TEST\",\"type\":\"OK\",\"contentType\":\"text/plain\"}";
        WebSocketMessage msg = WebSocketMessage.fromString(json);
        assertNotNull(msg);
        assertEquals("TEST", msg.getCmd());
        assertEquals(WebSocketMessageType.OK, msg.getType());
    }

    @Test
    void fromStringReturnsNullOnInvalidJson() {
        WebSocketMessage msg = WebSocketMessage.fromString("NOT_VALID_JSON{{{");
        assertNull(msg);
    }

    @Test
    void fromStringReturnsNullOnNull() {
        WebSocketMessage msg = WebSocketMessage.fromString(null);
        assertNull(msg);
    }

    @Test
    void toJsonSerializesMessage() {
        WebSocketMessage msg = WebSocketMessage.createMessage("cmd1", "data".getBytes(), WebSocketMessageType.RESULT);
        String json = msg.toJson();
        assertNotNull(json);
        assertTrue(json.contains("cmd1"));
    }

    @Test
    void settersAndGettersWork() {
        WebSocketMessage msg = new WebSocketMessage();
        msg.setCmd("c");
        msg.setPayload(new byte[]{1, 2});
        msg.setType(WebSocketMessageType.ERROR);
        msg.setContentType("application/json");

        HashMap<String, String> params = new HashMap<>();
        params.put("key", "val");
        msg.setParams(params);

        assertEquals("c", msg.getCmd());
        assertArrayEquals(new byte[]{1, 2}, msg.getPayload());
        assertEquals(WebSocketMessageType.ERROR, msg.getType());
        assertEquals("application/json", msg.getContentType());
        assertEquals("val", msg.getParams().get("key"));
    }

    @Test
    void paramsCanBeModifiedDirectly() {
        WebSocketMessage msg = new WebSocketMessage();
        msg.getParams().put("sender", "alice");
        assertEquals("alice", msg.getParams().get("sender"));
    }

    @Test
    void fromStringRoundTripPreservesCmd() {
        WebSocketMessage original = WebSocketMessage.createMessage("ROUND_TRIP", "payload".getBytes(), WebSocketMessageType.OK);
        original.getParams().put("sender", "bob");
        String json = original.toJson();
        WebSocketMessage restored = WebSocketMessage.fromString(json);
        assertNotNull(restored);
        assertEquals("ROUND_TRIP", restored.getCmd());
        assertEquals(WebSocketMessageType.OK, restored.getType());
    }
}
