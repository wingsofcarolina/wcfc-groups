package org.wingsofcarolina.groups.server;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.handlers.BlockingHandler;
import io.undertow.server.handlers.form.EagerFormParsingHandler;
import io.undertow.server.handlers.resource.PathResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;
import java.nio.file.Paths;

public class HttpServer {

  public void run(final String[] args) {
    RoutingHandler handler = new RoutingHandler();
    handler.addHandler("GET", "*", new GetHandler());
    handler.addHandler(
      "POST",
      "/upload",
      new BlockingHandler(new EagerFormParsingHandler(new UploadHandler()))
    );
    handler.addHandler("POST", "/update", new BlockingHandler(new UpdateHandler()));
    handler.addHandler("GET", "/api/version", new VersionHandler());

    Undertow server = Undertow
      .builder()
      .addHttpListener(9301, "0.0.0.0")
      //.setHandler(handler)
      .setHandler(
        Handlers
          .path()
          .addPrefixPath("/", handler)
          // Serve all static files from a folder
          .addPrefixPath(
            "/static",
            new ResourceHandler(new PathResourceManager(Paths.get("/tmp/"), 100))
              .setWelcomeFiles("index.html")
          )
      )
      .build();
    server.start();
  }
}
