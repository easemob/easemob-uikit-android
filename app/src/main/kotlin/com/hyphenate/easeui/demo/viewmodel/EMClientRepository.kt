package com.hyphenate.easeui.demo.viewmodel

import com.hyphenate.easeui.ChatUIKitClient
import com.hyphenate.easeui.common.ChatClient
import com.hyphenate.easeui.common.ChatError
import com.hyphenate.easeui.common.ChatException
import com.hyphenate.easeui.common.ChatHttpClientManager
import com.hyphenate.easeui.common.ChatLog
import com.hyphenate.easeui.common.ChatValueCallback
import com.hyphenate.easeui.demo.BuildConfig
import com.hyphenate.easeui.demo.DemoApplication
import com.hyphenate.easeui.demo.R
import com.hyphenate.easeui.demo.bean.LoginResult
import com.hyphenate.easeui.model.ChatUIKitProfile
import com.hyphenate.easeui.model.ChatUIKitUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * 作为EMClient的repository,处理EMClient相关的逻辑
 */
class EMClientRepository {
    /**
     * 注册
     * @param userName
     * @param pwd
     * @return
     */
    suspend fun registerToHx(userName: String?, pwd: String?): String? =
        withContext(Dispatchers.IO) {
            suspendCoroutine { continuation ->
                try {
                    ChatClient.getInstance().createAccount(userName, pwd)
                    continuation.resume(userName)
                } catch (e: ChatException) {
                    continuation.resumeWithException(ChatException(e.errorCode, e.message))
                }
            }
        }

    /**
     * 登录到服务器。
     * @param userName 用户名
     * @param pwd 登录凭证。当 [isTokenFlag] 为 `true` 时该值为 chat token；为 `false` 时为账号密码，
     *            此时参考 emclient-linux 中 `EMConfigManager::fetchTokenForUser` 先用密码换取 token，再用 token 登录。
     * @param isTokenFlag 是否直接使用 token 登录
     * @return
     */
    suspend fun loginToServer(
        userName: String,
        pwd: String,
        isTokenFlag: Boolean
    ): ChatUIKitUser =
        withContext(Dispatchers.IO) {
            // 密码登录场景：先通过 {restBaseUrl}/token 用密码换取 chat token（参考 fetchTokenForUser），
            // 不再把原始密码传给 SDK，登录统一走 token 通道。
            val token = if (isTokenFlag) pwd else fetchTokenForUser(userName, pwd)
            suspendCoroutine { continuation ->
                ChatUIKitClient.login(ChatUIKitProfile(userName), token, onSuccess = {
                    successForCallBack(continuation)
                }, onError = { code, error ->
                    continuation.resumeWithException(ChatException(code, error))
                })
            }
        }

    /**
     * 使用用户名 + 密码换取 chat token。
     *
     * 该实现参考 emclient-linux 中 `EMConfigManager::fetchTokenForUser`：
     * 向 `{restBaseUrl}/token` 发起 POST 请求，body 为 `grant_type=password`，
     * 成功后从响应 JSON 中读取 `access_token`。
     *
     * 注意：此方式需要客户端持有原始密码并直接访问 chat REST 服务，仅适用于示例/测试场景；
     * 生产环境推荐由 app server 代理完成密码到 token 的换取（见 [loginFromServe]）。
     */
    private fun fetchTokenForUser(userName: String, password: String): String {
        if (userName.isEmpty() || password.isEmpty()) {
            throw ChatException(ChatError.INVALID_PARAM, "username or password is empty")
        }
        val baseUrl = ChatClient.getInstance().chatConfigPrivate?.getBaseUrl(true, false)
        if (baseUrl.isNullOrEmpty()) {
            throw ChatException(ChatError.SERVER_NOT_REACHABLE, "no matching rest base url")
        }
        val url = "$baseUrl/token"
        ChatLog.d("fetchTokenForUser url : ", url)
        try {
            val headers: MutableMap<String, String> = HashMap()
            headers["Content-Type"] = "application/json"
            val request = JSONObject()
            request.putOpt("grant_type", "password")
            request.putOpt("username", userName)
            request.putOpt("password", password)
            val response = ChatHttpClientManager.httpExecute(
                url,
                headers,
                request.toString(),
                ChatHttpClientManager.Method_POST
            )
            val code = response.code
            val responseInfo = response.content
            when (code) {
                200 -> {
                    val token = JSONObject(responseInfo).optString("access_token")
                    if (token.isNullOrEmpty()) {
                        throw ChatException(ChatError.SERVER_UNKNOWN_ERROR, "access_token is empty")
                    }
                    return token
                }
                400 -> throw ChatException(ChatError.USER_AUTHENTICATION_FAILED, responseInfo)
                404 -> throw ChatException(ChatError.USER_NOT_FOUND, responseInfo)
                else -> throw ChatException(ChatError.SERVER_NOT_REACHABLE, responseInfo)
            }
        } catch (e: ChatException) {
            throw e
        } catch (e: Exception) {
            throw ChatException(ChatError.NETWORK_ERROR, e.message)
        }
    }

