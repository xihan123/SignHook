@file:OptIn(
    androidx.compose.material.ExperimentalMaterialApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package cn.xihan.sign.ui

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import cn.xihan.sign.R
import cn.xihan.sign.component.AnywhereDropdown
import cn.xihan.sign.component.ApkInfoItem
import cn.xihan.sign.component.EmptyList
import cn.xihan.sign.component.MyDropdownMenuItem
import cn.xihan.sign.component.Scaffold
import cn.xihan.sign.component.SearchByTextAppBar
import cn.xihan.sign.component.items
import cn.xihan.sign.utli.defaultScopeSet
import cn.xihan.sign.utli.getApkSignature
import cn.xihan.sign.utli.hideAppIcon
import cn.xihan.sign.utli.rememberMutableStateOf
import cn.xihan.sign.utli.showAppIcon
import cn.xihan.sign.utli.showSignatureDialog
import cn.xihan.sign.utli.toast
import com.highcapable.yukihookapi.hook.factory.prefs
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.orbitmvi.orbit.compose.collectAsState
import java.io.File

class MainActivity : AppCompatActivity() {

    private val viewModel by viewModel<MainViewModel>()

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
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            STheme {
                ComposeContent()
            }
        }
    }

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    fun ComposeContent() {
        val state by viewModel.collectAsState()
        val coroutineScope = rememberCoroutineScope()
        val snackbarHostState = remember { SnackbarHostState() }
        var query by rememberMutableStateOf(value = "")
        val signatureList = state.apkSignatureFlow.collectAsLazyPagingItems()
        val gridState = rememberLazyGridState()
        val topAppBarExpanded = rememberMutableStateOf(value = false)
        val packageName =
            rememberMutableStateOf(
                value = prefs().getStringSet("packageNameList", defaultScopeSet).joinToString(";")
            )
        val scopePackageNameState = rememberMutableStateOf(value = false)
        val refreshState = rememberPullRefreshState(state.refreshing, onRefresh = {
            viewModel.updateSignatureList()
            viewModel.querySignature(query)
        })
        val hideIcon =
            rememberMutableStateOf(value = prefs().native().getBoolean("hideIcon", false))
        LaunchedEffect(hideIcon.value) {
            prefs().native().edit().putBoolean("hideIcon", hideIcon.value).apply()
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
                                                onCheckedChange = hideIcon::value::set
                                            )
                                        }
                                    },
                                    onClick = {
                                        topAppBarExpanded.value = true
                                        hideIcon.value = !hideIcon.value
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
                        prefs().edit().putStringSet(
                            "packageNameList",
                            packageName.value.split(";").toSet()
                        ).apply()
                        scopePackageNameState.value = false
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

@Composable
fun STheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> darkColorScheme()
        else -> lightColorScheme()
    }
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}