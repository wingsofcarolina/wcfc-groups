package org.wingsofcarolina.groups.http;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface GroupsIoAPI {

	@SuppressWarnings("rawtypes")
	@GET("login")
	Call<Map> login(@Query("email") String email, @Query("password") String password);
	
	@Headers({
        "Content-Type: application/x-www-form-urlencoded",
	})
	@FormUrlEncoded
	@POST("directadd")
	Call<Void> addMember(@Header("Cookie") String cookie, @Field("csrf")String csrf, @Field("emails")String emails, @Field("group_id")String group_id);
	
	@Headers({
        "Content-Type: application/x-www-form-urlencoded",
	})
	@FormUrlEncoded
	@POST("bulkremovemembers")
	Call<Void> removeMember(@Header("Cookie") String cookie, @Field("csrf")String csrf, @Field("group_id")String group_id, @Field("emails")String emails);
}
