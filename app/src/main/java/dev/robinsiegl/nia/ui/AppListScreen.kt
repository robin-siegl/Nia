package dev.robinsiegl.nia.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.robinsiegl.nia.model.AppEntry
import dev.robinsiegl.nia.model.AppSearch
import dev.robinsiegl.nia.model.CornerPosition
import dev.robinsiegl.nia.R
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
    onAssignCorner: ((AppEntry, CornerPosition) -> Unit)? = null,
    onAddHome: ((AppEntry) -> Unit)? = null, canAddHome: Boolean = true,
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
    var menuApp by remember { mutableStateOf<AppEntry?>(null) }
    BackHandler(enabled = interactionEnabled) {
        if (imeVisible) { focusManager.clearFocus(); keyboard?.hide() } else onBack()
    }
    BackHandler(enabled = !interactionEnabled) { /* Do not leave midway through a transition or saved selection. */ }
    LaunchedEffect(autofocus, interactionEnabled) {
        if (!interactionEnabled) { menuApp = null; focusManager.clearFocus(); keyboard?.hide() }
        else if (autofocus) { focus.requestFocus(); keyboard?.show() }
    }
    LaunchedEffect(query) { menuApp = null; listState.scrollToItem(0) }
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
            modifier = Modifier.fillMaxWidth().padding(end = 12.dp).testTag("apps-search").focusRequester(focus)
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
                        Box {
                        Column(Modifier.fillMaxWidth().combinedClickable(
                            enabled = interactionEnabled,
                            role = Role.Button, onClick = { onSelect(app) },
                            onLongClick = if (onInfo != null || onAssignCorner != null || onAddHome != null) ({
                                focusManager.clearFocus(); keyboard?.hide(); menuApp = app
                            }) else null,
                            onLongClickLabel = if (onInfo != null || onAssignCorner != null || onAddHome != null) "App actions" else null,
                        ).padding(vertical = 14.dp)) {
                            Text(app.label, fontSize = 22.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            if (app.label in duplicates) Text(app.target.key, fontSize = 11.sp, maxLines = 1,
                                overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f))
                        }
                        DropdownMenu(expanded = interactionEnabled && menuApp?.target == app.target,
                            modifier = Modifier.widthIn(min = 256.dp, max = 320.dp).testTag("app-actions-menu"),
                            shape = RoundedCornerShape(20.dp), tonalElevation = 0.dp, shadowElevation = 8.dp,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)),
                            onDismissRequest = { menuApp = null }) {
                            Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                                SettingsDescription("App actions")
                                Text(app.label, style = MaterialTheme.typography.titleMedium, maxLines = 2,
                                    overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                            }
                            if (onAddHome != null) DropdownMenuItem(text = { Column {
                                Text("Add to Home")
                                if (!canAddHome) SettingsDescription("Home already has nine shortcuts")
                            } }, enabled = canAddHome,
                                leadingIcon = { Icon(painterResource(R.drawable.ic_material_add), null) }, onClick = {
                                    menuApp = null; onAddHome(app)
                                })
                            if (onInfo != null) DropdownMenuItem(text = { Text("App info") },
                                leadingIcon = { Icon(painterResource(R.drawable.ic_material_info), null) }, onClick = {
                                menuApp = null; onInfo(app)
                            })
                            if (onAssignCorner != null && (onInfo != null || onAddHome != null)) HorizontalDivider(
                                Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
                            if (onAssignCorner != null) CornerPosition.entries.forEach { position ->
                                DropdownMenuItem(text = { Text("${position.label} quick corner") }, leadingIcon = {
                                    Icon(painterResource(if (position == CornerPosition.LEFT) R.drawable.ic_material_south_west
                                        else R.drawable.ic_material_south_east), null)
                                }, onClick = {
                                    menuApp = null; onAssignCorner(app, position)
                                })
                            }
                        }
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
