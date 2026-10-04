package com.example.chatfamiliar

import android.Manifest
import android.content.Context
import android.view.SurfaceView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import io.agora.rtc2.video.VideoCanvas

// Reemplaza con tu App ID de Agora (Console Agora.io)
private const val AGORA_APP_ID = "TU_AGORA_APP_ID_AQUI"

@Composable
fun VideoCallScreen(
    channelName: String = "ChatFamiliarRoom",
    onEndCall: () -> Unit
) {
    val context = LocalContext.current
    var isJoined by remember { mutableStateOf(false) }
    var remoteUid by remember { mutableStateOf<Int?>(null) }
    var rtcEngine by remember { mutableStateOf<RtcEngine?>(null) }

    var localSurfaceView by remember { mutableStateOf<SurfaceView?>(null) }
    var remoteSurfaceView by remember { mutableStateOf<SurfaceView?>(null) }

    // Solicitar permisos de Cámara y Micrófono
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it }
        if (granted) {
            val engine = initAgoraEngine(context, object : IRtcEngineEventHandler() {
                override fun onUserJoined(uid: Int, elapsed: Int) {
                    remoteUid = uid
                }

                override fun onUserOffline(uid: Int, reason: Int) {
                    if (remoteUid == uid) remoteUid = null
                }
            })

            rtcEngine = engine
            engine?.let {
                // Crear vista local
                val localView = SurfaceView(context)
                localSurfaceView = localView
                it.setupLocalVideo(VideoCanvas(localView, VideoCanvas.RENDER_MODE_HIDDEN, 0))

                // Unirse al canal
                it.joinChannel(null, channelName, "", 0)
                isJoined = true
            }
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            )
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            rtcEngine?.leaveChannel()
            RtcEngine.destroy()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Renderizar video remoto (Pantalla completa)
        if (remoteUid != null && rtcEngine != null) {
            AndroidView(
                factory = { ctx ->
                    SurfaceView(ctx).also { view ->
                        remoteSurfaceView = view
                        rtcEngine?.setupRemoteVideo(
                            VideoCanvas(view, VideoCanvas.RENDER_MODE_HIDDEN, remoteUid!!)
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Esperando a otro participante...")
            }
        }

        // Renderizar video local (Miniatura flotante arriba a la derecha)
        localSurfaceView?.let { localView ->
            Box(
                modifier = Modifier
                    .size(120.dp, 160.dp)
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                AndroidView(
                    factory = { localView },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Botón para colgar la llamada
        Button(
            onClick = {
                rtcEngine?.leaveChannel()
                onEndCall()
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(32.dp)
        ) {
            Text("Finalizar Llamada")
        }
    }
}

private fun initAgoraEngine(context: Context, handler: IRtcEngineEventHandler): RtcEngine? {
    return try {
        val config = RtcEngineConfig().apply {
            mContext = context
            mAppId = AGORA_APP_ID
            mEventHandler = handler
        }
        val engine = RtcEngine.create(config)
        engine.enableVideo()
        engine
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}