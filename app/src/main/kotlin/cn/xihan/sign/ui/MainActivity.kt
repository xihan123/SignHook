package cn.xihan.sign.ui

import android.net.Uri
import android.os.Bundle
import android.os.Environment
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.Checkbox
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import cn.xihan.sign.R
import cn.xihan.sign.base.BaseActivity
import cn.xihan.sign.component.AnywhereDropdown
import cn.xihan.sign.component.ApkInfoItem
import cn.xihan.sign.component.EmptyList
import cn.xihan.sign.component.MyDropdownMenuItem
import cn.xihan.sign.component.Scaffold
import cn.xihan.sign.component.SearchByTextAppBar
import cn.xihan.sign.component.items
import cn.xihan.sign.hook.HookEntry.Companion.optionModel
import cn.xihan.sign.utli.getApkSignature
import cn.xihan.sign.utli.hideAppIcon
import cn.xihan.sign.utli.jumpToPermission
import cn.xihan.sign.utli.rememberMutableStateOf
import cn.xihan.sign.utli.requestPermission
import cn.xihan.sign.utli.restartApplication
import cn.xihan.sign.utli.showAppIcon
import cn.xihan.sign.utli.showSignatureDialog
import cn.xihan.sign.utli.toast
import cn.xihan.sign.utli.writeConfigFile
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.compose.collectAsState
import java.io.File

@OptIn(ExperimentalMaterialApi::class)
@AndroidEntryPoint
class MainActivity : BaseActivity() {

    private val viewModel by viewModels<MainViewModel>()

