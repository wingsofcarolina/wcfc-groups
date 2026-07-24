package org.wingsofcarolina.groups.http;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.or;
import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.set;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wingsofcarolina.groups.MemberReader;
import org.wingsofcarolina.groups.domain.DepositsChange;
import org.wingsofcarolina.groups.domain.DepositsMember;
import org.wingsofcarolina.groups.domain.Member;

public class DepositsService implements AutoCloseable {

  private static final Logger logger = LoggerFactory.getLogger(DepositsService.class);

  private static final String DEFAULT_MONGO_URI = "mongodb://localhost:27019";
  private static final String DEFAULT_DATABASE = "wcfc-deposits";

  private MongoClient client;
  private MongoCollection<Document> members;

  public DepositsService initialize() {
    String mongoUri = env("DEPOSITS_MONGODB", env("MONGODB", DEFAULT_MONGO_URI));
    String database = env("DEPOSITS_MONGODB_DATABASE", DEFAULT_DATABASE);
    logger.info("Connecting to deposits MongoDB database {}", database);
    client = MongoClients.create(mongoUri);
    members = client.getDatabase(database).getCollection("members");
    return this;
  }

  public DepositsDiff diff(MemberReader updateList) {
    return diff(updateList, Set.of());
  }

  public DepositsDiff diff(MemberReader updateList, Set<Integer> ignoredMemberIds) {
    Map<String, DepositsMember> csvMembers = depositsMembersByNumber(updateList);
    Map<String, DepositsMember> savedMembers = depositsMembersByNumber();
    removeIgnoredMembers(savedMembers, ignoredMemberIds);
    Map<String, DepositsMember> savedMembersByEmail = depositsMembersByEmail(
      savedMembers
    );
    Set<String> matchedSavedMemberNumbers = new HashSet<String>();

    DepositsDiff diff = new DepositsDiff();
    for (Map.Entry<String, DepositsMember> entry : csvMembers.entrySet()) {
      DepositsMember newMember = entry.getValue();
      DepositsMember oldMember = savedMembers.get(entry.getKey());
      if (oldMember == null) {
        DepositsMember emailMatch = savedMembersByEmail.get(
          DepositsMember.normalizeEmail(newMember.getEmail())
        );
        if (
          emailMatch != null &&
          !matchedSavedMemberNumbers.contains(emailMatch.getNumberNormalized())
        ) {
          oldMember = emailMatch;
        }
      }

      if (oldMember == null) {
        diff.getAdded().add(newMember);
      } else {
        matchedSavedMemberNumbers.add(oldMember.getNumberNormalized());
        if (!oldMember.matches(newMember)) {
          diff.getChanged().add(new DepositsChange(oldMember, newMember));
        }
      }
    }

    for (Map.Entry<String, DepositsMember> entry : savedMembers.entrySet()) {
      if (
        !matchedSavedMemberNumbers.contains(entry.getKey()) &&
        !Boolean.TRUE.equals(entry.getValue().getInactive())
      ) {
        diff.getRemoved().add(entry.getValue());
      }
    }
    return diff;
  }

  private void removeIgnoredMembers(
    Map<String, DepositsMember> membersByNumber,
    Set<Integer> ignoredMemberIds
  ) {
    Iterator<Integer> it = ignoredMemberIds.iterator();
    while (it.hasNext()) {
      membersByNumber.remove(
        DepositsMember.normalizeMemberNumber(String.valueOf(it.next()))
      );
    }
  }

  private Map<String, DepositsMember> depositsMembersByEmail(
    Map<String, DepositsMember> membersByNumber
  ) {
    Map<String, DepositsMember> mapped = new HashMap<String, DepositsMember>();
    for (DepositsMember member : membersByNumber.values()) {
      String email = DepositsMember.normalizeEmail(member.getEmail());
      if (email != null && !email.isBlank()) {
        mapped.put(email, member);
      }
    }
    return mapped;
  }

  public void addMultipleMembers(List<DepositsMember> added) {
    Iterator<DepositsMember> it = added.iterator();
    while (it.hasNext()) {
      addMember(it.next());
    }
  }

  public void addMember(DepositsMember member) {
    member.prepareForInsert();
    logger.info(
      "Adding deposits member {} <{}> #{}",
      member.displayName(),
      member.getEmail(),
      member.getMemberNumber()
    );
    members.insertOne(member.toDocument());
  }

  public void removeMultipleMembers(List<DepositsMember> removed) {
    Iterator<DepositsMember> it = removed.iterator();
    while (it.hasNext()) {
      removeMember(it.next());
    }
  }

  public void removeMember(DepositsMember member) {
    String numberNormalized = DepositsMember.normalizeMemberNumber(
      member.getMemberNumber()
    );
    logger.info("Marking deposits member #{} inactive", numberNormalized);
    members.updateOne(memberNumberFilter(member), set("inactive", true));
  }

  public void updateMultipleMembers(List<DepositsChange> changed) {
    Iterator<DepositsChange> it = changed.iterator();
    while (it.hasNext()) {
      updateMember(it.next());
    }
  }

  public void updateMember(DepositsChange change) {
    DepositsMember oldMember = change.getOldMember();
    DepositsMember member = change.getNewMember();
    member.prepareForInsert();
    logger.info(
      "Updating deposits member {} <{}> #{}",
      member.displayName(),
      member.getEmail(),
      member.getMemberNumber()
    );
    members.updateOne(
      or(memberNumberFilter(oldMember), memberNumberFilter(member)),
      combine(
        set("first_name", member.getFirstName()),
        set("last_name", member.getLastName()),
        set("email", member.getEmail()),
        set("member_number", member.getMemberNumber()),
        set("full_name_normalized", member.getFullNameNormalized()),
        set("number_normalized", member.getNumberNormalized()),
        set("inactive", Boolean.TRUE.equals(member.getInactive()))
      )
    );
  }

  private Bson memberNumberFilter(DepositsMember member) {
    String memberNumber = member.getMemberNumber();
    String numberNormalized = DepositsMember.normalizeMemberNumber(memberNumber);
    return or(
      eq("number_normalized", numberNormalized),
      eq("member_number", memberNumber)
    );
  }

  @Override
  public void close() {
    if (client != null) {
      client.close();
    }
  }

  private Map<String, DepositsMember> depositsMembersByNumber(MemberReader updateList) {
    Map<String, DepositsMember> mapped = new HashMap<String, DepositsMember>();
    for (Member member : updateList.members().values()) {
      DepositsMember depositsMember = DepositsMember.fromGroupsMember(member);
      mapped.put(depositsMember.getNumberNormalized(), depositsMember);
    }
    return mapped;
  }

  private Map<String, DepositsMember> depositsMembersByNumber() {
    Map<String, DepositsMember> mapped = new HashMap<String, DepositsMember>();
    FindIterable<Document> documents = members.find();
    for (Document document : documents) {
      DepositsMember member = DepositsMember.fromDocument(document);
      mapped.put(member.getNumberNormalized(), member);
    }
    return mapped;
  }

  private String env(String name, String defaultValue) {
    String value = System.getenv(name);
    if (value == null || value.trim().isEmpty()) {
      return defaultValue;
    }
    return value.trim();
  }

  public static class DepositsDiff {

    private List<DepositsMember> added = new ArrayList<DepositsMember>();
    private List<DepositsMember> removed = new ArrayList<DepositsMember>();
    private List<DepositsChange> changed = new ArrayList<DepositsChange>();

    public List<DepositsMember> getAdded() {
      return added;
    }

    public List<DepositsMember> getRemoved() {
      return removed;
    }

    public List<DepositsChange> getChanged() {
      return changed;
    }
  }
}
