package org.wingsofcarolina.groups.domain.dao;

import java.util.List;

import org.wingsofcarolina.groups.domain.Member;
import org.wingsofcarolina.groups.domain.SuperDAO;
import org.wingsofcarolina.groups.persistence.Persistence;

import dev.morphia.Datastore;
import dev.morphia.query.Query;
import dev.morphia.query.experimental.filters.Filters;

public class MemberDAO extends SuperDAO {
	public MemberDAO() {
		super(Member.class);
	}

	@SuppressWarnings("unchecked")
	public Member getByEmail(String email) {
		Datastore ds = Persistence.instance().datastore();
		Query<?> query = ds.find(Member.class);
		List<Member> users = (List<Member>) query.filter(Filters.eq("email", email)).iterator().toList();
		if (users.size() > 0) {
			return users.get(0);
		} else {
			return null;
		}
	}
		
}
