package cn.xihan.signhook.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import cn.xihan.signhook.model.AlertDialogModel
import cn.xihan.signhook.util.AgeException
import cn.xihan.signhook.util.IUiState

/**
 * @项目名 : SignHook
 * @作者 : MissYang
 * @创建时间 : 2025/1/26 18:07
 * @介绍 :
 */
@Composable
fun <VS : IUiState> SignScaffold(
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
    onRefresh: () -> Unit = {},
    content: @Composable (PaddingValues, VS) -> Unit,
) {
    Scaffold(
        modifier = modifier,
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
//                .padding(paddingValues)
//                .padding(top = paddingValues.calculateTopPadding())
            ,
            state = state,
            onShowSnackBar = onShowSnackbar,
            onPositiveAction = onErrorPositiveAction,
            onNegativeAction = onErrorNegativeAction,
            onDismissErrorDialog = onDismissErrorDialog,
            onRefresh = onRefresh
        ) { viewState ->
            content.invoke(paddingValues, viewState)
        }
    }
}


@OptIn(ExperimentalMaterialApi::class)
@Composable
fun <VS : IUiState> HandleError(
    state: VS,
    modifier: Modifier = Modifier,
    onShowSnackBar: (message: String) -> Unit = {},
    onPositiveAction: (value: Any?) -> Unit = { _ -> },
    onNegativeAction: (value: Any?) -> Unit = { _ -> },
    onDismissErrorDialog: () -> Unit = {},
    onRefresh: () -> Unit = {},
    content: @Composable (VS) -> Unit,
) {
    val pullRefreshState = rememberPullRefreshState(
        refreshing = state.refreshing,
//        refreshingOffset = 124.dp,
        onRefresh = onRefresh
    )
    Box(
        modifier = modifier.pullRefresh(pullRefreshState)
    ) {

        if (state.loading) {
            FullScreenLoading()
        } else {
            content(state)
        }
        if (state.error != null) {
            HandleError(
                error = state.error!!, // Not null
                onPositiveAction = onPositiveAction,
                onNegativeAction = onNegativeAction,
                onShowSnackBar = onShowSnackBar,
                onDismissRequest = onDismissErrorDialog,
            )
        }

        PullRefreshIndicator(
            modifier = Modifier.align(Alignment.TopCenter),
            refreshing = state.refreshing,
            state = pullRefreshState,
            contentColor = MaterialTheme.colorScheme.onSurface,
        )
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


/**
 * 错误模型
 * @param errorMessage 错误信息
 * @param onRetryClick 点击事件
 */
@Composable
fun ErrorItem(
    modifier: Modifier = Modifier,
    errorMessage: String?, onRetryClick: () -> Unit = {},
) {
    Box(modifier = modifier
        .padding(16.dp)
        .clickable {
            onRetryClick()
        }) {
        Text(
            text = errorMessage ?: "未知错误",
            modifier = Modifier
                .padding(top = 8.dp)
                .align(Alignment.Center),
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun HandleError(
    error: AgeException,
    onPositiveAction: (value: Any?) -> Unit = { _ -> },
    onNegativeAction: (value: Any?) -> Unit = { _ -> },
    onShowSnackBar: (message: String) -> Unit = {},
    onDismissRequest: () -> Unit = {},
) {
    val context = LocalContext.current

    when (error) {
        is AgeException.AlertException -> {
            AlertDialogItem(
                alertDialogModel = error.alertDialogModel,
                onDismissRequest = onDismissRequest,
                positiveAction = onPositiveAction,
                negativeAction = onNegativeAction,
            )
        }

        is AgeException.InlineException -> {
            ErrorItem(modifier = Modifier
                .wrapContentSize()
                .padding(8.dp),
                errorMessage = "${error.type.name} ${error.message}",
                onRetryClick = {
                    onPositiveAction.invoke(null)
                })
        }

        else -> {
            LaunchedEffect(key1 = true) {
                onShowSnackBar.invoke(error.message ?: "未知错误")
                onDismissRequest.invoke()
            }
        }
    }
}

@Composable
fun AlertDialogItem(
    alertDialogModel: AlertDialogModel,
    positiveAction: (value: Any?) -> Unit = { _ -> },
    negativeAction: (value: Any?) -> Unit = { _ -> },
    onDismissRequest: () -> Unit = {},
) {
    AlertDialog(
        modifier = Modifier
            .wrapContentSize()
            .zIndex(1f),
        onDismissRequest = onDismissRequest,
        title = {
            Text(text = alertDialogModel.title)
        },
        text = {
            Text(text = alertDialogModel.message)
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismissRequest.invoke()
                    positiveAction.invoke(alertDialogModel.positiveObject)
                },
            ) {
                Text(
                    text = alertDialogModel.positiveMessage
                        ?: stringResource(id = android.R.string.ok)
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismissRequest.invoke()
                    negativeAction.invoke(alertDialogModel.positiveObject)
                },
            ) {
                Text(
                    text = alertDialogModel.negativeMessage
                        ?: stringResource(id = android.R.string.cancel)
                )
            }
        },
    )
}