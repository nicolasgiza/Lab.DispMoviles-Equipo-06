package com.example.chatfamiliar

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.SurfaceView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import io.agora.rtc2.video.VideoCanvas

@Composable
fun VideoCallScreen(
    channelName: String,
    onEndCall: () -> Unit
) {
    val context = LocalContext.current
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val latestOnEndCall by rememberUpdatedState(onEndCall)
    val rtcEngine = remember { mutableStateOf<RtcEngine?>(null) }
    val remoteUids = remember { mutableStateListOf<Int>() }
    val remoteVideoUids = remember { mutableStateListOf<Int>() }
    var isMuted by remember { mutableStateOf(false) }
    var isCameraEnabled by remember { mutableStateOf(true) }
    var isJoined by remember { mutableStateOf(false) }
    var permissionMessage by remember { mutableStateOf<String?>(null) }
    var callError by remember { mutableStateOf<String?>(null) }
    var connectionMessage by remember { mutableStateOf<String?>(null) }
    var connectionStatus by remember { mutableStateOf("Disconnected") }
    var permissionRequestStarted by remember { mutableStateOf(false) }
    var isEnding by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] == true
        val microphoneGranted = permissions[Manifest.permission.RECORD_AUDIO] == true
        if (cameraGranted && microphoneGranted) {
            permissionMessage = null
            if (rtcEngine.value == null) {
                val engine = createAgoraEngine(
                    context = context,
                    onRemoteUserJoined = { uid ->
                        mainHandler.post {
                            if (uid !in remoteUids) remoteUids.add(uid)
                        }
                    },
                    onRemoteUserLeft = { uid ->
                        mainHandler.post {
                            remoteUids.remove(uid)
                            remoteVideoUids.remove(uid)
                        }
                    },
                    onRemoteVideoStateChanged = { uid, hasVideo ->
                        mainHandler.post {
                            if (hasVideo && uid !in remoteVideoUids) {
                                remoteVideoUids.add(uid)
                            } else if (!hasVideo) {
                                remoteVideoUids.remove(uid)
                            }
                        }
                    },
                    onError = { code ->
                        mainHandler.post {
                            callError = if (code == 109 || code == 110) {
                                context.getString(R.string.video_error_token_invalid)
                            } else {
                                context.getString(R.string.video_error_agora, code)
                            }
                        }
                    },
                    onEngineCreationError = { message ->
                        mainHandler.post { callError = message }
                    },
                    onConnectionChanged = { state, reason ->
                        mainHandler.post {
                            connectionStatus = when (state) {
                                Constants.CONNECTION_STATE_CONNECTED -> "Connected"
                                Constants.CONNECTION_STATE_CONNECTING -> "Connecting"
                                Constants.CONNECTION_STATE_DISCONNECTED -> "Disconnected"
                                Constants.CONNECTION_STATE_FAILED -> "Failed"
                                else -> "State $state"
                            }
                            connectionMessage = when (state) {
                                Constants.CONNECTION_STATE_CONNECTED -> null
                                Constants.CONNECTION_STATE_CONNECTING ->
                                    context.getString(R.string.video_connecting)
                                Constants.CONNECTION_STATE_DISCONNECTED ->
                                    context.getString(R.string.video_error_connection_lost, reason)
                                Constants.CONNECTION_STATE_FAILED ->
                                    context.getString(R.string.video_error_connection_failed, reason)
                                else -> null
                            }
                        }
                    }
                )

                if (engine == null) {
                    callError = context.getString(R.string.video_error_app_id)
                } else {
                    rtcEngine.value = engine
                    engine.enableVideo()
                    engine.enableAudio()
                    engine.startPreview()
                    // TODO: Generate a production token on a server for each channel.
                    val token = BuildConfig.AGORA_TEMP_TOKEN.takeIf { it.isNotBlank() }
                    val options = ChannelMediaOptions().apply {
                        clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
                        autoSubscribeAudio = true
                        autoSubscribeVideo = true
                        publishMicrophoneTrack = true
                        publishCameraTrack = isCameraEnabled
                    }
                    Log.d(TAG, "Joining channel: $channelName")
                    val result = engine.joinChannel(token, channelName, 0, options)
                    if (result < 0) {
                        callError = context.getString(R.string.video_error_channel, result)
                    } else {
                        isJoined = true
                    }
                }
            }
        } else {
            val denied = when {
                !cameraGranted && !microphoneGranted ->
                    context.getString(R.string.video_permission_camera_and_microphone)
                !cameraGranted -> context.getString(R.string.video_permission_camera)
                else -> context.getString(R.string.video_permission_microphone)
            }
            permissionMessage = context.getString(R.string.video_permission_denied, denied)
        }
    }

    LaunchedEffect(channelName) {
        if (BuildConfig.AGORA_APP_ID.isBlank()) {
            callError = context.getString(R.string.video_error_missing_app_id)
        } else if (!permissionRequestStarted) {
            permissionRequestStarted = true
            val permissions = buildList {
                add(Manifest.permission.CAMERA)
                add(Manifest.permission.RECORD_AUDIO)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    add(Manifest.permission.BLUETOOTH_CONNECT)
                }
            }
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            rtcEngine.value?.leaveChannel()
            RtcEngine.destroy()
            rtcEngine.value = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        callError?.let { message ->
            Text(message, color = MaterialTheme.colorScheme.error)
        }
        permissionMessage?.let { message ->
            Text(message, color = MaterialTheme.colorScheme.error)
        }
        connectionMessage?.let { message ->
            Text(message, color = MaterialTheme.colorScheme.error)
        }

        if (permissionMessage == null && rtcEngine.value == null && callError == null) {
            Text(context.getString(R.string.video_initializing))
        }

        if (BuildConfig.DEBUG) {
            Text(
                "Canal: $channelName | Conexión: $connectionStatus | Participantes: ${remoteUids.size}",
                style = MaterialTheme.typography.labelSmall
            )
        }

        if (remoteUids.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(context.getString(R.string.video_waiting))
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 150.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(remoteUids, key = { it }) { uid ->
                    RemoteParticipantTile(
                        engine = rtcEngine.value,
                        uid = uid,
                        hasVideo = uid in remoteVideoUids
                    )
                }
            }
        }

        rtcEngine.value?.let { engine ->
            if (isCameraEnabled) {
                LocalPreview(engine = engine, modifier = Modifier.fillMaxWidth())
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        ) {
            Button(
                enabled = rtcEngine.value != null && !isEnding,
                onClick = {
                    isMuted = !isMuted
                    rtcEngine.value?.muteLocalAudioStream(isMuted)
                }
            ) {
                Text(
                    context.getString(
                        if (isMuted) R.string.video_unmute else R.string.video_mute
                    )
                )
            }
            Button(
                enabled = rtcEngine.value != null && !isEnding,
                onClick = {
                    isCameraEnabled = !isCameraEnabled
                    rtcEngine.value?.enableLocalVideo(isCameraEnabled)
                }
            ) {
                Text(
                    context.getString(
                        if (isCameraEnabled) R.string.video_camera_off else R.string.video_camera_on
                    )
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        ) {
            Button(
                enabled = rtcEngine.value != null && isJoined && !isEnding,
                onClick = { rtcEngine.value?.switchCamera() }
            ) {
                Text(context.getString(R.string.video_switch_camera))
            }
            Button(
                enabled = !isEnding,
                onClick = {
                    isEnding = true
                    rtcEngine.value?.leaveChannel()
                    latestOnEndCall()
                },
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(context.getString(R.string.video_hang_up))
            }
        }
    }
}

@Composable
private fun RemoteParticipantTile(engine: RtcEngine?, uid: Int, hasVideo: Boolean) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.8f),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium
    ) {
        if (hasVideo) {
            AndroidView(
                factory = { context ->
                    SurfaceView(context).also { view ->
                        engine?.setupRemoteVideo(
                            VideoCanvas(view, VideoCanvas.RENDER_MODE_HIDDEN, uid)
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
                Text("Cámara apagada")
            }
        }
    }
}

@Composable
private fun LocalPreview(engine: RtcEngine, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.aspectRatio(1.6f),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium
    ) {
        AndroidView(
            factory = { context ->
                SurfaceView(context).also { view ->
                    engine.setupLocalVideo(
                        VideoCanvas(view, VideoCanvas.RENDER_MODE_HIDDEN, 0)
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

private fun createAgoraEngine(
    context: Context,
    onRemoteUserJoined: (Int) -> Unit,
    onRemoteUserLeft: (Int) -> Unit,
    onRemoteVideoStateChanged: (Int, Boolean) -> Unit,
    onError: (Int) -> Unit,
    onEngineCreationError: (String) -> Unit,
    onConnectionChanged: (Int, Int) -> Unit
): RtcEngine? {
    val config = RtcEngineConfig().apply {
        mContext = context
        mAppId = BuildConfig.AGORA_APP_ID
        mChannelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
        mEventHandler = object : IRtcEngineEventHandler() {
            override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
                Log.d(TAG, "Joined channel: $channel")
            }

            override fun onUserJoined(uid: Int, elapsed: Int) {
                Log.d(TAG, "Remote user joined: $uid")
                onRemoteUserJoined(uid)
            }

            override fun onUserOffline(uid: Int, reason: Int) {
                Log.d(TAG, "Remote user left: $uid, reason: $reason")
                onRemoteUserLeft(uid)
            }

            override fun onRemoteVideoStateChanged(
                uid: Int,
                state: Int,
                reason: Int,
                elapsed: Int
            ) {
                when (state) {
                    Constants.REMOTE_VIDEO_STATE_STARTING,
                    Constants.REMOTE_VIDEO_STATE_DECODING -> onRemoteVideoStateChanged(uid, true)
                    Constants.REMOTE_VIDEO_STATE_STOPPED,
                    Constants.REMOTE_VIDEO_STATE_FAILED -> onRemoteVideoStateChanged(uid, false)
                }
            }

            override fun onError(err: Int) {
                Log.d(TAG, "Agora error: $err")
                onError.invoke(err)
            }

            override fun onConnectionStateChanged(state: Int, reason: Int) {
                onConnectionChanged(state, reason)
            }
        }
    }

    return try {
        RtcEngine.create(config)
    } catch (exception: Exception) {
        onEngineCreationError(
            context.getString(
                R.string.video_error_create_engine,
                exception.localizedMessage ?: context.getString(R.string.unknown_error)
            )
        )
        null
    }
}

private const val TAG = "VideoCallManager"
