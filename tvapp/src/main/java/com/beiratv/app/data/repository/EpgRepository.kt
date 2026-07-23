package com.beiratv.app.data.repository

import com.beiratv.app.data.local.BeiraTVDatabase
import com.beiratv.app.data.local.EpgProgramEntity
import com.beiratv.app.data.parser.XmltvParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class EpgRepository(private val db: BeiraTVDatabase) {
    private val epgDao = db.epgDao()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun getCurrentProgram(tvgId: String?): EpgProgramEntity? = withContext(Dispatchers.IO) {
        if (tvgId.isNullOrBlank()) return@withContext null
        epgDao.getCurrentProgram(tvgId)
    }

    suspend fun getNextProgram(tvgId: String?): EpgProgramEntity? = withContext(Dispatchers.IO) {
        if (tvgId.isNullOrBlank()) return@withContext null
        epgDao.getNextProgram(tvgId)
    }

    suspend fun importXmltvFromUrl(url: String): Result<Int> = withContext(Dispatchers.IO) {
        if (url.isBlank()) return@withContext Result.success(0)
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 TekasTV-TV/0.3.6")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Erro ao baixar EPG XMLTV: HTTP ${response.code}"))
            }

            val bodyString = response.body?.string() ?: ""
            if (bodyString.isBlank()) {
                return@withContext Result.success(0)
            }

            val programs = XmltvParser.parse(bodyString)
            if (programs.isNotEmpty()) {
                epgDao.insertPrograms(programs)
            }

            Result.success(programs.size)
        } catch (e: Exception) {
            // Gracefully handle network/parse failure without crashing app
            Result.failure(e)
        }
    }
}
