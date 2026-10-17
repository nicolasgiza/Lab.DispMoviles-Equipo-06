package com.example.chatfamiliar

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource

fun changeAppLanguage(languageCode: String) {
    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageCode))
}

@Composable
fun LanguageSelectorScreen() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = stringResource(R.string.language_selector_title), style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { changeAppLanguage("es") }) {
                Text(stringResource(R.string.language_spanish))
            }
            Button(onClick = { changeAppLanguage("en") }) {
                Text(stringResource(R.string.language_english))
            }
        }
    }
}