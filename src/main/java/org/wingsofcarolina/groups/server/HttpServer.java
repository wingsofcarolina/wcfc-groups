package org.wingsofcarolina.groups.server;

import java.nio.file.Paths;

import org.wingsofcarolina.groups.persistence.Persistence;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.handlers.BlockingHandler;
import io.undertow.server.handlers.form.EagerFormParsingHandler;
import io.undertow.server.handlers.resource.PathResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;

public class HttpServer {
	public void run(final String[] args) {
		// Set up the Persistence singleton
		new Persistence().initialize(System.getenv("MONGODB"));

		RoutingHandler handler = new RoutingHandler();
		handler.addHandler("GET", "*", new GetHandler());
		handler.addHandler("POST", "/upload", new BlockingHandler(new EagerFormParsingHandler(new UploadHandler())));
		handler.addHandler("POST", "/update", new BlockingHandler(new UpdateHandler()));
		handler.addHandler("POST", "/populate", new BlockingHandler(new EagerFormParsingHandler(new PopulateHandler())));
		handler.addHandler("POST", "/test", new BlockingHandler(new TestHandler()));

		Undertow server = Undertow.builder()
				.addHttpListener(8080, "0.0.0.0")
				//.setHandler(handler)
			    .setHandler(Handlers.path()
			    		.addPrefixPath("/", handler)
			            // Serve all static files from a folder
			            .addPrefixPath("/static", new ResourceHandler(
			                new PathResourceManager(Paths.get("/tmp/"), 100))
			                .setWelcomeFiles("index.html")))

		.build();
		server.start();
	}
}