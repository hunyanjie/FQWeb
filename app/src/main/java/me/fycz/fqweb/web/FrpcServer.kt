package me.fycz.fqweb.web

import android.widget.Toast
import de.robv.android.xposed.XposedHelpers
import frpclib.Frpclib
import me.fycz.fqweb.MainHook.Companion.moduleRes
import me.fycz.fqweb.utils.GlobalApp
import me.fycz.fqweb.utils.HttpUtils
import me.fycz.fqweb.utils.SPUtils
import me.fycz.fqweb.utils.log
import java.io.File

/**
 * @author fengyue
 * @date 2023/7/24 13:53
 * @description
 */
class FrpcServer {
    private var myThread: Thread? = null

    private val configFile: File by lazy {
        File(GlobalApp.application?.getExternalFilesDir(null)?.absolutePath + "/frpc.ini")
    }

    fun start() {
        if (!configFile.exists()) {
            writeConfig()
        }
        if (myThread?.isAlive == true) return
        myThread = Thread {
            try {
                Frpclib.run(configFile.absolutePath)
            } catch (e: Throwable) {
                Toast.makeText(
                    GlobalApp.application,
                    "Frpc服务启动失败\n${e.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
                log(e)
            }
        }.apply {
            isDaemon = true
            name = "Frp Client"
        }.also {
            it.start()
        }
    }

    fun writeConfig() {
        val timestamp = System.currentTimeMillis()
        val config =
            XposedHelpers.assetAsByteArray(moduleRes, "frpc.ini").inputStream().reader()
                .readText()
                .replace("{port}", SPUtils.getInt("port", 9999).toString())
                .replace("{timestamp}", timestamp.toString())
        configFile.writeText(config)
        val domain = "$timestamp.api-fanqienovel.sunianyun.live"
        SPUtils.putString("publicDomain", domain)
        Thread {
            HttpUtils.doGet("http://list.api-fanqienovel.sunianyun.live/upload?domain=$domain")
        }.start()
    }

    fun isAlive(): Boolean {
        return myThread?.isAlive == true
    }
}