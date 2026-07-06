package org.wingsofcarolina.groups.http;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.set;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.bson.Document;
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
    Map<String, DepositsMember> csvMembers = depositsMembersByNumber(updateList);
    Map<String, DepositsMember> savedMembers = depositsMembersByNumber();

    DepositsDiff diff = new DepositsDiff();
    for (Map.Entry<String, DepositsMember> entry : csvMembers.entrySet()) {
      DepositsMember newMember = entry.getValue();
      DepositsMember oldMember = savedMembers.get(entry.getKey());
      if (oldMember == null) {
        diff.getAdded().add(newMember);
      } else if (!oldMember.matches(newMember)) {
        diff.getChanged().add(new DepositsChange(oldMember, newMember));
      }
    }

    for (Map.Entry<String, DepositsMember> entry : savedMembers.entrySet()) {
      if (
        !csvMembers.containsKey(entry.getKey()) &&
        !Boolean.TRUE.equals(entry.getValue().getInactive())
      ) {
        diff.getRemoved().add(entry.getValue());
      }
    }
    return diff;
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
    members.updateOne(eq("number_normalized", numberNormalized), set("inactive", true));
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
