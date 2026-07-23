package com.beiratv.app.data.repository

import android.util.Log
import androidx.room.withTransaction
import com.beiratv.app.data.local.BeiraTVDatabase
import com.beiratv.app.data.local.ChannelAliasEntity
import com.beiratv.app.data.local.ChannelLogoEntity
import com.beiratv.app.data.local.ChannelSourceEntity
import com.beiratv.app.data.local.FavoriteEntity
import com.beiratv.app.data.local.HistoryEntity
import com.beiratv.app.data.local.SourceSyncEntity
import com.beiratv.app.data.normalization.ChannelConsolidator
import com.beiratv.app.data.normalization.ChannelNameNormalizer
import com.beiratv.app.data.normalization.TextNormalizer
import com.beiratv.app.data.parser.M3uParser
import com.beiratv.app.data.parser.ParsedChannel
import com.beiratv.app.data.source.BuiltInSources
import com.beiratv.app.data.source.RemoteCatalogs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class SourceRepository(private val db: BeiraTVDatabase) {
    private val sourceDao = db.sourceDao()
    private val channelDao = db.channelDao()
    private val streamDao = db.streamDao()
    private val aliasDao = db.aliasDao()
    private val logoDao = db.logoDao()
    private val favoriteDao = db.favoriteDao()
    private val metadataDao = db.metadataDao()
    private val syncDao = db.syncDao()
    private val playlistDao = db.playlistDao()
    private val historyDao = db.historyDao()

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val catalogs = RemoteCatalogs()
    val lastSuccessfulSync: Flow<Long?> = syncDao.getLastSuccessfulSync()

    suspend fun hasCachedChannels(): Boolean = withContext(Dispatchers.IO) {
        channelDao.countChannels() > 0
    }

    suspend fun syncIfNeeded(force: Boolean = false): SyncResult = withContext(Dispatchers.IO) {
        sourceDao.upsertAll(BuiltInSources.channels)
        val now = System.currentTimeMillis()
        val globalSync = syncDao.get("__global__")
        val fresh = globalSync?.lastSuccessAt?.let { now - it < CACHE_TTL_MS } == true
        if (!force && fresh && channelDao.countChannels() > 0) {
            return@withContext SyncResult.Skipped(globalSync?.lastSuccessAt)
        }
        syncAll(now)
    }

    private suspend fun syncAll(now: Long): SyncResult = coroutineScope {
        val sources = BuiltInSources.channels.filter { it.enabled }
        val sourceMap = sources.associateBy { it.id }

        val oldFavoriteRows = channelDao.getFavoriteSnapshotRaw()
        val oldFavoriteIds = oldFavoriteRows.mapTo(mutableSetOf()) { it.id }
        val oldFavoriteNames = oldFavoriteRows.mapTo(mutableSetOf()) { ChannelNameNormalizer.normalized(it.name) }
        val oldFavoriteTvgIds = oldFavoriteRows.mapNotNullTo(mutableSetOf()) { it.tvgId?.let(TextNormalizer::fold) }
        val oldHistory = historyDao.getAllSnapshot()
        val previousStreamHealth = streamDao.getAllSnapshot().associateBy { it.id }

        val aliasDeferred = async(Dispatchers.IO) { fetchText(BuiltInSources.ALIASES_URL) }
        val logoDeferred = async(Dispatchers.IO) { fetchText(BuiltInSources.LOGOS_URL) }

        val downloadGate = Semaphore(4)
        val results = sources.map { source ->
            async(Dispatchers.IO) {
                downloadGate.withPermit {
                val text = fetchText(source.url)
                if (text.isNullOrBlank()) {
                    sourceDao.markFailure(source.id, now)
                    syncDao.upsert(SourceSyncEntity(source.id, now, lastSuccessAt = syncDao.get(source.id)?.lastSuccessAt, lastError = "download_failed"))
                    SourceFetch(source, null, cachedRows(source))
                } else {
                    val parsed = runCatching { M3uParser.parse(text, source.id) }.getOrElse { emptyList() }
                    if (parsed.isEmpty()) {
                        sourceDao.markFailure(source.id, now)
                        syncDao.upsert(SourceSyncEntity(source.id, now, syncDao.get(source.id)?.lastSuccessAt, "empty_playlist"))
                        SourceFetch(source, null, cachedRows(source))
                    } else {
                        sourceDao.markSuccess(source.id, now)
                        syncDao.upsert(SourceSyncEntity(source.id, now, now, null))
                        Log.d(TAG, "Fonte ${source.id}: ${parsed.size} canais recebidos")
                        SourceFetch(source, parsed, emptyList())
                    }
                }
                }
            }
        }.awaitAll()

        val usableRows = results.flatMap { it.fresh ?: it.cached }
        if (usableRows.isEmpty()) {
            syncDao.upsert(SourceSyncEntity("__global__", now, syncDao.get("__global__")?.lastSuccessAt, "no_sources"))
            return@coroutineScope SyncResult.Failure("Não foi possível atualizar as fontes. A grade em cache foi preservada.")
        }

        var aliasLookup = aliasDao.getAll().associate { it.alias to it.channelId }
        var logoLookup = logoDao.getAll().associate { it.key to it.logoUrl }
        var aliasEntities: List<ChannelAliasEntity>? = null
        var logoEntities: List<ChannelLogoEntity>? = null

        aliasDeferred.await()?.let { json ->
            runCatching { catalogs.parseAliases(json) }.getOrNull()?.let { (lookup, entities) ->
                aliasLookup = lookup
                aliasEntities = entities
            }
        }
        logoDeferred.await()?.let { json ->
            runCatching { catalogs.parseLogos(json) }.getOrNull()?.let { (lookup, entities) ->
                logoLookup = lookup
                logoEntities = entities
            }
        }

        val consolidated = ChannelConsolidator.consolidate(usableRows, sourceMap, logoLookup, aliasLookup)
        if (consolidated.isEmpty()) {
            return@coroutineScope SyncResult.Failure("As fontes responderam, mas nenhum canal público elegível foi encontrado.")
        }

        db.withTransaction {
            streamDao.clearAll()
            metadataDao.clearAll()
            channelDao.clearAllChannels()
            playlistDao.clearAllLegacyPlaylists()
            channelDao.insertChannels(consolidated.map { it.channel })
            val streamsWithHealth = consolidated.flatMap { item ->
                item.streams.map { stream ->
                    val prior = previousStreamHealth[stream.id]
                    if (prior == null) stream else stream.copy(
                        lastSuccess = prior.lastSuccess,
                        lastFailure = prior.lastFailure,
                        failureCount = prior.failureCount
                    )
                }
            }
            streamDao.insertAll(streamsWithHealth)
            metadataDao.insertAll(consolidated.map { it.metadata })

            aliasEntities?.let {
                aliasDao.clearAll()
                aliasDao.insertAll(it)
            }
            logoEntities?.let {
                logoDao.clearAll()
                logoDao.insertAll(it)
            }

            // Existing favorites are migrated by normalized channel name if the previous id was URL-based.
            // Stale URL-based ids are removed so favorites never accumulate dead records.
            favoriteDao.clearAll()
            consolidated.forEach { item ->
                val tvgKey = item.channel.tvgId?.let(TextNormalizer::fold)
                val favoriteMatches = item.channel.id in oldFavoriteIds ||
                    ChannelNameNormalizer.normalized(item.channel.name) in oldFavoriteNames ||
                    (tvgKey != null && tvgKey in oldFavoriteTvgIds)
                if (favoriteMatches) favoriteDao.add(FavoriteEntity(item.channel.id))
            }

            val byName = consolidated.associateBy { ChannelNameNormalizer.normalized(it.channel.name) }
            val migratedHistory = oldHistory.mapNotNull { old ->
                val match = byName[ChannelNameNormalizer.normalized(old.channelName)]?.channel ?: return@mapNotNull null
                HistoryEntity(
                    channelId = match.id,
                    channelName = match.name,
                    logoUrl = match.logo,
                    streamUrl = match.streamUrl,
                    category = match.category,
                    lastPlayedTimestamp = old.lastPlayedTimestamp
                )
            }.distinctBy { it.channelId }
            historyDao.clearAll()
            if (migratedHistory.isNotEmpty()) historyDao.insertHistoryItems(migratedHistory)
        }

        syncDao.upsert(SourceSyncEntity("__global__", now, now, null))
        val duplicateCount = usableRows.size - consolidated.size
        Log.d(TAG, "Consolidação TekasTV: ${usableRows.size} entradas, ${consolidated.size} canais, $duplicateCount duplicatas agrupadas")
        SyncResult.Success(consolidated.size, duplicateCount, results.count { it.fresh != null }, results.count { it.fresh == null })
    }

    private suspend fun cachedRows(source: ChannelSourceEntity): List<ParsedChannel> {
        return streamDao.getCachedForSource(source.id).map { cached ->
            ParsedChannel(
                sourceId = cached.sourceId,
                name = cached.name,
                logo = cached.logo,
                streamUrl = cached.streamUrl,
                groupTitle = cached.category,
                tvgId = cached.tvgId,
                tvgName = cached.name,
                qualityHint = cached.quality
            )
        }
    }

    private fun fetchText(url: String): String? = runCatching {
        client.newCall(
            Request.Builder()
                .url(url)
                .header("User-Agent", "TekasTV-TV/0.3.6 (Android TV)")
                .build()
        ).execute().use { response ->
            if (!response.isSuccessful) null else response.body?.string()
        }
    }.getOrNull()

    data class SourceFetch(
        val source: ChannelSourceEntity,
        val fresh: List<ParsedChannel>?,
        val cached: List<ParsedChannel>
    )

    sealed interface SyncResult {
        data class Success(
            val channels: Int,
            val duplicatesGrouped: Int,
            val freshSources: Int,
            val cachedSources: Int
        ) : SyncResult
        data class Skipped(val lastSync: Long?) : SyncResult
        data class Failure(val message: String) : SyncResult
    }

    companion object {
        private const val CACHE_TTL_MS = 12L * 60L * 60L * 1000L
        private const val TAG = "TekasTVSync"
    }
}
