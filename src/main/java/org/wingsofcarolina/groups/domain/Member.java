package org.wingsofcarolina.groups.domain;

import java.util.List;

import org.bson.types.ObjectId;
import org.wingsofcarolina.groups.domain.dao.MemberDAO;

import com.fasterxml.jackson.annotation.JsonIgnore;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;

@Entity("Members")
public class Member {
	private static MemberDAO dao = new MemberDAO();

	@Id
	@JsonIgnore
	private ObjectId dbid;
	
	Integer id;
	String name;
	String email;
	Integer level;
	Boolean checked;

	public Member() {}
	
	// Note, this is used ONLY for Groups.io auditing!
	public Member(Integer id, String name, String email) {
		this.id = id;
		this.name = name;
		this.email = email;
		this.level = -1;
		this.checked = false;
	}
	
	public Member(Integer id, String fname, String lname, String email, Integer level) {
		this.id = id;
		this.name = fname + " " + lname;
		this.email = email;
		this.level = level;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Boolean getChecked() {
		return checked;
	}

	public void setChecked(Boolean checked) {
		this.checked = checked;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public Integer getLevel() {
		return level;
	}

	public void setLevel(Integer level) {
		this.level = level;
	}

	public String output() {
		return name + " <" + email + ">";
	}

	@Override
	public String toString() {
		return "Member [id=" + id + ", name=" + name + ", email=" + email + ", level=" + level + ", checked=" + checked + "]";
	}

	/*
	 * Database Management Functionality
	 */
	public static long count() {
		return dao.count();
	}
	
	public static void drop() {
		dao.drop();
	}
	
	@SuppressWarnings("unchecked")
	public static List<Member> getAll() {
		return (List<Member>) dao.getAll();
	}

	public static Member getByID(long id) {
		return (Member) dao.getByID(id);
	}

	public static Member getByEmail(String email) {
		return (Member) dao.getByEmail(email);
	}
	
	public void save() {
		dao.save(this);
	}
	
	public void delete() {
		dao.delete(this);
	}
}
