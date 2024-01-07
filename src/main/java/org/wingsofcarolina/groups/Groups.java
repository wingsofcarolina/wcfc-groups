package org.wingsofcarolina.groups;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wingsofcarolina.groups.domain.Member;
import org.wingsofcarolina.groups.http.APIException;
import org.wingsofcarolina.groups.http.GroupsIoService;
import org.wingsofcarolina.groups.server.HttpServer;

public class Groups {
	private static final Logger logger = LoggerFactory.getLogger(Groups.class);
	
	private static final String SAVED_LIST = "./Members-saved.xls";
	private static final String UPDATE_LIST = "./Members-update.xls";
	
	private ArrayList<Member> added = new ArrayList<Member>();
	private ArrayList<Member> removed = new ArrayList<Member>();
			
	public static void main(String[] args) throws Exception {
		logger.info("Starting wcfc-groups server");
		HttpServer server = new HttpServer();
		server.run(args);
		
		//new Groups().process();
	}
	
	private void experiment() throws APIException, IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

		GroupsIoService gio = new GroupsIoService().initialize();
		String csrf = gio.login("dfrye@wingsofcarolina.org", "Iman1tw1t@1143");
		System.out.println("csrf ==> " + csrf);
		boolean result = gio.addMember(csrf, "Dwight Frye <dwightrfrye@gmail.com>");
		System.out.println("Add result == > " + result);
		
		System.out.print("Pausing ... ");
		br.readLine();
		
		result = gio.removeMember(csrf, "dwightrfrye@gmail.com");
		System.out.println("Add result == > " + result);
	}
	
	private void process() throws Exception {
		Iterator<Map.Entry<Integer, Member>> iterator;
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

	    File saved = new File(SAVED_LIST);
	    File update = new File(UPDATE_LIST);

	    if ( ! saved.exists() || ! update.exists()) {
	    	System.out.println("Missing either " + SAVED_LIST + " or " + UPDATE_LIST);
	    } else {
			MemberListXLS savedList = new MemberListXLS(new FileInputStream(SAVED_LIST));
			MemberListXLS updateList = new MemberListXLS(new FileInputStream(UPDATE_LIST));
			
			boolean found;
			
			// First, remove all waitlist and cruft entries
		    savedList.clean();
		    updateList.clean();
		    
		    // Look for any new members
	    	found = false;
		    System.out.println("\nMembers to be added : \n================================");
		    iterator = updateList.members().entrySet().iterator();
		    while (iterator.hasNext()) {
		        Map.Entry<Integer, Member> entry = iterator.next();
		        Member member = entry.getValue();
		        if ( ! savedList.hasMember(member)) {
		        	System.out.println(member.output());
		        	added.add(member);
		        	found = true;
		        }
		    }
		    if (!found) System.out.println("No new members added.");
	
		    // Look for any removed members
	    	found = false;
		    System.out.println("\nMembers to be removed : \n================================");
		    iterator = savedList.members().entrySet().iterator();
		    while (iterator.hasNext()) {
		        Map.Entry<Integer, Member> entry = iterator.next();
		        Member member = entry.getValue();
		        if ( ! updateList.hasMember(member)) {
		        	System.out.println(member.output());
		        	removed.add(member);
		        	found = true;
		        }
		    }
		    if (!found) System.out.println("No members removed.");
		    
		    // Get a yes/no to proceeding
//		    System.out.print("\nPerform automated updating of Wings-of-Carolina mailing list? (yes/no) : ");
//		    String answer = br.readLine();
//		    if (answer.equalsIgnoreCase("yes")) {
//		    	System.out.println("We'll surely do that once I've got it fully implemented, until then do it yourself.");
//		    } else if (answer.equalsIgnoreCase("no")) {
//		    	System.out.println("As you wish.");
//		    } else {
//		    	System.out.println("Can't you read directions. A simple yes/no is all that was asked for.");
//		    }
		    
		    // Make the most recent list the "saved" list
		    boolean success = update.renameTo(saved);
		    if (success) System.out.println("\nSuccessfully updated membership.");
	    }
	}
}
