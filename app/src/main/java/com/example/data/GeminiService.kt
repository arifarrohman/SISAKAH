package com.example.data

import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

// --- Gemini REST API Data Classes (Moshi Compatible) ---

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val temperature: Float? = null,
    val topP: Float? = null,
    val topK: Int? = null,
    val maxOutputTokens: Int? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content? = null
)

// --- Retrofit Network Interface ---

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
        retrofit.create(GeminiApiService::class.java)
    }
}

// --- Gemini API Call Manager ---

object GeminiManager {
    
    suspend fun generateAcademicInsight(
        studentName: String,
        classroom: String,
        grades: List<Assessment>,
        attendance: List<Attendance>,
        capaianList: List<CapaianPembelajaran>
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Layanan Konseling AI Siakad sedang offline. Bapak/Ibu Wali Murid dapat menghubungi sekolah secara langsung atau meminta admin untuk mengaktifkan API Key pada panel AI Studio Secrets."
        }

        // Aggregate info
        val gradesSummary = grades.joinToString("\n") { 
            "- Mapel: ${it.subjectName}, Kategori: ${it.category}, Nilai: ${it.score}/${it.maxScore}, feedback: ${it.feedback ?: "-"}"
        }
        
        val totalDays = attendance.size
        val attendedDays = attendance.count { it.status == "HADIR" }
        val attendanceRate = if (totalDays > 0) (attendedDays.toFloat() / totalDays * 100).toInt() else 100
        
        val cpSummary = capaianList.joinToString("\n") {
            "- ${it.subjectName} (${it.code}): ${it.description} -> Progress ${it.completionPercentage}%"
        }

        val prompt = """
            Nama Siswa: $studentName
            Kelas: $classroom
            
            == Rincian Penilaian Harian ==
            $gradesSummary
            
            == Tingkat Kehadiran ==
            Tingkat kehadiran: $attendanceRate% ($attendedDays dari $totalDays sesi pelajaran)
            
            == Capaian Pembelajaran Kurikulum Merdeka ==
            $cpSummary
            
            Sebagai Wali Kelas/Sistem AI Siakad SMK, berikan analisis akademik otomatis singkat (maksimal 3 paragraf, dalam Bahasa Indonesia yang formal dan sopan) untuk dilaporkan ke Orang Tua siswa. 
            Isi analisis harus mencakup:
            1. Apresiasi atas kekuatan siswa berdasarkan nilai tertinggi atau kehadiran yang baik.
            2. Rekomendasi konstruktif untuk mata pelajaran atau Kompetensi Kejuruan (RPL) yang nilainya kurang atau progres CP-nya masih rendah.
            3. Rencana aksi konkret yang dapat dilakukan orang tua di rumah untuk membantu peningkatan hasil belajar anak.
            
            Gunakan format penulisan yang ramah dan profesional, diawali dengan sapaan hangat kepada Bapak/Ibu Wali Murid dari $studentName.
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(
                temperature = 0.7f,
                maxOutputTokens = 1000
            ),
            systemInstruction = Content(
                parts = listOf(Part(text = "Anda adalah Konselor Pendidikan AI handal untuk SMK (Sekolah Menengah Kejuruan) bidang Rekayasa Perangkat Lunak (RPL). Berikan jawaban yang membimbing, profesional, dan dalam bahasa Indonesia yang sangat ramah."))
            )
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                ?: "Gagal memproses data laporan. Hubungi Administrator Siakad."
        } catch (e: Exception) {
            "Gagal melakukan analisis otomatis: ${e.localizedMessage ?: "Koneksi terganggu. Silakan coba kembali."}"
        }
    }
}
