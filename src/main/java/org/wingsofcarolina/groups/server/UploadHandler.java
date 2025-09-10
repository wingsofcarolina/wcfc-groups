package org.wingsofcarolina.groups.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.form.FormData;
import io.undertow.server.handlers.form.FormData.FileItem;
import io.undertow.server.handlers.form.FormData.FormValue;
import io.undertow.server.handlers.form.FormDataParser;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wingsofcarolina.groups.MemberListXLS;
import org.wingsofcarolina.groups.domain.Member;

public class UploadHandler implements HttpHandler {

  private static final Logger logger = LoggerFactory.getLogger(UploadHandler.class);

  private static ObjectMapper mapper = new ObjectMapper();

  @Override
  public void handleRequest(HttpServerExchange hse) throws Exception {
    ArrayList<Member> added = new ArrayList<Member>();
    ArrayList<Member> removed = new ArrayList<Member>();
    Iterator<Map.Entry<Integer, Member>> iterator;

    String uri = hse.getRequestURI();
    String method = hse.getRequestMethod().toString();
    logger.info("==> " + method + " : " + uri);

    FormData attachment = hse.getAttachment(FormDataParser.FORM_DATA);
    if (attachment != null) {
      Deque<FormValue> members = attachment.get("members");
      if (members != null) {
        FileItem first = members.getFirst().getFileItem();
        if (first != null) {
          InputStream is = first.getInputStream();
          try {
            MemberListXLS updateList = new MemberListXLS(is);
            MemberListXLS savedList = new MemberListXLS(Member.getAll());

            // First, remove all waitlist and cruft entries
            boolean found = false;
            savedList.clean();
            updateList.clean();

            // Look for any new members
            found = false;
            logger.info("Members to be added : ");
            logger.info("================================");
            iterator = updateList.members().entrySet().iterator();
            while (iterator.hasNext()) {
              Map.Entry<Integer, Member> entry = iterator.next();
              Member member = entry.getValue();
              if (!savedList.hasMember(member)) {
                logger.info(member.output());
                added.add(member);
                found = true;
              }
            }
            if (!found) logger.info("No new members added.");

            // Look for any removed members
            found = false;
            logger.info("Members to be removed : ");
            logger.info("================================");
            iterator = savedList.members().entrySet().iterator();
            while (iterator.hasNext()) {
              Map.Entry<Integer, Member> entry = iterator.next();
              Member member = entry.getValue();
              if (!updateList.hasMember(member)) {
                logger.info(member.output());
                removed.add(member);
                found = true;
              }
            }
            if (!found) logger.info("No members removed.");
          } catch (Exception ex) {
            logger.info(ex.getMessage());
            ex.printStackTrace();
          }

          Map<String, Object> response = new HashMap<String, Object>();
          //				    response.put("file", basename);
          response.put("removed", removed);
          response.put("added", added);
          String json = mapper.writeValueAsString(response);

          hse.getResponseSender().send(json);
          return;
        }
      }
    }
    hse.getResponseSender().send("Upload attempt failed");
  }
}
