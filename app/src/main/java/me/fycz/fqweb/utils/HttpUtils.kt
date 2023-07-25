package me.fycz.fqweb.utils

import me.fycz.fqweb.constant.Config.DEFAULT_USER_AGENT
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * @author fengyue
 * @date 2023/7/24 18:08
 * @description
 */
object HttpUtils {
    fun doGet(url: String): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15 * 1000
            readTimeout = 15 * 1000
            setRequestProperty("Accept", "*/*")
            addRequestProperty("Keep-Alive", "300")
            addRequestProperty("Connection", "Keep-Alive")
            addRequestProperty("Cache-Control", "no-cache")
            addRequestProperty("User-Agent", DEFAULT_USER_AGENT)
        }
        connection.inputStream.reader().use { inp ->
            return inp.readText()
        }
    }

    fun doPost(url: String, body: String): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15 * 1000
            readTimeout = 15 * 1000
            doInput = true
            doOutput = true
            setRequestProperty("Accept", "*/*")
            addRequestProperty("Keep-Alive", "300")
            addRequestProperty("Connection", "Keep-Alive")
            addRequestProperty("Cache-Control", "no-cache")
            addRequestProperty("User-Agent", DEFAULT_USER_AGENT)
            addRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        }
        DataOutputStream(connection.outputStream).use { it.writeBytes(body) }
        connection.inputStream.reader().use { inp ->
            return inp.readText()
        }
    }
}