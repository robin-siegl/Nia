package dev.robinsiegl.nia.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.robinsiegl.nia.model.AppEntry
import dev.robinsiegl.nia.model.AppSearch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppListScreen(
    apps: List<AppEntry>, loading: Boolean, autofocus: Boolean, title: String = "Apps",
    onBack: () -> Unit, onSelect: (AppEntry) -> Unit, onSettings: (() -> Unit)? = null,
    onInfo: ((AppEntry) -> Unit)? = null,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val results = remember(apps, query) { AppSearch.filter(apps, query) }
    val duplicates = remember(apps) { apps.groupingBy { it.label }.eachCount().filterValues { it > 1 }.keys }
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    BackHandler {
        if (imeVisible) { focusManager.clearFocus(); keyboard?.hide() } else onBack()
    }
    LaunchedEffect(autofocus) { if (autofocus) { focus.requestFocus(); keyboard?.show() } }
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        ScreenHeader(title, onBack, trailing = if (onSettings != null) {
            { TextButton(onClick = onSettings) { Text("nia settings", fontSize = 13.sp) } }
        } else null)
        OutlinedTextField(query, { query = it }, singleLine = true, label = { Text("Search apps") },
            modifier = Modifier.fillMaxWidth().focusRequester(focus),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { keyboard?.hide(); focusManager.clearFocus() }),
            trailingIcon = if (query.isNotEmpty()) {
                { TextButton(onClick = { query = "" }) { Text("Clear", fontSize = 12.sp) } }
            } else null)
        Spacer(Modifier.height(16.dp))
        when {
            loading -> Text("Loading apps…", modifier = Modifier.padding(vertical = 24.dp))
            results.isEmpty() -> Text(if (apps.isEmpty()) "No launchable apps available." else "No apps found.",
                modifier = Modifier.padding(vertical = 24.dp))
            else -> LazyColumn(Modifier.fillMaxSize()) {
                items(results, key = { it.target.key }) { app ->
                    Column(Modifier.fillMaxWidth().combinedClickable(
                        role = Role.Button, onClick = { onSelect(app) },
                        onLongClick = onInfo?.let { action -> { action(app) } },
                        onLongClickLabel = if (onInfo != null) "App info" else null,
                    ).padding(vertical = 14.dp)) {
                        Text(app.label, fontSize = 22.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        if (app.label in duplicates) Text(app.target.key, fontSize = 11.sp, maxLines = 1,
                            overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f))
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}
