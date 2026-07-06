package org.wingsofcarolina.groups.server;

import java.util.ArrayList;
import java.util.List;
import org.wingsofcarolina.groups.domain.DepositsChange;
import org.wingsofcarolina.groups.domain.DepositsMember;
import org.wingsofcarolina.groups.domain.EmailChange;
import org.wingsofcarolina.groups.domain.Member;

public class UpdateRequest {

  private List<Member> added = new ArrayList<Member>();
  private List<Member> removed = new ArrayList<Member>();
  private List<EmailChange> changed = new ArrayList<EmailChange>();
  private List<DepositsMember> depositsAdded = new ArrayList<DepositsMember>();
  private List<DepositsMember> depositsRemoved = new ArrayList<DepositsMember>();
  private List<DepositsChange> depositsChanged = new ArrayList<DepositsChange>();

  public List<Member> getAdded() {
    return added;
  }

  public void setAdded(List<Member> added) {
    this.added = added;
  }

  public List<Member> getRemoved() {
    return removed;
  }

  public void setRemoved(List<Member> removed) {
    this.removed = removed;
  }

  public List<EmailChange> getChanged() {
    return changed;
  }

  public void setChanged(List<EmailChange> changed) {
    this.changed = changed;
  }

  public List<DepositsMember> getDepositsAdded() {
    return depositsAdded;
  }

  public void setDepositsAdded(List<DepositsMember> depositsAdded) {
    this.depositsAdded = depositsAdded;
  }

  public List<DepositsMember> getDepositsRemoved() {
    return depositsRemoved;
  }

  public void setDepositsRemoved(List<DepositsMember> depositsRemoved) {
    this.depositsRemoved = depositsRemoved;
  }

  public List<DepositsChange> getDepositsChanged() {
    return depositsChanged;
  }

  public void setDepositsChanged(List<DepositsChange> depositsChanged) {
    this.depositsChanged = depositsChanged;
  }
}
