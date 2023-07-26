package me.fycz.fqweb

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.AlertDialog
import android.app.Application
import android.content.Context
import android.content.res.Resources
import android.content.res.XModuleResources
import android.graphics.Color
import android.text.Html
import android.text.InputFilter
import android.text.InputType
import android.text.method.LinkMovementMethod
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import de.robv.android.xposed.IXposedHookInitPackageResources
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_InitPackageResources
import de.robv.android.xposed.callbacks.XC_LoadPackage
import frpclib.Frpclib
import me.fycz.fqweb.constant.Config
import me.fycz.fqweb.utils.GlobalApp
import me.fycz.fqweb.utils.NetworkUtils
import me.fycz.fqweb.utils.SPUtils
import me.fycz.fqweb.utils.ToastUtils
import me.fycz.fqweb.utils.callMethod
import me.fycz.fqweb.utils.findClass
import me.fycz.fqweb.utils.getObjectField
import me.fycz.fqweb.utils.hookAfterMethod
import me.fycz.fqweb.utils.invokeOriginalMethod
import me.fycz.fqweb.utils.log
import me.fycz.fqweb.utils.new
import me.fycz.fqweb.utils.replaceMethod
import me.fycz.fqweb.utils.setObjectField
import me.fycz.fqweb.web.FrpcServer
import me.fycz.fqweb.web.HttpServer
import java.io.File
import java.util.LinkedList


/**
 * @author fengyue
 * @date 2022/5/3 12:59
 */
class MainHook : IXposedHookLoadPackage, IXposedHookZygoteInit, IXposedHookInitPackageResources {

    companion object {
        lateinit var moduleRes: Resources
    }

    private lateinit var httpServer: HttpServer

    private lateinit var frpcServer: FrpcServer

