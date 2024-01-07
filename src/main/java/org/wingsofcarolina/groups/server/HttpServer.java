package org.wingsofcarolina.groups.server;

import org.wingsofcarolina.groups.persistence.Persistence;

import io.undertow.Undertow;
import io.undertow.server.handlers.BlockingHandler;
import io.undertow.server.handlers.form.EagerFormParsingHandler;

public class HttpServer {
	public void run(final String[] args) {
		// Set up the Persistence singleton
		new Persistence().initialize(System.getenv("MONGODB"));

		RoutingHandler handler = new RoutingHandler();
		handler.addHandler("GET", "*", new GetHandler());
		handler.addHandler("POST", "/upload", new BlockingHandler(new EagerFormParsingHandler(new UploadHandler())));
		handler.addHandler("POST", "/update", new BlockingHandler(new UpdateHandler()));
		handler.addHandler("POST", "/populate", new BlockingHandler(new EagerFormParsingHandler(new PopulateHandler())));
		handler.addHandler("POST", "/audit", new BlockingHandler(new EagerFormParsingHandler(new AuditMembers())));
		
		Undertow server = Undertow.builder()
				.addHttpListener(8080, "0.0.0.0")
				.setHandler(handler)
		.build();
		server.start();
	}
}