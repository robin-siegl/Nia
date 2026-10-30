package dev.robinsiegl.nia.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.testTag
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppListScreen(
    apps: List<AppEntry>, loading: Boolean, autofocus: Boolean, title: String = "Apps",
    onBack: () -> Unit, onSelect: (AppEntry) -> Unit, onSettings: (() -> Unit)? = null,
    onInfo: ((AppEntry) -> Unit)? = null,
    alphabetScrubber: Boolean = false,
    interactionEnabled: Boolean = true, gesturesEnabled: Boolean = true,
    listState: LazyListState = rememberLazyListState(), headerDragModifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val results = remember(apps, query) { AppSearch.filter(apps, query) }
    val duplicates = remember(apps) { apps.groupingBy { it.label }.eachCount().filterValues { it > 1 }.keys }
    val sections = remember(results) { AppSearch.sections(results) }
    val scope = rememberCoroutineScope()
    var jumpJob by remember { mutableStateOf<Job?>(null) }
    val showScrubber = alphabetScrubber && interactionEnabled && query.isBlank() && sections.size > 1
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    BackHandler(enabled = interactionEnabled) {
        if (imeVisible) { focusManager.clearFocus(); keyboard?.hide() } else onBack()
    }
    BackHandler(enabled = !interactionEnabled) { /* Do not leave midway through a transition or saved selection. */ }
    LaunchedEffect(autofocus, interactionEnabled) {
        if (!interactionEnabled) { focusManager.clearFocus(); keyboard?.hide() }
        else if (autofocus) { focus.requestFocus(); keyboard?.show() }
    }
    LaunchedEffect(query) { listState.scrollToItem(0) }
    fun submitSearch() {
        if (!interactionEnabled) return
        keyboard?.hide(); focusManager.clearFocus()
        if (!loading && results.size == 1) onSelect(results.single())
    }
    Column(Modifier.fillMaxSize().testTag("apps-screen").padding(start = 24.dp, end = 12.dp)) {
        Column(headerDragModifier) {
        ScreenHeader(title, { if (interactionEnabled) onBack() }, trailing = if (onSettings != null) {
            { SettingsButton({ if (interactionEnabled) onSettings() }) }
        } else null)
        OutlinedTextField(query, { query = it }, singleLine = true, label = { Text("Search apps") },
            readOnly = !interactionEnabled,
            modifier = Modifier.fillMaxWidth().padding(end = 12.dp).focusRequester(focus)
                .focusProperties { canFocus = interactionEnabled }.onPreviewKeyEvent { event ->
                    if (interactionEnabled && (event.key == Key.Enter || event.key == Key.NumPadEnter)) {
                        if (event.type == KeyEventType.KeyUp) submitSearch()
                        true
                    } else false
                },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { submitSearch() }),
            trailingIcon = if (query.isNotEmpty()) {
                { TextButton(onClick = { if (interactionEnabled) query = "" }) { Text("Clear", fontSize = 12.sp) } }
            } else null)
        Spacer(Modifier.height(16.dp))
        }
        when {
            loading || results.isEmpty() -> Box(Modifier.weight(1f).fillMaxWidth().then(headerDragModifier)) {
                Text(if (loading) "Loading apps…" else if (apps.isEmpty()) "No launchable apps available." else "No apps found.",
                    modifier = Modifier.padding(vertical = 24.dp))
            }
            else -> Box(Modifier.weight(1f).fillMaxWidth()) {
                LazyColumn(Modifier.fillMaxSize().testTag("apps-list").padding(end = if (showScrubber) 48.dp else 12.dp),
                    state = listState, userScrollEnabled = gesturesEnabled) {
                    items(results, key = { it.target.key }) { app ->
                        Column(Modifier.fillMaxWidth().combinedClickable(
                            enabled = interactionEnabled,
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
                if (showScrubber) AlphabetScrubber(sections, listState.firstVisibleItemIndex, { index ->
                    jumpJob?.cancel()
                    jumpJob = scope.launch { listState.scrollToItem(index) }
                }, Modifier.align(Alignment.CenterEnd))
            }
        }
    }
}
