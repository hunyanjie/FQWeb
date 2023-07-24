package me.fycz.fqweb.utils

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL

/**
 * @author fengyue
 * @date 2023/7/24 18:08
 * @description
 */
object HttpUtils {
    fun doGet(url: String): String {
        val connection = URL(url).openConnection()
        BufferedReader(InputStreamReader(connection.getInputStream())).use { inp ->
            return inp.readText()
        }
    }
}