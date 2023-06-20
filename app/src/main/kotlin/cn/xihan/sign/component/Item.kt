package cn.xihan.sign.component

import android.annotation.SuppressLint
import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.compose.LazyPagingItems
import cn.xihan.sign.R
import cn.xihan.sign.base.IUiState
import cn.xihan.sign.model.ApkSignature
import cn.xihan.sign.utli.copyToClipboard
import cn.xihan.sign.utli.rememberMutableStateOf

/**
 * @项目名 : 签名助手
 * @作者 : MissYang
 * @创建时间 : 2023/6/19 20:43
 * @介绍 :
 */
@Composable
fun <VS : IUiState> Scaffold(
    state: VS,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = SnackbarHostState(),
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    containerColor: Color = MaterialTheme.colorScheme.background,
    contentColor: Color = contentColorFor(containerColor),
    onShowSnackbar: (message: String) -> Unit = {},
    onErrorPositiveAction: (value: Any?) -> Unit = { _ -> },
    onErrorNegativeAction: (value: Any?) -> Unit = { _ -> },
    onDismissErrorDialog: () -> Unit = {},
    content: @Composable (PaddingValues, VS) -> Unit,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = topBar,
        bottomBar = bottomBar,
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.navigationBarsPadding(),
            )
        },
        floatingActionButton = floatingActionButton,
        floatingActionButtonPosition = floatingActionButtonPosition,
        containerColor = containerColor,
        contentColor = contentColor,
    ) { paddingValues ->
        HandleError(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            state = state,
            onShowSnackBar = onShowSnackbar,
            onPositiveAction = onErrorPositiveAction,
            onNegativeAction = onErrorNegativeAction,
            onDismissErrorDialog = onDismissErrorDialog,
        ) { viewState ->
            content.invoke(paddingValues, viewState)
        }
    }
}

@Composable
fun <VS : IUiState> HandleError(
    state: VS,
    modifier: Modifier = Modifier,
    onShowSnackBar: (message: String) -> Unit = {},
    onPositiveAction: (value: Any?) -> Unit = { _ -> },
    onNegativeAction: (value: Any?) -> Unit = { _ -> },
    onDismissErrorDialog: () -> Unit = {},
    content: @Composable (VS) -> Unit,
) {
    Box(modifier = modifier) {
        content(state)

        if (state.loading) {
            FullScreenLoading()
        }

        if (state.error != null) {
            HandleError(
                error = state.error!!, // Not null
                onShowSnackBar = onShowSnackBar,
                onDismissRequest = onDismissErrorDialog,
            )
        }
    }

}


@Composable
fun FullScreenLoading(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.then(
            Modifier
                .fillMaxSize()
                .background(Color.Transparent)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {},
                ),
        ), contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.background(Color.Transparent),
            strokeWidth = 5.dp,
        )
    }
}

@Composable
fun HandleError(
    error: Throwable,
    context: Context = LocalContext.current,
    onShowSnackBar: (message: String) -> Unit = {},
    onDismissRequest: () -> Unit = {},
) {
    LaunchedEffect(key1 = true) {
        onShowSnackBar.invoke(error.message ?: context.getString(R.string.unknown_error))
        onDismissRequest.invoke()
    }
}

@Composable
fun SearchByTextAppBar(
    modifier: Modifier = Modifier,
    text: String = "",
    onTextChange: (String) -> Unit = {},
    onClickClear: () -> Unit = {},
    onClickSearch: () -> Unit = {},
) {
    TextField(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 10.dp, end = 10.dp, top = 10.dp),
        value = text,
        onValueChange = onTextChange,
        singleLine = true,
        shape = RoundedCornerShape(30.dp),
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
        ),
        leadingIcon = {
//            IconButton(onClick = onClickBack) {
//                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null)
//            }
        },
        trailingIcon = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (text.isNotBlank()) {
                    IconButton(onClick = onClickClear) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = null)
                    }
                }

                IconButton(onClick = onClickSearch) {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null)
                }
            }

        },
        placeholder = {

        },
    )
}

@Composable
fun EmptyList(
    text: String,
    modifier: Modifier = Modifier,
    onRetryClick: () -> Unit = {},
) {
    Box(
        modifier = modifier
            .height(60.dp)
            .clickable {
                onRetryClick.invoke()
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
        )
    }
}

