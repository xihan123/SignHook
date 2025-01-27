package cn.xihan.signhook.model


import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class PageableModel<T>(
    @SerialName("content")
    var content: List<T> = listOf(),
    @SerialName("empty")
    var empty: Boolean = false,
    @SerialName("first")
    var first: Boolean = false,
    @SerialName("last")
    var last: Boolean = false,
    @SerialName("number")
    var number: Int = 0,
    @SerialName("numberOfElements")
    var numberOfElements: Int = 0,
    @SerialName("pageable")
    var pageable: PageableModel = PageableModel(),
    @SerialName("size")
    var size: Int = 0,
    @SerialName("sort")
    var sort: SortModel = SortModel(),
    @SerialName("totalElements")
    var totalElements: Int = 0,
    @SerialName("totalPages")
    var totalPages: Int = 0
) {

    @Keep
    @Serializable
    data class PageableModel(
        @SerialName("offset")
        var offset: Int = 0,
        @SerialName("pageNumber")
        var pageNumber: Int = 0,
        @SerialName("pageSize")
        var pageSize: Int = 0,
        @SerialName("paged")
        var paged: Boolean = false,
        @SerialName("sort")
        var sort: SortModel = SortModel(),
        @SerialName("unpaged")
        var unpaged: Boolean = false
    ) {
        @Keep
        @Serializable
        data class SortModel(
            @SerialName("empty")
            var empty: Boolean = false,
            @SerialName("sorted")
            var sorted: Boolean = false,
            @SerialName("unsorted")
            var unsorted: Boolean = false
        )
    }

    @Keep
    @Serializable
    data class SortModel(
        @SerialName("empty")
        var empty: Boolean = false,
        @SerialName("sorted")
        var sorted: Boolean = false,
        @SerialName("unsorted")
        var unsorted: Boolean = false
    )
}