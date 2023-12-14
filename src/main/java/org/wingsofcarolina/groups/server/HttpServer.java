package org.wingsofcarolina.groups.server;

import io.undertow.Undertow;
import io.undertow.server.handlers.BlockingHandler;
import io.undertow.server.handlers.form.EagerFormParsingHandler;

public class HttpServer {
	public void run(final String[] args) {
		RoutingHandler handler = new RoutingHandler();
		handler.addHandler("GET", "*", new GetHandler());
		handler.addHandler("POST", "/upload", new BlockingHandler(new EagerFormParsingHandler(new UploadHandler())));
		handler.addHandler("POST", "/update", new BlockingHandler(new UpdateHandler()));
		
		Undertow server = Undertow.builder()
				.addHttpListener(8080, "0.0.0.0")
				.setHandler(handler)
		.build();
		server.start();
	}
}