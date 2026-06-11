package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SiakadViewModel(application: Application) : AndroidViewModel(application) {
    private val db = SiakadDatabase.getDatabase(application)
    private val dao = db.siakadDao

    // --- Active User Selection ---
    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    // --- Reactive Database Streams ---
    val allProfiles: StateFlow<List<UserProfile>> = dao.getAllProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSchedules: StateFlow<List<Schedule>> = dao.getAllSchedules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCP: StateFlow<List<CapaianPembelajaran>> = dao.getAllCP()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAttendance: StateFlow<List<Attendance>> = dao.getAllAttendance()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allLmsMaterials: StateFlow<List<LmsMaterial>> = dao.getAllLmsMaterials()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAssessments: StateFlow<List<Assessment>> = dao.getAllAssessments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Screen UI/State Controls ---
    private val _isInitializing = MutableStateFlow(true)
    val isInitializing: StateFlow<Boolean> = _isInitializing.asStateFlow()

    private val _aiInsightState = MutableStateFlow<String?>(null)
    val aiInsightState: StateFlow<String?> = _aiInsightState.asStateFlow()

    private val _isAnalyzingAI = MutableStateFlow(false)
    val isAnalyzingAI: StateFlow<Boolean> = _isAnalyzingAI.asStateFlow()

    init {
        checkAndSeedDatabase()
    }

    private fun checkAndSeedDatabase() {
        viewModelScope.launch {
            _isInitializing.value = true
            // Check if profiles are empty
            val currentProfiles = dao.getAllProfiles().first()
            if (currentProfiles.isEmpty()) {
                seedInitialData()
            } else {
                // Set default user to Student (siswa_agus)
                val defaultUser = dao.getProfileById("siswa_agus")
                _currentUser.value = defaultUser
            }
            _isInitializing.value = false
        }
    }

    fun switchUser(userId: String) {
        viewModelScope.launch {
            val user = dao.getProfileById(userId)
            if (user != null) {
                _currentUser.value = user
                // Clear state when switching user
                _aiInsightState.value = null
            }
        }
    }

    // --- Action Handlers ---

    // 1. Submit Attendance via simulated face-recognition
    fun submitFaceAttendance(
        studentId: String,
        studentName: String,
        classroom: String,
        subjectName: String,
        status: String,
        faceScore: Float,
        wasVerified: Boolean,
        notes: String? = null
    ) {
        viewModelScope.launch {
            val attendance = Attendance(
                studentId = studentId,
                studentName = studentName,
                classroom = classroom,
                date = getCurrentDateString(),
                subjectName = subjectName,
                status = status,
                faceScanSimilarity = faceScore,
                wasFaceVerified = wasVerified,
                notes = notes
            )
            dao.insertAttendance(attendance)
        }
    }

    // 2. Add Assessment Grade (Guru)
    fun addAssessment(
        studentId: String,
        studentName: String,
        subjectName: String,
        category: String,
        score: Int,
        feedback: String?
    ) {
        viewModelScope.launch {
            val assessment = Assessment(
                studentId = studentId,
                studentName = studentName,
                subjectName = subjectName,
                category = category,
                score = score,
                date = getCurrentDateString(),
                feedback = feedback
            )
            dao.insertAssessment(assessment)
        }
    }

    // 3. Update Capaian Pembelajaran Completion Progress (Guru)
    fun updateCPProgress(cpId: Int, newProgress: Int) {
        viewModelScope.launch {
            // Find in current list
            val currentList = allCP.value
            val match = currentList.find { it.id == cpId }
            if (match != null) {
                val updated = match.copy(completionPercentage = newProgress)
                dao.updateCP(updated)
            }
        }
    }

    // 4. Submit LMS Student Assignment (Siswa)
    fun submitLmsAssignment(materialId: Int, answerText: String) {
        viewModelScope.launch {
            val currentLms = allLmsMaterials.value
            val match = currentLms.find { it.id == materialId }
            if (match != null) {
                val updated = match.copy(
                    isSubmitted = true,
                    submissionText = answerText
                )
                dao.updateLmsMaterial(updated)
            }
        }
    }

    // 5. Grade LMS Student Assignment (Guru)
    fun gradeLmsAssignment(materialId: Int, score: Int, feedback: String) {
        viewModelScope.launch {
            val currentLms = allLmsMaterials.value
            val match = currentLms.find { it.id == materialId }
            if (match != null) {
                val updated = match.copy(
                    score = score,
                    teacherFeedback = feedback
                )
                dao.updateLmsMaterial(updated)
            }
        }
    }

    // 6. Create Material or Assignment (Guru)
    fun insertLmsMaterial(subject: String, title: String, type: String, content: String, dueDate: String?) {
        viewModelScope.launch {
            val material = LmsMaterial(
                subjectName = subject,
                title = title,
                type = type,
                content = content,
                dueDate = dueDate
            )
            dao.insertLmsMaterial(material)
        }
    }

    // 7. Generate Parent Portal Gemini Academic Progress report
    fun generateParentAcademicReport(studentId: String) {
        viewModelScope.launch {
            _isAnalyzingAI.value = true
            _aiInsightState.value = null

            // Find matching student
            val studentProfile = dao.getProfileById(studentId) ?: return@launch
            
            // Collect historic grades
            val allStudentGrades = dao.getAssessmentsForStudent(studentId).first()
            
            // Collect attendance records
            val allStudentAttendance = dao.getAttendanceForStudent(studentId).first()
            
            // Core curriculum goals list
            val cps = dao.getAllCP().first()

            val response = GeminiManager.generateAcademicInsight(
                studentName = studentProfile.name,
                classroom = studentProfile.classroom,
                grades = allStudentGrades,
                attendance = allStudentAttendance,
                capaianList = cps
            )

            _aiInsightState.value = response
            _isAnalyzingAI.value = false
        }
    }

    // --- Helper Utility Methods ---
    private fun getCurrentDateString(): String {
        return java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
    }

    // --- Seed Database Sequences ---
    private suspend fun seedInitialData() {
        // 1. Roles & Profiles
        val mockProfiles = listOf(
            UserProfile(
                id = "siswa_agus",
                name = "Agus Saputra",
                role = "SISWA",
                idNumber = "NISN.1009842",
                classroom = "XII RPL 1",
                registeredFaceDesc = "Wajah Oval, Alis Tebal, Kacamata Frame Bulat Hitam"
            ),
            UserProfile(
                id = "siswa_rizky",
                name = "Rizky Pratama",
                role = "SISWA",
                idNumber = "NISN.1009848",
                classroom = "XII RPL 1",
                registeredFaceDesc = "Wajah Bulat, Rambut Spike, Tahi Lalat di Pipi Kiri"
            ),
            UserProfile(
                id = "guru_budi",
                name = "Budi Santoso, S.Kom",
                role = "GURU",
                idNumber = "NIP.198510252011011002",
                classroom = "XII RPL 1"
            ),
            UserProfile(
                id = "ortu_agus",
                name = "Bambang Saputra",
                role = "ORANG_TUA",
                idNumber = "WMR-10029",
                classroom = "XII RPL 1",
                parentOf = "siswa_agus"
            )
        )
        dao.insertProfiles(mockProfiles)

        // 2. Class Rosters & Schedule (XII RPL 1)
        val mockSchedules = listOf(
            Schedule(subjectName = "Pemrograman Web & Bergerak (PWPB)", teacherName = "Budi Santoso, S.Kom", classroom = "XII RPL 1", day = "Senin", timeStart = "07:30", timeEnd = "09:30", room = "Lab RPL Alpha"),
            Schedule(subjectName = "Pemrograman Berorientasi Objek (PBO)", teacherName = "Budi Santoso, S.Kom", classroom = "XII RPL 1", day = "Senin", timeStart = "10:00", timeEnd = "12:00", room = "Lab RPL Alpha"),
            Schedule(subjectName = "Basis Data (SQL & Cloud Database)", teacherName = "Siti Rahma, S.T.", classroom = "XII RPL 1", day = "Selasa", timeStart = "08:00", timeEnd = "10:00", room = "Lab Database Beta"),
            Schedule(subjectName = "Produk Kreatif Kewirausahaan (PKK)", teacherName = "Hadi Wijaya, M.Pd.", classroom = "XII RPL 1", day = "Rabu", timeStart = "07:30", timeEnd = "11:00", room = "Ruang Teori 3"),
            Schedule(subjectName = "Matematika Matematika Terapan", teacherName = "Dewi Fortuna, M.Si", classroom = "XII RPL 1", day = "Kamis", timeStart = "08:00", timeEnd = "09:30", room = "Ruang Teori 3"),
            Schedule(subjectName = "Pendidikan Jasmani & Olahraga", teacherName = "Supardi, S.Pd", classroom = "XII RPL 1", day = "Jumat", timeStart = "07:00", timeEnd = "09:00", room = "Lapangan Utama")
        )
        dao.insertSchedules(mockSchedules)

        // 3. Kurikulum Merdeka Capaian Pembelajaran (CP)
        val mockCPs = listOf(
            CapaianPembelajaran(subjectName = "Pemrograman Web & Bergerak (PWPB)", phase = "Fase F", code = "CP-PW-1", description = "Siswa mampu memahami, mendesain, dan membangun REST API backend berbasis Node.js/Kotlin serta menyajikannya ke platform web secara dinamis.", classTarget = "XII", completionPercentage = 85),
            CapaianPembelajaran(subjectName = "Pemrograman Berorientasi Objek (PBO)", phase = "Fase F", code = "CP-PB-2", description = "Siswa menguasai konsep OOP tingkat lanjut (Inheritance, Polymorphism, Abstraction, Interface) serta pola desain MVVM terstruktur di Jetpack Compose.", classTarget = "XII", completionPercentage = 75),
            CapaianPembelajaran(subjectName = "Basis Data (SQL & Cloud Database)", phase = "Fase F", code = "CP-BD-3", description = "Siswa mahir merancang skema datastore relasional (3NF), mengoptimalkan query JOIN Kompleks, serta integrasi Cloud SQL atau Firebase Room di perangkat mobile.", classTarget = "XII", completionPercentage = 90),
            CapaianPembelajaran(subjectName = "Produk Kreatif Kewirausahaan (PKK)", phase = "Fase F", code = "CP-PK-4", description = "Siswa memahami kelayakan industri software, metodologi Scrum Agile, estimasi budget pengembangan aplikasi, serta pemasaran produk digital.", classTarget = "XII", completionPercentage = 60)
        )
        dao.insertCPs(mockCPs)

        // 4. Academic Assessments Grades (Penilaian Harian) of student "siswa_agus"
        val mockAssessments = listOf(
            Assessment(studentId = "siswa_agus", studentName = "Agus Saputra", subjectName = "Pemrograman Web & Bergerak (PWPB)", category = "TUGAS", score = 92, date = "2026-06-01", feedback = "Sangat bagus, penataan folder MVC sangat rapi"),
            Assessment(studentId = "siswa_agus", studentName = "Agus Saputra", subjectName = "Pemrograman Web & Bergerak (PWPB)", category = "UH", score = 88, date = "2026-06-05", feedback = "Memahami HTTP Methods dengan baik"),
            Assessment(studentId = "siswa_agus", studentName = "Agus Saputra", subjectName = "Pemrograman Berorientasi Objek (PBO)", category = "TUGAS", score = 78, date = "2026-06-03", feedback = "Perlu melatih lagi penerapan Polymorphism"),
            Assessment(studentId = "siswa_agus", studentName = "Agus Saputra", subjectName = "Pemrograman Berorientasi Objek (PBO)", category = "UH", score = 84, date = "2026-06-08", feedback = "Analisis diagram class UML cukup komprehensif"),
            Assessment(studentId = "siswa_agus", studentName = "Agus Saputra", subjectName = "Basis Data (SQL & Cloud Database)", category = "UH", score = 95, date = "2026-06-04", feedback = "Sempurna! Sangat mahir merancang ERD dan query optimasi"),
            // Rizky Grades for comparisons
            Assessment(studentId = "siswa_rizky", studentName = "Rizky Pratama", subjectName = "Pemrograman Web & Bergerak (PWPB)", category = "TUGAS", score = 80, date = "2026-06-01", feedback = "Cukup baik, pertahankan konsistensi kode")
        )
        dao.insertAssessments(mockAssessments)

        // 5. Attendance log history
        val mockAttendanceList = listOf(
            Attendance(studentId = "siswa_agus", studentName = "Agus Saputra", classroom = "XII RPL 1", date = "2026-06-08", subjectName = "Pemrograman Web & Bergerak (PWPB)", status = "HADIR", faceScanSimilarity = 98.4f, wasFaceVerified = true),
            Attendance(studentId = "siswa_agus", studentName = "Agus Saputra", classroom = "XII RPL 1", date = "2026-06-09", subjectName = "Basis Data (SQL & Cloud Database)", status = "HADIR", faceScanSimilarity = 97.9f, wasFaceVerified = true),
            Attendance(studentId = "siswa_agus", studentName = "Agus Saputra", classroom = "XII RPL 1", date = "2026-06-10", subjectName = "Produk Kreatif Kewirausahaan (PKK)", status = "IZIN", notes = "Mengikuti turnamen e-Sport sekolah"),
            Attendance(studentId = "siswa_rizky", studentName = "Rizky Pratama", classroom = "XII RPL 1", date = "2026-06-10", subjectName = "Produk Kreatif Kewirausahaan (PKK)", status = "HADIR", faceScanSimilarity = 95.8f, wasFaceVerified = true)
        )
        dao.insertAttendances(mockAttendanceList)

        // 6. LMS Course Materials & Exercises
        val mockLms = listOf(
            LmsMaterial(
                subjectName = "Pemrograman Web & Bergerak (PWPB)",
                title = "Pengenalan REST API dengan Node.js dan Express",
                type = "MATERI",
                content = "Halo Siswa XII RPL 1. Pada materi kali ini, kita mempelajari bagaimana membangun arsitektur Web Service API yang bersih. Silakan pelajari HTTP status codes (200 OK, 201 Created, 400 Bad Request, 401 Unauthorized, 404 Not Found, serta 500 Internal Server Error) dan buat sketsa REST API sederhana untuk mengelola resource data barang di SMK kita."
            ),
            LmsMaterial(
                subjectName = "Pemrograman Berorientasi Objek (PBO)",
                title = "Tugas: Implementasi Pola Singleton & abstract class",
                type = "TUGAS",
                content = "TUGAS INDIVIDU:\n\n1. Buatlah class diagram dan code Java/Kotlin yang mengimplementasikan class Abstract DatabaseConnection.\n2. Buat sub-class MySQLConnection dan PostgreSQLConnection.\n3. Implementasikan pola Singleton pada koneksi database ini agar hanya ada 1 instance aktif.\n4. Kumpulkan ringkasan kode dalam format teks langsung disini.",
                dueDate = "2026-06-18",
                isSubmitted = false
            ),
            LmsMaterial(
                subjectName = "Basis Data (SQL & Cloud Database)",
                title = "Latihan Mandiri: Trigger SQL & optimasi indexing",
                type = "TUGAS",
                content = "Selesaikan kueri pembuatan Trigger otomatis yang mencatat riwayat perubahan (audit log) setiap kali tabel 'SISWA' mengalami edit data UPDATE. Kumpulkan syntax SQL lengkap Anda.",
                dueDate = "2026-06-15",
                isSubmitted = true,
                submissionText = "CREATE TRIGGER audit_siswa BEFORE UPDATE ON siswa FOR EACH ROW INSERT INTO audit_log(nisn, action, date) VALUES (OLD.nisn, 'UPDATE', NOW());",
                score = 94,
                teacherFeedback = "Jawaban kueri tepat sasaran dan hemat memori."
            )
        )
        dao.insertLmsMaterials(mockLms)

        // Default set
        _currentUser.value = mockProfiles.first()
    }
}
