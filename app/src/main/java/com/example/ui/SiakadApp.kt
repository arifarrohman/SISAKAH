package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.viewmodel.SiakadViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SiakadApp(viewModel: SiakadViewModel) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val allProfiles by viewModel.allProfiles.collectAsStateWithLifecycle()
    val allSchedules by viewModel.allSchedules.collectAsStateWithLifecycle()
    val allCP by viewModel.allCP.collectAsStateWithLifecycle()
    val allAttendance by viewModel.allAttendance.collectAsStateWithLifecycle()
    val allLmsMaterials by viewModel.allLmsMaterials.collectAsStateWithLifecycle()
    val allAssessments by viewModel.allAssessments.collectAsStateWithLifecycle()
    val isInitializing by viewModel.isInitializing.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Navigation sub-tabs (local state)
    var activeTab by remember(currentUser?.role) { mutableStateOf(0) }

    if (isInitializing) {
        Box(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Memuat Sistem Akademik SIAKAD SMK...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    } else {
        Scaffold(
            topBar = {
                Column {
                    // Modern App Banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.secondary
                                    )
                                )
                            )
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 20.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Home,
                                    contentDescription = "School Logo",
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "SIAKAD SMK NEGERI",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 1.2.sp
                                        ),
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Platform Akademik Kurikulum Merdeka Terintegrasi",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                }
                            }
                        }
                    }

                    // Role Switcher Panel
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                            .padding(8.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "PILIH PORTAL AKSES (Mode Demostrasi)",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                allProfiles.forEach { profile ->
                                    val isSelected = currentUser?.id == profile.id
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.switchUser(profile.id) },
                                        label = {
                                            Text(
                                                text = when (profile.role) {
                                                    "GURU" -> "👨‍🏫 Guru: " + profile.name.substringBefore(",")
                                                    "SISWA" -> "👨‍🎓 Siswa: " + profile.name.substringBefore(" ")
                                                    "ORANG_TUA" -> "👪 Wali: " + profile.name.substringBefore(" ")
                                                    else -> profile.name
                                                }
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        ),
                                        modifier = Modifier.testTag("portal_chip_${profile.id}")
                                    )
                                }
                            }
                        }
                    }
                }
            },
            bottomBar = {
                currentUser?.let { user ->
                    NavigationBar(
                        windowInsets = WindowInsets.navigationBars,
                        modifier = Modifier.testTag("app_navigation_bar")
                    ) {
                        when (user.role) {
                            "GURU" -> {
                                NavigationBarItem(
                                    selected = activeTab == 0,
                                    onClick = { activeTab = 0 },
                                    icon = { Icon(Icons.Default.DateRange, "Jadwal") },
                                    label = { Text("Jadwal & CP") }
                                )
                                NavigationBarItem(
                                    selected = activeTab == 1,
                                    onClick = { activeTab = 1 },
                                    icon = { Icon(Icons.Default.Face, "Absensi") },
                                    label = { Text("Face Absensi") }
                                )
                                NavigationBarItem(
                                    selected = activeTab == 2,
                                    onClick = { activeTab = 2 },
                                    icon = { Icon(Icons.Default.Edit, "Nilai") },
                                    label = { Text("LMS & Penilaian") }
                                )
                            }
                            "SISWA" -> {
                                NavigationBarItem(
                                    selected = activeTab == 0,
                                    onClick = { activeTab = 0 },
                                    icon = { Icon(Icons.Default.DateRange, "Jadwal") },
                                    label = { Text("Jadwal") }
                                )
                                NavigationBarItem(
                                    selected = activeTab == 1,
                                    onClick = { activeTab = 1 },
                                    icon = { Icon(Icons.Default.Face, "Attendance") },
                                    label = { Text("Absensi Face") }
                                )
                                NavigationBarItem(
                                    selected = activeTab == 2,
                                    onClick = { activeTab = 2 },
                                    icon = { Icon(Icons.Default.List, "LMS") },
                                    label = { Text("LMS & Raport") }
                                )
                            }
                            "ORANG_TUA" -> {
                                NavigationBarItem(
                                    selected = activeTab == 0,
                                    onClick = { activeTab = 0 },
                                    icon = { Icon(Icons.Default.DateRange, "Schedule") },
                                    label = { Text("Jadwal & Hadir") }
                                )
                                NavigationBarItem(
                                    selected = activeTab == 1,
                                    onClick = { activeTab = 1 },
                                    icon = { Icon(Icons.Default.Star, "Report") },
                                    label = { Text("Raport Anak") }
                                )
                                NavigationBarItem(
                                    selected = activeTab == 2,
                                    onClick = { activeTab = 2 },
                                    icon = { Icon(Icons.Default.Notifications, "AI Advisor") },
                                    label = { Text("Konseling AI") }
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                currentUser?.let { user ->
                    AnimatedContent(
                        targetState = Pair(user.role, activeTab),
                        transitionSpec = {
                            fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                        },
                        label = "MainScreenTransition"
                    ) { (role, tab) ->
                        when (role) {
                            "GURU" -> TeacherPortal(tab, viewModel, allSchedules, allCP, allAttendance, allAssessments, allProfiles, allLmsMaterials)
                            "SISWA" -> StudentPortal(tab, viewModel, user, allSchedules, allCP, allAttendance, allLmsMaterials, allAssessments)
                            "ORANG_TUA" -> ParentPortal(tab, viewModel, user, allProfiles, allSchedules, allAttendance, allAssessments, allCP)
                        }
                    }
                } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Pilih akun portal terlebih dahulu.")
                }
            }
        }
    }
}

// ==========================================
// 1. TEACHER PORTAL SUB-SCREENS
// ==========================================
@Composable
fun TeacherPortal(
    tabIndex: Int,
    viewModel: SiakadViewModel,
    schedules: List<Schedule>,
    cps: List<CapaianPembelajaran>,
    attendanceLogs: List<Attendance>,
    assessments: List<Assessment>,
    allProfiles: List<UserProfile>,
    lmsMaterials: List<LmsMaterial>
) {
    when (tabIndex) {
        0 -> TeacherSchedulesAndCP(viewModel, schedules, cps)
        1 -> TeacherAttendanceManager(viewModel, attendanceLogs, allProfiles, schedules)
        2 -> TeacherLmsAndGrading(viewModel, allProfiles, assessments, lmsMaterials)
    }
}

@Composable
fun TeacherSchedulesAndCP(
    viewModel: SiakadViewModel,
    schedules: List<Schedule>,
    cps: List<CapaianPembelajaran>
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Mengajar Hari Ini & Capaian Kurikulum",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Kelola jadwal mengajar aktif Anda beserta target Capaian Pembelajaran (CP) Merdeka.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )
        }

        // Section: Schedules Lists
        item {
            Card {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DateRange, "Schedule", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Jadwal Mengajar Kejuruan",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    if (schedules.isEmpty()) {
                        Text("Belum ada jadwal terdaftar.", style = MaterialTheme.typography.bodySmall)
                    } else {
                        schedules.forEach { sched ->
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = sched.subjectName,
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "${sched.day} • ${sched.timeStart} - ${sched.timeEnd} • ${sched.room}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text(sched.classroom, style = MaterialTheme.typography.labelMedium) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section: Capaian Pembelajaran Merdeka (CP) Sliders
        item {
            Text(
                text = "Monitoring Capaian Pembelajaran (Kurikulum Merdeka)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        items(cps) { cp ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                    Text(
                                        text = cp.code,
                                        modifier = Modifier.padding(horizontal = 4.dp),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = cp.subjectName,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = cp.description, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Progres Capaian: ${cp.completionPercentage}%",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.width(130.dp)
                        )
                        Slider(
                            value = cp.completionPercentage.toFloat(),
                            onValueChange = { value -> viewModel.updateCPProgress(cp.id, value.toInt()) },
                            valueRange = 0f..100f,
                            modifier = Modifier.weight(1f).testTag("cp_slider_${cp.id}")
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TeacherAttendanceManager(
    viewModel: SiakadViewModel,
    attendanceLogs: List<Attendance>,
    allProfiles: List<UserProfile>,
    schedules: List<Schedule>
) {
    val students = allProfiles.filter { it.role == "SISWA" }
    var selectedStudentId by remember { mutableStateOf("") }
    var selectedSubject by remember { mutableStateOf("") }
    var attendanceStatus by remember { mutableStateOf("HADIR") }
    var notesInput by remember { mutableStateOf("") }

    // Dropdown toggle states
    var isStudentDropdownExpanded by remember { mutableStateOf(false) }
    var isSubjectDropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(students, schedules) {
        if (students.isNotEmpty() && selectedStudentId.isEmpty()) {
            selectedStudentId = students.first().id
        }
        if (schedules.isNotEmpty() && selectedSubject.isEmpty()) {
            selectedSubject = schedules.first().subjectName
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Kelola Kehadiran Siswa",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "Input absensi harian manual atau tinjau verifikasi absensi face recognition otomatis.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }

        // Section: Manual Input Form
        item {
            Card(
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Add, "Input Absensi", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Input Absensi Manual",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 1. Selector Siswa
                    Text("Pilih Siswa:", style = MaterialTheme.typography.labelMedium)
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        val currentStudentName = students.find { it.id == selectedStudentId }?.name ?: "Pilih Siswa"
                        OutlinedButton(
                            onClick = { isStudentDropdownExpanded = true },
                            modifier = Modifier.fillMaxWidth().testTag("select_student_dropdown")
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(currentStudentName)
                                Text("▼")
                            }
                        }
                        DropdownMenu(
                            expanded = isStudentDropdownExpanded,
                            onDismissRequest = { isStudentDropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            students.forEach { std ->
                                DropdownMenuItem(
                                    text = { Text("${std.name} (${std.classroom})") },
                                    onClick = {
                                        selectedStudentId = std.id
                                        isStudentDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 2. Selector Mata Pelajaran
                    Text("Pilih Mata Pelajaran:", style = MaterialTheme.typography.labelMedium)
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        val currentSubName = if (selectedSubject.isNotEmpty()) selectedSubject else "Pilih Pelajaran"
                        OutlinedButton(
                            onClick = { isSubjectDropdownExpanded = true },
                            modifier = Modifier.fillMaxWidth().testTag("select_subject_dropdown")
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(currentSubName)
                                Text("▼")
                            }
                        }
                        DropdownMenu(
                            expanded = isSubjectDropdownExpanded,
                            onDismissRequest = { isSubjectDropdownExpanded = false }
                        ) {
                            schedules.map { it.subjectName }.distinct().forEach { sub ->
                                DropdownMenuItem(
                                    text = { Text(sub) },
                                    onClick = {
                                        selectedSubject = sub
                                        isSubjectDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 3. Status Checkbox row
                    Text("Status Kehadiran:", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("HADIR", "SAKIT", "IZIN", "ALPHA").forEach { stat ->
                            val isSelected = attendanceStatus == stat
                            ElevatedFilterChip(
                                selected = isSelected,
                                onClick = { attendanceStatus = stat },
                                label = { Text(stat) },
                                modifier = Modifier.weight(1f).testTag("status_chip_$stat")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 4. Notes input
                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        label = { Text("Catatan Tambahan (Keterangan)") },
                        modifier = Modifier.fillMaxWidth().testTag("notes_attendance_input"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val student = students.find { it.id == selectedStudentId }
                            if (student != null) {
                                viewModel.submitFaceAttendance(
                                    studentId = student.id,
                                    studentName = student.name,
                                    classroom = student.classroom,
                                    subjectName = selectedSubject,
                                    status = attendanceStatus,
                                    faceScore = if (attendanceStatus == "HADIR") 99.8f else 0f,
                                    wasVerified = attendanceStatus == "HADIR",
                                    notes = notesInput.ifEmpty { null }
                                )
                                notesInput = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("submit_manual_attendance_button")
                    ) {
                        Text("Simpan Rekor Kehadiran")
                    }
                }
            }
        }

        // Section: Face Recognition Logs
        item {
            Text(
                text = "Log Presensi Digital (Face Recognition)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        if (attendanceLogs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Belum ada logs presensi untuk saat ini.", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            items(attendanceLogs) { log ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (log.status == "ALPHA") MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = log.studentName,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "${log.subjectName} • ${log.date}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (log.wasFaceVerified) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Face Verified",
                                        tint = Color(0xFF2E7D32),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Verifikasi Wajah: ${log.faceScanSimilarity}% Cocok",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color(0xFF2E7D32),
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                            if (!log.notes.isNullOrEmpty()) {
                                Text(
                                    text = "Ket: ${log.notes}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }

                        // Badge representation
                        Badge(
                            containerColor = when (log.status) {
                                "HADIR" -> Color(0xFF2E7D32)
                                "SAKIT" -> Color(0xFFE65100)
                                "IZIN" -> Color(0xFF1565C0)
                                else -> MaterialTheme.colorScheme.error
                            },
                        ) {
                            Text(
                                text = log.status,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TeacherLmsAndGrading(
    viewModel: SiakadViewModel,
    allProfiles: List<UserProfile>,
    allAssessments: List<Assessment>,
    lmsMaterials: List<LmsMaterial>
) {
    val students = allProfiles.filter { it.role == "SISWA" }
    var selectedStudentId by remember { mutableStateOf("") }
    var selectedSubject by remember { mutableStateOf("") }
    var assessmentCategory by remember { mutableStateOf("TUGAS") }
    var scoreInput by remember { mutableStateOf("") }
    var feedbackInput by remember { mutableStateOf("") }

    // Dropdowns
    var studentExpanded by remember { mutableStateOf(false) }
    var subjectExpanded by remember { mutableStateOf(false) }

    // Distinct subjects
    val subjectsList = listOf(
        "Pemrograman Web & Bergerak (PWPB)",
        "Pemrograman Berorientasi Objek (PBO)",
        "Basis Data (SQL & Cloud Database)",
        "Produk Kreatif Kewirausahaan (PKK)"
    )

    LaunchedEffect(students) {
        if (students.isNotEmpty() && selectedStudentId.isEmpty()) {
            selectedStudentId = students.first().id
        }
        if (selectedSubject.isEmpty()) {
            selectedSubject = subjectsList.first()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. Header ---
        item {
            Text(
                text = "Penilaian & LMS Sesi",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "Input Penilaian Harian (UH, UTS, UAS) atau kelola tugas LMS digital.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }

        // --- 2. Grade Input Form ---
        item {
            Card(
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Add, "Add Score", tint = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Penilaian Harian Baru",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Student Selector
                    Text("Pilih Siswa:", style = MaterialTheme.typography.labelMedium)
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        val currentStudName = students.find { it.id == selectedStudentId }?.name ?: "Pilih Siswa"
                        OutlinedButton(
                            onClick = { studentExpanded = true },
                            modifier = Modifier.fillMaxWidth().testTag("grading_student_spinner")
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(currentStudName)
                                Text("▼")
                            }
                        }
                        DropdownMenu(
                            expanded = studentExpanded,
                            onDismissRequest = { studentExpanded = false }
                        ) {
                            students.forEach { std ->
                                DropdownMenuItem(
                                    text = { Text("${std.name} (${std.classroom})") },
                                    onClick = {
                                        selectedStudentId = std.id
                                        studentExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Subject Selector
                    Text("Pilih Mata Pelajaran:", style = MaterialTheme.typography.labelMedium)
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        val currentSub = if (selectedSubject.isNotEmpty()) selectedSubject else "Pilih Pelajaran"
                        OutlinedButton(
                            onClick = { subjectExpanded = true },
                            modifier = Modifier.fillMaxWidth().testTag("grading_subject_spinner")
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(currentSub)
                                Text("▼")
                            }
                        }
                        DropdownMenu(
                            expanded = subjectExpanded,
                            onDismissRequest = { subjectExpanded = false }
                        ) {
                            subjectsList.forEach { s ->
                                DropdownMenuItem(
                                    text = { Text(s) },
                                    onClick = {
                                        selectedSubject = s
                                        subjectExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Exam Category Chips
                    Text("Kategori Nilai:", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("TUGAS", "KUIS", "UH", "UTS", "UAS").forEach { cat ->
                            val isSelected = assessmentCategory == cat
                            FilterChip(
                                selected = isSelected,
                                onClick = { assessmentCategory = cat },
                                label = { Text(cat, fontSize = 11.sp) },
                                modifier = Modifier.weight(1f).testTag("category_chip_$cat")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Score & Feedback Inputs
                    OutlinedTextField(
                        value = scoreInput,
                        onValueChange = { scoreInput = it },
                        label = { Text("Skor Nilai (0-100)") },
                        modifier = Modifier.fillMaxWidth().testTag("score_entry_field"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = feedbackInput,
                        onValueChange = { feedbackInput = it },
                        label = { Text("Feedback Kualitatif Guru") },
                        modifier = Modifier.fillMaxWidth().testTag("feedback_entry_field")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val scoreValue = scoreInput.toIntOrNull() ?: 0
                            val studentMatched = students.find { it.id == selectedStudentId }
                            if (studentMatched != null && scoreInput.isNotEmpty()) {
                                viewModel.addAssessment(
                                    studentId = studentMatched.id,
                                    studentName = studentMatched.name,
                                    subjectName = selectedSubject,
                                    category = assessmentCategory,
                                    score = scoreValue,
                                    feedback = feedbackInput.ifEmpty { null }
                                )
                                scoreInput = ""
                                feedbackInput = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("btn_save_grade")
                    ) {
                        Text("Simpan Hasil Penilaian")
                    }
                }
            }
        }

        // --- 3. LMS Homework Tasks Lists for grading ---
        item {
            Text(
                text = "Tinjau Pengumpulan Tugas LMS",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        // Only show assignment types
        val assignList = lmsMaterials.filter { it.type == "TUGAS" }
        if (assignList.isEmpty()) {
            item {
                Text("Tidak ada penugasan LMS.")
            }
        } else {
            items(assignList) { task ->
                var lmsFeedback by remember { mutableStateOf("") }
                var lmsGrade by remember { mutableStateOf("") }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(task.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            Badge { Text("TUGAS") }
                        }
                        Text(task.subjectName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Rincian Tugas: ${task.content}", style = MaterialTheme.typography.bodySmall)

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        if (task.isSubmitted) {
                            Column {
                                Box(
                                    modifier = Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.6f)).padding(8.dp).clip(RoundedCornerShape(4.dp))
                                ) {
                                    Column {
                                        Text("Pengumpulan Siswa (Agus Saputra):", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        Text(task.submissionText ?: "", style = MaterialTheme.typography.bodySmall)
                                    }
                                }

                                if (task.score != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Check, "Graded", tint = Color(0xFF2E7D32))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Sudah Dinilai: ${task.score}/100 • Feedback: ${task.teacherFeedback}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                        )
                                    }
                                } else {
                                    // Grading fields
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Beri Nilai:", style = MaterialTheme.typography.labelSmall)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        OutlinedTextField(
                                            value = lmsGrade,
                                            onValueChange = { lmsGrade = it },
                                            placeholder = { Text("Skor") },
                                            modifier = Modifier.width(80.dp).testTag("lms_score_input_${task.id}"),
                                            singleLine = true
                                        )
                                        OutlinedTextField(
                                            value = lmsFeedback,
                                            onValueChange = { lmsFeedback = it },
                                            placeholder = { Text("Komentar Guru") },
                                            modifier = Modifier.weight(1f).testTag("lms_comment_input_${task.id}"),
                                            singleLine = true
                                        )
                                        IconButton(
                                            onClick = {
                                                val gr = lmsGrade.toIntOrNull()
                                                if (gr != null) {
                                                    viewModel.gradeLmsAssignment(task.id, gr, lmsFeedback)
                                                }
                                            }
                                        ) {
                                            Icon(Icons.Default.Send, "Submit Grade", tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                            }
                        } else {
                            Text("Status: Belum ada pengumpulan dari kelas ini.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}


// ==========================================
// 2. STUDENT PORTAL SUB-SCREENS
// ==========================================
@Composable
fun StudentPortal(
    tabIndex: Int,
    viewModel: SiakadViewModel,
    profile: UserProfile,
    schedules: List<Schedule>,
    cps: List<CapaianPembelajaran>,
    attendanceLogs: List<Attendance>,
    lmsMaterials: List<LmsMaterial>,
    assessments: List<Assessment>
) {
    when (tabIndex) {
        0 -> StudentSchedulesAndCP(profile, schedules, cps)
        1 -> StudentFaceRecognitionScan(viewModel, profile, schedules)
        2 -> StudentLmsAndReport(viewModel, profile, lmsMaterials, assessments, attendanceLogs)
    }
}

@Composable
fun StudentSchedulesAndCP(
    profile: UserProfile,
    schedules: List<Schedule>,
    cps: List<CapaianPembelajaran>
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Dashboard Siswa: " + profile.name,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "Kelas: ${profile.classroom} • ${profile.idNumber}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Section: Schedules Lists
        item {
            Card {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DateRange, "Schedule", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Jadwal Pelajaran Saya",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    val filtered = schedules.filter { it.classroom == profile.classroom }
                    if (filtered.isEmpty()) {
                        Text("Tidak ada jadwal pelajaran terdaftar untuk kelas Anda.")
                    } else {
                        filtered.forEach { sched ->
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text(sched.subjectName, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                                    Text(
                                        text = "${sched.day} • ${sched.timeStart} - ${sched.timeEnd}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Guru: ${sched.teacherName} • ${sched.room}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section: Capaian Pembelajaran Progress Bar (Monitoring Target)
        item {
            Text(
                text = "Target Capaian Pembelajaran Mandiri",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        items(cps) { cp ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Badge { Text(cp.code) }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(cp.subjectName, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = cp.description, style = MaterialTheme.typography.bodyMedium)
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Kelompok Belajar Kelas Progres: ${cp.completionPercentage}%",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { cp.completionPercentage.toFloat() / 100f },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                    )
                }
            }
        }
    }
}

@Composable
fun StudentFaceRecognitionScan(
    viewModel: SiakadViewModel,
    profile: UserProfile,
    schedules: List<Schedule>
) {
    var isScanning by remember { mutableStateOf(false) }
    var scanStatus by remember { mutableStateOf("TAP TOMBOL DI BAWAH") }
    var scanResultText by remember { mutableStateOf<String?>(null) }
    var matchingSubject by remember { mutableStateOf("") }
    var scaleProgress by remember { mutableStateOf(0f) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(schedules) {
        if (schedules.isNotEmpty()) {
            matchingSubject = schedules.first().subjectName
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Presensi Face Recognition",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center
        )
        Text(
            text = "Verifikasi kehadiran Anda menggunakan teknologi scan wajah biometrik terenkripsi di kelas.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Futuristic Scanning Area Viewport
        Box(
            modifier = Modifier
                .size(260.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.Black)
                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (isScanning) {
                // Interactive Scanner Animation lines
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val lineY = scaleProgress * size.height
                    drawLine(
                        color = Color(0xFF00E676),
                        start = androidx.compose.ui.geometry.Offset(0f, lineY),
                        end = androidx.compose.ui.geometry.Offset(size.width, lineY),
                        strokeWidth = 6f
                    )
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Face,
                        contentDescription = "Scanning",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Memetakan Landmark Wajah...",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${(scaleProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = Color(0xFF00E676)
                    )
                }
            } else {
                if (scanResultText != null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Verify Match",
                            tint = Color(0xFF00E676),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Cocok! (98.7%)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF00E676)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = scanResultText ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Face,
                            contentDescription = "Placeholder camera",
                            tint = Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.size(80.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Kamera Siap",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Selection of active subject lesson in progress
        Text("Pilih Pelajaran Pelaksanaan Absen:", style = MaterialTheme.typography.labelMedium)
        var lessonDropdownExpanded by remember { mutableStateOf(false) }
        Box {
            OutlinedButton(
                onClick = { lessonDropdownExpanded = true },
                modifier = Modifier.fillMaxWidth().testTag("student_attendance_subject_btn")
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(if (matchingSubject.isNotEmpty()) matchingSubject else "Pilih Pelajaran")
                    Text("▼")
                }
            }
            DropdownMenu(
                expanded = lessonDropdownExpanded,
                onDismissRequest = { lessonDropdownExpanded = false }
            ) {
                schedules.map { it.subjectName }.distinct().forEach { sub ->
                    DropdownMenuItem(
                        text = { Text(sub) },
                        onClick = {
                            matchingSubject = sub
                            lessonDropdownExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                if (!isScanning) {
                    scope.launch {
                        isScanning = true
                        scanStatus = "MEMINDAI WAJAH..."
                        scanResultText = null
                        scaleProgress = 0f
                        
                        // Live simulation of scan progress lines
                        while (scaleProgress < 1f) {
                            delay(50)
                            scaleProgress += 0.04f
                        }
                        
                        isScanning = false
                        scanResultText = "Wajah Terverifikasi: ${profile.name} (${profile.classroom})\nStatus Kehadiran: HADIR"
                        scanStatus = "PRESENSI SELESAI"
                        
                        // Insert database attendance
                        viewModel.submitFaceAttendance(
                            studentId = profile.id,
                            studentName = profile.name,
                            classroom = profile.classroom,
                            subjectName = matchingSubject,
                            status = "HADIR",
                            faceScore = 98.7f,
                            wasVerified = true,
                            notes = "Presensi Digital Mandiri via Face Scanner"
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(0.8f).height(50.dp).testTag("trigger_face_scan_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isScanning) Color.Red else MaterialTheme.colorScheme.primary
            ),
            enabled = !isScanning
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Face, "Verify Sensor")
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isScanning) "Sedang Pemindaian..." else "Mulai Scan Wajah Biometrik")
            }
        }
    }
}

@Composable
fun StudentLmsAndReport(
    viewModel: SiakadViewModel,
    profile: UserProfile,
    lmsMaterials: List<LmsMaterial>,
    assessments: List<Assessment>,
    attendanceLogs: List<Attendance>
) {
    var lmsFormToggle by remember { mutableStateOf(false) }
    var selectedLmsId by remember { mutableStateOf(-1) }
    var submissionText by remember { mutableStateOf("") }

    val myGrades = assessments.filter { it.studentId == profile.id }
    val myAttendance = attendanceLogs.filter { it.studentId == profile.id }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // LMS MATERIALS SECTION
        item {
            Text(
                text = "LMS (Learning Management System)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        items(lmsMaterials) { mat ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = mat.subjectName,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Badge(
                            containerColor = if (mat.type == "TUGAS") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                        ) {
                            Text(
                                text = mat.type,
                                modifier = Modifier.padding(horizontal = 4.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = mat.title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = mat.content, style = MaterialTheme.typography.bodySmall)

                    if (mat.type == "TUGAS") {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                        if (mat.isSubmitted) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Check, "Checked", tint = Color(0xFF2E7D32))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Sudah Dikumpulkan",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Jawaban Anda: \"${mat.submissionText}\"", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            
                            if (mat.score != null) {
                                Text(
                                    text = "Nilai Tugas: ${mat.score}/100 • Evaluasi Guru: ${mat.teacherFeedback}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32)),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            } else {
                                Text("Hasil: Sedang Menunggu Koreksi Guru", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            }
                        } else {
                            if (selectedLmsId == mat.id) {
                                OutlinedTextField(
                                    value = submissionText,
                                    onValueChange = { submissionText = it },
                                    label = { Text("Tulis Jawaban Tugas Anda") },
                                    modifier = Modifier.fillMaxWidth().testTag("text_lms_submit_${mat.id}")
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            if (submissionText.isNotEmpty()) {
                                                viewModel.submitLmsAssignment(mat.id, submissionText)
                                                submissionText = ""
                                                selectedLmsId = -1
                                            }
                                        },
                                        modifier = Modifier.testTag("btn_submit_done_${mat.id}")
                                    ) {
                                        Text("Submit")
                                    }
                                    TextButton(onClick = { selectedLmsId = -1 }) {
                                        Text("Batal")
                                    }
                                }
                            } else {
                                Button(
                                    onClick = { selectedLmsId = mat.id },
                                    modifier = Modifier.testTag("btn_start_submit_${mat.id}")
                                ) {
                                    Text("Kerjakan Tugas")
                                }
                            }
                        }
                    }
                }
            }
        }

        // PHYSICAL GRADES REPORT CARD VIEW
        item {
            Text(
                text = "Dokumen Penilaian Harian (Raport Digital)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        if (myGrades.isEmpty()) {
            item {
                Text("Belum ada rekor nilai terdaftar.")
            }
        } else {
            items(myGrades) { g ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    ListItem(
                        headlineContent = { Text(g.subjectName, fontWeight = FontWeight.Bold) },
                        supportingContent = {
                            Column {
                                Text("Ujian: ${g.category} • Tanggal: ${g.date}", style = MaterialTheme.typography.bodySmall)
                                if (!g.feedback.isNullOrEmpty()) {
                                    Text("Feedback Guru: \"${g.feedback}\"", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        },
                        trailingContent = {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (g.score >= 80) Color(0xFF2E7D32).copy(alpha = 0.15f)
                                        else Color(0xFFC62828).copy(alpha = 0.15f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = g.score.toString(),
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (g.score >= 80) Color(0xFF2E7D32) else Color(0xFFC62828),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}


// ==========================================
// 3. PARENT PORTAL SUB-SCREENS
// ==========================================
@Composable
fun ParentPortal(
    tabIndex: Int,
    viewModel: SiakadViewModel,
    profile: UserProfile,
    allProfiles: List<UserProfile>,
    schedules: List<Schedule>,
    attendanceLogs: List<Attendance>,
    assessments: List<Assessment>,
    cps: List<CapaianPembelajaran>
) {
    val childId = profile.parentOf ?: ""
    val childName = allProfiles.find { it.id == childId }?.name ?: "Agus Saputra"
    val myChildAttendance = attendanceLogs.filter { it.studentId == childId }
    val myChildGrades = assessments.filter { it.studentId == childId }

    when (tabIndex) {
        0 -> ParentAttendanceTab(childName, schedules, myChildAttendance)
        1 -> ParentGradesTab(childName, myChildGrades)
        2 -> ParentAiCounselingTab(viewModel, childId, childName, myChildGrades, myChildAttendance, cps)
    }
}

@Composable
fun ParentAttendanceTab(
    childName: String,
    schedules: List<Schedule>,
    attendanceLogs: List<Attendance>
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Portal Orang Tua: Monitoring $childName",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "Tinjau real-time kehadiran anak Anda harian langsung dari sensor face recognition di gawai.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }

        // Section: Live Attendance Summary
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    val total = attendanceLogs.size
                    val hadir = attendanceLogs.count { it.status == "HADIR" }
                    val izin = attendanceLogs.count { it.status == "IZIN" }
                    val alpha = attendanceLogs.count { it.status == "ALPHA" }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Kehadiran", style = MaterialTheme.typography.labelSmall)
                        Text(text = "$hadir/$total", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Izin/Sakit", style = MaterialTheme.typography.labelSmall)
                        Text(text = "$izin", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Ketidakhadiran (Alpha)", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = "$alpha", 
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (alpha > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "Riwayat Absensi Kehadiran Terakhir",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        if (attendanceLogs.isEmpty()) {
            item {
                Text("Anak Anda belum memiliki log presensi.")
            }
        } else {
            items(attendanceLogs) { log ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ListItem(
                        headlineContent = { Text(log.subjectName, fontWeight = FontWeight.Bold) },
                        supportingContent = {
                            Column {
                                Text(text = "Tanggal: ${log.date} ${if (log.wasFaceVerified) "• Terverifikasi Wajah" else ""}", style = MaterialTheme.typography.bodySmall)
                                if (!log.notes.isNullOrEmpty()) {
                                    Text(text = "Keterangan: ${log.notes}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        },
                        trailingContent = {
                            Badge(
                                containerColor = when (log.status) {
                                    "HADIR" -> Color(0xFF2E7D32)
                                    "SAKIT" -> Color(0xFFE65100)
                                    "IZIN" -> Color(0xFF1565C0)
                                    else -> MaterialTheme.colorScheme.error
                                }
                            ) {
                                Text(
                                    text = log.status,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ParentGradesTab(
    childName: String,
    gradesList: List<Assessment>
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Laporan Penilaian Harian",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "Monitoring nilai ulasan tugas, kuis, ulangan harian, UTS, dan UAS anak Anda secara langsung.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }

        if (gradesList.isEmpty()) {
            item {
                Text("Belum ada rekor nilai harian untuk anak Anda.")
            }
        } else {
            items(gradesList) { g ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (g.score >= 80) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.1f)
                        else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (g.score >= 80) Color(0xFF2E7D32).copy(alpha = 0.15f)
                        else Color(0xFFC62828).copy(alpha = 0.15f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(g.subjectName, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                Text("Uji Pelajaran: ${g.category} • ${g.date}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                text = "Skor: ${g.score}/100",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (g.score >= 80) Color(0xFF2E7D32) else Color(0xFFC62828)
                                )
                            )
                        }

                        if (!g.feedback.isNullOrEmpty()) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(Icons.Default.Info, "Notes", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Evaluasi: ${g.feedback}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ParentAiCounselingTab(
    viewModel: SiakadViewModel,
    childId: String,
    childName: String,
    grades: List<Assessment>,
    attendance: List<Attendance>,
    cps: List<CapaianPembelajaran>
) {
    val isAnalyzing by viewModel.isAnalyzingAI.collectAsStateWithLifecycle()
    val aiInsight by viewModel.aiInsightState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // AI Integration Header Card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Notifications, "Gemini Icon", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Monitoring AI Portals Kurikulum Merdeka",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Sistem pelaporan nilai harian pintar yang terintegrasi dengan Konseling AI Gemini. Dapatkan laporan analisis perkembangan akademik, kepribadian belajar, serta rekomendasi terarah bagi orang tua di rumah.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Button(
            onClick = { viewModel.generateParentAcademicReport(childId) },
            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("trigger_gemini_analysis_parent_portal"),
            enabled = !isAnalyzing
        ) {
            if (isAnalyzing) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Gemini Sedang Menganalisis...")
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Refresh, "Analyze")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Buat Laporan Analisis Perkembangan AI")
                }
            }
        }

        if (aiInsight != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CATATAN PERKEMBANGAN & BIMBINGAN AI",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        Badge { Text("Active AI") }
                    }
                    Text(
                        text = aiInsight ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 22.sp
                    )
                }
            }
        } else {
            // Placeholder empty state
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Empty Report",
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Laporan bimbingan otomatis belum dibuat. Tekan tombol di atas untuk menganalisis dan mendownload log akademik digital otomatis menggunakan AI.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
