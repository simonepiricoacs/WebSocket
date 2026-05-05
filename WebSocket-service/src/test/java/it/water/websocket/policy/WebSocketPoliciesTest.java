package it.water.websocket.policy;

import org.eclipse.jetty.websocket.api.Session;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("java:S3415") // assertion argument order is correct; SonarQube cannot infer expected/actual for computed values
class WebSocketPoliciesTest {

    @Mock
    private Session session;

    // ===================== MaxPayloadBytesPolicy =====================

    @Test
    void maxPayloadBytesPolicySatisfiedWhenUnderLimit() {
        MaxPayloadBytesPolicy policy = new MaxPayloadBytesPolicy(session, 100);
        assertTrue(policy.isSatisfied(Collections.emptyMap(), new byte[50]));
    }

    @Test
    void maxPayloadBytesPolicySatisfiedAtExactLimit() {
        MaxPayloadBytesPolicy policy = new MaxPayloadBytesPolicy(session, 100);
        assertTrue(policy.isSatisfied(Collections.emptyMap(), new byte[100]));
    }

    @Test
    void maxPayloadBytesPolicyFailsWhenOverLimit() {
        MaxPayloadBytesPolicy policy = new MaxPayloadBytesPolicy(session, 10);
        assertFalse(policy.isSatisfied(Collections.emptyMap(), new byte[11]));
    }

    @Test
    void maxPayloadBytesPolicyBehaviorFlags() {
        MaxPayloadBytesPolicy policy = new MaxPayloadBytesPolicy(session, 100);
        assertFalse(policy.closeWebSocketOnFail());
        assertTrue(policy.printWarningOnFail());
        assertTrue(policy.sendWarningBackToClientOnFail());
        assertTrue(policy.ignoreMessageOnFail());
    }

    // ===================== MaxMessagesPerSecondPolicy =====================

    @Test
    void maxMessagesPerSecondPolicyFirstCallAlwaysSatisfied() {
        MaxMessagesPerSecondPolicy policy = new MaxMessagesPerSecondPolicy(session, 5);
        assertTrue(policy.isSatisfied(Collections.emptyMap(), new byte[0]));
        assertNotEquals(-1, policy.getStartTimestamp()); // after first call startTimestamp is initialized
    }

    @Test
    void maxMessagesPerSecondPolicySatisfiedWithinLimit() {
        MaxMessagesPerSecondPolicy policy = new MaxMessagesPerSecondPolicy(session, 10);
        // First call sets timestamp
        policy.isSatisfied(Collections.emptyMap(), new byte[0]);
        // Next calls within limit
        for (int i = 0; i < 9; i++) {
            assertTrue(policy.isSatisfied(Collections.emptyMap(), new byte[0]));
        }
    }

    @Test
    void maxMessagesPerSecondPolicyFailsWhenExceedingLimit() {
        MaxMessagesPerSecondPolicy policy = new MaxMessagesPerSecondPolicy(session, 2);
        policy.isSatisfied(Collections.emptyMap(), new byte[0]); // init
        policy.isSatisfied(Collections.emptyMap(), new byte[0]); // count=2 → ok
        policy.isSatisfied(Collections.emptyMap(), new byte[0]); // count=3 → fail
        assertFalse(policy.isSatisfied(Collections.emptyMap(), new byte[0])); // count=4 → fail
    }

    @Test
    void maxMessagesPerSecondPolicyResetsAfterTimeWindow() throws Exception {
        MaxMessagesPerSecondPolicy policy = new MaxMessagesPerSecondPolicy(session, 2);
        policy.isSatisfied(Collections.emptyMap(), new byte[0]); // init
        policy.isSatisfied(Collections.emptyMap(), new byte[0]); // count=2
        policy.isSatisfied(Collections.emptyMap(), new byte[0]); // count=3 → fail

        Field startTimestampField = MaxMessagesPerSecondPolicy.class.getDeclaredField("startTimestamp");
        startTimestampField.setAccessible(true);
        startTimestampField.setLong(policy, System.currentTimeMillis() - 2100);

        assertTrue(policy.isSatisfied(Collections.emptyMap(), new byte[0]));
        assertEquals(1, policy.getCount());
    }

    @Test
    void maxMessagesPerSecondPolicyBehaviorFlags() {
        MaxMessagesPerSecondPolicy policy = new MaxMessagesPerSecondPolicy(session, 5);
        assertTrue(policy.closeWebSocketOnFail());
        assertTrue(policy.printWarningOnFail());
        assertFalse(policy.sendWarningBackToClientOnFail());
    }

