package org.wingsofcarolina.groups;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.wingsofcarolina.groups.domain.Member;

public abstract class MemberReader {
	Map<Integer, Member> memberList = new HashMap<Integer, Member>();
	
	public MemberReader() {}
	
	public MemberReader(InputStream is) throws Exception {
		List<String[]> list;
		
		list = readAllLines(is);
		Iterator<String[]> it = list.iterator();
		while (it.hasNext()) {
			String[] row = it.next();
			Integer id = Integer.parseInt(row[0]);
			String fname = row[1];
			String lname = row[2];
			String email = row[3];
			Integer level = Integer.parseInt(row[4]);
			Member member = new Member(id, fname, lname, email, level);
			
			addMember(member);
		}
	}
	
	abstract public List<String[]> readAllLines(InputStream is) throws Exception;
	
	public Map<Integer, Member> members() {
		return memberList;
	}
	
	public void addMember(Member member) {
		memberList.put(member.getId(), member);
	}
	
	public void printAll(String txtFileName) {
		int i = 0;
		System.out.println("Lines to write : " + memberList.size());
        FileWriter fileWriter;
		try {
			fileWriter = new FileWriter(txtFileName);
		    Iterator<Map.Entry<Integer, Member>> iterator = memberList.entrySet().iterator();
		    while (iterator.hasNext()) {
		        Map.Entry<Integer, Member> entry = iterator.next();
		        //Integer id = entry.getKey();
		        Member member = entry.getValue();
		        
		        fileWriter.write(member.output() + "\n");
		        i++;
		    }
			
			fileWriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Lines written : " + i);

	}

	public void remove(Member target) {
	    Iterator<Map.Entry<Integer, Member>> iterator = memberList.entrySet().iterator();
	    while (iterator.hasNext()) {
	        Map.Entry<Integer, Member> entry = iterator.next();
	        //Integer id = entry.getKey();
	        Member member = entry.getValue();
	        
	        if (target.getEmail().compareTo(member.getEmail()) == 0) {
	        	iterator.remove();
	        }
	    }
	}

	public boolean hasMember(Member member) {
		if (memberList.containsKey(member.getId())) {
			return true;
		} else {
			return false;
		}
	}

	public void clean() {
	    Iterator<Map.Entry<Integer, Member>> iterator = memberList.entrySet().iterator();
	    while (iterator.hasNext()) {
	        Map.Entry<Integer, Member> entry = iterator.next();
	        Member member = entry.getValue();
	        if (isNotActive(member)|| isCruft(member)) {
	        	iterator.remove();
	        }
	    }
	}
	
	private boolean isNotActive(Member member) {
		Integer level = member.getLevel();
		switch (level) {
			case 2 : return true;
			case 6 : return true;
			case 7 : return true;
			default: return false;
		}
	}
	
	private boolean isCruft(Member member) {
		String name = member.getName();
		switch (name) {
			case "Childrens Flight Hope" : return true;
			case "Joe Pilot" : return true;
			case "Jane Pilot" : return true;
			case "Maintenance Maintenance" : return true;
			case "Book Keeper" : return true;
			case "Club Trips" : return true;
		}
		return false;
	}
}
