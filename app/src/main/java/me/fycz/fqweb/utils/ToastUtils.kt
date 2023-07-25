package me.fycz.fqweb.utils

import android.os.Handler
import android.os.Looper
import android.widget.Toast

/**
 * @author fengyue
 * @date 2023/7/25 10:12
 * @description
 */
object ToastUtils {

    private val handle = Handler(Looper.getMainLooper())

    fun toast(msg: String) {
        handle.post {
            Toast.makeText(GlobalApp.application, msg, Toast.LENGTH_SHORT).show()
        }
    }

    fun toastLong(msg: String) {
        handle.post {
            Toast.makeText(GlobalApp.application, msg, Toast.LENGTH_LONG).show()
        }
    }
}