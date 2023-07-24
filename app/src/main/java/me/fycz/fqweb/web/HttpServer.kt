package me.fycz.fqweb.web

import android.graphics.Bitmap
import de.robv.android.xposed.XposedHelpers
import fi.iki.elonen.NanoHTTPD
import me.fycz.fqweb.MainHook.Companion.moduleRes
import me.fycz.fqweb.utils.JsonUtils
import me.fycz.fqweb.web.controller.DragonController
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * @author fengyue
 * @date 2023/5/29 17:58
 * @description
 */
class HttpServer(port: Int) : NanoHTTPD(port) {
    override fun serve(session: IHTTPSession): Response {
        var returnData: ReturnData? = null
        val ct = ContentType(session.headers["content-type"]).tryUTF8()
        session.headers["content-type"] = ct.contentTypeHeader
        val uri = session.uri
        try {
            if (session.method == Method.GET) {
                val parameters = session.parameters
                returnData = when {
                    uri.endsWith("/search") -> DragonController.search(parameters)
                    uri.endsWith("/info") -> DragonController.info(parameters)
                    uri.endsWith("/catalog") -> DragonController.catalog(parameters)
                    uri.endsWith("/content") -> DragonController.content(parameters)
                    uri.endsWith("/reading/bookapi/bookmall/cell/change/v1/") -> DragonController.bookMall(parameters)
                    uri.endsWith("/reading/bookapi/new_category/landing/v/") -> DragonController.newCategory(parameters)
                    else -> null
                }
            }/* else if (session.method == Method.POST) {
                val parameters = session.parameters
                val files = HashMap<String, String>()
                session.parseBody(files)
                val postBody = files["postData"]
            }*/
            if (returnData == null) {
                return newChunkedResponse(
                    Response.Status.NOT_FOUND,
                    MIME_HTML,
                    XposedHelpers.assetAsByteArray(moduleRes, "404.html").inputStream()
                )
            }
            val response = if (returnData.data is Bitmap) {
                val outputStream = ByteArrayOutputStream()
                (returnData.data as Bitmap).compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                val byteArray = outputStream.toByteArray()
                outputStream.close()
                val inputStream = ByteArrayInputStream(byteArray)
                newFixedLengthResponse(
                    Response.Status.OK,
                    "image/png",
                    inputStream,
                    byteArray.size.toLong()
                )
            } else {
                newFixedLengthResponse(JsonUtils.toJson(returnData))
            }
            response.addHeader("Access-Control-Allow-Methods", "GET, POST")
            response.addHeader("Access-Control-Allow-Origin", session.headers["origin"])
            return response
        } catch (e: Exception) {
            return newFixedLengthResponse(e.stackTraceToString())
        }
    }
}