package org.wingsofcarolina.groups.http;

import org.wingsofcarolina.groups.domain.GroupsIoMembersResponse;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface GroupsIoAPI {
  @GET("getmembers")
  Call<GroupsIoMembersResponse> getMembers(
    @Header("Authorization") String authorization,
    @Query("group_id") String groupId,
    @Query("type") String type,
    @Query("limit") int limit,
    @Query("page_token") String pageToken
  );

  @Headers({ "Content-Type: application/x-www-form-urlencoded" })
  @FormUrlEncoded
  @POST("directadd")
  Call<Void> addMember(
    @Header("Authorization") String authorization,
    @Field("emails") String emails,
    @Field("group_id") String group_id
  );

  @Headers({ "Content-Type: application/x-www-form-urlencoded" })
  @FormUrlEncoded
  @POST("bulkremovemembers")
  Call<Void> removeMember(
    @Header("Authorization") String authorization,
    @Field("group_id") String group_id,
    @Field("emails") String emails
  );
}