    @Test
    void maxMessagesPerSecondPolicyEqualityRequiresSameThreshold() {
        MaxMessagesPerSecondPolicy policy = new MaxMessagesPerSecondPolicy(session, 5);
        MaxMessagesPerSecondPolicy same = new MaxMessagesPerSecondPolicy(session, 5);
        MaxMessagesPerSecondPolicy different = new MaxMessagesPerSecondPolicy(session, 6);

        assertEquals(policy, same);
        assertEquals(policy.hashCode(), same.hashCode());
        assertNotEquals(policy, different);
    }

    // ===================== MaxBinaryMessageBufferSizePolicy =====================

    @Test
    void maxBinaryMessageBufferSizePolicyAlwaysSatisfied() {
        MaxBinaryMessageBufferSizePolicy policy = new MaxBinaryMessageBufferSizePolicy(session, 1024);
        Map<String, Object> params = Collections.emptyMap();
        assertTrue(policy.isSatisfied(params, new byte[512]));
        assertTrue(policy.isSatisfied(params, new byte[2048])); // limit is advisory via Jetty
    }

    @Test
    void maxBinaryMessageBufferSizePolicyExposesFlagsAndEquality() {
        MaxBinaryMessageBufferSizePolicy policy = new MaxBinaryMessageBufferSizePolicy(session, 1024);
        MaxBinaryMessageBufferSizePolicy same = new MaxBinaryMessageBufferSizePolicy(session, 1024);
        MaxBinaryMessageBufferSizePolicy different = new MaxBinaryMessageBufferSizePolicy(session, 2048);

        assertEquals(1024, policy.getMaxBinaryMessageBufferSize());
        assertFalse(policy.closeWebSocketOnFail());
        assertFalse(policy.printWarningOnFail());
        assertFalse(policy.sendWarningBackToClientOnFail());
        assertFalse(policy.ignoreMessageOnFail());
        assertEquals(policy, same);
        assertEquals(policy.hashCode(), same.hashCode());
        assertNotEquals(policy, different);
    }

    // ===================== MaxTextMessageSizePolicy =====================
    // NOTE: isSatisfied() always returns true — enforcement is delegated to Jetty internally

    @Test
    void maxTextMessageSizePolicyAlwaysSatisfied() {
        MaxTextMessageSizePolicy policy = new MaxTextMessageSizePolicy(session, 256);
        assertTrue(policy.isSatisfied(Collections.emptyMap(), new byte[100]));
        assertTrue(policy.isSatisfied(Collections.emptyMap(), new byte[512])); // over limit → still true
    }

    @Test
    void maxTextMessageSizePolicyGetterWorks() {
        MaxTextMessageSizePolicy policy = new MaxTextMessageSizePolicy(session, 1024);
        assertEquals(1024, policy.getMaxTextMessageSizePolicy());
    }

    @Test
    void maxTextMessageSizePolicyBehaviorFlags() {
        MaxTextMessageSizePolicy policy = new MaxTextMessageSizePolicy(session, 256);
        assertFalse(policy.closeWebSocketOnFail());
        assertFalse(policy.printWarningOnFail());
        assertFalse(policy.sendWarningBackToClientOnFail());
        assertFalse(policy.ignoreMessageOnFail());
    }

    @Test
    void maxTextMessageSizePolicyEqualityRequiresSameTypeAndSize() {
        MaxTextMessageSizePolicy policy = new MaxTextMessageSizePolicy(session, 256);
        MaxTextMessageSizePolicy same = new MaxTextMessageSizePolicy(session, 256);
        MaxTextMessageSizePolicy different = new MaxTextMessageSizePolicy(session, 512);

        assertEquals(policy, same);
        assertEquals(policy.hashCode(), same.hashCode());
        assertNotEquals(policy, different);
        assertNotEquals(policy, new MaxPayloadBytesPolicy(session, 256));
    }

    // ===================== MaxTextMessageBufferSizePolicy =====================

    @Test
    void maxTextMessageBufferSizePolicyAlwaysSatisfied() {
        MaxTextMessageBufferSizePolicy policy = new MaxTextMessageBufferSizePolicy(session, 1024);
        assertTrue(policy.isSatisfied(Collections.emptyMap(), new byte[512]));
    }

    @Test
    void maxTextMessageBufferSizePolicyExposesFlagsAndEquality() {
        MaxTextMessageBufferSizePolicy policy = new MaxTextMessageBufferSizePolicy(session, 1024);
        MaxTextMessageBufferSizePolicy same = new MaxTextMessageBufferSizePolicy(session, 1024);
        MaxTextMessageBufferSizePolicy different = new MaxTextMessageBufferSizePolicy(session, 2048);

        assertEquals(1024, policy.getMaxTextMessageBufferSize());
        assertFalse(policy.closeWebSocketOnFail());
        assertFalse(policy.printWarningOnFail());
        assertFalse(policy.sendWarningBackToClientOnFail());
        assertFalse(policy.ignoreMessageOnFail());
        assertEquals(policy, same);
        assertEquals(policy.hashCode(), same.hashCode());
        assertNotEquals(policy, different);
    }

