package com.beiratv.app.data.repository

import com.beiratv.app.data.local.BeiraTVDatabase
import com.beiratv.app.data.local.ChannelEntity
import com.beiratv.app.data.local.ChannelStreamEntity
import com.beiratv.app.data.local.FavoriteEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ChannelRepository(private val db: BeiraTVDatabase) {
    private val channelDao = db.channelDao()
    private val favoriteDao = db.favoriteDao()
    private val streamDao = db.streamDao()
    private val playlistDao = db.playlistDao()
    private val historyDao = db.historyDao()

    val allChannels: Flow<List<ChannelEntity>> = channelDao.getAllChannels()
    val favoriteChannels: Flow<List<ChannelEntity>> = channelDao.getFavoriteChannels()
    val categories: Flow<List<String>> = channelDao.getCategories()

    suspend fun removeLegacySampleContent() = withContext(Dispatchers.IO) {
        channelDao.deleteLegacySampleChannels()
        playlistDao.deleteLegacySamplePlaylists()
        historyDao.deleteLegacySampleHistory()
    }

    suspend fun toggleFavorite(channelId: String, makeFavorite: Boolean) = withContext(Dispatchers.IO) {
        if (makeFavorite) favoriteDao.add(FavoriteEntity(channelId)) else favoriteDao.remove(channelId)
    }

    suspend fun getStreams(channelId: String): List<ChannelStreamEntity> = withContext(Dispatchers.IO) {
        streamDao.getStreamsForChannel(channelId)
    }

    suspend fun markStreamSuccess(streamId: String) = withContext(Dispatchers.IO) {
        streamDao.markSuccess(streamId)
    }

    suspend fun markStreamFailure(streamId: String) = withContext(Dispatchers.IO) {
        streamDao.markFailure(streamId)
    }
}
