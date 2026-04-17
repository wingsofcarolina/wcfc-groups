package org.wingsofcarolina.groups.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wingsofcarolina.groups.domain.EmailChange;
import org.wingsofcarolina.groups.domain.Member;
import org.wingsofcarolina.groups.http.GroupsIoService;
import org.wingsofcarolina.groups.http.ManualsService;

public class UpdateHandler implements HttpHandler {

  private static final Logger logger = LoggerFactory.getLogger(UpdateHandler.class);

  private static ObjectMapper mapper = new ObjectMapper();

  @Override
  public void handleRequest(HttpServerExchange hse) throws Exception {
    String uri = hse.getRequestURI();
    String method = hse.getRequestMethod().toString();
    logger.info("==> " + method + " : " + uri);

    hse.startBlocking();
    InputStream is = hse.getInputStream();

    UpdateRequest result = null;
    try {
      result = mapper.readValue(new InputStreamReader(is), UpdateRequest.class);
      logger.info("===> " + result);
    } catch (Exception ex) {
      logger.info(ex.getMessage());
    }

    // Remove all members that are not "checked"
    List<Member> added = clean(result.getAdded());
    List<Member> removed = clean(result.getRemoved());
    List<EmailChange> changed = cleanChanges(result.getChanged());
    logger.info("Added   --> " + added.size() + " : " + added);
    logger.info("Removed --> " + removed.size() + " : " + removed);
    logger.info("Changed --> " + changed.size() + " : " + changed);

    // Create service to access the Manuals website for database updates
    ManualsService mio = new ManualsService().initialize();

    GroupsIoService gio = null;
    if (added.size() > 0 || removed.size() > 0) {
      // Initialize Groups.io service with API key only when Groups.io must change.
      gio = new GroupsIoService().initialize();
      String apiKey = System.getenv("GROUPS_IO_API_KEY");
      if (apiKey == null || apiKey.trim().isEmpty()) {
        logger.error("GROUPS_IO_API_KEY environment variable is not set");
        hse.setStatusCode(StatusCodes.INTERNAL_SERVER_ERROR);
        hse.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
        hse
          .getResponseSender()
          .send("{ \"code\": 500, \"message\" : \"API key not configured\" }");
        return;
      }
      gio.setApiKey(apiKey);
      logger.info("Groups.io service initialized with API key");
    }

    logger.info("Updating Groups.io membership list and member database.");
    if (added.size() > 0) {
      gio.addMultipleMembers(added);
      mio.addMultipleMembers(added);
      Iterator<Member> it = added.iterator();
      while (it.hasNext()) {
        Member member = it.next();
        member.save();
      }
    }
    if (removed.size() > 0) {
      gio.removeMultipleMembers(removed);
      mio.removeMultipleMembers(removed);
      Iterator<Member> it = removed.iterator();
      while (it.hasNext()) {
        Member member = it.next();
        member = Member.getByID(member.getId());
        member.delete();
      }
    }
    if (changed.size() > 0) {
      logger.info("Updating changed email addresses in Manuals and local database only.");
      Iterator<EmailChange> it = changed.iterator();
      while (it.hasNext()) {
        EmailChange emailChange = it.next();
        Member oldMember = emailChange.getOldMember();
        Member newMember = emailChange.getNewMember();

        mio.removeMember(oldMember);
        mio.addMember(newMember);
        updateLocalMember(newMember);
      }
    }

    hse.setStatusCode(StatusCodes.OK);
    hse.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
    hse
      .getResponseSender()
      .send("{ \"code\": 200, \"message\" : \"Membership List Updated\" }");
  }

  List<Member> clean(List<Member> members) {
    if (members == null) {
      return List.of();
    }
    int originalSize = members.size();
    int removedCount = 0;

    Iterator<Member> removedIt = members.iterator();
    while (removedIt.hasNext()) {
      Member member = removedIt.next();
      // Handle null values properly - if checked is null or false, remove the member
      if (!Boolean.TRUE.equals(member.getChecked())) {
        logger.debug(
          "Removing unchecked member: {} (checked={})",
          member.getName(),
          member.getChecked()
        );
        removedIt.remove();
        removedCount++;
      } else {
        logger.debug(
          "Keeping checked member: {} (checked={})",
          member.getName(),
          member.getChecked()
        );
      }
    }

    logger.info(
      "Cleaned member list: {} -> {} (removed {} unchecked members)",
      originalSize,
      members.size(),
      removedCount
    );
    return members;
  }

  List<EmailChange> cleanChanges(List<EmailChange> changes) {
    if (changes == null) {
      return List.of();
    }

    Iterator<EmailChange> it = changes.iterator();
    while (it.hasNext()) {
      EmailChange emailChange = it.next();
      if (!Boolean.TRUE.equals(emailChange.getChecked())) {
        it.remove();
      }
    }
    return changes;
  }

  private void updateLocalMember(Member newMember) {
    Member existingMember = Member.getByID(newMember.getId());
    if (existingMember == null) {
      logger.warn(
        "Could not find local member {} while updating changed email; saving new record",
        newMember.getId()
      );
      newMember.save();
      return;
    }

    existingMember.setName(newMember.getName());
    existingMember.setEmail(newMember.getEmail());
    existingMember.setLevel(newMember.getLevel());
    existingMember.save();
  }
}
