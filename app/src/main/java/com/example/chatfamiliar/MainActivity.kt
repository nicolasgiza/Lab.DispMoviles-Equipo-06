package com.example.chatfamiliar

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.chatfamiliar.ui.theme.ChatFamiliarTheme
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        auth = FirebaseAuth.getInstance()

        // Comprobar si hay una sesión activa
        if (auth.currentUser == null) {

            // No hay usuario → ir al Login
            val intent = Intent(this, LoginAcyivity::class.java)
            startActivity(intent)
            finish()

            return
        }

        setContent {
            ChatFamiliarTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ChatScreen(
                        auth = auth,
                        onLogout = {
                            auth.signOut()

                            val intent = Intent(
                                this,
                                LoginAcyivity::class.java
                            )

                            startActivity(intent)
                            finish()
                        }
                    )
                }
            }
        }
    }
}

data class Message(
    val sender: String,
    val text: String,
    val isMine: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    auth: FirebaseAuth,
    onLogout: () -> Unit
) {
    var messageText by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }

    val currentUserEmail = auth.currentUser?.email ?: "Usuario"

    var messages by remember {
        mutableStateOf(
            listOf(
                Message(
                    sender = "Mamá",
                    text = "Hola familia ❤️",
                    isMine = false
                ),
                Message(
                    sender = "Papá",
                    text = "Buenas tardes a todos",
                    isMine = false
                ),
                Message(
                    sender = "Tú",
                    text = "Hola, ya llegué 👍",
                    isMine = true
                )
            )
        )
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        TopAppBar(
            title = {
                Column {
                    Text("Chat Familiar")
                    Text(
                        text = currentUserEmail,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            actions = {

                Box {
                    IconButton(
                        onClick = {
                            showMenu = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Opciones"
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = {
                            showMenu = false
                        }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text("Cerrar sesión")
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ExitToApp,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                showMenu = false
                                onLogout()
                            }
                        )
                    }
                }
            }
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages) { message ->
                MessageBubble(message)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            OutlinedTextField(
                value = messageText,
                onValueChange = {
                    messageText = it
                },
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text("Escribe un mensaje...")
                },
                singleLine = true
            )

            IconButton(
                onClick = {

                    if (messageText.trim().isNotEmpty()) {

                        messages = messages + Message(
                            sender = "Tú",
                            text = messageText.trim(),
                            isMine = true
                        )

                        messageText = ""
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Enviar"
                )
            }
        }
    }
}

@Composable
fun MessageBubble(message: Message) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isMine) {
            Arrangement.End
        } else {
            Arrangement.Start
        }
    ) {

        Column(
            modifier = Modifier
                .background(
                    color = if (message.isMine) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    },
                    shape = MaterialTheme.shapes.medium
                )
                .padding(12.dp)
        ) {

            if (!message.isMine) {
                Text(
                    text = message.sender,
                    style = MaterialTheme.typography.labelMedium
                )
            }

            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}