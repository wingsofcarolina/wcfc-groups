package org.wingsofcarolina.groups.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.form.FormData;
import io.undertow.server.handlers.form.FormData.FileItem;
import io.undertow.server.handlers.form.FormData.FormValue;
import io.undertow.server.handlers.form.FormDataParser;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;
import java.io.InputStream;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wingsofcarolina.groups.MemberListCSV;
import org.wingsofcarolina.groups.MemberListXLS;
import org.wingsofcarolina.groups.MemberReader;
import org.wingsofcarolina.groups.domain.MemberDiff;
import org.wingsofcarolina.groups.http.DepositsService;
import org.wingsofcarolina.groups.http.DepositsService.DepositsDiff;
import org.wingsofcarolina.groups.http.GroupsIoService;
import org.wingsofcarolina.groups.http.ManualsDatabaseService;

public class UploadHandler implements HttpHandler {

  private static final Logger logger = LoggerFactory.getLogger(UploadHandler.class);

  private static ObjectMapper mapper = new ObjectMapper();

  @Override
  public void handleRequest(HttpServerExchange hse) throws Exception {
    MemberDiff groupsDiff = new MemberDiff();
    MemberDiff manualsDiff = new MemberDiff();
    DepositsDiff depositsDiff = new DepositsDiff();

    String uri = hse.getRequestURI();
    String method = hse.getRequestMethod().toString();
    logger.info("==> " + method + " : " + uri);

    FormData attachment = hse.getAttachment(FormDataParser.FORM_DATA);
    if (attachment != null) {
      Deque<FormValue> members = attachment.get("members");
      if (members != null) {
        FormValue upload = members.getFirst();
        FileItem first = upload.getFileItem();
        if (first != null) {
          try (InputStream is = first.getInputStream()) {
            MemberReader updateList = readMemberList(is, upload.getFileName());
            Set<Integer> ignoredForSyncMemberIds = updateList.ignoredForSyncMemberIds();
            List<org.wingsofcarolina.groups.domain.Member> excludedMembers = updateList.excludedFromSyncMembers();
            updateList.clean();

            GroupsIoService groupsIoService = configuredGroupsIoService();
            groupsDiff = groupsIoService.diff(updateList, excludedMembers);
            try (
              ManualsDatabaseService manualsService = new ManualsDatabaseService()
                .initialize();
              DepositsService depositsService = new DepositsService().initialize()
            ) {
              manualsDiff = manualsService.diff(updateList, ignoredForSyncMemberIds);
              depositsDiff = depositsService.diff(updateList, ignoredForSyncMemberIds);
            }
          } catch (Exception ex) {
            logger.error("Membership comparison failed", ex);
            sendError(hse, ex);
            return;
          }

          Map<String, Object> response = new HashMap<String, Object>();
          response.put("groupsRemoved", groupsDiff.getRemoved());
          response.put("groupsAdded", groupsDiff.getAdded());
          response.put("groupsChanged", groupsDiff.getChanged());
          response.put("manualsRemoved", manualsDiff.getRemoved());
          response.put("manualsAdded", manualsDiff.getAdded());
          response.put("manualsChanged", manualsDiff.getChanged());
          response.put("depositsRemoved", depositsDiff.getRemoved());
          response.put("depositsAdded", depositsDiff.getAdded());
          response.put("depositsChanged", depositsDiff.getChanged());
          String json = mapper.writeValueAsString(response);

          hse.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
          hse.getResponseSender().send(json);
          return;
        }
      }
    }
    hse.getResponseSender().send("Upload attempt failed");
  }

  private GroupsIoService configuredGroupsIoService() {
    String apiKey = System.getenv("GROUPS_IO_API_KEY");
    if (apiKey == null || apiKey.trim().isEmpty()) {
      throw new IllegalStateException("GROUPS_IO_API_KEY is not configured");
    }
    GroupsIoService service = new GroupsIoService().initialize();
    service.setApiKey(apiKey);
    return service;
  }

  private MemberReader readMemberList(InputStream is, String fileName) throws Exception {
    if (fileName != null && fileName.toLowerCase(Locale.ROOT).endsWith(".csv")) {
      return new MemberListCSV(is);
    }
    return new MemberListXLS(is);
  }

  private void sendError(HttpServerExchange hse, Exception ex) throws Exception {
    hse.setStatusCode(StatusCodes.INTERNAL_SERVER_ERROR);
    hse.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
    hse
      .getResponseSender()
      .send(
        mapper.writeValueAsString(
          Map.of(
            "code",
            StatusCodes.INTERNAL_SERVER_ERROR,
            "message",
            ex.getMessage() == null ? "Membership comparison failed" : ex.getMessage()
          )
        )
      );
  }
}
