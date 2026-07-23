package com.beiratv.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

private const val CHANNEL_WITH_FAVORITE_COLUMNS = """
    c.id AS id,
    c.name AS name,
    c.logo AS logo,
    c.streamUrl AS streamUrl,
    c.category AS category,
    c.tvgId AS tvgId,
    CASE WHEN f.channelId IS NULL THEN 0 ELSE 1 END AS isFavorite,
    c.playlistId AS playlistId
"""

@Dao
interface ChannelDao {
    @Query("""
        SELECT c.id, c.name, c.logo, c.streamUrl, c.category, c.tvgId,
               CASE WHEN f.channelId IS NULL THEN 0 ELSE 1 END AS isFavorite,
               c.playlistId
        FROM channels c
        LEFT JOIN favorites f ON f.channelId = c.id
        ORDER BY c.name COLLATE NOCASE ASC
    """)
    fun getAllChannels(): Flow<List<ChannelEntity>>

    @Query("""
        SELECT c.id, c.name, c.logo, c.streamUrl, c.category, c.tvgId,
               1 AS isFavorite, c.playlistId
        FROM channels c
        INNER JOIN favorites f ON f.channelId = c.id
        ORDER BY c.name COLLATE NOCASE ASC
    """)
    fun getFavoriteChannels(): Flow<List<ChannelEntity>>

    @Query("SELECT DISTINCT category FROM channels ORDER BY category COLLATE NOCASE ASC")
    fun getCategories(): Flow<List<String>>

    @Query("""
        SELECT c.id, c.name, c.logo, c.streamUrl, c.category, c.tvgId,
               CASE WHEN f.channelId IS NULL THEN 0 ELSE 1 END AS isFavorite,
               c.playlistId
        FROM channels c LEFT JOIN favorites f ON f.channelId = c.id
        WHERE c.category = :category
        ORDER BY c.name COLLATE NOCASE ASC
    """)
    fun getChannelsByCategory(category: String): Flow<List<ChannelEntity>>

    @Query("""
        SELECT c.id, c.name, c.logo, c.streamUrl, c.category, c.tvgId,
               CASE WHEN f.channelId IS NULL THEN 0 ELSE 1 END AS isFavorite,
               c.playlistId
        FROM channels c LEFT JOIN favorites f ON f.channelId = c.id
        WHERE c.name LIKE '%' || :query || '%' OR c.category LIKE '%' || :query || '%'
        ORDER BY c.name COLLATE NOCASE ASC
    """)
    fun searchChannels(query: String): Flow<List<ChannelEntity>>

    @Query("""
        SELECT c.id, c.name, c.logo, c.streamUrl, c.category, c.tvgId,
               CASE WHEN f.channelId IS NULL THEN 0 ELSE 1 END AS isFavorite,
               c.playlistId
        FROM channels c LEFT JOIN favorites f ON f.channelId = c.id
        WHERE c.id = :id LIMIT 1
    """)
    suspend fun getChannelById(id: String): ChannelEntity?

    @Query("SELECT COUNT(*) FROM channels")
    suspend fun countChannels(): Int

    @Query("SELECT * FROM channels")
    suspend fun getAllSnapshotRaw(): List<ChannelEntity>

    @Query("SELECT c.* FROM channels c INNER JOIN favorites f ON f.channelId = c.id")
    suspend fun getFavoriteSnapshotRaw(): List<ChannelEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<ChannelEntity>)

    @Query("DELETE FROM channels WHERE playlistId = 'builtin'")
    suspend fun clearBuiltInChannels()

    @Query("DELETE FROM channels")
    suspend fun clearAllChannels()

    @Query("DELETE FROM channels WHERE playlistId = :playlistId")
    suspend fun deleteByPlaylistId(playlistId: String)

    @Query("DELETE FROM channels WHERE playlistId = 'default' OR id LIKE 'sample_%'")
    suspend fun deleteLegacySampleChannels()
}

@Dao
interface FavoriteDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun add(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE channelId = :channelId")
    suspend fun remove(channelId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE channelId = :channelId)")
    suspend fun isFavorite(channelId: String): Boolean

    @Query("DELETE FROM favorites")
    suspend fun clearAll()
}

@Dao
interface StreamDao {
    @Query("SELECT * FROM channel_streams WHERE channelId = :channelId AND enabled = 1 ORDER BY failureCount ASC, CASE WHEN lastSuccess IS NULL THEN 1 ELSE 0 END ASC, lastSuccess DESC, priority DESC, quality DESC")
    suspend fun getStreamsForChannel(channelId: String): List<ChannelStreamEntity>

    @Query("""
        SELECT s.channelId, c.name, c.logo, c.category, c.tvgId,
               s.streamUrl, s.sourceId, s.priority, s.quality
        FROM channel_streams s
        INNER JOIN channels c ON c.id = s.channelId
        WHERE s.sourceId = :sourceId AND s.enabled = 1
    """)
    suspend fun getCachedForSource(sourceId: String): List<CachedChannelStream>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(streams: List<ChannelStreamEntity>)

