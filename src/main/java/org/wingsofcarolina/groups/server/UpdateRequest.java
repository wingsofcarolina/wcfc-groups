package org.wingsofcarolina.groups.server;

import java.util.ArrayList;
import java.util.List;
import org.wingsofcarolina.groups.domain.EmailChange;
import org.wingsofcarolina.groups.domain.Member;

public class UpdateRequest {

  private List<Member> added = new ArrayList<Member>();
  private List<Member> removed = new ArrayList<Member>();
  private List<EmailChange> changed = new ArrayList<EmailChange>();

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
}
