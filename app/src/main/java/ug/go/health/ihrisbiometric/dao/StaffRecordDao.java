package ug.go.health.ihrisbiometric.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import ug.go.health.ihrisbiometric.models.StaffRecord;

@Dao
public interface StaffRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(StaffRecord staffRecord);

    @Query("SELECT * FROM staff_records")
    List<StaffRecord> getAllStaffRecords();

    @Query("SELECT * FROM staff_records WHERE ihris_pid = :ihrisPid")
    StaffRecord getStaffRecordByIhrisPid(String ihrisPid);

    @Query("SELECT * FROM staff_records WHERE template_id = :templateId")
    StaffRecord getStaffRecordByTemplate(int templateId);

    @Query("SELECT * FROM staff_records WHERE face_data IS NOT NULL")
    List<StaffRecord> getStaffRecordsWithEmbeddings();

    @Query("SELECT * FROM staff_records WHERE synced = 0")
    List<StaffRecord> getUnsyncedStaffRecords();

    @Query("SELECT COUNT(*) FROM staff_records WHERE synced = 0")
    int countUnsyncedStaffRecords();

    @Query("SELECT * FROM staff_records WHERE synced = 0")
    List<StaffRecord> getStaffRecordsReadyForSync();

    @Query("SELECT * FROM staff_records WHERE synced = 0")
    List<StaffRecord> getStaffRecordsMissingInfo();

    @Query("SELECT COUNT(*) FROM staff_records WHERE synced = 1")
    int countSyncedStaffRecords();

    @Query("SELECT COUNT(*) FROM staff_records")
    int countStaffRecords();

    @Update
    void update(StaffRecord staffRecord);

    @Query("DELETE FROM staff_records")
    void deleteAll();

    @Query("SELECT * FROM staff_records WHERE (ihris_pid LIKE :filter OR template_id LIKE :filter) AND enrolled_at BETWEEN :startTimestamp AND :endTimestamp")
    List<StaffRecord> getFilteredStaffRecords(String filter, Long startTimestamp, Long endTimestamp);
}