    // ===================== MaxBinaryMessageSizePolicy =====================

    @Test
    void maxBinaryMessageSizePolicySatisfiedUnderLimit() {
        MaxBinaryMessageSizePolicy policy = new MaxBinaryMessageSizePolicy(session, 512);
        assertTrue(policy.isSatisfied(Collections.emptyMap(), new byte[100]));
    }

    @Test
    void maxBinaryMessageSizePolicyAlwaysSatisfied() {
        // isSatisfied() always returns true — enforcement is delegated to Jetty internally
        MaxBinaryMessageSizePolicy policy = new MaxBinaryMessageSizePolicy(session, 10);
        assertTrue(policy.isSatisfied(Collections.emptyMap(), new byte[20]));
    }

    @Test
    void maxBinaryMessageSizePolicyExposesFlagsAndEquality() {
        MaxBinaryMessageSizePolicy policy = new MaxBinaryMessageSizePolicy(session, 512);
        MaxBinaryMessageSizePolicy same = new MaxBinaryMessageSizePolicy(session, 512);
        MaxBinaryMessageSizePolicy different = new MaxBinaryMessageSizePolicy(session, 1024);

        assertEquals(512, policy.getMaxBinaryMessageSize());
        assertFalse(policy.closeWebSocketOnFail());
        assertFalse(policy.printWarningOnFail());
        assertFalse(policy.sendWarningBackToClientOnFail());
        assertFalse(policy.ignoreMessageOnFail());
        assertEquals(policy, same);
        assertEquals(policy.hashCode(), same.hashCode());
        assertNotEquals(policy, different);
    }

    // ===================== InputBufferSizePolicy =====================

    @Test
    void inputBufferSizePolicyAlwaysSatisfied() {
        InputBufferSizePolicy policy = new InputBufferSizePolicy(session, 4096);
        assertTrue(policy.isSatisfied(Collections.emptyMap(), new byte[0]));
        assertTrue(policy.isSatisfied(Collections.emptyMap(), new byte[8192]));
    }

    @Test
    void inputBufferSizePolicyExposesFlagsAndEquality() {
        InputBufferSizePolicy policy = new InputBufferSizePolicy(session, 4096);
        InputBufferSizePolicy same = new InputBufferSizePolicy(session, 4096);
        InputBufferSizePolicy different = new InputBufferSizePolicy(session, 8192);

        assertEquals(4096, policy.getInputBufferSize());
        assertFalse(policy.closeWebSocketOnFail());
        assertFalse(policy.printWarningOnFail());
        assertFalse(policy.sendWarningBackToClientOnFail());
        assertFalse(policy.ignoreMessageOnFail());
        assertEquals(policy, same);
        assertEquals(policy.hashCode(), same.hashCode());
        assertNotEquals(policy, different);
    }

    // ===================== WebSocketAbstractPolicy equals/hashCode =====================

    @Test
    void abstractPolicyEqualityBasedOnSession() {
        MaxPayloadBytesPolicy p1 = new MaxPayloadBytesPolicy(session, 100);
        MaxPayloadBytesPolicy p2 = new MaxPayloadBytesPolicy(session, 100);
        assertEquals(p1, p2); // same session + same field value → equal
        assertEquals(p1.hashCode(), p2.hashCode());
    }

    @Test
    void abstractPolicyNotEqualWhenFieldDiffers() {
        MaxPayloadBytesPolicy p1 = new MaxPayloadBytesPolicy(session, 100);
        MaxPayloadBytesPolicy p2 = new MaxPayloadBytesPolicy(session, 200);
        assertNotEquals(p1, p2); // same session but different maxPayloadBytes → not equal
    }

    @Test
    void abstractPolicySameInstanceIsEqual() {
        MaxPayloadBytesPolicy p1 = new MaxPayloadBytesPolicy(session, 100);
        assertEquals(p1, p1);
    }

    @Test
    void abstractPolicyNotEqualToDifferentType() {
        MaxPayloadBytesPolicy p = new MaxPayloadBytesPolicy(session, 100);
        assertNotEquals(p, "some-string");
    }

    @Test
    void abstractPolicyNotEqualToNull() {
        MaxPayloadBytesPolicy p = new MaxPayloadBytesPolicy(session, 100);
        assertNotEquals(null, p);
    }
}
