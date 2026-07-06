package org.wingsofcarolina.groups.domain;

public class DepositsChange {

  private DepositsMember oldMember;
  private DepositsMember newMember;

  public DepositsChange() {}

  public DepositsChange(DepositsMember oldMember, DepositsMember newMember) {
    this.oldMember = oldMember;
    this.newMember = newMember;
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
}
