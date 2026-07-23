package com.beiratv.app.data.parser

import android.util.Xml
import com.beiratv.app.data.local.EpgProgramEntity
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object XmltvParser {
    fun parse(xmlContent: String): List<EpgProgramEntity> {
        val programs = mutableListOf<EpgProgramEntity>()
        if (xmlContent.isBlank()) return programs

        try {
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(StringReader(xmlContent))

            var eventType = parser.eventType
            var currentChannelId: String? = null
            var currentStartTime: Long = 0
            var currentEndTime: Long = 0
            var currentTitle: String? = null
            var currentDesc: String? = null

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val name = parser.name
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (name.equals("programme", ignoreCase = true)) {
                            currentChannelId = parser.getAttributeValue(null, "channel")
                            val startAttr = parser.getAttributeValue(null, "start")
                            val stopAttr = parser.getAttributeValue(null, "stop")
                            currentStartTime = parseXmltvDate(startAttr)
                            currentEndTime = parseXmltvDate(stopAttr)
                            currentTitle = null
                            currentDesc = null
                        } else if (name.equals("title", ignoreCase = true) && currentChannelId != null) {
                            currentTitle = parser.nextText()
                        } else if (name.equals("desc", ignoreCase = true) && currentChannelId != null) {
                            currentDesc = parser.nextText()
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (name.equals("programme", ignoreCase = true) && currentChannelId != null && currentTitle != null) {
                            val id = "${currentChannelId}_${currentStartTime}"
                            programs.add(
                                EpgProgramEntity(
                                    id = id,
                                    channelId = currentChannelId,
                                    title = currentTitle,
                                    description = currentDesc,
                                    startTime = currentStartTime,
                                    endTime = currentEndTime
                                )
                            )
                            currentChannelId = null
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            // Log error safely without crashing
            e.printStackTrace()
        }

        return programs
    }

    private fun parseXmltvDate(dateStr: String?): Long {
        if (dateStr.isNullOrBlank()) return System.currentTimeMillis()
        return try {
            val clean = dateStr.trim().take(14) // YYYYMMDDHHMMSS
            val sdf = SimpleDateFormat("yyyyMMddHHmmss", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            val date: Date? = sdf.parse(clean)
            date?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
}
