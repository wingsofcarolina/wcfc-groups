package org.wingsofcarolina.groups.server;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.wingsofcarolina.groups.domain.Member;
import org.wingsofcarolina.groups.http.GroupsIoService;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;

public class UpdateHandler implements HttpHandler {
	private static ObjectMapper mapper = new ObjectMapper();

	@Override
	public void handleRequest(HttpServerExchange hse) throws Exception {
        String uri = hse.getRequestURI();
        String method = hse.getRequestMethod().toString();
        System.out.println("==> " + method + " : " + uri);
        
        hse.startBlocking();
        InputStream is = hse.getInputStream();

        Map<String, List<Member>> result = null;
        try {
        	result = mapper.readValue(new InputStreamReader(is), new TypeReference<Map<String, List<Member>>>(){});
            System.out.println("===> " + result);
        } catch (Exception ex) {
        	System.out.println(ex.getMessage());
        }
        
        // Log into Groups.io
		GroupsIoService gio = new GroupsIoService().initialize();
		String csrf = gio.login("dfrye@wingsofcarolina.org", "Iman1tw1t@1143");
		System.out.println("csrf ==> " + csrf);

		// Remove all members that are not "checked"
		List<Member>added = clean(result.get("added"));
		List<Member>removed = clean(result.get("removed"));
		System.out.println("Added   --> " + added.size() + " : " + added);
		System.out.println("Removed --> " + removed.size() + " : " + removed);
		
		System.out.println("Updating Groups.io membership list and member database.");
		if (added.size() > 0) {
			gio.addMultipleMembers(added);
			Iterator<Member> it = added.iterator();
			while (it.hasNext()) {
				Member member = it.next();
				member.save();
			}
		}
		if (removed.size() > 0) {
			gio.removeMultipleMembers(removed);
			Iterator<Member> it = removed.iterator();
			while (it.hasNext()) {
				Member member = it.next();
				member= Member.getByID(member.getId());
				member.delete();
			}
		}
		
		hse.setStatusCode(StatusCodes.OK);
	    hse.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/html");
	    hse.getResponseSender().send("{ \"code\": 200, \"message\" : \"Membership List Updated\" }");
	}
	
	List<Member> clean(List<Member> members) {
		Iterator<Member> removedIt = members.iterator();
		while (removedIt.hasNext()) {
			Member member = removedIt.next();
			if (member.getChecked() == false) {
				removedIt.remove();
			}
		}
		return members;
	}
}