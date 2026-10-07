package com.arnauquest.parnaugo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.arnauquest.parnaugo.ui.theme.PARNAUGOTheme

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Handle permissions if needed
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissions = arrayOf(
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(permissions)
            }
        }
        
        enableEdgeToEdge()
        setContent {
            PARNAUGOTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    NavHost(navController = navController, startDestination = "setup") {
        composable("setup") {
            SetupScreen(onStart = { webUrl, mqttUrl, _ ->
                if (webUrl.isNotBlank()) {
                    navController.navigate("webview/${java.net.URLEncoder.encode(webUrl, "UTF-8")}")
                } else if (mqttUrl.isNotBlank()) {
                    android.widget.Toast.makeText(context, "Servicio MQTT iniciado en segundo plano", android.widget.Toast.LENGTH_SHORT).show()
                }
            })
        }
        composable("webview/{url}") { backStackEntry ->
            val encodedUrl = backStackEntry.arguments?.getString("url") ?: ""
            val url = java.net.URLDecoder.decode(encodedUrl, "UTF-8")
            WebViewScreen(url = url)
        }
    }
}

@Composable
fun SetupScreen(onStart: (String, String, String) -> Unit) {
    var webUrl by remember { mutableStateOf("") }
    var mqttUrl by remember { mutableStateOf("") }
    var topics by remember { mutableStateOf("") }
    var useSsl by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "Configuración", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = webUrl,
                onValueChange = { webUrl = it },
                label = { Text("URL Interfaz Web (opcional)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = mqttUrl,
                onValueChange = { mqttUrl = it },
                label = { Text("Dominio/IP y Puerto MQTT (opcional)") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("ej. 192.168.1.10:1883") }
            )
            
            Row(
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text(text = "Usar SSL/TLS", modifier = Modifier.weight(1f))
                Switch(
                    checked = useSsl,
                    onCheckedChange = { useSsl = it }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = topics,
                onValueChange = { topics = it },
                label = { Text("Topics MQTT (separados por coma)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (mqttUrl.isNotBlank()) {
                        val cleanMqttUrl = mqttUrl.replace(Regex("^.*://"), "")
                        val protocol = if (useSsl) "ssl://" else "tcp://"
                        val finalMqttUrl = "$protocol$cleanMqttUrl"
                        
                        val intent = Intent(context, MqttService::class.java).apply {
                            putExtra("brokerUrl", finalMqttUrl)
                            val topicsArray = topics.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toTypedArray()
                            putExtra("topics", topicsArray)
                        }
                        context.startForegroundService(intent)
                    }
                    onStart(webUrl, mqttUrl, topics)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Iniciar")
            }
        }
    }
}

@Composable
fun WebViewScreen(url: String) {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    webViewClient = WebViewClient()
                    loadUrl(url)
                }
            },
            update = { view ->
                view.loadUrl(url)
            }
        )
    }
}
