package cn.xihan.signhook.ui

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.util.Patterns
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import cn.xihan.signhook.component.AnywhereDropdown
import cn.xihan.signhook.component.MyDropdownMenuItem
import cn.xihan.signhook.component.SignScaffold
import cn.xihan.signhook.model.TitleAndPackageNameModel
import cn.xihan.signhook.util.ModuleSettings
import cn.xihan.signhook.util.ModuleSettings.isLogin
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.orbitmvi.orbit.compose.collectAsState

class MainActivity : AppCompatActivity() {

    val viewModel: MainViewModel by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val darkTheme: Boolean = isSystemInDarkTheme()
            val colorScheme = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    val context = LocalContext.current
                    if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(
                        context
                    )
                }

                darkTheme -> darkColorScheme()
                else -> lightColorScheme()
            }
            MaterialTheme(
                colorScheme = colorScheme,
                content = { ComposeContent() }
            )
        }
    }


    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
    @Composable
    fun ComposeContent() {
        val state by viewModel.collectAsState()
        val coroutineScope = rememberCoroutineScope()
        val snackbarHostState = remember { SnackbarHostState() }

        val topAppBarExpanded = remember { mutableStateOf(false) }
        val signatures = state.signatures.collectAsLazyPagingItems()
        val gridState = rememberLazyGridState()

        val refreshState =
            rememberPullRefreshState(state.refreshing, onRefresh = viewModel::onRefresh)

        SignScaffold(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            state = state,
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(text = "SignHook") },
                    navigationIcon = { },
                    actions = {

                        if (state.isLogin) {
                            IconButton(onClick = viewModel::onRefresh) {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = "")
                            }
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
                                        imageVector = Icons.Default.MoreHoriz,
                                        contentDescription = "",
                                    )
                                }
                            }
                        ) {

                            if (state.isLogin) {

                                MyDropdownMenuItem(
                                    topAppBarExpanded = topAppBarExpanded,
                                    text = { Text("弹框状态") },
                                    onClick = viewModel::checkDialogStatus,
                                    trailingIcon = {
                                        Checkbox(
                                            checked = state.showDialog,
                                            onCheckedChange = { viewModel.updateDialogStatus(if (it) 1 else 0) }
                                        )
                                    }
                                )


                                MyDropdownMenuItem(
                                    topAppBarExpanded = topAppBarExpanded,
                                    text = { Text("注销登录") },
                                    onClick = viewModel::logout,
                                )


                            }

                            MyDropdownMenuItem(
                                topAppBarExpanded = topAppBarExpanded,
                                text = { Text("退出应用") },
                                onClick = ::finish,
                            )
                        }

                    })
            },
            snackbarHostState = snackbarHostState,
            onShowSnackbar = {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(it)
                }
            },
            onDismissErrorDialog = viewModel::hideError,
        ) { padding, _ ->
            if (!isLogin) {
                AuthScreen()
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    SearchByTextAppBar(
                        text = state.currentQuery,
                        onTextChange = viewModel::search,
                        onClickClear = viewModel::search,
                        onClickSearch = viewModel::search
                    )

                    Box(modifier = Modifier.pullRefresh(refreshState)) {
                        when {
                            signatures.loadState.refresh is LoadState.Loading -> {
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.TopCenter)
                                        .background(Color.Transparent)
                                )
                            }

                            signatures.loadState.append is LoadState.Error -> {
                                val error = signatures.loadState.append as LoadState.Error
                                Text(
                                    "错误信息: ${error.error.localizedMessage}",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.BottomCenter)
                                        .padding(16.dp)
                                )
                            }

                            signatures.itemCount == 0 -> {
                                Text(
                                    "暂无数据",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.TopCenter)
                                        .padding(16.dp)
                                )
                            }

                            else -> {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(1),
                                    contentPadding = PaddingValues(15.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    state = gridState,
                                    modifier = Modifier.fillMaxSize()
                                ) {

                                    items(signatures, key = { item ->
                                        item.packageName
                                    }) { item ->
                                        item?.let {
                                            SignatureItem(
                                                signature = item,
                                                isSelected = state.selectedPackages.contains(item.packageName),
                                                onCheckedChange = {
                                                    if (it) {
                                                        viewModel.submitPackages(item.packageName)
                                                    } else {
                                                        viewModel.deletePackage(item.packageName)
                                                    }
                                                }
                                            )
                                        }
                                    }

                                    item {
                                        if (signatures.loadState.append is LoadState.Loading) {
                                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                        }
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
        }

    }


    @Composable
    fun AuthScreen() {
        var isLoginScreen by remember { mutableStateOf(true) }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 标题动画
                AnimatedContent(
                    targetState = isLoginScreen,
                    transitionSpec = {
                        (slideInVertically { height -> height } + fadeIn()).togetherWith(
                            slideOutVertically { height -> -height } + fadeOut())
                    }
                ) { isLogin ->
                    Text(
                        text = if (isLogin) "欢迎回来" else "创建账户",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // 切换选项卡
                TabRow(
                    selectedTabIndex = if (isLoginScreen) 0 else 1,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = isLoginScreen,
                        onClick = { isLoginScreen = true },
                        text = { Text("登录") }
                    )
                    Tab(
                        selected = !isLoginScreen,
                        onClick = { isLoginScreen = false },
                        text = { Text("注册") }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 表单内容
                AuthForm(isLoginScreen = isLoginScreen)

                Spacer(modifier = Modifier.height(24.dp))

            }
        }
    }

    @Composable
    private fun AuthForm(isLoginScreen: Boolean) {
        var email by remember { mutableStateOf(ModuleSettings.email) }
        var password by remember { mutableStateOf(ModuleSettings.password) }

        val emailError = remember { mutableStateOf(false) }
        val passwordError = remember { mutableStateOf(false) }


        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
        ) {
            AnimatedInputField(
                value = email,
                onValueChange = {
                    email = it
                    emailError.value = !isValidEmail(it)
                },
                label = "邮箱地址",
                isError = emailError.value,
                errorMessage = "请输入有效的邮箱"
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoginScreen) {

                // 密码输入
                AnimatedInputField(
                    value = password,
                    onValueChange = {
                        password = it
                        passwordError.value = it.length < 6
                    },
                    label = "密码",
                    isPassword = true,
                    isError = passwordError.value,
                    errorMessage = "密码至少6位"
                )

            }

            Spacer(modifier = Modifier.height(32.dp))

            // 提交按钮
            Button(
                onClick = {
                    if (isLoginScreen) {
                        viewModel.login(email, password)
                    } else {
                        viewModel.register(email)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                )
            ) {
                Text(text = if (isLoginScreen) "立即登录" else "注册账户")
            }
        }
    }

    @Composable
    fun AnimatedInputField(
        value: String,
        onValueChange: (String) -> Unit,
        label: String,
        isPassword: Boolean = false,
        isError: Boolean = false,
        errorMessage: String = ""
    ) {
        var isFocused by remember { mutableStateOf(false) }

        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = when {
                            isError -> MaterialTheme.colorScheme.error
                            isFocused -> MaterialTheme.colorScheme.primary
                            else -> Color.Gray.copy(alpha = 0.3f)
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty() && !isFocused) {
                    Text(
                        text = label,
                        color = Color.Gray.copy(alpha = 0.6f),
                        modifier = Modifier.alpha(0.8f)
                    )
                }

                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isFocused = it.isFocused },
                    textStyle = LocalTextStyle.current.copy(
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
                    singleLine = true
                )
            }

            AnimatedVisibility(visible = isError) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }
        }
    }

    @Composable
    fun SignatureItem(
        signature: TitleAndPackageNameModel?,
        isSelected: Boolean,
        onCheckedChange: (Boolean) -> Unit
    ) {
        signature?.let {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(16.dp)
                    ) {
                        Text(text = it.title, style = MaterialTheme.typography.titleMedium)
                        Text(text = it.packageName, style = MaterialTheme.typography.bodySmall)
                    }

                    Checkbox(checked = isSelected, onCheckedChange = onCheckedChange)
                }

            }
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

    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}
