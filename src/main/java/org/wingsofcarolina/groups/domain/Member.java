package org.wingsofcarolina.groups.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Transient;
import java.util.List;
import org.bson.types.ObjectId;
import org.wingsofcarolina.groups.domain.dao.MemberDAO;

@Entity("Members")
public class Member {

  private static MemberDAO dao = new MemberDAO();

  @Id
  @JsonIgnore
  private ObjectId dbid;

  Integer id;
  String firstName;
  String lastName;
  String name;
  String email;
  Integer level;

  @Transient
  Boolean checked;

  public Member() {
    this.checked = false; // Initialize checked to false by default for JSON deserialization
  }

  // Note, this is used ONLY for Groups.io auditing!
  public Member(Integer id, String name, String email) {
    this.id = id;
    this.name = name;
    setNamePartsFromName(name);
    this.email = email;
    this.level = -1;
    this.checked = false;
  }

  public Member(Integer id, String fname, String lname, String email, Integer level) {
    this.id = id;
    this.firstName = fname;
    this.lastName = lname;
    this.name = fname + " " + lname;
    this.email = email;
    this.level = level;
    this.checked = false; // Initialize checked to false by default
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
    setNamePartsFromName(name);
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
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
    return (
      "Member [id=" +
      id +
      ", name=" +
      name +
      ", email=" +
      email +
      ", level=" +
      level +
      ", checked=" +
      checked +
      "]"
    );
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

  private void setNamePartsFromName(String name) {
    if (name == null) {
      firstName = "";
      lastName = "";
      return;
    }

    String trimmed = name.trim();
    int splitAt = trimmed.indexOf(' ');
    if (splitAt < 0) {
      firstName = trimmed;
      lastName = "";
      return;
    }

    firstName = trimmed.substring(0, splitAt).trim();
    lastName = trimmed.substring(splitAt + 1).trim();
  }
}