//@Preview
@Composable
fun ApkInfoItem(
    modifier: Modifier = Modifier,
    apkInfoItem: ApkSignature = ApkSignature(
        apkName = "应用名称",
        packageName = "包名",
        originalSignature = "原始签名",
        forgedSignature = "伪造签名",
        isForged = false,
    ),
    context: Context = LocalContext.current,
    updateForgedSignatureAction: (packageName: String, forgedSignature: String) -> Unit = { _, _ -> },
    updateForgedAction: (packageName: String, forged: Boolean) -> Unit = { _, _ -> }
) {
    val openDialog = rememberMutableStateOf(value = false)
    val forgedSignature = rememberMutableStateOf(value = apkInfoItem.forgedSignature)
    val isForged = rememberMutableStateOf(value = apkInfoItem.isForged)
    val state = rememberMutableStateOf(value = false)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(10.dp)
            .clickable {
                state.value = !state.value
            },
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp,
        ),
    ) {
        Column {

            TwoText(
                title = stringResource(id = R.string.title_app_name),
                apkInfoItem.apkName,
                icon = {
                    Checkbox(
                        checked = isForged.value,
                        onCheckedChange = {
                            isForged.value = it
                            updateForgedAction.invoke(apkInfoItem.packageName, it)
                        }
                    )
                })


            TwoText(
                title = stringResource(id = R.string.title_package_name),
                apkInfoItem.packageName
            )

            AnimatedVisibility(visible = state.value) {
                Column {
                    TwoText(
                        title = stringResource(id = R.string.title_original_signature),
                        apkInfoItem.originalSignature,
                        icon = {
                            IconButton(onClick = {
                                context.copyToClipboard(apkInfoItem.originalSignature)
                            }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.content_copy),
                                    contentDescription = null
                                )
                            }
                        }
                    )

                    TwoText(
                        title = stringResource(id = R.string.title_fake_signature),
                        content = forgedSignature.value,
                        icon = {
                            Row(
                                modifier = Modifier
                                    .wrapContentWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceAround,
                                verticalAlignment = Alignment.CenterVertically,
                            )
                            {
                                IconButton(onClick = {
                                    openDialog.value = true
                                }) {
                                    Icon(
                                        imageVector = Icons.Filled.Create,
                                        contentDescription = null
                                    )
                                }
                                IconButton(onClick = {
                                    context.copyToClipboard(apkInfoItem.forgedSignature)
                                }) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.content_copy),
                                        contentDescription = null
                                    )
                                }
                            }
                        }
                    )
                }
            }


        }

        if (openDialog.value) {
            AlertDialog(
                onDismissRequest = {
                    // 当用户点击对话框以外的地方或者按下系统返回键将会执行的代码
                    openDialog.value = false
                },
                title = {
                    Text(
                        text = stringResource(id = R.string.fake_signature_tip),
                        fontWeight = FontWeight.W700,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight(1f)
                            .padding(16.dp),
                    ) {
                        Text(
                            text = stringResource(id = R.string.fake_signature_tip),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.W500,
                        )

                        Text(text = stringResource(id = R.string.fake_signature_tip_text))

                        TextField(
                            value = forgedSignature.value,
                            onValueChange = forgedSignature::value::set,
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(top = 10.dp),
                        )


                    }


                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            openDialog.value = false
                            updateForgedSignatureAction.invoke(
                                apkInfoItem.packageName,
                                forgedSignature.value
                            )
                        },
                    ) {
                        Text(
                            text = stringResource(id = android.R.string.ok),
                            fontWeight = FontWeight.W700,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                dismissButton = {
                    Row(
                        modifier = Modifier
                            .wrapContentWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(
                            onClick = {
                                openDialog.value = false
                                forgedSignature.value = ""
                                updateForgedSignatureAction.invoke(
                                    apkInfoItem.packageName,
                                    forgedSignature.value
                                )
                            }
                        ) {
                            Text(
                                stringResource(id = R.string.clear),
                                fontWeight = FontWeight.W700,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        TextButton(
                            onClick = {
                                openDialog.value = false
                            }
                        ) {
                            Text(
                                stringResource(id = android.R.string.cancel),
                                fontWeight = FontWeight.W700,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                },
                modifier = Modifier
                    .padding(10.dp)
            )
        }

    }
}

/**
 * 横向双文本模型
 */
@Composable
fun TwoText(
    title: String,
    content: String,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit = {}
) {
    var state by rememberMutableStateOf(value = true)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = TextStyle.Default.copy(
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
        )
        Text(
            text = content,
            color = MaterialTheme.colorScheme.onSurface,
            overflow = TextOverflow.Ellipsis,
            maxLines = if (state) 2 else Int.MAX_VALUE,
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp)
                .clickable {
                    state = !state
                },

            )

        icon.invoke()
    }
}


inline fun <T : Any> LazyGridScope.items(
    items: LazyPagingItems<T>,
    noinline key: ((item: T) -> Any)? = null,
    noinline span: (LazyGridItemSpanScope.(item: T?) -> GridItemSpan)? = null,
    noinline contentType: (item: T?) -> Any? = { null },
    crossinline itemContent: @Composable LazyGridItemScope.(item: T?) -> Unit
) {
    items(
        count = items.itemCount,
        key = if (key == null) null else { index ->
            val item = items.peek(index)
            if (item == null) {
                MyPagingPlaceholderKey(index)
            } else {
                key(item)
            }
        },
        span = if (span != null) {
            { span(items[it]) }
        } else null,
        contentType = { index: Int -> contentType(items[index]) }
    ) { index ->
        itemContent(items[index])
    }
}

@SuppressLint("BanParcelableUsage")
data class MyPagingPlaceholderKey(private val index: Int) : Parcelable {
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(index)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object {
        @Suppress("unused")
        @JvmField
        val CREATOR: Parcelable.Creator<MyPagingPlaceholderKey> =
            object : Parcelable.Creator<MyPagingPlaceholderKey> {
                override fun createFromParcel(parcel: Parcel) =
                    MyPagingPlaceholderKey(parcel.readInt())

                override fun newArray(size: Int) =
                    arrayOfNulls<MyPagingPlaceholderKey?>(size)
            }
    }
}