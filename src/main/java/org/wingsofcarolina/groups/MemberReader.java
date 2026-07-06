package org.wingsofcarolina.groups;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wingsofcarolina.groups.domain.Member;

public abstract class MemberReader {

  private static final Logger logger = LoggerFactory.getLogger(MemberReader.class);

  Map<Integer, Member> memberList = new HashMap<Integer, Member>();

  public MemberReader() {}

  public MemberReader(InputStream is) throws Exception {
    List<String[]> list;

    list = readAllLines(is);
    if (list.isEmpty()) {
      return;
    }

    if (isFlightCircleHeader(list.getFirst())) {
      Map<String, Integer> header = headerIndex(list.getFirst());
      for (int i = 1; i < list.size(); i++) {
        Member member = readFlightCircleMember(header, list.get(i));
        if (member != null) {
          addMember(member);
        }
      }
    } else {
      Iterator<String[]> it = list.iterator();
      while (it.hasNext()) {
        String[] row = it.next();
        if (row.length < 5 || isBlank(row[0])) {
          continue;
        }
        Integer id = Integer.parseInt(row[0].trim());
        String fname = row[1];
        String lname = row[2];
        String email = row[3];
        Integer level = Integer.parseInt(row[4].trim());
        Member member = new Member(id, fname, lname, email, level);

        addMember(member);
      }
    }
  }

  public MemberReader(List<Member> all) {
    Iterator<Member> it = all.iterator();
    while (it.hasNext()) {
      Member member = it.next();
      memberList.put(member.getId(), member);
    }
  }

  public abstract List<String[]> readAllLines(InputStream is) throws Exception;

  private boolean isFlightCircleHeader(String[] row) {
    Map<String, Integer> header = headerIndex(row);
    return (
      header.containsKey("customerid") &&
      header.containsKey("first name") &&
      header.containsKey("last name") &&
      header.containsKey("email") &&
      header.containsKey("groups") &&
      header.containsKey("member number")
    );
  }

  private Map<String, Integer> headerIndex(String[] row) {
    Map<String, Integer> header = new HashMap<String, Integer>();
    for (int i = 0; i < row.length; i++) {
      header.put(normalizeHeader(row[i]), i);
    }
    return header;
  }

  private Member readFlightCircleMember(Map<String, Integer> header, String[] row) {
    String idText = value(header, row, "member number");
    if (isBlank(idText)) {
      logger.info(
        "Skipping Flight Circle member without a Member Number: {}",
        value(header, row, "email")
      );
      return null;
    }

    String email = value(header, row, "email");
    if (isBlank(email)) {
      logger.info("Skipping Flight Circle member {} without an email address", idText);
      return null;
    }

    Integer id = Integer.parseInt(idText.trim());
    String fname = value(header, row, "first name");
    String lname = value(header, row, "last name");
    String groups = value(header, row, "groups");
    String status = value(header, row, "status");
    Integer level = flightCircleLevel(groups, status);
    Member member = new Member(id, fname, lname, email, level);
    member.setIgnoredForSync(
      isIgnoredFlightCircleGroup(groups) || !isActiveFlightCircleStatus(status)
    );
    return member;
  }

  private String value(Map<String, Integer> header, String[] row, String column) {
    Integer index = header.get(normalizeHeader(column));
    if (index == null || index >= row.length) {
      return "";
    }
    return row[index].trim();
  }

  private String normalizeHeader(String header) {
    return header.trim().toLowerCase(Locale.ROOT);
  }

  private Integer flightCircleLevel(String groups, String status) {
    if (!isActiveFlightCircleStatus(status)) {
      return 7;
    }
    if (isIgnoredFlightCircleGroup(groups)) {
      return 3;
    }

    String normalizedGroups = groups.toLowerCase(Locale.ROOT);
    if (normalizedGroups.contains("full member")) {
      return 0;
    }
    if (normalizedGroups.contains("non-flying")) {
      return 1;
    }
    if (normalizedGroups.contains("key")) {
      return 3;
    }
    if (
      normalizedGroups.contains("maintenance") ||
      normalizedGroups.contains("instructor") ||
      normalizedGroups.contains("staff")
    ) {
      return 4;
    }
    if (normalizedGroups.contains("resign")) {
      return 6;
    }
    if (normalizedGroups.contains("terminat")) {
      return 7;
    }
    if (normalizedGroups.contains("deployed")) {
      return 8;
    }
    if (normalizedGroups.contains("associate")) {
      return 9;
    }

    logger.info(
      "Treating unrecognized Flight Circle groups '{}' as waiting list",
      groups
    );
    return 2;
  }

