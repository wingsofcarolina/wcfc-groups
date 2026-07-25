package org.wingsofcarolina.groups.domain;

import com.google.gson.annotations.SerializedName;

public class GroupsIoMember {

  private Long id;
  private String email;

  @SerializedName("full_name")
  private String fullName;

  public Long getId() {
    return id;
  }

  public String getEmail() {
    return email;
  }

  public String getFullName() {
    return fullName;
  }
}
