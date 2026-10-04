package com.example.chatfamiliar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.chatfamiliar.ui.theme.ChatFamiliarTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChatFamiliarTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var inCall by remember { mutableStateOf(false) }

                    if (inCall) {
                        // Muestra la pantalla de videollamada
                        VideoCallScreen(
                            channelName = "SalaFamiliar",
                            onEndCall = { inCall = false }
                        )
                    } else {
                        // Vista principal con el botón para iniciar la videollamada
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Button(
                                onClick = { inCall = true },
                                modifier = Modifier
                                    .padding(16.dp)
                                    .height(50.dp)
                            ) {
                                Text("Iniciar Videollamada")
                            }
                        }
                    }
                }
            }
        }
    }
}