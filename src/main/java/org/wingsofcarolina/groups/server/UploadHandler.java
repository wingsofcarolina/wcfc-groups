package org.wingsofcarolina.groups.server;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Deque;
import java.util.Iterator;

import org.wingsofcarolina.groups.MemberListXLS;
import org.wingsofcarolina.groups.domain.Member;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.form.FormData;
import io.undertow.server.handlers.form.FormData.FileItem;
import io.undertow.server.handlers.form.FormData.FormValue;
import io.undertow.server.handlers.form.FormDataParser;

public class UploadHandler implements HttpHandler { 

	private static final String DATA_DIR = System.getenv("DATA_DIR");
	
	private static ObjectMapper mapper = new ObjectMapper();
			
    @Override
    public void handleRequest(HttpServerExchange hse) throws Exception {
    	ArrayList<Member> added = new ArrayList<Member>();
    	ArrayList<Member> removed = new ArrayList<Member>();
		Iterator<Map.Entry<Integer, Member>> iterator;

        String uri = hse.getRequestURI();
        String method = hse.getRequestMethod().toString();
        System.out.println("==> " + method + " : " + uri);

        FormData attachment = hse.getAttachment(FormDataParser.FORM_DATA);
        if (attachment != null) {
	        Deque<FormValue> members = attachment.get("members");
	        if (members != null) {
		        FileItem first = members.getFirst().getFileItem();
		        if (first != null) {
		        	InputStream is = first.getInputStream();
		        
		        	String SAVED_LIST = "/data/Members-saved.xls";
		        	if (DATA_DIR != null) {
		        		SAVED_LIST = DATA_DIR + SAVED_LIST;
		        	}
					System.out.println("Retrieving previous list from : " + SAVED_LIST);
					try {
						MemberListXLS updateList = new MemberListXLS(is);
						MemberListXLS savedList = new MemberListXLS(new FileInputStream(SAVED_LIST));
						// First, remove all waitlist and cruft entries
						boolean found = false;
						savedList.clean();
						updateList.clean();

						// Look for any new members
						found = false;
						System.out.println("\nMembers to be added : \n================================");
						iterator = updateList.members().entrySet().iterator();
						while (iterator.hasNext()) {
							Map.Entry<Integer, Member> entry = iterator.next();
							Member member = entry.getValue();
							if (!savedList.hasMember(member)) {
								System.out.println(member.output());
								added.add(member);
								found = true;
							}
						}
						if (!found)
							System.out.println("No new members added.");

						// Look for any removed members
						found = false;
						System.out.println("\nMembers to be removed : \n================================");
						iterator = savedList.members().entrySet().iterator();
						while (iterator.hasNext()) {
							Map.Entry<Integer, Member> entry = iterator.next();
							Member member = entry.getValue();
							if (!updateList.hasMember(member)) {
								System.out.println(member.output());
								removed.add(member);
								found = true;
							}
						}
						if (!found)
							System.out.println("No members removed.");
					} catch (Exception ex) {
						System.out.println(ex.getMessage());
						ex.printStackTrace();
					}
				    
				    Map<String, List<Member>> response = new HashMap<String, List<Member>>();
				    response.put("removed", removed);
				    response.put("added", added);
				    String json = mapper.writeValueAsString(response);

			    	hse.getResponseSender().send(json);
			    	return;
		        }
	        }
        }
    	hse.getResponseSender().send("Upload attempt to Undertow failed");
    }
}