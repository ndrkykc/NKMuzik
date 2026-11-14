package com.nkapps.nkmuzik.utils

import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class NewPipeDownloader : Downloader() {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    override fun execute(request: Request): Response {
        val httpMethod = request.httpMethod()
        val url = request.url()
        val headers = request.headers()
        val dataToSend = request.dataToSend()

        val reqBody = dataToSend?.toRequestBody(null)

        val reqBuilder = okhttp3.Request.Builder()
            .url(url)
            
        if (httpMethod == "GET") {
            // GET metodu parametresiz çağrılır
            reqBuilder.method("GET", null)
        } else if (reqBody != null) {
            reqBuilder.method(httpMethod, reqBody)
        }

        headers.forEach { (key, valueList) ->
            (valueList as? List<String>)?.forEach { value ->
                reqBuilder.addHeader(key, value)
            }
        }

        val okResponse = client.newCall(reqBuilder.build()).execute()

        val responseHeaders = mutableMapOf<String, List<String>>()
        okResponse.headers.names().forEach { name ->
            responseHeaders[name] = okResponse.headers.values(name)
        }

        val body = okResponse.body?.string() ?: ""

        return Response(
            okResponse.code,
            okResponse.message,
            responseHeaders,
            body,
            request.url()
        )
    }
}