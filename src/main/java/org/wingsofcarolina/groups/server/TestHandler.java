package org.wingsofcarolina.groups.server;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wingsofcarolina.groups.domain.Member;
import org.wingsofcarolina.groups.http.APIException;
import org.wingsofcarolina.groups.http.ManualsService;

public class TestHandler implements HttpHandler {

  private static final Logger logger = LoggerFactory.getLogger(TestHandler.class);

  @Override
  public void handleRequest(HttpServerExchange hse) throws Exception {
    ManualsService mio = new ManualsService().initialize();

    Member member = new Member(4321, "Herbie", "Frye", "herbie@planez.co", 99);

    logger.info("Adding ---> " + member.toString());
    try {
      mio.removeMember(member);
    } catch (APIException ex) {
      logger.info("Ooopsie! Had a boo-boo!");
      ex.printStackTrace();
    }
  }
}