    private lateinit var modulePath: String

    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam) {
        modulePath = startupParam.modulePath
    }

    override fun handleInitPackageResources(resParam: XC_InitPackageResources.InitPackageResourcesParam) {
        moduleRes = XModuleResources.createInstance(modulePath, resParam.res)
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName == "com.dragon.read") {
            GlobalApp.initClassLoader(lpparam.classLoader)
            "com.dragon.read.app.MainApplication".hookAfterMethod(
                lpparam.classLoader,
                "onCreate"
            ) {
                val app = it.thisObject as Application
                if (lpparam.packageName == getProcessName(app)) {
                    GlobalApp.application = app
                    log("versionCode = ${Config.versionCode}")
                    SPUtils.init(app)
                    hookSetting(lpparam.classLoader)
                    hookUpdate(lpparam.classLoader)
                    httpServer = HttpServer(SPUtils.getInt("port", 9999))
                    frpcServer = FrpcServer()
                    SPUtils.putString("publicDomain", "未获取")
                    if (!httpServer.isAlive && SPUtils.getBoolean("autoStart", false)) {
                        try {
                            httpServer.start()
                            if (SPUtils.getBoolean("traversal", false)) frpcServer.start()
                        } catch (e: Throwable) {
                            log(e)
                        }
                    }

                }
            }
        }
    }

    private fun getProcessName(context: Context): String {
        return try {
            for (runningAppProcessInfo in (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).runningAppProcesses) {
                if (runningAppProcessInfo.pid == android.os.Process.myPid()) {
                    return runningAppProcessInfo.processName
                }
            }
            "unknown"
        } catch (unused: Exception) {
            "unknown"
        }
    }

    private fun hookSetting(classLoader: ClassLoader) {
        var adapter: Any? = null
        "com.dragon.read.component.biz.impl.mine.settings.SettingsActivity"
            .hookAfterMethod(
                classLoader,
                "a",
                Config.settingRecyclerAdapterClz.findClass(classLoader)
            ) {
                adapter = it.thisObject.getObjectField("b")
                val list = it.result as LinkedList<Any>
                if (list[0].getObjectField("e") != "Web服务") {
                    val context = it.thisObject as Context
                    val setting =
                        Config.settingItemQSNClz.findClass(classLoader)
                            .new(context)
                    setting.setObjectField("e", "Web服务")
                    setting.setObjectField(
                        Config.settingItemStrFieldName,
                        if (httpServer.isAlive)
                            "已开启(http://${NetworkUtils.getLocalIPAddress()?.hostAddress ?: "localhost"}:${
                                SPUtils.getInt(
                                    "port",
                                    9999
                                )
                            })"
                        else "未开启"
                    )
                    list.add(0, setting)
                }
            }
        "${Config.settingItemQSNClz}$1"
            .replaceMethod(
                classLoader, "a",
                View::class.java,
                "com.dragon.read.pages.mine.settings.e".findClass(classLoader),
                Int::class.java
            ) {
                val context = (it.args[0] as View).context
                if (it.args[1].getObjectField("e") == "Web服务") {
                    if (!SPUtils.getBoolean("disclaimer", false)) {
                        AlertDialog.Builder(context)
                            .setTitle("免责声明")
                            .setCancelable(true)
                            .setMessage(
                                XposedHelpers.assetAsByteArray(moduleRes, "disclaimer.txt")
                                    .inputStream().reader().readText()
                            )
                            .setPositiveButton("同意并继续") { _, _ ->
                                dialog(context, adapter, it.args[1])
                                SPUtils.putBoolean("disclaimer", true)
                            }.setNegativeButton("不同意", null)
                            .show()
                    } else {
                        dialog(context, adapter, it.args[1])
                    }
                } else {
                    it.invokeOriginalMethod()
                }
            }
    }

    @SuppressLint("SetTextI18n")
    fun dialog(context: Context, adapter: Any?, settingView: Any) {
        val textColor = Color.parseColor("#060606")

        val layout_root = LinearLayout(context)
        layout_root.setPadding(
            dp2px(context, 10F),
            dp2px(context, 10F),
            dp2px(context, 10F),
            dp2px(context, 10F)
        )
        val linearlayout_0 = LinearLayout(context)
        val layoutParams_0 = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        linearlayout_0.orientation = LinearLayout.VERTICAL
        val linearlayout_1 = LinearLayout(context)
        val layoutParams_1 = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        linearlayout_1.setPadding(
            dp2px(context, 10F),
            dp2px(context, 10F),
            dp2px(context, 10F),
            dp2px(context, 10F)
        )
        linearlayout_1.orientation = LinearLayout.HORIZONTAL


        val textview_2 = TextView(context)
        val layoutParams_2 = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        textview_2.text = "服务端口："
        textview_2.setTextColor(textColor)
        textview_2.textSize = 16F
        linearlayout_1.addView(textview_2, layoutParams_2)
        val et_port = EditText(context).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            filters = arrayOf(InputFilter.LengthFilter(5))
        }
        val layoutParams_3 = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        et_port.hint = "请输入1024-65535之间的值"
        et_port.setText(SPUtils.getInt("port", 9999).toString())
        et_port.setTextColor(textColor)
        et_port.textSize = 16F
        linearlayout_1.addView(et_port, layoutParams_3)

        linearlayout_0.addView(linearlayout_1, layoutParams_1)
        val linearlayout_4 = LinearLayout(context)
        val layoutParams_4 = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        linearlayout_4.setPadding(
            dp2px(context, 10F),
            dp2px(context, 10F),
            dp2px(context, 10F),
            dp2px(context, 10F)
        )
        linearlayout_4.orientation = LinearLayout.HORIZONTAL
        val textview_5 = TextView(context)
        val layoutParams_5 = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        textview_5.text = "随番茄自动启动服务："
        textview_5.setTextColor(textColor)
        textview_5.textSize = 16F
        linearlayout_4.addView(textview_5, layoutParams_5)
        val s_auto_start = Switch(context).apply {
            isChecked = SPUtils.getBoolean("autoStart", false)
        }
        val layoutParams_6 = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        linearlayout_4.addView(s_auto_start, layoutParams_6)
        linearlayout_0.addView(linearlayout_4, layoutParams_4)
        val linearlayout_7 = LinearLayout(context)
        val layoutParams_7 = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        linearlayout_7.setPadding(
            dp2px(context, 10F),
            dp2px(context, 10F),
            dp2px(context, 10F),
            dp2px(context, 10F)
        )
        linearlayout_7.orientation = LinearLayout.HORIZONTAL
        val textview_8 = TextView(context)
        val layoutParams_8 = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        textview_8.text = "开启服务："
        textview_8.setTextColor(textColor)
        textview_8.textSize = 16F
        linearlayout_7.addView(textview_8, layoutParams_8)
        val s_enable = Switch(context).apply {
            isChecked = httpServer.isAlive
        }
        val layoutParams_9 = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        linearlayout_7.addView(s_enable, layoutParams_9)
        linearlayout_0.addView(linearlayout_7, layoutParams_7)

        val linearlayout_9 = LinearLayout(context)
        val layoutParams_12 = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        linearlayout_9.setPadding(
            dp2px(context, 10F),
            dp2px(context, 10F),
            dp2px(context, 10F),
            dp2px(context, 10F)
        )
        linearlayout_9.orientation = LinearLayout.HORIZONTAL
        val textview_10 = TextView(context)
        val layoutParams_13 = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        textview_10.text = "内网穿透服务(Frp)："
        textview_10.setTextColor(textColor)
        textview_10.textSize = 16F
        linearlayout_9.addView(textview_10, layoutParams_13)
        val s_enable_2 = Switch(context).apply {
            isChecked = SPUtils.getBoolean("traversal", false)
            setOnClickListener {
                if (isChecked) {
                    AlertDialog.Builder(context)
                        .setTitle("内网穿透风险警告和免责声明")
                        .setMessage(
                            Html.fromHtml(
                                XposedHelpers.assetAsByteArray(
                                    moduleRes,
                                    "TraversalDisclaimer.html"
                                ).inputStream().reader().readText()
                            )
                        )
                        .setCancelable(false)
                        .setPositiveButton("我已阅读并同意") { _, _ ->
                            isChecked = true
                        }.setNegativeButton("不同意") { _, _ ->
                            isChecked = false
                        }.show()
                }
            }
        }
        val layoutParams_14 = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        linearlayout_9.addView(s_enable_2, layoutParams_14)
        linearlayout_0.addView(linearlayout_9, layoutParams_12)

        if (SPUtils.getBoolean("traversal", false)) {
            val linearlayout_10 = LinearLayout(context)
            val layoutParams_15 = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            linearlayout_10.setPadding(
                dp2px(context, 10F),
                dp2px(context, 10F),
                dp2px(context, 10F),
                dp2px(context, 10F)
            )
            linearlayout_10.orientation = LinearLayout.HORIZONTAL
            val textview_11 = TextView(context)
            val layoutParams_16 = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            textview_11.text =
                "内网穿透服务状态：${if (frpcServer.isAlive()) "已开启" else "未开启"}"
            textview_11.textSize = 16F
            linearlayout_10.addView(textview_11, layoutParams_16)
            linearlayout_0.addView(linearlayout_10, layoutParams_15)

            //公网地址
            val linearlayout_11 = LinearLayout(context)
            val layoutParams_17 = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            linearlayout_11.setPadding(
                dp2px(context, 10F),
                dp2px(context, 10F),
                dp2px(context, 10F),
                dp2px(context, 10F)
            )
            linearlayout_11.orientation = LinearLayout.HORIZONTAL

            val textview_12 = TextView(context)
            val layoutParams_18 = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            textview_12.text = "公网地址："
            textview_12.setTextColor(textColor)
            textview_12.textSize = 16F
            linearlayout_11.addView(textview_12, layoutParams_18)
            val et_domain = EditText(context).apply {
                isSingleLine = true
            }
            val layoutParams_19 = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            et_domain.setText(SPUtils.getString("publicDomain", "未获取"))
            et_domain.setTextColor(textColor)
            et_domain.textSize = 16F
            linearlayout_11.addView(et_domain, layoutParams_19)

            linearlayout_0.addView(linearlayout_11, layoutParams_17)
        }


        val linearlayout_8 = LinearLayout(context)
        val layoutParams_10 = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        linearlayout_8.setPadding(
            dp2px(context, 10F),
            dp2px(context, 10F),
            dp2px(context, 10F),
            dp2px(context, 10F)
        )
        linearlayout_8.orientation = LinearLayout.HORIZONTAL
        val textview_9 = TextView(context)
        val layoutParams_11 = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        textview_9.text = Html.fromHtml(
            """
            <a href="https://github.com/fengyuecanzhu/FQWeb">Github</a>&nbsp;&nbsp;&nbsp;<a href="https://github.com/fengyuecanzhu/FQWeb#免责声明">免责声明</a>&nbsp;&nbsp;&nbsp;<a href="http://qm.qq.com/cgi-bin/qm/qr?_wv=1027&k=2cgswx48xaTgYmQjSLfH0XNom5n4vm1z&authKey=npnemJO7L6NyLdKvjePLU%2Ffav5v75Q8alXVzCK%2FypGJtFqp1DV35fyukWhhmvTQU&noverify=0&group_code=887847462">QQ群(887847462)</a>
        """.trimIndent()
        )
        textview_9.movementMethod = LinkMovementMethod.getInstance()
        textview_9.textSize = 16F
        linearlayout_8.addView(textview_9, layoutParams_11)
        linearlayout_0.addView(linearlayout_8, layoutParams_10)


        layout_root.addView(linearlayout_0, layoutParams_0)

        AlertDialog.Builder(context)
            .setTitle("番茄Web")
            .setView(layout_root)
            .setCancelable(false)
            .setNegativeButton("取消", null)
            .setPositiveButton("保存设置") { dialog, _ ->
                SPUtils.putBoolean("autoStart", s_auto_start.isChecked)
                val port = et_port.text.toString().toInt()
                if (port !in 1024..65535) {
                    Toast.makeText(context, "端口只能在1024-65535之间", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    SPUtils.putInt("port", port)
                }
                if (s_enable.isChecked) {
                    try {
                        restartServe()
                        settingView.setObjectField(
                            Config.settingItemStrFieldName,
                            "已开启(http://${NetworkUtils.getLocalIPAddress()?.hostAddress ?: "localhost"}:${
                                SPUtils.getInt(
                                    "port",
                                    9999
                                )
                            })"
                        )
                        adapter?.callMethod("notifyItemChanged", 0)
                    } catch (e: Throwable) {
                        log(e)
                        Toast.makeText(context, e.localizedMessage ?: "", Toast.LENGTH_SHORT)
                            .show()
                    }
                } else {
                    httpServer.stop()
                    settingView.setObjectField(Config.settingItemStrFieldName, "未开启")
                    adapter?.callMethod("notifyItemChanged", 0)
                }
                if (s_enable_2.isChecked) {
                    frpcServer.start(true)
                    SPUtils.putBoolean("traversal", true)
                } else {
                    SPUtils.putBoolean("traversal", false)
                    ToastUtils.toast("内网穿透服务配置将在重启应用后生效")
                }
            }.create().show()
    }

    private fun restartServe(port: Int = SPUtils.getInt("port", 9999)) {
        if (httpServer.isAlive) {
            if (httpServer.listeningPort != port) {
                httpServer.stop()
                httpServer = HttpServer(port)
                httpServer.start()
            }
        } else {
            httpServer = HttpServer(port)
            httpServer.start()
        }
    }

    private fun dp2px(context: Context, dipValue: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (dipValue * scale + 0.5f).toInt()
    }

    private fun hookUpdate(classLoader: ClassLoader) {
        val unhook = "com.dragon.read.update.d".replaceMethod(
            classLoader,
            "a",
            Int::class.java,
            "com.ss.android.update.OnUpdateStatusChangedListener"
        ) {}
        if (unhook != null) {
            return
        }
        "com.dragon.read.update.d".replaceMethod(classLoader, "a", Int::class.java) {}
    }
}