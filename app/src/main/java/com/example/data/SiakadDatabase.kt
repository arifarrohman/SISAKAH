package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// --- Room Entities ---

@Entity(tableName = "user_profiles")
data class UserProfile(
    @PrimaryKey val id: String, // e.g., "siswa_agus", "guru_budi", "ortu_agus"
    val name: String,
    val role: String, // "SISWA", "GURU", "ORANG_TUA"
    val idNumber: String, // NISN / NIP
    val classroom: String, // e.g., "XII RPL 1"
    val parentOf: String? = null, // Student's id if role is ORANG_TUA
    val registeredFaceDesc: String? = null, // Simulated face recognition profile desc
    val photoUrl: String = ""
)

@Entity(tableName = "schedules")
data class Schedule(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subjectName: String,
    val teacherName: String,
    val classroom: String, // XII RPL 1
    val day: String, // Senin, Selasa, Rabu, Kamis, Jumat
    val timeStart: String, // "07:00"
    val timeEnd: String, // "08:30"
    val room: String // "Lab Komputer 1"
)

@Entity(tableName = "capaian_pembelajaran")
data class CapaianPembelajaran(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subjectName: String,
    val phase: String, // "Fase F"
    val code: String, // "CP 3.2"
    val description: String,
    val classTarget: String, // "XII"
    val completionPercentage: Int = 0 // Monitoring progress
)

@Entity(tableName = "attendance_records")
data class Attendance(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: String,
    val studentName: String,
    val classroom: String,
    val date: String, // "YYYY-MM-DD"
    val subjectName: String,
    val status: String, // "HADIR", "SAKIT", "IZIN", "ALPHA"
    val timestamp: Long = System.currentTimeMillis(),
    val faceScanSimilarity: Float = 0f, // Face scan matching score (e.g. 98.2)
    val wasFaceVerified: Boolean = false,
    val notes: String? = null
)

@Entity(tableName = "lms_materials")
data class LmsMaterial(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subjectName: String,
    val title: String,
    val type: String, // "MATERI", "TUGAS"
    val content: String,
    val dueDate: String? = null, // "2026-06-18"
    val submissionText: String? = null,
    val isSubmitted: Boolean = false,
    val score: Int? = null,
    val teacherFeedback: String? = null
)

@Entity(tableName = "assessments")
data class Assessment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: String,
    val studentName: String,
    val subjectName: String,
    val category: String, // "TUGAS", "KUIS", "UH" (Ulangan Harian), "UTS", "UAS"
    val score: Int,
    val maxScore: Int = 100,
    val date: String, // "YYYY-MM-DD"
    val feedback: String? = null
)

// --- Daos ---

@Dao
interface SiakadDao {
    // Profiles
    @Query("SELECT * FROM user_profiles")
    fun getAllProfiles(): Flow<List<UserProfile>>

    @Query("SELECT * FROM user_profiles WHERE id = :id")
    suspend fun getProfileById(id: String): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfiles(profiles: List<UserProfile>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: UserProfile)

    // Schedule
    @Query("SELECT * FROM schedules WHERE classroom = :classroom ORDER BY timeStart ASC")
    fun getSchedulesByClass(classroom: String): Flow<List<Schedule>>

    @Query("SELECT * FROM schedules ORDER BY day, timeStart ASC")
    fun getAllSchedules(): Flow<List<Schedule>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedules(schedules: List<Schedule>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: Schedule)

    // Capaian Pembelajaran
    @Query("SELECT * FROM capaian_pembelajaran")
    fun getAllCP(): Flow<List<CapaianPembelajaran>>

    @Query("SELECT * FROM capaian_pembelajaran WHERE subjectName = :subjectName")
    fun getCPBySubject(subjectName: String): Flow<List<CapaianPembelajaran>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCPs(cps: List<CapaianPembelajaran>)

    @Update
    suspend fun updateCP(cp: CapaianPembelajaran)

    // Attendance
    @Query("SELECT * FROM attendance_records ORDER BY timestamp DESC")
    fun getAllAttendance(): Flow<List<Attendance>>

    @Query("SELECT * FROM attendance_records WHERE studentId = :studentId ORDER BY timestamp DESC")
    fun getAttendanceForStudent(studentId: String): Flow<List<Attendance>>

    @Query("SELECT * FROM attendance_records WHERE classroom = :classroom AND date = :date")
    fun getAttendanceByClassAndDate(classroom: String, date: String): Flow<List<Attendance>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: Attendance)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendances(attendances: List<Attendance>)

    // LMS
    @Query("SELECT * FROM lms_materials ORDER BY id DESC")
    fun getAllLmsMaterials(): Flow<List<LmsMaterial>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLmsMaterial(material: LmsMaterial)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLmsMaterials(materials: List<LmsMaterial>)

    @Update
    suspend fun updateLmsMaterial(material: LmsMaterial)

    // Assessments
    @Query("SELECT * FROM assessments ORDER BY date DESC")
    fun getAllAssessments(): Flow<List<Assessment>>

    @Query("SELECT * FROM assessments WHERE studentId = :studentId ORDER BY date DESC")
    fun getAssessmentsForStudent(studentId: String): Flow<List<Assessment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssessment(assessment: Assessment)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssessments(assessments: List<Assessment>)
}

// --- AppDatabase ---

@Database(
    entities = [
        UserProfile::class,
        Schedule::class,
        CapaianPembelajaran::class,
        Attendance::class,
        LmsMaterial::class,
        Assessment::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SiakadDatabase : RoomDatabase() {
    abstract val siakadDao: SiakadDao

    companion object {
        @Volatile
        private var INSTANCE: SiakadDatabase? = null

        fun getDatabase(context: Context): SiakadDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SiakadDatabase::class.java,
                    "siakad_smk_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
