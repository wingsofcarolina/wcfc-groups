package org.wingsofcarolina.groups;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wingsofcarolina.groups.server.HttpServer;

public class Groups {

  private static final Logger logger = LoggerFactory.getLogger(Groups.class);

  public static void main(String[] args) throws Exception {
    logger.info("Starting wcfc-groups server");
    HttpServer server = new HttpServer();
    server.run(args);
  }
}
