package ug.go.health.ihrisbiometric.services;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.util.Log;

import java.io.File;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import ug.go.health.ihrisbiometric.models.DeviceSettings;

public class ApiService {

    private static final String TAG = ApiService.class.getSimpleName();
    private static final int CACHE_SIZE = 10 * 1024 * 1024; // 10 MB
    private static final int MAX_AGE = 60; // 1 minute
    private static final int MAX_STALE = 7 * 24 * 60 * 60; // 7 days

    private static ApiInterface apiInterface;
    private static OkHttpClient okHttpClient;
    private static TokenInterceptor tokenInterceptor;

    private ApiService() {
        // Private constructor to prevent instantiation
    }

    public static ApiInterface getApiInterface(Context context, String token) {
        if (apiInterface == null) {
            synchronized (ApiService.class) {
                if (apiInterface == null) {
                    apiInterface = createApiInterface(context);
                }
            }
        }
        return apiInterface;
    }

    private static ApiInterface createApiInterface(Context context) {
        okHttpClient = buildOkHttpClient(context);
        String baseUrl = getBaseUrl(context);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build();

        return retrofit.create(ApiInterface.class);
    }

    private static OkHttpClient buildOkHttpClient(Context context) {
        File cacheDirectory = new File(context.getCacheDir(), "api_cache");
        Cache cache = new Cache(cacheDirectory, CACHE_SIZE);

        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        tokenInterceptor = new TokenInterceptor();

        return new OkHttpClient.Builder()
                .cache(cache)
                .addInterceptor(loggingInterceptor)
                .addInterceptor(tokenInterceptor)
                .addNetworkInterceptor(provideNetworkInterceptor(context))
                .build();
    }

    private static Interceptor provideNetworkInterceptor(Context context) {
        return chain -> {
            Request originalRequest = chain.request();
            Request.Builder requestBuilder = originalRequest.newBuilder()
                    .addHeader("Accept", "application/json")
                    .addHeader("Content-Type", "application/json");

            if (!isInternetAvailable(context)) {
                requestBuilder.cacheControl(new CacheControl.Builder()
                        .maxStale(MAX_STALE, TimeUnit.SECONDS)
                        .build());
            }

            Request request = requestBuilder.build();
            Response response = chain.proceed(request);

            if (isInternetAvailable(context)) {
                response = response.newBuilder()
                        .header("Cache-Control", "public, max-age=" + MAX_AGE)
                        .build();
            } else {
                response = response.newBuilder()
                        .header("Cache-Control", "public, only-if-cached, max-stale=" + MAX_STALE)
                        .build();
            }

            return response;
        };
    }

    private static String getBaseUrl(Context context) {
        SessionService session = new SessionService(context);
        DeviceSettings deviceSettings = session.getDeviceSettings();
        return deviceSettings.getServerUrl();
    }

    public static boolean isInternetAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
            return capabilities != null && (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                    || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI));
        }
        return false;
    }

    public static void setToken(String token) {
        if (tokenInterceptor != null) {
            tokenInterceptor.setToken(token);
        } else {
            Log.e(TAG, "TokenInterceptor is null. Make sure ApiInterface is initialized before setting the token.");
        }
    }

    private static class TokenInterceptor implements Interceptor {
        private String token;

        public void setToken(String token) {
            this.token = token;
        }

        @Override
        public Response intercept(Chain chain) throws java.io.IOException {
            Request original = chain.request();
            if (token == null || token.isEmpty()) {
                return chain.proceed(original);
            }

            Request.Builder requestBuilder = original.newBuilder()
                    .header("Authorization", "Bearer " + token);

            Request request = requestBuilder.build();
            return chain.proceed(request);
        }
    }
}