package org.wingsofcarolina.groups.http;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.LoggerFactory;
import org.wingsofcarolina.groups.domain.Member;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import okhttp3.logging.HttpLoggingInterceptor.Logger;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ManualsService {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(ManualsService.class);

	static String BASE_URL = "https://manuals.wingsofcarolina.org/api/member/";
	
	private static ManualsService instance = null;

	private Retrofit retrofit;
	private ManualsAPI api;
	private String WCFC_TOKEN = "adfasd58df57a8adf68dsafd";
	
	public ManualsService() {}
	
	public static ManualsService instance() {
		return instance;
	}
	
	public ManualsService initialize() {
		if (instance == null) {
			
			
			HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor( new Logger() 
			{
			    @Override public void log(String message) 
			    {
			        logger.debug(message);
			    }
			});
			interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
			OkHttpClient client = new OkHttpClient.Builder()
			        .readTimeout(60, TimeUnit.SECONDS)
			        .connectTimeout(60, TimeUnit.SECONDS)
					.addInterceptor(interceptor)
					.build();
	
			retrofit = new Retrofit.Builder()
					.baseUrl(BASE_URL)
					.addConverterFactory(GsonConverterFactory.create())
					.client(client)
					.build();
	
			api = retrofit.create(ManualsAPI.class);
			instance = this;
		}

		return instance;
	}
	
	public boolean addMultipleMembers(List<Member> members) throws APIException {
		if (members.size() > 0) {
			Iterator<Member> it = members.iterator();
			while (it.hasNext()) {
				Member member = it.next();
				addMember(member);
			}
			return true;
		} else {
			return false;
		}
	}
	
	public boolean removeMultipleMembers(List<Member> members) throws APIException {
		if (members.size() > 0) {
			Iterator<Member> it = members.iterator();
			while (it.hasNext()) {
				Member member = it.next();
				removeMember(member);
			}
			return true;
		} else {
			return false;
		}
	}
	
	public boolean addMember(Member member) throws APIException {
		logger.info("Adding : {}", member);
		Call<Void> call = api.addMember(WCFC_TOKEN, member);
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
	
	
	public boolean removeMember(Member member) throws APIException {
		logger.info("Removing : {}", member);
		Call<Void> call = api.removeMember(WCFC_TOKEN, member);
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
