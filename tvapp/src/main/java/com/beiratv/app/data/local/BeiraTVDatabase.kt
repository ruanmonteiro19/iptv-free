package com.beiratv.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ChannelEntity::class,
        EpgProgramEntity::class,
        HistoryEntity::class,
        PlaylistEntity::class,
        ChannelSourceEntity::class,
        ChannelStreamEntity::class,
        ChannelAliasEntity::class,
        ChannelLogoEntity::class,
        FavoriteEntity::class,
        ChannelMetadataEntity::class,
        SourceSyncEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class BeiraTVDatabase : RoomDatabase() {
    abstract fun channelDao(): ChannelDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun metadataDao(): MetadataDao
    abstract fun streamDao(): StreamDao
    abstract fun sourceDao(): SourceDao
    abstract fun aliasDao(): AliasDao
    abstract fun logoDao(): LogoDao
    abstract fun syncDao(): SyncDao
    abstract fun epgDao(): EpgDao
    abstract fun historyDao(): HistoryDao
    abstract fun playlistDao(): PlaylistDao

    companion object {
        @Volatile private var INSTANCE: BeiraTVDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS channel_sources (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        url TEXT NOT NULL,
                        priority INTEGER NOT NULL,
                        enabled INTEGER NOT NULL,
                        region TEXT,
                        verifiedLicensed INTEGER NOT NULL,
                        lastSuccessAt INTEGER,
                        lastFailureAt INTEGER
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS channel_streams (
                        id TEXT NOT NULL PRIMARY KEY,
                        channelId TEXT NOT NULL,
                        sourceId TEXT NOT NULL,
                        streamUrl TEXT NOT NULL,
                        priority INTEGER NOT NULL,
                        quality INTEGER,
                        lastSuccess INTEGER,
                        lastFailure INTEGER,
                        failureCount INTEGER NOT NULL,
                        enabled INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_channel_streams_channelId ON channel_streams(channelId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_channel_streams_sourceId ON channel_streams(sourceId)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_channel_streams_channelId_streamUrl ON channel_streams(channelId, streamUrl)")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS channel_aliases (
                        alias TEXT NOT NULL PRIMARY KEY,
                        channelId TEXT NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_channel_aliases_channelId ON channel_aliases(channelId)")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS channel_logos (
                        `key` TEXT NOT NULL PRIMARY KEY,
                        logoUrl TEXT NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS favorites (
                        channelId TEXT NOT NULL PRIMARY KEY,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT OR IGNORE INTO favorites(channelId, createdAt)
                    SELECT id, CAST(strftime('%s','now') AS INTEGER) * 1000 FROM channels WHERE isFavorite = 1
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS channel_metadata (
                        channelId TEXT NOT NULL PRIMARY KEY,
                        network TEXT,
                        state TEXT,
                        city TEXT,
                        isRegional INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS source_sync (
                        sourceId TEXT NOT NULL PRIMARY KEY,
                        lastFetchedAt INTEGER NOT NULL,
                        lastSuccessAt INTEGER,
                        lastError TEXT
                    )
                """.trimIndent())
            }
        }

        fun getInstance(context: Context): BeiraTVDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                BeiraTVDatabase::class.java,
                "beiratv_database"
            )
                .addMigrations(MIGRATION_1_2)
                .build()
                .also { INSTANCE = it }
        }
    }
}