    private val getContent =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                // uri 转为 File
                val path = it.path?.removePrefix("/document/primary:")
                path?.let { it1 -> File("${Environment.getExternalStorageDirectory().path}/$it1") }
                    ?.let { file ->
                        getApkSignature(file)?.let { signature ->
                            showSignatureDialog(signature)
                        } ?: toast(getString(R.string.get_sign_error))
                    } ?: toast(getString(R.string.get_file_error))
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // TODO: 在模拟器上会无限崩溃 暂时无法解决 如需要可以自己取消注释
        /*
        val getAllApkInfo = OneTimeWorkRequestBuilder<ApkInfoWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        WorkManager.getInstance(this).enqueue(getAllApkInfo)

         */

    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun ComposeContent() {
        val state by viewModel.collectAsState()
        val coroutineScope = rememberCoroutineScope()
        val snackbarHostState = remember { SnackbarHostState() }
        var query by rememberMutableStateOf(value = "")
        val signatureList = state.apkSignatureFlow.collectAsLazyPagingItems()
        val permission = rememberMutableStateOf(
            value = XXPermissions.isGranted(
                this, if (this.applicationInfo.targetSdkVersion > 30) arrayOf(
                    Permission.MANAGE_EXTERNAL_STORAGE, Permission.REQUEST_INSTALL_PACKAGES
                ) else Permission.Group.STORAGE.plus(Permission.REQUEST_INSTALL_PACKAGES)
            )
        )
        val gridState = rememberLazyGridState()
        val topAppBarExpanded = rememberMutableStateOf(value = false)
        val packageName =
            rememberMutableStateOf(value = optionModel.packageNameList.joinToString(";"))
        val scopePackageNameState = rememberMutableStateOf(value = false)
        val refreshState = rememberPullRefreshState(state.refreshing, onRefresh = {
            viewModel.updateSignatureList()
            viewModel.querySignature(query)
        })
        val hideIcon = rememberMutableStateOf(value = optionModel.hideIcon)

        LaunchedEffect(hideIcon.value) {
            if (hideIcon.value) {
                hideAppIcon()
            } else {
                showAppIcon()
            }
        }


        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            state = state,
            onShowSnackbar = {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(it)
                }
            },
            onDismissErrorDialog = { viewModel.hideError() },
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(text = stringResource(id = R.string.app_name))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    actions = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = {
                                coroutineScope.launch {
                                    gridState.animateScrollToItem(0)
                                }
                            }) {
                                Icon(Icons.Filled.KeyboardArrowUp, null)
                            }
                            AnywhereDropdown(
                                expanded = topAppBarExpanded.value,
                                onDismissRequest = { topAppBarExpanded.value = false },
                                onClick = { topAppBarExpanded.value = true },
                                surface = {
                                    IconButton(onClick = {
                                        topAppBarExpanded.value = true
                                    }) {
                                        Icon(
                                            imageVector = Icons.Filled.MoreVert,
                                            contentDescription = null
                                        )
                                    }
                                }
                            ) {
                                MyDropdownMenuItem(
                                    topAppBarExpanded = topAppBarExpanded,
                                    text = { Text(stringResource(id = R.string.scope_package_name)) },
                                    onClick = {
                                        scopePackageNameState.value = true
                                    }
                                )

                                MyDropdownMenuItem(
                                    topAppBarExpanded = topAppBarExpanded,
                                    text = { Text(stringResource(id = R.string.title_select_apk)) },
                                    onClick = {
                                        // 选择后缀为apk的文件
                                        getContent.launch("application/vnd.android.package-archive")
                                    }
                                )

                                MyDropdownMenuItem(
                                    topAppBarExpanded = topAppBarExpanded,
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(stringResource(id = R.string.hide_icon))
                                            Checkbox(
                                                checked = hideIcon.value,
                                                onCheckedChange = {
                                                    hideIcon.value = it
                                                    optionModel.hideIcon = it
                                                    writeConfigFile()
                                                }
                                            )
                                        }
                                    },
                                    onClick = {
                                        topAppBarExpanded.value = true
                                        hideIcon.value = !hideIcon.value
                                        optionModel.hideIcon = hideIcon.value
                                        writeConfigFile()
                                    }
                                )

                                MyDropdownMenuItem(
                                    topAppBarExpanded = topAppBarExpanded,
                                    text = { Text(stringResource(id = R.string.exit)) },
                                    onClick = {
                                        finish()
                                    }
                                )
                            }
                        }
                    },
                    navigationIcon = {},
                    scrollBehavior = null
                )

            },
        ) { _, _ ->

            if (permission.value) {
                Column {
                    SearchByTextAppBar(
                        text = query,
                        onTextChange = {
                            query = it
                            viewModel.querySignature(it)
                        },
                        onClickClear = {
                            query = ""
                            viewModel.querySignature()
                        },
                        onClickSearch = {
                            viewModel.querySignature(query)
                        },
                    )

                    Box(modifier = Modifier.pullRefresh(refreshState)) {

                        if (signatureList.itemCount == 0) {
                            EmptyList(
                                modifier = Modifier.fillMaxWidth(),
                                text = "空空如也",
                                onRetryClick = {
                                    viewModel.updateSignatureList()
                                }
                            )
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(1),
                                contentPadding = PaddingValues(15.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                state = gridState,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(
                                    items = signatureList,
                                    key = { it.packageName },
                                    span = { GridItemSpan(1) }
                                ) { item ->
                                    item?.let {
                                        ApkInfoItem(
                                            apkInfoItem = item,
                                            updateForgedSignatureAction = { packageName, forgedSignature ->
                                                viewModel.updateForgedSignature(
                                                    packageName,
                                                    forgedSignature
                                                )
                                            },
                                            updateForgedAction = { packageName, forged ->
                                                viewModel.updateIsForged(
                                                    packageName,
                                                    forged
                                                )
                                            }
                                        )
                                    }
                                }
                            }

                        }

                        PullRefreshIndicator(
                            state.refreshing, refreshState, Modifier.align(Alignment.TopCenter)
                        )
                    }


                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
                        text = stringResource(id = R.string.permisson_channel_text),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )

                    Button(onClick = {
                        requestPermission(onGranted = {
                            permission.value = true
                            restartApplication()
                        }, onDenied = {
                            permission.value = false
                            jumpToPermission()
                        })
                    }) {
                        Text(stringResource(id = R.string.request_permission))
                    }
                }
            }


        }
        if (scopePackageNameState.value) {
            AlertDialog(
                onDismissRequest = {
                    scopePackageNameState.value = false
                },
                title = {
                    Text(text = stringResource(id = R.string.scope_package_name))
                },
                text = {
                    Column {
                        Text(text = stringResource(id = R.string.fake_signature_tip_text))
                        TextField(
                            value = packageName.value,
                            onValueChange = packageName::value::set,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                },
                confirmButton = {
                    Button(onClick = {
                        optionModel.packageNameList = packageName.value.split(";")
                        scopePackageNameState.value = false
                        writeConfigFile()
                    }) {
                        Text(stringResource(id = android.R.string.ok))
                    }
                },
                dismissButton = {
                    Button(onClick = {
                        scopePackageNameState.value = false
                    }) {
                        Text(stringResource(id = android.R.string.cancel))
                    }
                }
            )

        }
    }
}
