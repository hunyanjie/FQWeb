package me.fycz.fqweb.web

import android.widget.Toast
import frpclib.Frpclib
import me.fycz.fqweb.utils.GlobalApp
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

    fun start(manual: Boolean = false) {
        if (!configFile.exists()) {
            if (manual) Toast.makeText(
                GlobalApp.application,
                "Frpc配置文件不存在(${configFile.absolutePath})，无法启动服务",
                Toast.LENGTH_SHORT
            ).show()
            return
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

    fun stop() {
        try {
            myThread?.interrupt()
        } catch (e: Throwable) {
            log(e)
        }
    }

    fun isAlive(): Boolean {
        return myThread?.isAlive == true
    }
}