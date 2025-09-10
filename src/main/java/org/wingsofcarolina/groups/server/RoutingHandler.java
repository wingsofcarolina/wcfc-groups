package org.wingsofcarolina.groups.server;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RoutingHandler implements HttpHandler {

  private static final Logger logger = LoggerFactory.getLogger(RoutingHandler.class);

  private Map<String, Map<String, HttpHandler>> methodMap = new HashMap<String, Map<String, HttpHandler>>();

  public RoutingHandler() {
    methodMap.put("GET", new HashMap<String, HttpHandler>());
    methodMap.put("POST", new HashMap<String, HttpHandler>());
  }

  public void addHandler(String method, String uri, HttpHandler handler) {
    Map<String, HttpHandler> map = methodMap.get(method);
    map.put(uri, handler);
  }

  @Override
  public void handleRequest(HttpServerExchange httpServerExchange) throws Exception {
    HttpHandler handler = null;
    String uri = httpServerExchange.getRequestURI();
    String method = httpServerExchange.getRequestMethod().toString();

    Map<String, HttpHandler> map = methodMap.get(method);
    Iterator<Entry<String, HttpHandler>> it = map.entrySet().iterator();
    while (it.hasNext()) {
      Entry<String, HttpHandler> entry = it.next();
      String key = entry.getKey();
      if (key.equals("*") || key.equals(uri)) {
        handler = entry.getValue();
        break;
      }
    }
    if (handler != null) {
      handler.handleRequest(httpServerExchange);
    } else {
      logger.info("Handler not found .... " + method + " : " + uri);
    }
  }
}
