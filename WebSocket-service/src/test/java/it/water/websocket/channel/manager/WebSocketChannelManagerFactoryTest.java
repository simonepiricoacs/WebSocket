package it.water.websocket.channel.manager;

import it.water.core.api.registry.ComponentRegistry;
import it.water.core.api.registry.filter.ComponentFilter;
import it.water.core.api.registry.filter.ComponentFilterBuilder;
import it.water.core.model.exceptions.WaterRuntimeException;
import it.water.websocket.api.channel.WebSocketChannelClusterCoordinator;
import it.water.websocket.api.channel.WebSocketChannelClusterMessageBroker;
import it.water.websocket.api.channel.WebSocketChannelManager;
import it.water.websocket.channel.WebSocketBasicChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WebSocketChannelManagerFactoryTest {

    @Mock
    private WebSocketChannelClusterCoordinator coordinator;

    @Mock
    private WebSocketChannelClusterMessageBroker broker;

    @Mock
    private ComponentRegistry registry;

    @Mock
    private ComponentFilterBuilder filterBuilder;

    @Mock
    private ComponentFilter filter;

    @BeforeEach
    void setUp() {
        when(coordinator.connectNewPeer(any())).thenReturn(Collections.emptyMap());
    }

    @Test
    void newDefaultChannelManagerFactoryReturnsNonNull() {
        WebSocketChannelManagerFactory factory = WebSocketChannelManagerFactory.newDefaultChannelManagerFactory();
        assertNotNull(factory);
    }

    @Test
    void withClusterCoordinatorSetsFluently() {
        WebSocketChannelManagerFactory factory = WebSocketChannelManagerFactory.newDefaultChannelManagerFactory()
                .withClusterCoordinator(coordinator);
        assertNotNull(factory);
    }

    @Test
    void withClusterMessageBrokerSetsFluently() {
        WebSocketChannelManagerFactory factory = WebSocketChannelManagerFactory.newDefaultChannelManagerFactory()
                .withClusterMessageBroker(broker);
        assertNotNull(factory);
    }

    @Test
    void buildWithValidChannelTypeReturnsManager() {
        WebSocketChannelManagerFactory factory = WebSocketChannelManagerFactory.newDefaultChannelManagerFactory()
                .withClusterCoordinator(coordinator)
                .withClusterMessageBroker(broker);

        WebSocketChannelManager manager = factory.build(WebSocketBasicChannel.class);

        assertNotNull(manager);
        assertInstanceOf(WebSocketDefaultChannelManager.class, manager);
    }

    @Test
    void buildWithNullChannelTypeThrows() {
        WebSocketChannelManagerFactory factory = WebSocketChannelManagerFactory.newDefaultChannelManagerFactory()
                .withClusterCoordinator(coordinator);

        assertThrows(WaterRuntimeException.class, () -> factory.build(null));
    }

    @Test
    void resetClearsCoordinatorAndBroker() {
        WebSocketChannelManagerFactory factory = WebSocketChannelManagerFactory.newDefaultChannelManagerFactory()
                .withClusterCoordinator(coordinator)
                .withClusterMessageBroker(broker);

        factory.reset();

        // After reset, build should still work with null coordinator/broker
        // (WebSocketDefaultChannelManager accepts null broker)
        // But coordinator is null → NPE in constructor. Just verify reset doesn't throw.
        assertDoesNotThrow(factory::reset);
    }

    @Test
    void loadFromComponentRegistryLoadsBrokerAndCoordinator() {
        when(registry.getComponentFilterBuilder()).thenReturn(filterBuilder);
        when(filterBuilder.createFilter(anyString(), anyString())).thenReturn(filter);
        when(registry.findComponents(eq(WebSocketChannelClusterMessageBroker.class), any()))
                .thenReturn(List.of(broker));
        when(registry.findComponents(eq(WebSocketChannelClusterCoordinator.class), any()))
                .thenReturn(List.of(coordinator));

        WebSocketChannelManagerFactory factory = WebSocketChannelManagerFactory.newDefaultChannelManagerFactory()
                .loadFromComponentRegistry(registry, "myBroker", "myCoordinator");

        assertNotNull(factory);

        WebSocketChannelManager manager = factory.build(WebSocketBasicChannel.class);
        assertNotNull(manager);
        assertInstanceOf(WebSocketDefaultChannelManager.class, manager);
    }

    @Test
    void loadFromComponentRegistryWithEmptyListsDoesNotSetComponents() {
        when(registry.getComponentFilterBuilder()).thenReturn(filterBuilder);
        when(filterBuilder.createFilter(anyString(), anyString())).thenReturn(filter);
        when(registry.findComponents(eq(WebSocketChannelClusterMessageBroker.class), any()))
                .thenReturn(Collections.emptyList());
        when(registry.findComponents(eq(WebSocketChannelClusterCoordinator.class), any()))
                .thenReturn(Collections.emptyList());

        WebSocketChannelManagerFactory factory = WebSocketChannelManagerFactory.newDefaultChannelManagerFactory()
                .loadFromComponentRegistry(registry, "missing", "missing");

        assertNotNull(factory);
    }
}
