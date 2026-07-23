package com.beiratv.app.data.parser

object XtreamCodesClient {
    fun buildM3uUrl(serverUrl: String, username: String, password: String): String {
        var cleanServer = serverUrl.trim()
        if (!cleanServer.startsWith("http://") && !cleanServer.startsWith("https://")) {
            cleanServer = "http://$cleanServer"
        }
        cleanServer = cleanServer.trimEnd('/')

        return "$cleanServer/get.php?username=${username.trim()}&password=${password.trim()}&type=m3u_plus&output=m3u8"
    }

    fun buildEpgUrl(serverUrl: String, username: String, password: String): String {
        var cleanServer = serverUrl.trim()
        if (!cleanServer.startsWith("http://") && !cleanServer.startsWith("https://")) {
            cleanServer = "http://$cleanServer"
        }
        cleanServer = cleanServer.trimEnd('/')

        return "$cleanServer/xmltv.php?username=${username.trim()}&password=${password.trim()}"
    }
}