    @Query("SELECT * FROM channel_streams")
    suspend fun getAllSnapshot(): List<ChannelStreamEntity>

    @Query("DELETE FROM channel_streams")
    suspend fun clearAll()

    @Query("UPDATE channel_streams SET lastSuccess = :timestamp, failureCount = 0 WHERE id = :streamId")
    suspend fun markSuccess(streamId: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE channel_streams SET lastFailure = :timestamp, failureCount = failureCount + 1 WHERE id = :streamId")
    suspend fun markFailure(streamId: String, timestamp: Long = System.currentTimeMillis())
}

@Dao
interface SourceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(sources: List<ChannelSourceEntity>)

    @Query("SELECT * FROM channel_sources WHERE enabled = 1 ORDER BY priority DESC")
    suspend fun getEnabled(): List<ChannelSourceEntity>

    @Query("UPDATE channel_sources SET lastSuccessAt = :time WHERE id = :id")
    suspend fun markSuccess(id: String, time: Long)

    @Query("UPDATE channel_sources SET lastFailureAt = :time WHERE id = :id")
    suspend fun markFailure(id: String, time: Long)
}

@Dao
interface AliasDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ChannelAliasEntity>)

    @Query("SELECT * FROM channel_aliases")
    suspend fun getAll(): List<ChannelAliasEntity>

    @Query("DELETE FROM channel_aliases")
    suspend fun clearAll()
}

@Dao
interface LogoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ChannelLogoEntity>)

    @Query("SELECT * FROM channel_logos")
    suspend fun getAll(): List<ChannelLogoEntity>

    @Query("DELETE FROM channel_logos")
    suspend fun clearAll()
}

@Dao
interface MetadataDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ChannelMetadataEntity>)

    @Query("SELECT * FROM channel_metadata WHERE channelId = :channelId LIMIT 1")
    suspend fun get(channelId: String): ChannelMetadataEntity?

    @Query("SELECT DISTINCT state FROM channel_metadata WHERE isRegional = 1 AND state IS NOT NULL ORDER BY state")
    fun getRegionalStates(): Flow<List<String>>

    @Query("SELECT channelId FROM channel_metadata WHERE state = :state")
    suspend fun channelIdsForState(state: String): List<String>

    @Query("DELETE FROM channel_metadata")
    suspend fun clearAll()
}

@Dao
interface SyncDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(sync: SourceSyncEntity)

    @Query("SELECT * FROM source_sync WHERE sourceId = :sourceId LIMIT 1")
    suspend fun get(sourceId: String): SourceSyncEntity?

    @Query("SELECT MAX(lastSuccessAt) FROM source_sync")
    fun getLastSuccessfulSync(): Flow<Long?>
}

@Dao
interface EpgDao {
    @Query("SELECT * FROM epg_programs WHERE channelId = :tvgId AND startTime <= :currentTime AND endTime >= :currentTime LIMIT 1")
    suspend fun getCurrentProgram(tvgId: String, currentTime: Long = System.currentTimeMillis()): EpgProgramEntity?

    @Query("SELECT * FROM epg_programs WHERE channelId = :tvgId AND endTime >= :currentTime ORDER BY startTime ASC LIMIT 10")
    fun getProgramsForChannel(tvgId: String, currentTime: Long = System.currentTimeMillis()): Flow<List<EpgProgramEntity>>

    @Query("SELECT * FROM epg_programs WHERE channelId = :tvgId AND startTime > :currentTime ORDER BY startTime ASC LIMIT 1")
    suspend fun getNextProgram(tvgId: String, currentTime: Long = System.currentTimeMillis()): EpgProgramEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrograms(programs: List<EpgProgramEntity>)

    @Query("DELETE FROM epg_programs WHERE endTime < :currentTime")
    suspend fun purgeOldPrograms(currentTime: Long = System.currentTimeMillis())

    @Query("DELETE FROM epg_programs")
    suspend fun clearAll()
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY lastPlayedTimestamp DESC LIMIT 20")
    fun getHistory(): Flow<List<HistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: HistoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistoryItems(history: List<HistoryEntity>)

    @Query("SELECT * FROM history ORDER BY lastPlayedTimestamp DESC")
    suspend fun getAllSnapshot(): List<HistoryEntity>

    @Query("DELETE FROM history")
    suspend fun clearAll()

    @Query("DELETE FROM history WHERE channelId LIKE 'sample_%'")
    suspend fun deleteLegacySampleHistory()
}

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY title ASC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: String)

    @Query("DELETE FROM playlists WHERE id = 'default' OR type = 'SAMPLE' OR url = 'internal://sample'")
    suspend fun deleteLegacySamplePlaylists()

    @Query("DELETE FROM playlists")
    suspend fun clearAllLegacyPlaylists()
}
