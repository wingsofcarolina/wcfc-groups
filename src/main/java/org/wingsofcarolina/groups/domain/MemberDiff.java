package org.wingsofcarolina.groups.domain;

import java.util.ArrayList;
import java.util.List;

public class MemberDiff {

  private final List<Member> added = new ArrayList<Member>();
  private final List<Member> removed = new ArrayList<Member>();
  private final List<EmailChange> changed = new ArrayList<EmailChange>();

  public List<Member> getAdded() {
    return added;
  }

  public List<Member> getRemoved() {
    return removed;
  }

  public List<EmailChange> getChanged() {
    return changed;
  }
}
