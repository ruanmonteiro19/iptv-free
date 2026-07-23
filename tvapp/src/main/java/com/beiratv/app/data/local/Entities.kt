package com.beiratv.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A channel displayed to the user. The id is a stable canonical id and MUST NOT
 * depend on a stream URL, so favorites/history survive source refreshes.
 */
@Entity(tableName = "channels")
data class ChannelEntity(
    @PrimaryKey val id: String,
    val name: String,
    val logo: String? = null,
    val streamUrl: String,
    val category: String = "Geral",
    val tvgId: String? = null,
    val isFavorite: Boolean = false,
    val playlistId: String = "builtin"
)

@Entity(tableName = "epg_programs")
data class EpgProgramEntity(
    @PrimaryKey val id: String,
    val channelId: String,
    val title: String,
    val description: String? = null,
    val startTime: Long,
    val endTime: Long
)

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey val channelId: String,
    val channelName: String,
    val logoUrl: String? = null,
    val streamUrl: String,
    val category: String,
    val lastPlayedTimestamp: Long = System.currentTimeMillis()
)

/** Kept for backwards compatibility with installations from <= 0.3.6. */
@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val title: String,
    val url: String,
    val type: String,
    val username: String? = null,
    val password: String? = null,
    val epgUrl: String? = null,
    val isActive: Boolean = true
)

@Entity(tableName = "channel_sources")
data class ChannelSourceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val url: String,
    val priority: Int,
    val enabled: Boolean = true,
    val region: String? = null,
    val verifiedLicensed: Boolean = false,
    val lastSuccessAt: Long? = null,
    val lastFailureAt: Long? = null
)

@Entity(
    tableName = "channel_streams",
    indices = [Index("channelId"), Index("sourceId"), Index(value = ["channelId", "streamUrl"], unique = true)]
)
data class ChannelStreamEntity(
    @PrimaryKey val id: String,
    val channelId: String,
    val sourceId: String,
    val streamUrl: String,
    val priority: Int,
    val quality: Int? = null,
    val lastSuccess: Long? = null,
    val lastFailure: Long? = null,
    val failureCount: Int = 0,
    val enabled: Boolean = true
)

@Entity(tableName = "channel_aliases", indices = [Index("channelId")])
data class ChannelAliasEntity(
    @PrimaryKey val alias: String,
    val channelId: String
)

@Entity(tableName = "channel_logos")
data class ChannelLogoEntity(
    @PrimaryKey val key: String,
    val logoUrl: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val channelId: String,
    val createdAt: Long = System.currentTimeMillis()
)


@Entity(tableName = "channel_metadata")
data class ChannelMetadataEntity(
    @PrimaryKey val channelId: String,
    val network: String? = null,
    val state: String? = null,
    val city: String? = null,
    val isRegional: Boolean = false
)

@Entity(tableName = "source_sync")
data class SourceSyncEntity(
    @PrimaryKey val sourceId: String,
    val lastFetchedAt: Long,
    val lastSuccessAt: Long? = null,
    val lastError: String? = null
)

data class CachedChannelStream(
    val channelId: String,
    val name: String,
    val logo: String?,
    val category: String,
    val tvgId: String?,
    val streamUrl: String,
    val sourceId: String,
    val priority: Int,
    val quality: Int?
)
