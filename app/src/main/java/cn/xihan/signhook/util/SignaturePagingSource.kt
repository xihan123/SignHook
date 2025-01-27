package cn.xihan.signhook.util

import androidx.paging.PagingSource
import androidx.paging.PagingState
import cn.xihan.signhook.model.TitleAndPackageNameModel

/**
 * @项目名 : SignHook
 * @作者 : MissYang
 * @创建时间 : 2025/1/26 22:07
 * @介绍 :
 */
class SignaturePagingSource(
    private val apiService: ApiService,
    private val keyword: String?
) : PagingSource<Int, TitleAndPackageNameModel>() {
    override fun getRefreshKey(state: PagingState<Int, TitleAndPackageNameModel>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, TitleAndPackageNameModel> {
        return try {
            val page = params.key ?: 0  // 服务端页码从0开始
            val pageSize = params.loadSize
            val result = apiService.searchPackages(keyword = keyword, page = page, size = pageSize)
            val pageableModel =
                result.data ?: return LoadResult.Error(Exception("result.data is null"))
            LoadResult.Page(
                data = pageableModel.content,
                prevKey = if (page > 0) page - 1 else null,
                nextKey = if (page < pageableModel.totalPages - 1) page + 1 else null
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

}