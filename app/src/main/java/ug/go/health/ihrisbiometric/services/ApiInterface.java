package ug.go.health.ihrisbiometric.services;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;
import ug.go.health.ihrisbiometric.models.ClockHistory;
import ug.go.health.ihrisbiometric.models.FaceUploadResponse;
import ug.go.health.ihrisbiometric.models.FacilityListResponse;
import ug.go.health.ihrisbiometric.models.FingerprintUploadResponse;
import ug.go.health.ihrisbiometric.models.LoginRequest;
import ug.go.health.ihrisbiometric.models.LoginResponse;
import ug.go.health.ihrisbiometric.models.NotificationListResponse;
import ug.go.health.ihrisbiometric.models.OutOfStationRequest;
import ug.go.health.ihrisbiometric.models.OutOfStationResponse;
import ug.go.health.ihrisbiometric.models.StaffListResponse;
import ug.go.health.ihrisbiometric.models.StaffRecord;

public interface ApiInterface {

    // Get list of Notifications
    @GET("notifications_list")
    Call<NotificationListResponse> getNotificationList();

    // Get list of staff records
    @GET("staff_list")
    Call<StaffListResponse> getStaffList();

    // Get a single staff record by id
    @GET("staff_details/{id}")
    Call<StaffRecord> getStaffById(@Path("id") int id);

    // Login
    @POST("login")
    Call<LoginResponse> login(@Body LoginRequest request);

    // Download staff image
    @GET("staff/{facility_id}/images")
    Call<ResponseBody> downloadStaffImages(@Path("facility_id") int id);

    // Get Staff List by FacilityName using retrofit
    @GET("staff_list")
    Call<StaffListResponse> getStaffListByFacilityName(@Query("facility_name") String facilityName);

    // Get List of facilities
    @GET("facilities")
    Call<FacilityListResponse> getFacilities();

    // Sync Staff Record to remote database
    @POST("staff_list")
    Call<StaffRecord> syncStaffRecord(@Body StaffRecord staffRecord);

    @POST("clock_user")
    Call<ClockHistory> syncClockHistory(@Body ClockHistory clockHistory);


    @Multipart
    @POST("upload_face")
    Call<FaceUploadResponse> uploadFace(
            @Part MultipartBody.Part file
    );

    @Multipart
    @POST("upload_fingerprint")
    Call<FingerprintUploadResponse> uploadFingerprint(
            @Part MultipartBody.Part file
    );

    @Multipart
    @POST("submit_out_of_station_request")
    Call<OutOfStationResponse> submitOutOfStationRequest(
            @Part("startDate") RequestBody startDate,
            @Part("endDate") RequestBody endDate,
            @Part("reason") RequestBody reason,
            @Part("comments") RequestBody comments,
            @Part MultipartBody.Part document
    );

}
