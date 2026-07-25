package org.wingsofcarolina.groups.http;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import okhttp3.logging.HttpLoggingInterceptor.Logger;
import org.slf4j.LoggerFactory;
import org.wingsofcarolina.groups.MemberReader;
import org.wingsofcarolina.groups.domain.EmailChange;
import org.wingsofcarolina.groups.domain.GroupsIoMember;
import org.wingsofcarolina.groups.domain.GroupsIoMembersResponse;
import org.wingsofcarolina.groups.domain.Member;
import org.wingsofcarolina.groups.domain.MemberDiff;
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

  private Retrofit retrofit;
  private GroupsIoAPI api;
  private String apiKey = null;
  private String group_id = "121229"; // Wings-of-Carolina, hopefully immutable

  public GroupsIoService() {}

  public GroupsIoService initialize() {
    HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor(
      new Logger() {
        @Override
        public void log(String message) {
          logger.debug(message);
        }
      }
    );
    interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
    OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();

    retrofit =
      new Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .client(client)
        .build();

    api = retrofit.create(GroupsIoAPI.class);
    return this;
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
      for (Member member : members) {
        addMember(member.output());
      }
      return true;
    } else {
      return false;
    }
  }

  public boolean removeMultipleMembers(List<Member> members) throws APIException {
    if (apiKey != null && members.size() > 0) {
      for (Member member : members) {
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
      throw new APIException("Failed to add Groups.io member", e);
    }
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
      throw new APIException("Failed to remove Groups.io member", e);
    }
  }

  public MemberDiff diff(MemberReader desired) throws APIException {
    List<Member> actual = members();
    return calculateDiff(desired, actual);
  }

  public MemberDiff diff(MemberReader desired, List<Member> excluded)
    throws APIException {
    List<Member> actual = members();
    Set<String> excludedEmails = new HashSet<String>();
    Set<String> excludedNames = new HashSet<String>();
    for (Member member : excluded) {
      excludedEmails.add(normalizeEmail(member.getEmail()));
      excludedNames.add(normalizeName(member.getName()));
    }
    actual.removeIf(member -> MemberReader.isExcludedName(member.getName()));
    actual.removeIf(member ->
      excludedEmails.contains(normalizeEmail(member.getEmail())) ||
      excludedNames.contains(normalizeName(member.getName()))
    );
    return calculateDiff(desired, actual);
  }

  private MemberDiff calculateDiff(MemberReader desired, List<Member> actual) {
    Map<String, List<Member>> desiredByName = membersByName(
      new ArrayList<Member>(desired.members().values())
    );
    Map<String, List<Member>> actualByName = membersByName(actual);
    Map<String, Member> actualByEmail = new HashMap<String, Member>();
    Set<Member> matched = new HashSet<Member>();
    MemberDiff diff = new MemberDiff();

    for (Member member : actual) {
      actualByEmail.put(normalizeEmail(member.getEmail()), member);
    }

    for (Member wanted : desired.members().values()) {
      Member current = actualByEmail.get(normalizeEmail(wanted.getEmail()));
      if (current != null && !matched.contains(current)) {
        matched.add(current);
        continue;
      }

      String name = normalizeName(wanted.getName());
      List<Member> desiredMatches = desiredByName.getOrDefault(name, List.of());
      List<Member> actualMatches = actualByName.getOrDefault(name, List.of());
      if (desiredMatches.size() == 1 && actualMatches.size() == 1) {
        current = actualMatches.getFirst();
        if (!matched.contains(current)) {
          current.setId(wanted.getId());
          matched.add(current);
          diff.getChanged().add(new EmailChange(current, wanted));
          continue;
        }
      }
      diff.getAdded().add(wanted);
    }

    for (Member current : actual) {
      if (!matched.contains(current)) {
        diff.getRemoved().add(current);
      }
    }
    return diff;
  }

  private List<Member> members() throws APIException {
    List<Member> members = new ArrayList<Member>();
    String pageToken = null;
    do {
      Call<GroupsIoMembersResponse> call = api.getMembers(
        getBearerToken(),
        group_id,
        "members",
        100,
        pageToken
      );
      try {
        Response<GroupsIoMembersResponse> response = call.execute();
        if (!response.isSuccessful()) {
          APIError error = ErrorUtils.parseError(retrofit, response);
          throw new APIException(error.message());
        }
        GroupsIoMembersResponse body = response.body();
        if (body == null) {
          throw new APIException("Groups.io returned an empty member response");
        }
        for (GroupsIoMember member : body.getData()) {
          members.add(new Member(null, member.getFullName(), member.getEmail()));
        }
        pageToken = body.isHasMore() ? body.getNextPageToken() : null;
        if (body.isHasMore() && (pageToken == null || pageToken.isBlank())) {
          throw new APIException("Groups.io member response omitted its next page token");
        }
      } catch (IOException e) {
        throw new APIException("Failed to read Groups.io members", e);
      }
    } while (pageToken != null);
    return members;
  }

  private Map<String, List<Member>> membersByName(List<Member> members) {
    Map<String, List<Member>> result = new HashMap<String, List<Member>>();
    for (Member member : members) {
      result
        .computeIfAbsent(
          normalizeName(member.getName()),
          ignored -> new ArrayList<Member>()
        )
        .add(member);
    }
    return result;
  }

  private String normalizeEmail(String email) {
    return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
  }

  private String normalizeName(String name) {
    return name == null
      ? ""
      : name.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
  }
}
