package org.eu.john007.hdmikvm

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.tv.TvContract
import android.media.tv.TvInputInfo
import android.media.tv.TvInputManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.tv.material3.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.eu.john007.hdmikvm.model.KvmSource
import org.eu.john007.hdmikvm.network.EspHomeApi
import org.eu.john007.hdmikvm.ui.theme.HDMIKVMTheme
import org.eu.john007.hdmikvm.util.HomeChannelManager
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private val espHomeApi: EspHomeApi by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()

        val json = Json { ignoreUnknownKeys = true }

        Retrofit.Builder()
            .baseUrl("http://192.168.10.206/")
            .client(client)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(EspHomeApi::class.java)
    }

    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sources = listOf(
            KvmSource(-1, "Google TV", "Home", Icons.Default.Home, isSystemItem = true),
            KvmSource(1, "HDMI 1", "Reserved", Icons.Default.Lock, "Input 1"),
            KvmSource(2, "HDMI 2", "ali-monster-pc", Icons.Default.Computer, "Input 2"),
            KvmSource(3, "HDMI 3", "TV Live", Icons.Default.Router, "Input 3"),
            KvmSource(4, "HDMI 4", "Minisforum HomeLab", Icons.Default.Dns, "Input 4"),
            KvmSource(-2, "Input Settings", "System", Icons.Default.Settings, isSystemItem = true)
        )

        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                HomeChannelManager.updateHomeChannel(this@MainActivity, sources)
            } catch (e: Exception) {
                android.util.Log.e("KVM_DEBUG", "Channel registration failed", e)
            }
        }

        handleIntent(intent)

        setContent {
            HDMIKVMTheme {
                var activeInputName by remember { mutableStateOf<String?>(null) }
                val focusRequesters = remember { List(sources.size) { FocusRequester() } }
                var hasInitialFocus by remember { mutableStateOf(false) }
                val listState = rememberLazyListState()

                LaunchedEffect(Unit) {
                    while(true) {
                        refreshState { newState ->
                            activeInputName = newState
                            if (!hasInitialFocus && newState != null) {
                                val activeIndex = sources.indexOfFirst { it.optionName == newState }
                                if (activeIndex != -1) {
                                    focusRequesters[activeIndex].requestFocus()
                                    hasInitialFocus = true
                                }
                            }
                        }
                        delay(5000)
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(280.dp)
                            .background(Color.Black.copy(alpha = 0.94f))
                            .padding(top = 24.dp, bottom = 12.dp, start = 12.dp, end = 12.dp),
                        verticalArrangement = Arrangement.Top,
                        horizontalAlignment = Alignment.Start
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 12.dp, start = 8.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_hdmi_logo),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = Color.Unspecified
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Inputs",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                        }

                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            itemsIndexed(sources) { index, source ->
                                val isActive = activeInputName != null && activeInputName == source.optionName
                                SourceListItem(
                                    source = source,
                                    isActive = isActive,
                                    modifier = Modifier.focusRequester(focusRequesters[index])
                                ) {
                                    handleSourceClick(source)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun handleIntent(intent: Intent?) {
        val option = intent?.getStringExtra("source_option")
        val name = intent?.getStringExtra("source_name")
        if (option != null && name != null) {
            switchInput(KvmSource(0, name, null, Icons.Default.QuestionMark, option))
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleSourceClick(source: KvmSource) {
        if (source.isSystemItem) {
            when (source.id) {
                -1 -> goHome()
                -2 -> openInputSettings()
            }
        } else {
            switchInput(source)
        }
    }

    private fun goHome() {
        try {
            startActivity(Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            finish()
        } catch (e: Exception) {
            Toast.makeText(this, "Home failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openInputSettings() {
        try {
            val intent = Intent().apply {
                component = ComponentName("com.android.tv.settings", "com.android.tv.settings.system.InputsActivity")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            try {
                startActivity(Intent("android.settings.TV_INPUT_SETTINGS").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
                finish()
            } catch (e2: Exception) {
                try {
                    startActivity(Intent(android.provider.Settings.ACTION_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                    finish()
                } catch (e3: Exception) {}
            }
        }
    }

    @OptIn(ExperimentalTvMaterial3Api::class)
    @Composable
    fun SourceListItem(source: KvmSource, isActive: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
        val containerColor = if (isActive) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
        } else {
            Color.Transparent
        }

        Surface(
            onClick = onClick,
            modifier = modifier
                .fillMaxWidth()
                .height(40.dp),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = containerColor,
                contentColor = Color.White,
                focusedContainerColor = Color.White.copy(alpha = 0.15f),
                focusedContentColor = Color.White
            ),
            shape = ClickableSurfaceDefaults.shape(MaterialTheme.shapes.small)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = source.icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = if (isActive) MaterialTheme.colorScheme.primary else Color.White
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = source.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White
                    )
                    if (source.description != null) {
                        Text(
                            text = source.description,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
                if (isActive) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Active",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }

    private fun switchToTvHdmi1() {
        val tvInputManager = getSystemService(Context.TV_INPUT_SERVICE) as? TvInputManager ?: return
        
        try {
            // official TIF discovery
            val hdmiInputs = tvInputManager.tvInputList.filter { 
                it.isPassthroughInput && it.type == TvInputInfo.TYPE_HDMI 
            }
            
            if (hdmiInputs.isNotEmpty()) {
                val selectedInput = hdmiInputs.getOrNull(0) ?: hdmiInputs.first()
                val passthroughUri = TvContract.buildChannelUriForPassthroughInput(selectedInput.id)
                
                android.util.Log.d("KVM_DEBUG", "Switching to HDMI ID: ${selectedInput.id}")
                
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = passthroughUri
                    // Explicitly target Input Player to bypass TCL browser
                    setPackage("com.google.android.tv.inputplayer")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
                return
            }
        } catch (e: Exception) {
            android.util.Log.e("KVM_DEBUG", "TIF approach failed, using broadcast fallback", e)
        }

        // Silent Mediatek/JMGO fallback
        try {
            val intent = Intent("com.mediatek.tv.ui.intent.action.INPUT_SOURCE_CHANGE")
            intent.putExtra("input", 1)
            sendBroadcast(intent)
        } catch (e: Exception) {}

        try {
            val intent = Intent("com.jmgo.input.state.change")
            intent.putExtra("input", 1)
            sendBroadcast(intent)
        } catch (e: Exception) {}
    }

    private fun refreshState(onResult: (String?) -> Unit) {
        lifecycleScope.launch {
            try {
                // Corrected entity name for ESPHome v3 (KVM%20Input)
                val response = espHomeApi.getState("KVM%20Input")
                if (response.isSuccessful) {
                    onResult(response.body()?.state)
                } else {
                    android.util.Log.e("KVM_DEBUG", "Refresh state 404 or other: ${response.code()}")
                }
            } catch (e: Exception) {
                android.util.Log.e("KVM_DEBUG", "Failed to refresh state", e)
            }
        }
    }

    private fun switchInput(source: KvmSource) {
        lifecycleScope.launch {
            try {
                val host = "192.168.10.206"
                val baseUrl = "http://$host"
                val entity = "KVM%20Input" // Corrected entity name
                
                val optionValue = source.optionName?.replace(" ", "%20") ?: return@launch
                val fullUrl = "$baseUrl/select/$entity/set?option=$optionValue"
                
                val response = espHomeApi.setInput(
                    url = fullUrl,
                    origin = baseUrl,
                    referer = "$baseUrl/"
                )
                
                if (response.isSuccessful) {
                    Toast.makeText(this@MainActivity, "Switched to ${source.name}", Toast.LENGTH_SHORT).show()
                    
                    // Logic to avoid Home transition if already on HDMI 1
                    // For now, let's fire TIF which should be relatively clean
                    delay(200)
                    switchToTvHdmi1()
                    delay(800)
                    finish()
                } else {
                    Toast.makeText(this@MainActivity, "Failed: ${response.code()}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
