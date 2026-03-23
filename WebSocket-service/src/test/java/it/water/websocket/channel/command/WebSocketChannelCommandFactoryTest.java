package it.water.websocket.channel.command;

import it.water.core.api.registry.ComponentRegistry;
import it.water.core.api.registry.filter.ComponentFilter;
import it.water.core.api.registry.filter.ComponentFilterBuilder;
import it.water.core.model.exceptions.WaterRuntimeException;
import it.water.websocket.api.channel.WebSocketChannelCommand;
import it.water.websocket.api.channel.WebSocketChannelRemoteCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WebSocketChannelCommandFactoryTest {

    @Mock
    private ComponentRegistry registry;

    @Mock
    private ComponentFilterBuilder filterBuilder;

    @Mock
    private ComponentFilter filter;

    @Mock
    private WebSocketChannelCommand mockCommand;

    @Mock
    private WebSocketChannelRemoteCommand mockRemoteCommand;

    private WebSocketChannelCommandFactory factory;

    @BeforeEach
    void setUp() {
        when(registry.getComponentFilterBuilder()).thenReturn(filterBuilder);
        when(filterBuilder.createFilter(anyString(), anyString())).thenReturn(filter);
        factory = new WebSocketChannelCommandFactory(registry);
    }

    @Test
    void createCommandReturnsCommand() {
        when(registry.findComponents(WebSocketChannelCommand.class, filter))
                .thenReturn(List.of(mockCommand));
        WebSocketChannelCommand result = factory.createCommand("SOME_CMD");
        assertEquals(mockCommand, result);
    }

    @Test
    void createCommandNotFoundThrows() {
        when(registry.findComponents(WebSocketChannelCommand.class, filter))
                .thenReturn(Collections.emptyList());
        assertThrows(WaterRuntimeException.class, () -> factory.createCommand("MISSING_CMD"));
    }

    @Test
    void createCommandDuplicateThrows() {
        when(registry.findComponents(WebSocketChannelCommand.class, filter))
                .thenReturn(List.of(mockCommand, mockCommand));
        assertThrows(WaterRuntimeException.class, () -> factory.createCommand("DUPLICATE_CMD"));
    }

    @Test
    void createRemoteCommandReturnsRemoteCommand() {
        when(registry.findComponents(WebSocketChannelRemoteCommand.class, filter))
                .thenReturn(List.of(mockRemoteCommand));
        WebSocketChannelRemoteCommand result = factory.createRemoteCommand("SOME_REMOTE_CMD");
        assertEquals(mockRemoteCommand, result);
    }

    @Test
    void createRemoteCommandNotFoundThrows() {
        when(registry.findComponents(WebSocketChannelRemoteCommand.class, filter))
                .thenReturn(Collections.emptyList());
        assertThrows(WaterRuntimeException.class, () -> factory.createRemoteCommand("MISSING_CMD"));
    }
}
