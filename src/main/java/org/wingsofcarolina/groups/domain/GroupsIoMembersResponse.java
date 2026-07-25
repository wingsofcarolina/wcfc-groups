package org.wingsofcarolina.groups.domain;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class GroupsIoMembersResponse {

  @SerializedName("has_more")
  private boolean hasMore;

  @SerializedName("next_page_token")
  private String nextPageToken;

  private List<GroupsIoMember> data;

  public boolean isHasMore() {
    return hasMore;
  }

  public String getNextPageToken() {
    return nextPageToken;
  }

  public List<GroupsIoMember> getData() {
    return data == null ? List.of() : data;
  }
}
