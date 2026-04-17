package org.wingsofcarolina.groups.domain;

public class EmailChange {

  private Member oldMember;
  private Member newMember;
  private Boolean checked;

  public EmailChange() {
    this.checked = false;
  }

  public EmailChange(Member oldMember, Member newMember) {
    this.oldMember = oldMember;
    this.newMember = newMember;
    this.checked = false;
  }

  public Member getOldMember() {
    return oldMember;
  }

  public void setOldMember(Member oldMember) {
    this.oldMember = oldMember;
  }

  public Member getNewMember() {
    return newMember;
  }

  public void setNewMember(Member newMember) {
    this.newMember = newMember;
  }

  public Boolean getChecked() {
    return checked;
  }

  public void setChecked(Boolean checked) {
    this.checked = checked;
  }
}
