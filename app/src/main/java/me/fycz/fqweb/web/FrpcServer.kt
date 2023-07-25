package me.fycz.fqweb.web

import android.widget.Toast
import de.robv.android.xposed.XposedHelpers
import frpclib.Frpclib
import me.fycz.fqweb.BuildConfig
import me.fycz.fqweb.MainHook.Companion.moduleRes
import me.fycz.fqweb.constant.Config.TRAVERSAL_CONFIG_URL
import me.fycz.fqweb.entity.NATTraversalConfig
import me.fycz.fqweb.entity.ServerConfig
import me.fycz.fqweb.utils.GlobalApp
import me.fycz.fqweb.utils.HttpUtils
import me.fycz.fqweb.utils.JsonUtils
import me.fycz.fqweb.utils.SPUtils
import me.fycz.fqweb.utils.ToastUtils
import me.fycz.fqweb.utils.log
import java.io.File
import java.lang.RuntimeException

/**
 * @author fengyue
 * @date 2023/7/24 13:53
 * @description
 */
class FrpcServer {
    private var myThread: Thread? = null

    private val retry: Int = 3

    var traversalConfig: NATTraversalConfig? = null

    var currentServer: ServerConfig? = null

    private val configFile: File by lazy {
        File(GlobalApp.application?.filesDir?.absolutePath + "/frpc.ini")
    }

    fun start(manual: Boolean = false) {
        if (myThread?.isAlive == true) return
        if (manual) ToastUtils.toast("正在启动内网穿透服务...")
        initConfig() {
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
    }

    private fun initConfig(callback: () -> Unit) {
        Thread {
            var throwable: Throwable? = null
            for (i in 1..retry) {
                try {
                    val json = HttpUtils.doGet(TRAVERSAL_CONFIG_URL)
                    if (json.isEmpty()) throw RuntimeException("json数据为空")
                    traversalConfig = JsonUtils.fromJson(json, NATTraversalConfig::class.java)
                    if (traversalConfig?.enable != true) {
                        ToastUtils.toast("内网穿透服务已关闭")
                        return@Thread
                    }
                    if (BuildConfig.VERSION_CODE < traversalConfig!!.minVersion!!) {
                        ToastUtils.toast("当前番茄Web版本过低，已不支持内网穿透服务")
                        return@Thread
                    }
                    if (traversalConfig!!.servers.isNullOrEmpty()) {
                        ToastUtils.toast("当前没有可用的内网穿透服务")
                        return@Thread
                    }
                    throwable = null
                    break
                } catch (e: Throwable) {
                    throwable = e
                    log(e)
                }
            }
            if (throwable != null) {
                ToastUtils.toast("无法获取内网穿透服务配置，请更新番茄Web到最新版后重试\n${throwable.localizedMessage}")
                return@Thread
            }
            val selectServer = SPUtils.getString("selectServer")
            traversalConfig!!.servers!!.filter { it.check() }.forEach {
                if (it.name == selectServer) {
                    currentServer = it
                    return@forEach
                }
            }
            if (currentServer == null) {
                currentServer = traversalConfig!!.servers!!.firstOrNull { it.check() }
            }
            writeConfig(callback)
        }.start()
    }

    private fun writeConfig(callback: () -> Unit) {
        if (currentServer == null) {
            ToastUtils.toast("当前没有可用的内网穿透服务")
            return
        }
        val timestamp = System.currentTimeMillis().toString()
        val domain = currentServer!!.customDomain!!.replace("{timestamp}", timestamp)
        val config = currentServer!!.frpcConfig!!
            .replace("{port}", SPUtils.getInt("port", 9999).toString())
            .replace("{timestamp}", timestamp)
            .replace("{domain}", domain)
        configFile.writeText(
            if (currentServer!!.name == "测试接口") config.replace(
                "{token}",
                "www.126126.xyz"
            ) else config
        )
        SPUtils.putString("publicDomain", domain)
        Thread {
            for (i in 1..retry) {
                try {
                    HttpUtils.doGet(currentServer!!.uploadDomainUrl!!.replace("{domain}", domain))
                    break
                } catch (e: Throwable) {
                    log(e)
                }
            }
        }.start()
        callback()
    }

    fun isAlive(): Boolean {
        return myThread?.isAlive == true
    }
}