package com.beiratv.app.data.repository

import com.beiratv.app.data.local.BeiraTVDatabase
import com.beiratv.app.data.local.ChannelEntity
import com.beiratv.app.data.local.HistoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class HistoryRepository(private val db: BeiraTVDatabase) {
    private val historyDao = db.historyDao()

    val history: Flow<List<HistoryEntity>> = historyDao.getHistory()

    suspend fun recordWatch(channel: ChannelEntity) = withContext(Dispatchers.IO) {
        historyDao.insertHistory(
            HistoryEntity(
                channelId = channel.id,
                channelName = channel.name,
                logoUrl = channel.logo,
                streamUrl = channel.streamUrl,
                category = channel.category,
                lastPlayedTimestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        historyDao.clearAll()
    }
}
