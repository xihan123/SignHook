package website.xihan.signhelper.ui

import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.libxposed.service.XposedService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import website.xihan.signhelper.MyApp
import website.xihan.signhelper.R
import website.xihan.signhelper.data.InstalledAppEntity
import website.xihan.signhelper.data.InstalledAppRepository
import website.xihan.signhelper.data.PackageFakeSignatureSetting
import website.xihan.signhelper.util.ALog
import website.xihan.signhelper.util.AnywhereDropdown
import website.xihan.signhelper.util.Const.FAKE_SIGNATURE
import website.xihan.signhelper.util.Const.FAKE_SIGNATURE_ENABLED
import website.xihan.signhelper.util.Const.PREFS_DYNAMIC_KEY_SEPARATOR
import website.xihan.signhelper.util.Const.REMOTE_PREFS_GROUP
import website.xihan.signhelper.util.Const.fakeSignatureEnabledKey
import website.xihan.signhelper.util.Const.fakeSignatureKey
import website.xihan.signhelper.util.MyDropdownMenuItem
import website.xihan.signhelper.util.Settings
import website.xihan.signhelper.util.copyToClipboard
import website.xihan.signhelper.util.hideAppIcon
import website.xihan.signhelper.util.rememberMutableStateOf
import website.xihan.signhelper.util.showAppIcon
import website.xihan.signhelper.util.toast

