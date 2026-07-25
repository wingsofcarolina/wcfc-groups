package org.wingsofcarolina.groups.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wingsofcarolina.groups.domain.DepositsChange;
import org.wingsofcarolina.groups.domain.DepositsMember;
import org.wingsofcarolina.groups.domain.EmailChange;
import org.wingsofcarolina.groups.domain.Member;
import org.wingsofcarolina.groups.http.DepositsService;
import org.wingsofcarolina.groups.http.GroupsIoService;
import org.wingsofcarolina.groups.http.ManualsDatabaseService;

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
    List<Member> groupsAdded = clean(result.getGroupsAdded());
    List<Member> groupsRemoved = clean(result.getGroupsRemoved());
    List<Member> manualsAdded = clean(result.getManualsAdded());
    List<Member> manualsRemoved = clean(result.getManualsRemoved());
    List<EmailChange> manualsChanged = cleanChanges(result.getManualsChanged());
    List<DepositsMember> depositsAdded = cleanDeposits(result.getDepositsAdded());
    List<DepositsMember> depositsRemoved = cleanDeposits(result.getDepositsRemoved());
    List<DepositsChange> depositsChanged = cleanDepositsChanges(
      result.getDepositsChanged()
    );
    logger.info("Groups.io added   --> " + groupsAdded.size() + " : " + groupsAdded);
    logger.info("Groups.io removed --> " + groupsRemoved.size() + " : " + groupsRemoved);
    logger.info("Manuals added   --> " + manualsAdded.size() + " : " + manualsAdded);
    logger.info("Manuals removed --> " + manualsRemoved.size() + " : " + manualsRemoved);
    logger.info("Manuals changed --> " + manualsChanged.size() + " : " + manualsChanged);
    logger.info("Deposits added   --> " + depositsAdded.size() + " : " + depositsAdded);
    logger.info(
      "Deposits removed --> " + depositsRemoved.size() + " : " + depositsRemoved
    );
    logger.info(
      "Deposits changed --> " + depositsChanged.size() + " : " + depositsChanged
    );

    GroupsIoService gio = null;
    if (groupsAdded.size() > 0 || groupsRemoved.size() > 0) {
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

    logger.info("Updating Groups.io membership list.");
    if (groupsAdded.size() > 0) {
      gio.addMultipleMembers(groupsAdded);
    }
    if (groupsRemoved.size() > 0) {
      gio.removeMultipleMembers(groupsRemoved);
    }
    if (
      manualsAdded.size() > 0 || manualsRemoved.size() > 0 || manualsChanged.size() > 0
    ) {
      try (
        ManualsDatabaseService manualsService = new ManualsDatabaseService().initialize()
      ) {
        manualsService.removeMultipleMembers(manualsRemoved);
        manualsService.updateMultipleMembers(manualsChanged);
        manualsService.addMultipleMembers(manualsAdded);
      }
    }
    if (
      depositsAdded.size() > 0 || depositsRemoved.size() > 0 || depositsChanged.size() > 0
    ) {
      try (DepositsService depositsService = new DepositsService().initialize()) {
        depositsService.addMultipleMembers(depositsAdded);
        depositsService.removeMultipleMembers(depositsRemoved);
        depositsService.updateMultipleMembers(depositsChanged);
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

    var removedIt = members.iterator();
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

    var it = changes.iterator();
    while (it.hasNext()) {
      EmailChange emailChange = it.next();
      if (!Boolean.TRUE.equals(emailChange.getChecked())) {
        it.remove();
      }
    }
    return changes;
  }

  List<DepositsMember> cleanDeposits(List<DepositsMember> members) {
    if (members == null) {
      return List.of();
    }

    var it = members.iterator();
    while (it.hasNext()) {
      DepositsMember member = it.next();
      if (!Boolean.TRUE.equals(member.getChecked())) {
        it.remove();
      }
    }
    return members;
  }

  List<DepositsChange> cleanDepositsChanges(List<DepositsChange> changes) {
    if (changes == null) {
      return List.of();
    }
    var it = changes.iterator();
    while (it.hasNext()) {
      DepositsChange change = it.next();
      if (!Boolean.TRUE.equals(change.getChecked())) {
        it.remove();
      }
    }
    return changes;
  }
}