  private boolean isBlank(String value) {
    return value == null || value.trim().isEmpty();
  }

  private boolean isIgnoredFlightCircleGroup(String groups) {
    String normalizedGroups = groups.trim().toLowerCase(Locale.ROOT);
    return (
      normalizedGroups.equals("waiting for membership") ||
      normalizedGroups.equals("courtesy members")
    );
  }

  private boolean isActiveFlightCircleStatus(String status) {
    return !isBlank(status) && status.equalsIgnoreCase("Active");
  }

  public Map<Integer, Member> members() {
    return memberList;
  }

  public void addMember(Member member) {
    memberList.put(member.getId(), member);
  }

  public void printAll(String txtFileName) {
    int i = 0;
    logger.info("Lines to write : " + memberList.size());
    FileWriter fileWriter;
    try {
      fileWriter = new FileWriter(txtFileName);
      Iterator<Map.Entry<Integer, Member>> iterator = memberList.entrySet().iterator();
      while (iterator.hasNext()) {
        Map.Entry<Integer, Member> entry = iterator.next();
        //Integer id = entry.getKey();
        Member member = entry.getValue();

        fileWriter.write(member.output() + "\n");
        i++;
      }

      fileWriter.close();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    logger.info("Lines written : " + i);
  }

  public void remove(Member target) {
    Iterator<Map.Entry<Integer, Member>> iterator = memberList.entrySet().iterator();
    while (iterator.hasNext()) {
      Map.Entry<Integer, Member> entry = iterator.next();
      //Integer id = entry.getKey();
      Member member = entry.getValue();

      if (target.getEmail().compareTo(member.getEmail()) == 0) {
        iterator.remove();
      }
    }
  }

  public boolean hasMember(Member member) {
    if (memberList.containsKey(member.getId())) {
      return true;
    } else {
      return false;
    }
  }

  public void clean() {
    Iterator<Map.Entry<Integer, Member>> iterator = memberList.entrySet().iterator();
    while (iterator.hasNext()) {
      Map.Entry<Integer, Member> entry = iterator.next();
      Member member = entry.getValue();
      if (
        isNotActive(member) ||
        Boolean.TRUE.equals(member.getIgnoredForSync()) ||
        isCruft(member)
      ) {
        iterator.remove();
      }
    }
  }

  public Set<Integer> ignoredForSyncMemberIds() {
    Set<Integer> ignored = new HashSet<Integer>();
    Iterator<Map.Entry<Integer, Member>> iterator = memberList.entrySet().iterator();
    while (iterator.hasNext()) {
      Map.Entry<Integer, Member> entry = iterator.next();
      if (Boolean.TRUE.equals(entry.getValue().getIgnoredForSync())) {
        ignored.add(entry.getKey());
      }
    }
    return ignored;
  }

  public void removeMemberIds(Set<Integer> memberIds) {
    Iterator<Integer> iterator = memberIds.iterator();
    while (iterator.hasNext()) {
      memberList.remove(iterator.next());
    }
  }

  private boolean isNotActive(Member member) {
    Integer level = member.getLevel();
    switch (level) {
      case 2:
        return true;
      case 6:
        return true;
      case 7:
        return true;
      default:
        return false;
    }
  }

  private boolean isCruft(Member member) {
    String name = member.getName();
    switch (name) {
      case "Childrens Flight Hope":
        return true;
      case "Joe Pilot":
        return true;
      case "Jane Pilot":
        return true;
      case "Maintenance Maintenance":
        return true;
      case "Book Keeper":
        return true;
      case "Club Trips":
        return true;
    }
    return false;
  }
}
