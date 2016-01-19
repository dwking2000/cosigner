package io.emax.cosigner.core.resources.websocket;

import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

public class WebSocketSocket extends WebSocketServlet {
  @Override
  public void configure(WebSocketServletFactory webSocketServletFactory) {
    webSocketServletFactory.register(WebSocketResource.class);
  }
}
