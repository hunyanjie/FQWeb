package me.fycz.fqweb.web

import frpclib.Frpclib
import me.fycz.fqweb.utils.GlobalApp
import me.fycz.fqweb.utils.log
import java.io.File

/**
 * @author fengyue
 * @date 2023/7/24 13:53
 * @description
 */
class FrpcServer(private val config: String) {
    private var myThread: Thread? = null

    private val configPath: String by lazy {
        GlobalApp.application?.filesDir?.absolutePath + "/frpc.ini"
    }

    fun start() {
        if (myThread?.isAlive == true) return
        myThread = Thread {
            try {
                File(configPath).writeText(config)
                Frpclib.run(configPath)
            } catch (e: Throwable) {
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