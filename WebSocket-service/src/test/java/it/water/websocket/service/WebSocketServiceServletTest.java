package it.water.websocket.service;

import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WebSocketServiceServletTest {

    @Mock
    private WebSocketServletFactory mockFactory;

    @Test
    void configureRegistersWebSocketService() {
        WebSocketServiceServlet servlet = new WebSocketServiceServlet();
        servlet.configure(mockFactory);
        verify(mockFactory).register(WebSocketService.class);
    }
}
