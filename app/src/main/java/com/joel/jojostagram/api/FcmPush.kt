package com.joel.jojostagram.api

import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.joel.jojostagram.model.PushDTO
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class FcmPush {
    // JSON 세팅
    private val json = "application/json; charset=utf-8".toMediaType()
    // FCM Legacy HTTP URL
    private val url = "https://fcm.googleapis.com/fcm/send"
    // FCM Server Key
    private val serverKey = "AAAAtdSrkgE:APA91bHQISZQvakE03KEThwNhHRZlKL7gkh4UPe2rdH7nTz_Zwlkk17l_IutXk6p7iDiW3GQR0AGnHiJZxVXuHh3PgfoL9gYqU-M6IT2YJdBKLzdZSFPrYW5DgMSe0JL8_AYA8caGm0G"
    private var gson: Gson? = null
    private var okHttpClient: OkHttpClient? = null

    companion object {
        var FcmPushInstance = FcmPush()
    }

    init {
        gson = Gson()
        okHttpClient = OkHttpClient()
    }

    // 푸시 메시지 발송
    fun sendMessage(destinationUid: String, title: String, message: String) {
        FirebaseFirestore.getInstance().collection("pushToken").document(destinationUid).get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result?.get("pushToken").toString()

                val pushDTO = PushDTO()
                pushDTO.to = token
                pushDTO.notification?.title = title
                pushDTO.notification?.body = message

                if (gson == null) return@addOnCompleteListener

                val body = gson!!.toJson(pushDTO).toRequestBody(json)
                val request = Request
                    .Builder()
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", "key=$serverKey")
                    .url(url)
                    .post(body)
                    .build()
                okHttpClient?.newCall(request)?.enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        e.printStackTrace()
                    }
                    override fun onResponse(call: Call, response: Response) {
                        println(response.body?.string())
                    }
                })
            }
        }
    }
}