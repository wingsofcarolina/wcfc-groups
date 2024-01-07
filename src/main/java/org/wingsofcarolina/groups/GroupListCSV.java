package org.wingsofcarolina.groups;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.wingsofcarolina.groups.domain.Member;

import com.opencsv.CSVReader;

public class GroupListCSV  {
	Integer id = 1;
	Map<Integer, Member> memberList = new HashMap<Integer, Member>();

	public GroupListCSV(InputStream is) throws Exception {
		List<String[]> list;

		list = readAllLines(is);
		Iterator<String[]> it = list.iterator();
		while (it.hasNext()) {
			String[] row = it.next();
			String email = row[0];
			String name = row[1];
			Member member = new Member(id++, name, email);

			addMember(member);
		}
	}
		
	public Map<Integer, Member> members() {
		return memberList;
	}

	public void addMember(Member member) {
		memberList.put(member.getId(), member);
	}
	
	public List<String[]> readAllLines(InputStream is) throws Exception {
	    try (Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
	        try (CSVReader csvReader = new CSVReader(reader)) {
	            return csvReader.readAll();
	        }
	    }
	}
}
