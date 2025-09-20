package org.wingsofcarolina.groups.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VersionHandler implements HttpHandler {

  private static final Logger logger = LoggerFactory.getLogger(VersionHandler.class);
  private static final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public void handleRequest(HttpServerExchange hse) throws Exception {
    Map<String, String> version = getBuildMetadata();

    if (version == null) {
      hse.setStatusCode(StatusCodes.INTERNAL_SERVER_ERROR);
      hse.getResponseSender().send("Error retrieving version information");
      return;
    }

    try {
      String jsonResponse = objectMapper.writeValueAsString(version);
      hse.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
      hse.setStatusCode(StatusCodes.OK);
      hse.getResponseSender().send(jsonResponse);
    } catch (Exception e) {
      logger.error("Error serializing version information", e);
      hse.setStatusCode(StatusCodes.INTERNAL_SERVER_ERROR);
      hse.getResponseSender().send("Error serializing version information");
    }
  }

  public static Map<String, String> getBuildMetadata() {
    Map<String, String> version = new HashMap<String, String>();

    // Check if we're in development mode (you can adjust this logic as needed)
    String mode = System.getProperty("mode", "PROD");
    if ("DEV".equals(mode)) {
      version.put("version", "DEV");
      version.put("build", "DEV");
      return version;
    }

    try {
      InputStream gitPropsStream =
        VersionHandler.class.getClassLoader().getResourceAsStream("git.properties");
      if (gitPropsStream != null) {
        Properties gitProps = new Properties();
        gitProps.load(gitPropsStream);
        gitPropsStream.close();

        for (String prop : new String[] {
          "git.build.version",
          "git.commit.id",
          "git.branch",
          "git.build.time",
          "git.commit.user.name",
          "git.commit.id.describe",
        }) {
          String propVal = gitProps.getProperty(prop);
          if (propVal != null) {
            version.put(prop, propVal);
          }
        }
      } else {
        // Fallback if git.properties is not available
        version.put("version", "unknown");
        version.put("build", "unknown");
      }
    } catch (IOException e) {
      logger.info("IOException during git.properties retrieval: {}", e.getMessage());
      return null;
    }
    return version;
  }
}
