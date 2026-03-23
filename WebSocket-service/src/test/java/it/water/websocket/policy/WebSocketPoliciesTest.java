package it.water.websocket.policy;

import org.eclipse.jetty.websocket.api.Session;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    @SuppressWarnings("java:S2925") // Thread.sleep required to verify time-window reset in MaxMessagesPerSecondPolicy
    void maxMessagesPerSecondPolicyResetsAfterTimeWindow() throws InterruptedException {
        MaxMessagesPerSecondPolicy policy = new MaxMessagesPerSecondPolicy(session, 2);
        policy.isSatisfied(Collections.emptyMap(), new byte[0]); // init
        policy.isSatisfied(Collections.emptyMap(), new byte[0]); // count=2
        policy.isSatisfied(Collections.emptyMap(), new byte[0]); // count=3 → fail

        // Wait for time window to reset (>2 seconds — policy uses diff/1000 > 1 which requires >2s)
        Thread.sleep(2100);
        // After reset, should be satisfied again
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

    // ===================== MaxBinaryMessageBufferSizePolicy =====================

    @Test
    void maxBinaryMessageBufferSizePolicyAlwaysSatisfied() {
        MaxBinaryMessageBufferSizePolicy policy = new MaxBinaryMessageBufferSizePolicy(session, 1024);
        Map<String, Object> params = Collections.emptyMap();
        assertTrue(policy.isSatisfied(params, new byte[512]));
        assertTrue(policy.isSatisfied(params, new byte[2048])); // limit is advisory via Jetty
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

    // ===================== MaxTextMessageBufferSizePolicy =====================

    @Test
    void maxTextMessageBufferSizePolicyAlwaysSatisfied() {
        MaxTextMessageBufferSizePolicy policy = new MaxTextMessageBufferSizePolicy(session, 1024);
        assertTrue(policy.isSatisfied(Collections.emptyMap(), new byte[512]));
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

    // ===================== InputBufferSizePolicy =====================

    @Test
    void inputBufferSizePolicyAlwaysSatisfied() {
        InputBufferSizePolicy policy = new InputBufferSizePolicy(session, 4096);
        assertTrue(policy.isSatisfied(Collections.emptyMap(), new byte[0]));
        assertTrue(policy.isSatisfied(Collections.emptyMap(), new byte[8192]));
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
}
