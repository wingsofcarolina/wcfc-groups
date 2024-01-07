package org.wingsofcarolina.groups.server;

import java.io.InputStream;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wingsofcarolina.groups.GroupListCSV;
import org.wingsofcarolina.groups.domain.Member;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.form.FormData;
import io.undertow.server.handlers.form.FormDataParser;
import io.undertow.server.handlers.form.FormData.FileItem;
import io.undertow.server.handlers.form.FormData.FormValue;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;

public class AuditMembers implements HttpHandler {
	private static final Logger logger = LoggerFactory.getLogger(AuditMembers.class);

	@Override
	public void handleRequest(HttpServerExchange hse) throws Exception {
		Iterator<Map.Entry<Integer, Member>> iterator;

		String uri = hse.getRequestURI();
		String method = hse.getRequestMethod().toString();
		logger.info("==> " + method + " : " + uri);

		FormData attachment = hse.getAttachment(FormDataParser.FORM_DATA);
		if (attachment != null) {
			Deque<FormValue> members = attachment.get("groups-io");
			if (members != null) {
				FileItem first = members.getFirst().getFileItem();
				if (first != null) {
					InputStream is = first.getInputStream();
					try {
						GroupListCSV updateList = new GroupListCSV(is);

						// Snag all the members
						iterator = updateList.members().entrySet().iterator();
						while (iterator.hasNext()) {
							Map.Entry<Integer, Member> entry = iterator.next();
							Member audit = entry.getValue();
							Member member = Member.getByEmail(audit.getEmail().toLowerCase());
							if (member == null) {
								logger.info("In Groups.io/Not in Database ==> " + audit.getName() + " : " + audit.getEmail());
							}
						}
					} catch (Exception ex) {
						logger.info(ex.getMessage());
						ex.printStackTrace();
					}
				}
			}
			hse.setStatusCode(StatusCodes.OK);
			hse.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/html");
			hse.getResponseSender().send("{ \"code\": 200, \"message\" : \"Audit completed.\" }");
		} else {
			hse.setStatusCode(StatusCodes.BAD_REQUEST);
			hse.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/html");
			hse.getResponseSender().send("{ \"code\": 400, \"message\" : \"We are not ammused.\" }");			
		}
	}
}