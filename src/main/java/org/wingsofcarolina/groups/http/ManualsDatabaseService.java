package org.wingsofcarolina.groups.http;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.inc;
import static com.mongodb.client.model.Updates.set;

import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wingsofcarolina.groups.MemberReader;
import org.wingsofcarolina.groups.domain.EmailChange;
import org.wingsofcarolina.groups.domain.Member;
import org.wingsofcarolina.groups.domain.MemberDiff;

public class ManualsDatabaseService implements AutoCloseable {

  private static final Logger logger = LoggerFactory.getLogger(
    ManualsDatabaseService.class
  );
  private static final String DEFAULT_MONGO_URI = "mongodb://localhost:27019";
  private static final String DEFAULT_DATABASE = "wcfc-manuals";
  private static final String MEMBER_ID_KEY = "members";

  private MongoClient client;
  private MongoCollection<Document> members;
  private MongoCollection<Document> ids;

  public ManualsDatabaseService initialize() {
    String mongoUri = env("MANUALS_MONGODB", env("MONGODB", DEFAULT_MONGO_URI));
    String database = env("MANUALS_MONGODB_DATABASE", DEFAULT_DATABASE);
    logger.info("Connecting to manuals MongoDB database {}", database);
    client = MongoClients.create(mongoUri);
    members = client.getDatabase(database).getCollection("Members");
    ids = client.getDatabase(database).getCollection("IDs");
    return this;
  }

  public MemberDiff diff(MemberReader desired, Set<Integer> ignoredMemberIds) {
    Map<Integer, Member> actual = membersById();
    for (Integer ignoredMemberId : ignoredMemberIds) {
      actual.remove(ignoredMemberId);
    }

    MemberDiff diff = new MemberDiff();
    Set<Integer> matched = new HashSet<Integer>();
    for (Member wanted : desired.members().values()) {
      Member current = actual.get(wanted.getId());
      if (current == null) {
        diff.getAdded().add(wanted);
      } else {
        matched.add(current.getId());
        if (
          !normalizeEmail(current.getEmail()).equals(normalizeEmail(wanted.getEmail()))
        ) {
          diff.getChanged().add(new EmailChange(current, wanted));
        }
      }
    }
    for (Member current : actual.values()) {
      if (!matched.contains(current.getId())) {
        diff.getRemoved().add(current);
      }
    }
    return diff;
  }

  public void addMultipleMembers(List<Member> added) {
    for (Member member : added) {
      addMember(member);
    }
  }

  public void addMember(Member member) {
    Document document = new Document()
      .append("_t", "Member")
      .append("memberId", nextMemberId())
      .append("id", member.getId())
      .append("uuid", UUID.randomUUID().toString())
      .append("name", member.getName())
      .append("email", member.getEmail())
      .append("level", member.getLevel())
      .append("admin", false);
    logger.info("Adding manuals member {}", member.output());
    members.insertOne(document);
  }

  public void removeMultipleMembers(List<Member> removed) {
    for (Member member : removed) {
      logger.info("Removing manuals member #{}", member.getId());
      DeleteResult result = members.deleteOne(eq("id", member.getId()));
      requireSingleMatch("remove", member.getId(), result.getDeletedCount());
    }
  }

  public void updateMultipleMembers(List<EmailChange> changed) {
    for (EmailChange change : changed) {
      Member oldMember = change.getOldMember();
      Member newMember = change.getNewMember();
      logger.info(
        "Updating manuals member #{} email from {} to {}",
        newMember.getId(),
        oldMember.getEmail(),
        newMember.getEmail()
      );
      UpdateResult result = members.updateOne(
        eq("id", newMember.getId()),
        combine(
          set("name", newMember.getName()),
          set("email", newMember.getEmail()),
          set("level", newMember.getLevel())
        )
      );
      requireSingleMatch("update", newMember.getId(), result.getMatchedCount());
    }
  }

  private Map<Integer, Member> membersById() {
    Map<Integer, Member> result = new HashMap<Integer, Member>();
    for (Document document : members.find()) {
      Number id = document.get("id", Number.class);
      if (id == null) {
        logger.warn("Ignoring manuals member without a Flight Circle id: {}", document);
        continue;
      }
      Integer memberId = id.intValue();
      Number level = document.get("level", Number.class);
      String name = document.getString("name");
      if (!MemberReader.isExcludedName(name)) {
        result.put(
          memberId,
          new Member(
            memberId,
            name,
            document.getString("email"),
            level == null ? -1 : level.intValue()
          )
        );
      }
    }
    return result;
  }

  private long nextMemberId() {
    FindOneAndUpdateOptions options = new FindOneAndUpdateOptions()
      .returnDocument(ReturnDocument.AFTER);
    Document updated = ids.findOneAndUpdate(
      eq("_id", MEMBER_ID_KEY),
      inc("value", 1L),
      options
    );
    if (updated != null) {
      return ((Number) updated.get("value")).longValue();
    }

    try {
      ids.insertOne(new Document("_id", MEMBER_ID_KEY).append("value", 1000L));
      return 1000L;
    } catch (MongoWriteException ex) {
      if (ex.getError().getCode() != 11000) {
        throw ex;
      }
      updated = ids.findOneAndUpdate(eq("_id", MEMBER_ID_KEY), inc("value", 1L), options);
      if (updated == null) {
        throw new IllegalStateException("Unable to allocate a Manuals member id");
      }
      return ((Number) updated.get("value")).longValue();
    }
  }

  private void requireSingleMatch(String operation, Integer id, long count) {
    if (count != 1) {
      throw new IllegalStateException(
        "Manuals " + operation + " expected one member #" + id + " but matched " + count
      );
    }
  }

  private String normalizeEmail(String email) {
    return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
  }

  private String env(String name, String defaultValue) {
    String value = System.getenv(name);
    return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
  }

  @Override
  public void close() {
    if (client != null) {
      client.close();
    }
  }
}