    /**
     * 退出登录
     * @param unbindDeviceToken
     * @return
     */
    suspend fun logout(unbindDeviceToken: Boolean): Int =
        withContext(Dispatchers.IO) {
            suspendCoroutine { continuation ->
                ChatUIKitClient.logout(unbindDeviceToken, onSuccess = {
                    continuation.resume(ChatError.EM_NO_ERROR)
                }, onError = { code, error ->
                    continuation.resumeWithException(ChatException(code, error))
                })
            }
        }

    private fun successForCallBack(continuation: Continuation<ChatUIKitUser>) {
        // get current user id
        val currentUser = ChatClient.getInstance().currentUser
        val user = ChatUIKitUser(currentUser)
        continuation.resume(user)
    }

    suspend fun loginFromServe(userName: String, userPassword: String): LoginResult =
        withContext(Dispatchers.IO) {
            suspendCoroutine { continuation ->
                LoginFromAppServe(userName, userPassword, object : ChatValueCallback<LoginResult> {
                    override fun onSuccess(value: LoginResult?) {
                        continuation.resume(value!!)
                    }

                    override fun onError(code: Int, error: String?) {
                        continuation.resumeWithException(ChatException(code, error))
                    }
                })
            }
        }

    private fun LoginFromAppServe(
        userName: String,
        userPassword: String,
        callBack: ChatValueCallback<LoginResult>
    ) {
        try {
            val headers: MutableMap<String, String> = HashMap()
            headers["Content-Type"] = "application/json"
            val request = JSONObject()
            request.putOpt("phoneNumber", userName)
            request.putOpt("smsCode", userPassword)
            val url: String =
                BuildConfig.APP_SERVER_PROTOCOL + "://" + BuildConfig.APP_SERVER_DOMAIN + BuildConfig.APP_BASE_USER + BuildConfig.APP_SERVER_LOGIN
            ChatLog.d("LoginToAppServer url : ", url)
            val response = ChatHttpClientManager.httpExecute(
                url,
                headers,
                request.toString(),
                ChatHttpClientManager.Method_POST
            )
            val code = response.code
            val responseInfo = response.content
            if (code == 200) {
                ChatLog.d("LoginToAppServer success : ", responseInfo)
                val `object` = JSONObject(responseInfo)
                val result = LoginResult()
                val phoneNumber = `object`.getString("phoneNumber")
                result.phone = phoneNumber
                result.token = `object`.getString("token")
                result.username = `object`.getString("chatUserName")
                result.code = code
                callBack.onSuccess(result)
            } else {
                if (responseInfo != null && responseInfo.length > 0) {
                    var errorInfo: String? = null
                    try {
                        val `object` = JSONObject(responseInfo)
                        errorInfo = `object`.getString("errorInfo")
                        if (errorInfo.contains("phone number illegal")) {
                            errorInfo = DemoApplication.getInstance().getString(R.string.em_login_phone_illegal)
                        } else if (errorInfo.contains("verification code error") || errorInfo.contains(
                                "send SMS to get mobile phone verification code"
                            )
                        ) {
                            errorInfo = DemoApplication.getInstance().getString(R.string.em_login_illegal_code)
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                        errorInfo = responseInfo
                    }
                    callBack.onError(code, errorInfo)
                } else {
                    callBack.onError(code, responseInfo)
                }
            }
        } catch (e: Exception) {
            callBack.onError(ChatError.NETWORK_ERROR, e.message)
        }
    }
}
