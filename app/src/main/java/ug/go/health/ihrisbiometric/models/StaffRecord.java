package ug.go.health.ihrisbiometric.models;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;
import androidx.room.ColumnInfo;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.Arrays;

import ug.go.health.ihrisbiometric.converters.ByteArrayConverter;
import ug.go.health.ihrisbiometric.converters.FloatArrayConverter;
import ug.go.health.ihrisbiometric.converters.LocationConverter;

@Entity(tableName = "staff_records")
@TypeConverters({ByteArrayConverter.class, FloatArrayConverter.class, LocationConverter.class})
public class StaffRecord {
    @PrimaryKey
    @ColumnInfo(name = "id")
    private int id;

    @SerializedName("ihris_pid")
    @Expose
    @ColumnInfo(name = "ihris_pid")
    private String ihrisPid;

    @SerializedName("surname")
    @Expose
    @ColumnInfo(name = "surname")
    private String surname;

    @SerializedName("firstname")
    @Expose
    @ColumnInfo(name = "firstname")
    private String firstname;

    @SerializedName("othername")
    @Expose
    @ColumnInfo(name = "othername")
    private String othername;

    @SerializedName("job")
    @Expose
    @ColumnInfo(name = "job")
    private String job;

    @SerializedName("facility_id")
    @Expose
    @ColumnInfo(name = "facility_id")
    private String facilityId;

    @SerializedName("facility")
    @Expose
    @ColumnInfo(name = "facility")
    private String facility;

    @SerializedName("fingerprint_data")
    @Expose
    @ColumnInfo(name = "fingerprint_data")
    private byte[] fingerprintData;

    @SerializedName("face_data")
    @Expose
    @ColumnInfo(name = "face_data")
    private float[] faceData;

    @SerializedName("face_enrolled")
    @Expose
    @ColumnInfo(name = "face_enrolled")
    private boolean faceEnrolled;

    @SerializedName("fingerprint_enrolled")
    @Expose
    @ColumnInfo(name = "fingerprint_enrolled")
    private boolean fingerprintEnrolled;

    @SerializedName("synced")
    @Expose
    @ColumnInfo(name = "synced")
    private boolean synced;

    @SerializedName("template_id")
    @Expose
    @ColumnInfo(name = "template_id")
    private int templateId;

    @SerializedName("location")
    @Expose
    private Location location;

    @SerializedName("face_image_path")
    @Expose
    @ColumnInfo(name = "face_image_path")
    private String faceImagePath;

    public StaffRecord() {
    }

    @Ignore
    public StaffRecord(int id, String ihrisPid, String surname, String firstname, String othername, String job, String facilityId, String facility, byte[] fingerprintData, float[] faceData, boolean synced, Location location, int templateId) {
        this.id = id;
        this.ihrisPid = ihrisPid;
        this.surname = surname;
        this.firstname = firstname;
        this.othername = othername;
        this.job = job;
        this.facilityId = facilityId;
        this.facility = facility;
        this.fingerprintData = fingerprintData;
        this.faceData = faceData;
        this.synced = synced;
        this.location = location;
        this.templateId = templateId;
    }

    // Getters and setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getIhrisPid() {
        return ihrisPid;
    }

    public void setIhrisPid(String ihrisPid) {
        this.ihrisPid = ihrisPid;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getOthername() {
        return othername;
    }

    public void setOthername(String othername) {
        this.othername = othername;
    }

    public String getJob() {
        return job;
    }

    public void setJob(String job) {
        this.job = job;
    }

    public String getFacilityId() {
        return facilityId;
    }

    public void setFacilityId(String facilityId) {
        this.facilityId = facilityId;
    }

    public String getFacility() {
        return facility;
    }

    public void setFacility(String facility) {
        this.facility = facility;
    }

    public byte[] getFingerprintData() {
        return fingerprintData;
    }

    public void setFingerprintData(byte[] fingerprintData) {
        this.fingerprintData = fingerprintData;
    }

    public float[] getFaceData() {
        return faceData;
    }

    public void setFaceData(float[] faceData) {
        this.faceData = faceData;
    }

    public boolean isFaceEnrolled() {
        return faceEnrolled;
    }

    public void setFaceEnrolled(boolean faceEnrolled) {
        this.faceEnrolled = faceEnrolled;
    }

    public boolean isFingerprintEnrolled() {
        return fingerprintEnrolled;
    }

    public void setFingerprintEnrolled(boolean fingerprintEnrolled) {
        this.fingerprintEnrolled = fingerprintEnrolled;
    }

    public boolean isSynced() {
        return synced;
    }

    public void setSynced(boolean synced) {
        this.synced = synced;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public int getTemplateId() {
        return templateId;
    }

    public void setTemplateId(int templateId) {
        this.templateId = templateId;
    }

    public String getFaceImage() {
        return faceImage;
    }

    public void setFaceImage(String faceImage) {
        this.faceImage = faceImage;
    }


    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public static StaffRecord fromJson(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, StaffRecord.class);
    }

    public String getName() {
        return String.format("%s %s %s",
                        capitalizeFirstLetter(firstname),
                        capitalizeFirstLetter(othername),
                        capitalizeFirstLetter(surname))
                .trim();
    }

    private String capitalizeFirstLetter(String name) {
        return name != null && !name.isEmpty()
                ? Character.toUpperCase(name.charAt(0)) + name.substring(1).toLowerCase()
                : "";
    }

    @Override
    public String toString() {
        return "StaffRecord{" +
                "id=" + id +
                ", ihrisPid='" + ihrisPid + '\'' +
                ", surname='" + surname + '\'' +
                ", firstname='" + firstname + '\'' +
                ", othername='" + othername + '\'' +
                ", job='" + job + '\'' +
                ", facilityId='" + facilityId + '\'' +
                ", facility='" + facility + '\'' +
                ", fingerprintData=" + Arrays.toString(fingerprintData) +
                ", faceData=" + Arrays.toString(faceData) +
                ", faceEnrolled=" + faceEnrolled +
                ", fingerprintEnrolled=" + fingerprintEnrolled +
                ", synced=" + synced +
                ", templateId=" + templateId +
                ", location=" + location +
                '}';
    }
}
