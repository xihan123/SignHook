package cn.xihan.signhook.util


import cn.xihan.signhook.model.LoginRequest
import cn.xihan.signhook.model.PackageNameAndSignatureModel
import cn.xihan.signhook.model.PageableModel
import cn.xihan.signhook.model.RegisterRequest
import cn.xihan.signhook.model.SaResult
import cn.xihan.signhook.model.TitleAndPackageNameModel
import de.jensklingenberg.ktorfit.http.Body
import de.jensklingenberg.ktorfit.http.DELETE
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Headers
import de.jensklingenberg.ktorfit.http.POST
import de.jensklingenberg.ktorfit.http.Query

/**
 * @项目名 : SignHook
 * @作者 : MissYang
 * @创建时间 : 2025/1/26 15:31
 * @介绍 :
 */
interface ApiService {


    @Headers("Content-Type: application/json")
    @POST("v1/user/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): SaResult<String>

    @Headers("Content-Type: application/json")
    @POST("v1/user/login")
    suspend fun login(
        @Body request: LoginRequest
    ): SaResult<String>

    @GET("v1/user/logout")
    suspend fun logout(): SaResult<String>

    @GET("v1/user/checkDialogStatus")
    suspend fun checkDialogStatus(): SaResult<Int>

    @POST("v1/user/updateDialogStatus")
    suspend fun updateDialogStatus(
        @Query("dialogStatus") dialogStatus: Int
    ): SaResult<String>

    @GET("v1/user/packages")
    suspend fun getUserPackages(): SaResult<ArrayList<PackageNameAndSignatureModel>>

    @Headers("Content-Type: application/text")
    @POST("v1/user/packages")
    suspend fun submitPackages(
        @Body packageName: String
    ): SaResult<String>

    @Headers("Content-Type: application/text")
    @DELETE("v1/user/packages")
    suspend fun deletePackage(
        @Body packageName: String
    ): SaResult<String>

    @GET("v1/packages/search")
    suspend fun searchPackages(
        @Query("keyword") keyword: String?,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): SaResult<PageableModel<TitleAndPackageNameModel>>

}