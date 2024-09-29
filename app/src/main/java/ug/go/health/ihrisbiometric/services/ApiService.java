package ug.go.health.ihrisbiometric.services;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;

import java.io.File;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import io.sentry.okhttp.SentryOkHttpInterceptor;
import io.sentry.okhttp.SentryOkHttpEventListener;
import okhttp3.logging.HttpLoggingInterceptor;
import okhttp3.logging.HttpLoggingInterceptor.Level;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import ug.go.health.ihrisbiometric.models.DeviceSettings;

public class ApiService {

    private static final String SECRET_KEY = "qwerty@123";
    //    private static final String ISSUER = "your_issuer_here";
    private static final int TOKEN_EXPIRATION_TIME = 3600 * 24; // 24 hours

    private static ApiInterface apiInterface;

    private static DbService dbService;

    public static ApiInterface getApiInterface(Context context, String token) {
        // Define cache size and cache directory
        int cacheSize = 10 * 1024 * 1024; // 10 MB
        File cacheDirectory = new File(context.getCacheDir(), "api_cache");

        // Create cache object
        Cache cache = new Cache(cacheDirectory, cacheSize);

        // Create logging interceptor
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        //loggingInterceptor.setLevel(Level.);

        // Create network interceptor
        Interceptor networkInterceptor = chain -> {
            Request request = chain.request();
            if (!ApiService.isInternetAvailable(context)) {
                CacheControl cacheControl = new CacheControl.Builder()
                        .maxStale(7, TimeUnit.DAYS)
                        .build();
                request = request.newBuilder()
                        .cacheControl(cacheControl)
                        .build();
            }

            // Add "Accept: application/json" header to RequestBuilder object
            request = request.newBuilder()
                    .addHeader("Accept", "application/json")
                    .addHeader("Content-Type", "application/json")
                    .build();

            // Add token to RequestBuilder object if it is not null
            if (token != null) {
                request = request.newBuilder()
                        .addHeader("Authorization", "Bearer " + token)
                        .build();
            }

            Response response = chain.proceed(request);
            if (ApiService.isInternetAvailable(context)) {
                int maxAge = 60; // 1 minute
                response.newBuilder()
                        .header("Cache-Control", "public, max-age=" + maxAge)
                        .build();
            } else {
                int maxStale = 7 * 24 * 60 * 60; // 7 days
                response.newBuilder()
                        .header("Cache-Control", "public, only-if-cached, max-stale=" + maxStale)
                        .build();
            }


            return response;
        };

        loggingInterceptor.setLevel(Level.BASIC);

        // Create OkHttpClient with cache, network interceptors, and Sentry logging
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .cache(cache)
                .addInterceptor(loggingInterceptor)
                .addNetworkInterceptor(networkInterceptor)
                .addInterceptor(new SentryOkHttpInterceptor())
                .eventListener(new SentryOkHttpEventListener())
                .build();

        // Get base url from device settings
        SessionService session = new SessionService(context);
        DeviceSettings deviceSettings = session.getDeviceSettings();
        String baseUrl = deviceSettings.getServerUrl();

        // Create Retrofit object
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build();

        // Create ApiInterface object
        apiInterface = retrofit.create(ApiInterface.class);

        return apiInterface;
    }

    // Check if internet is available
    public static boolean isInternetAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
            if (capabilities != null) {
                if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    return true;
                }
            }
        }
        return false;
    }

    // Decode the JWT token and return the payload
    public static Map<String, Claim> decodeToken(String token) throws JWTDecodeException {
        Algorithm algorithm = Algorithm.HMAC256(SECRET_KEY);
        JWTVerifier verifier = JWT.require(algorithm)
                .build();
        DecodedJWT jwt = verifier.verify(token);
        return jwt.getClaims();
    }
}
