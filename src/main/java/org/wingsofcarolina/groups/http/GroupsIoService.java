package org.wingsofcarolina.groups.http;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import okhttp3.logging.HttpLoggingInterceptor.Logger;
import org.slf4j.LoggerFactory;
import org.wingsofcarolina.groups.domain.Member;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class GroupsIoService {

  private static final org.slf4j.Logger logger = LoggerFactory.getLogger(
    GroupsIoService.class
  );

  static String BASE_URL = System.getProperty(
    "groupsio.base.url",
    "https://groups.io/api/v1/"
  );

  private static GroupsIoService instance = null;

  private Retrofit retrofit;
  private GroupsIoAPI api;
  private String apiKey = null;
  private String group_id = "121229"; // Wings-of-Carolina, hopefully immutable

  public GroupsIoService() {}

  public static GroupsIoService instance() {
    return instance;
  }

  public GroupsIoService initialize() {
    if (instance == null) {
      HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor(
        new Logger() {
          @Override
          public void log(String message) {
            logger.debug(message);
          }
        }
      );
      interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
      OkHttpClient client = new OkHttpClient.Builder()
        .addInterceptor(interceptor)
        .build();

      retrofit =
        new Retrofit.Builder()
          .baseUrl(BASE_URL)
          .addConverterFactory(GsonConverterFactory.create())
          .client(client)
          .build();

      api = retrofit.create(GroupsIoAPI.class);
      instance = this;
    }

    return instance;
  }

  public void setApiKey(String apiKey) {
    logger.debug("Setting API key for authentication");
    this.apiKey = apiKey;
  }

  private String getBearerToken() {
    return "Bearer " + apiKey;
  }

  public boolean addMultipleMembers(List<Member> members) throws APIException {
    if (apiKey != null && members.size() > 0) {
      Iterator<Member> it = members.iterator();
      while (it.hasNext()) {
        Member member = it.next();
        addMember(member.output());
      }
      return true;
    } else {
      return false;
    }
  }

  public boolean removeMultipleMembers(List<Member> members) throws APIException {
    if (apiKey != null && members.size() > 0) {
      Iterator<Member> it = members.iterator();
      while (it.hasNext()) {
        Member member = it.next();
        removeMember(member.getEmail());
      }
      return true;
    } else {
      return false;
    }
  }

  public boolean addMember(String emails) throws APIException {
    logger.info("Adding : {}", emails);
    Call<Void> call = api.addMember(getBearerToken(), emails, group_id);
    try {
      Response<Void> response = call.execute();
      if (response.isSuccessful()) {
        return true;
      } else {
        APIError error = ErrorUtils.parseError(retrofit, response);
        logger.info("Error message -- " + error.message());
        throw new APIException(error.message());
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return false;
  }

  public boolean removeMember(String emails) throws APIException {
    logger.info("Removing : {}", emails);
    Call<Void> call = api.removeMember(getBearerToken(), group_id, emails);
    try {
      Response<Void> response = call.execute();
      if (response.isSuccessful()) {
        return true;
      } else {
        APIError error = ErrorUtils.parseError(retrofit, response);
        logger.info("Error message -- " + error.message());
        throw new APIException(error.message());
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return false;
  }
}
