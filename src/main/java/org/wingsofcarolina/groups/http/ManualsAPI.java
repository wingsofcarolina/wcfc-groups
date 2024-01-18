package org.wingsofcarolina.groups.http;

import java.util.Map;

import org.wingsofcarolina.groups.domain.Member;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ManualsAPI {

	@SuppressWarnings("rawtypes")
	@GET("login")
	Call<Map> login(@Query("email") String email, @Query("password") String password);
	
	@POST("add")
	Call<Void> addMember(@Header("X-WCFC-TOKEN") String secret, @Body Member user);
	
	@POST("remove")
	Call<Void> removeMember(@Header("X-WCFC-TOKEN") String secret, @Body Member user);
}
