package org.wingsofcarolina.groups.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Optional;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;

public class GetHandler implements HttpHandler {

	@Override
	public void handleRequest(HttpServerExchange hse) throws Exception {
		StringBuffer buff = new StringBuffer();
		String path = hse.getRequestPath();
		if (path.equals("/"))
			path = "/index.html";

		InputStream in = getClass().getResourceAsStream("/assets" + path);
		if (in != null) {
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
				String line = reader.readLine();

				while (line != null) {
					buff.append(line).append("\n");
					// read next line
					line = reader.readLine();
				}

				reader.close();
			} catch (IOException ex) {
				buff.append("Resource not found.");
				hse.setStatusCode(StatusCodes.NOT_FOUND);
			}
		} else {
			buff.append("Resource not found.");
			hse.setStatusCode(StatusCodes.NOT_FOUND);
		}
		String type = mimeType(path);
        String uri = hse.getRequestURI();
        String method = hse.getRequestMethod().toString();
        System.out.println("==> " + method + "  : " + uri + " : " + type);
	    hse.getResponseHeaders().put(Headers.CONTENT_TYPE, type);
		hse.getResponseSender().send(buff.toString());
	}
	
	private String mimeType(String path) {
		String ext = getExtension(path).get();
		switch (ext) {
		case "html" : return "text/html";
		case "css" : return "text/css";
		case "js" : return "text/javascript";
		default : return "unknown";
		}
	}
	
	private Optional<String> getExtension(String filename) {
	    return Optional.ofNullable(filename)
	      .filter(f -> f.contains("."))
	      .map(f -> f.substring(filename.lastIndexOf(".") + 1));
	}
}