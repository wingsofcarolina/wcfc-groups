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
  private List<Member> groupsAdded;
  private List<Member> groupsRemoved;
  private List<Member> manualsAdded;
  private List<Member> manualsRemoved;
  private List<EmailChange> manualsChanged;
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

  public List<Member> getGroupsAdded() {
    return groupsAdded == null ? added : groupsAdded;
  }

  public void setGroupsAdded(List<Member> groupsAdded) {
    this.groupsAdded = groupsAdded;
  }

  public List<Member> getGroupsRemoved() {
    return groupsRemoved == null ? removed : groupsRemoved;
  }

  public void setGroupsRemoved(List<Member> groupsRemoved) {
    this.groupsRemoved = groupsRemoved;
  }

  public List<Member> getManualsAdded() {
    return manualsAdded == null ? added : manualsAdded;
  }

  public void setManualsAdded(List<Member> manualsAdded) {
    this.manualsAdded = manualsAdded;
  }

  public List<Member> getManualsRemoved() {
    return manualsRemoved == null ? removed : manualsRemoved;
  }

  public void setManualsRemoved(List<Member> manualsRemoved) {
    this.manualsRemoved = manualsRemoved;
  }

  public List<EmailChange> getManualsChanged() {
    return manualsChanged == null ? changed : manualsChanged;
  }

  public void setManualsChanged(List<EmailChange> manualsChanged) {
    this.manualsChanged = manualsChanged;
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
