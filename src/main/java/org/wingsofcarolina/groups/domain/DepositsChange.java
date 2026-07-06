package org.wingsofcarolina.groups.domain;

public class DepositsChange {

  private DepositsMember oldMember;
  private DepositsMember newMember;
  private Boolean checked;

  public DepositsChange() {
    this.checked = false;
  }

  public DepositsChange(DepositsMember oldMember, DepositsMember newMember) {
    this.oldMember = oldMember;
    this.newMember = newMember;
    this.checked = false;
  }

  public DepositsMember getOldMember() {
    return oldMember;
  }

  public void setOldMember(DepositsMember oldMember) {
    this.oldMember = oldMember;
  }

  public DepositsMember getNewMember() {
    return newMember;
  }

  public void setNewMember(DepositsMember newMember) {
    this.newMember = newMember;
  }

  public Boolean getChecked() {
    return checked;
  }

  public void setChecked(Boolean checked) {
    this.checked = checked;
  }
}