class MainActivity : ComponentActivity(), KoinComponent, MyApp.ServiceStateListener {
    private val repository: InstalledAppRepository by inject()
    private var mService: XposedService? = null
    private var remotePreferences: SharedPreferences? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            SignHelperTheme {
                MainScreen()
            }
        }
    }

    override fun onServiceStateChanged(service: XposedService?) {
        mService = service
        remotePreferences = service?.getRemotePreferences(REMOTE_PREFS_GROUP)
        if (service != null) {
            syncRemoteFakeSignatures()
        }
        ALog.d("service: $service")
    }

    private fun saveRemoteFakeSignature(
        packageName: String,
        fakeSignature: String,
        enabled: Boolean
    ) {
        val normalized = fakeSignature.trim()
        val hasFakeSignature = normalized.isNotEmpty()

        putRemotePreference(fakeSignatureKey(packageName), normalized.takeIf { hasFakeSignature })
        putRemotePreference(fakeSignatureEnabledKey(packageName), hasFakeSignature && enabled)
    }

    private fun putRemotePreference(key: String, value: Any?) {
        val preferences = remotePreferences ?: return

        runCatching {
            preferences.edit().apply {
                when (value) {
                    null -> remove(key)
                    is Boolean -> putBoolean(key, value)
                    is String -> putString(key, value)
                    else -> error("Unsupported remote preference value type: ${value::class.qualifiedName}")
                }
            }.commit()
        }.onSuccess { saved ->
            if (!saved) {
                ALog.w("Write remote preference failed: $key")
            }
        }.onFailure {
            ALog.e(it)
        }
    }

    private fun syncRemoteFakeSignatures() {
        val preferences = remotePreferences ?: return
        lifecycleScope.launch {
            val settings = withContext(Dispatchers.IO) {
                repository.getFakeSignatureSettings()
            }
            withContext(Dispatchers.IO) {
                writeRemoteFakeSignatureSettings(preferences, settings)
            }
        }
    }

    private fun writeRemoteFakeSignatureSettings(
        preferences: SharedPreferences,
        settings: List<PackageFakeSignatureSetting>
    ) {
        runCatching {
            preferences.edit().apply {
                preferences.all.keys
                    .filter(::isFakeSignaturePreferenceKey)
                    .forEach(::remove)

                settings.forEach { setting ->
                    val normalized = setting.fakeSignature.trim()
                    val enabled = normalized.isNotEmpty() && setting.fakeSignatureEnabled
                    if (normalized.isNotEmpty()) {
                        putString(fakeSignatureKey(setting.packageName), normalized)
                    }
                    putBoolean(fakeSignatureEnabledKey(setting.packageName), enabled)
                }
            }.commit()
        }.onSuccess { saved ->
            if (!saved) {
                ALog.w("Sync remote fake signatures failed")
            }
        }.onFailure(ALog::e)
    }

    private fun isFakeSignaturePreferenceKey(key: String): Boolean =
        key.startsWith(FAKE_SIGNATURE + PREFS_DYNAMIC_KEY_SEPARATOR) ||
                key.startsWith(FAKE_SIGNATURE_ENABLED + PREFS_DYNAMIC_KEY_SEPARATOR)

    override fun onStart() {
        super.onStart()
        MyApp.addServiceStateListener(this, true)
    }

    override fun onStop() {
        MyApp.removeServiceStateListener(this)
        super.onStop()
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
    @Composable
    fun MainScreen(viewModel: MainViewModel = koinViewModel()) {
        val uiState by viewModel.collectAsState()
        val context = LocalContext.current
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        val signatureCopied = stringResource(R.string.signature_copied)
        val topAppBarExpanded = rememberMutableStateOf(value = false)
        val hideHomeAppIcon = rememberMutableStateOf(value = Settings.hideHomeAppIcon)

        LaunchedEffect(hideHomeAppIcon.value) {
            Settings.hideHomeAppIcon = hideHomeAppIcon.value
            if (hideHomeAppIcon.value) {
                context.hideAppIcon()
            } else {
                context.showAppIcon()
            }
        }

        viewModel.collectSideEffect { sideEffect ->
            when (sideEffect) {
                is MainUiIntent.ShowError -> snackbarHostState.showSnackbar(
                    sideEffect.message.asString(context)
                )
            }
        }

        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }, actions = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            if (uiState.refreshing) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .padding(end = 16.dp)
                                        .size(22.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                IconButton(onClick = viewModel::refresh) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = stringResource(R.string.refresh)
                                    )
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
                                            imageVector = Icons.Filled.MoreVert,
                                            contentDescription = null
                                        )
                                    }
                                }) {
                                MyDropdownMenuItem(topAppBarExpanded = topAppBarExpanded, text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(stringResource(id = R.string.hide_icon))
                                        Checkbox(
                                            checked = hideHomeAppIcon.value,
                                            onCheckedChange = hideHomeAppIcon::value::set
                                        )
                                    }
                                }, onClick = {
                                    topAppBarExpanded.value = true
                                    hideHomeAppIcon.value = !hideHomeAppIcon.value
                                })

                                MyDropdownMenuItem(
                                    topAppBarExpanded = topAppBarExpanded,
                                    text = { Text(stringResource(id = R.string.exit)) },
                                    onClick = ::finish
                                )
                            }


                        }

                    }, colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                SearchAndFilters(
                    uiState = uiState,
                    onQueryChange = viewModel::onQueryChange,
                    onClearQuery = viewModel::clearQuery,
                    onHideSystemAppsChange = viewModel::onHideSystemAppsChange,
                    onHideAppIconsChange = viewModel::onHideAppIconsChange
                )

                if (uiState.apps.isEmpty() && uiState.refreshing) {
                    Box(
                        modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (uiState.apps.isEmpty()) {
                    EmptyState(
                        text = if (uiState.query.isBlank() && !uiState.hideSystemApps) {
                            stringResource(R.string.no_app_data)
                        } else {
                            stringResource(R.string.no_matching_apps)
                        }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(
                            start = 16.dp, top = 12.dp, end = 16.dp, bottom = 24.dp
                        ), verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(
                            items = uiState.apps, key = { it.packageName }) { app ->
                            AppCard(
                                app = app,
                                showIcon = !uiState.hideAppIcons,
                                onCopySignature = { _ ->
                                    scope.launch {
                                        snackbarHostState.currentSnackbarData?.dismiss()
                                        snackbarHostState.showSnackbar(signatureCopied)
                                    }
                                },
                                onSaveFakeSignature = { packageName, fakeSignature, enabled ->
                                    val normalized = fakeSignature.trim()
                                    viewModel.updateFakeSignature(packageName, normalized)
                                    saveRemoteFakeSignature(packageName, normalized, enabled)
                                },
                                onFakeSignatureEnabledChange = { packageName, fakeSignature, enabled ->
                                    viewModel.updateFakeSignatureEnabled(packageName, enabled)
                                    saveRemoteFakeSignature(packageName, fakeSignature, enabled)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun SearchAndFilters(
        uiState: MainUiState,
        onQueryChange: (String) -> Unit,
        onClearQuery: () -> Unit,
        onHideSystemAppsChange: (Boolean) -> Unit,
        onHideAppIconsChange: (Boolean) -> Unit
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            shadowElevation = 1.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                OutlinedTextField(
                    value = uiState.query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search, contentDescription = null
                        )
                    },
                    trailingIcon = {
                        if (uiState.query.isNotEmpty()) {
                            IconButton(onClick = onClearQuery) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = stringResource(R.string.clear_search)
                                )
                            }
                        }
                    },
                    placeholder = { Text(text = stringResource(R.string.search_apps_placeholder)) },
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = uiState.hideSystemApps,
                        onClick = { onHideSystemAppsChange(!uiState.hideSystemApps) },
                        label = { Text(text = stringResource(R.string.hide_system_apps)) })
                    FilterChip(
                        selected = uiState.hideAppIcons,
                        onClick = { onHideAppIconsChange(!uiState.hideAppIcons) },
                        label = { Text(text = stringResource(R.string.hide_icon)) })
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(
                            R.string.app_count, uiState.apps.size, uiState.totalAppCount
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(
                            R.string.enabled_fake_count, uiState.enabledFakeCount
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

    @Composable
    private fun EmptyState(text: String) {
        Box(
            modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
        ) {
            Text(
                text = text, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    @Composable
    private fun AppCard(
        app: InstalledAppEntity,
        showIcon: Boolean,
        onCopySignature: (String) -> Unit,
        onSaveFakeSignature: (String, String, Boolean) -> Unit,
        onFakeSignatureEnabledChange: (String, String, Boolean) -> Unit
    ) {
        val context = LocalContext.current
        val fakeSignatureActive = app.fakeSignatureEnabled && app.fakeSignature.isNotBlank()
        val fakeSignatureCleared = stringResource(R.string.fake_signature_cleared)
        val fakeSignatureSaved = stringResource(R.string.fake_signature_saved)
        val fakeSignatureEnabled = stringResource(R.string.fake_signature_enabled)
        val fakeSignatureDisabled = stringResource(R.string.fake_signature_disabled)
        val versionName = app.versionName.ifBlank { stringResource(R.string.empty_value) }
        val version = stringResource(R.string.version_format, versionName, app.versionCode)
        val signatureValue =
            app.signatureValue.ifBlank { stringResource(R.string.get_signature_failed) }
        var fakeSignature by remember(app.packageName, app.fakeSignature) {
            mutableStateOf(app.fakeSignature)
        }
        val changed = fakeSignature != app.fakeSignature

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (fakeSignatureActive) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f)
                } else {
                    MaterialTheme.colorScheme.surface
                }
            ),
            elevation = if (!fakeSignatureActive) CardDefaults.cardElevation(defaultElevation = 1.dp) else CardDefaults.cardElevation(
                0.dp
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showIcon) {
                        AppIcon(app = app)
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = app.appName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = app.packageName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatusPill(
                                text = if (fakeSignatureActive) {
                                    stringResource(R.string.status_enabled)
                                } else {
                                    stringResource(R.string.status_disabled)
                                }, active = fakeSignatureActive
                            )
                            if (app.isSystemApp) {
                                StatusPill(
                                    text = stringResource(R.string.status_system), active = false
                                )
                            }
                        }
                    }
                    IconButton(
                        onClick = {
                            context.copyToClipboard(app.signatureValue)
                            onCopySignature(app.signatureValue)
                        }, enabled = app.signatureValue.isNotBlank()
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = stringResource(R.string.copy_signature),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f))
                Spacer(modifier = Modifier.height(10.dp))

                InfoLine(label = stringResource(R.string.app_version_label), value = version)
                InfoLine(
                    label = stringResource(R.string.signature_value_label),
                    value = signatureValue,
                    mono = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = fakeSignature,
                        onValueChange = { fakeSignature = it },
                        modifier = Modifier.weight(1f),
                        minLines = 1,
                        maxLines = 3,
                        label = { Text(text = stringResource(R.string.fake_signature_label)) },
                        trailingIcon = {
                            if (fakeSignature.isNotEmpty()) {
                                IconButton(onClick = { fakeSignature = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = stringResource(R.string.clear_fake_signature)
                                    )
                                }
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                    IconButton(
                        onClick = {
                            onSaveFakeSignature(
                                app.packageName,
                                fakeSignature,
                                app.fakeSignatureEnabled
                            )
                            context.toast(
                                if (fakeSignature.isBlank()) fakeSignatureCleared else fakeSignatureSaved
                            )
                        }, enabled = changed
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = stringResource(R.string.save_fake_signature)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.enable_fake_signature),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (app.fakeSignature.isBlank()) {
                                stringResource(R.string.enable_fake_signature_after_save)
                            } else if (fakeSignatureActive) {
                                stringResource(R.string.current_enabled)
                            } else {
                                stringResource(R.string.current_disabled)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Switch(
                        checked = fakeSignatureActive, onCheckedChange = { enabled ->
                            onFakeSignatureEnabledChange(app.packageName, app.fakeSignature, enabled)
                            context.toast(if (enabled) fakeSignatureEnabled else fakeSignatureDisabled)
                        }, enabled = app.fakeSignature.isNotBlank()
                    )
                }
            }
        }
    }

    @Composable
    private fun StatusPill(
        text: String, active: Boolean
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp), color = if (active) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                style = MaterialTheme.typography.labelSmall,
                color = if (active) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1
            )
        }
    }

    @Composable
    private fun AppIcon(app: InstalledAppEntity) {
        val imageBitmap = remember(app.iconPng) {
            app.iconPng?.let { bytes ->
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            }
        }

        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = app.appName.firstOrNull()?.toString().orEmpty(),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    @Composable
    private fun InfoLine(
        label: String, value: String, mono: Boolean = false
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 3.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = label,
                modifier = Modifier.width(52.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = value,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = if (mono) FontFamily.Monospace else null,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (mono) 4 else 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

    @Composable
    fun SignHelperTheme(content: @Composable () -> Unit) {
        val darkTheme: Boolean = isSystemInDarkTheme()
        val colorScheme = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }

            darkTheme -> darkColorScheme()
            else -> lightColorScheme()
        }
        MaterialTheme(colorScheme = colorScheme) {
            Surface(
                modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
            ) { content() }
        }
    }

}
