package org.wingsofcarolina.groups.domain;

public class Member {
	Integer id;
	String name;
	String email;
	Integer level;
	Boolean checked;

	public Member() {}
	
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
		return "Member [name=" + name + ", email=" + email + ", level=" + level + ", checked=" + checked + "]";
	}
}
