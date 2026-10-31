package dev.robinsiegl.nia.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.robinsiegl.nia.R
import dev.robinsiegl.nia.model.IconCatalog
import dev.robinsiegl.nia.model.IconCategory
import dev.robinsiegl.nia.model.ShortcutIcon
import kotlinx.coroutines.launch

/** A transient preview; callers persist only after Use icon. Home and Quick corners use the same picker. */
@Composable
internal fun ShortcutIconPicker(title: String, selected: ShortcutIcon, onDismiss: () -> Unit, onSelect: (ShortcutIcon) -> Unit) {
    var pending by rememberSaveable(selected) { mutableStateOf(selected) }
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf<IconCategory?>(null) }
    var categoryMenu by remember { mutableStateOf(false) }
    val results = remember(query, category) { IconCatalog.search(query, category) }
    val grid = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current
    val imeVisible = WindowInsets.ime.getBottom(density) > 0
    fun changeQuery(value: String) {
        query = value
        // Search always covers the complete collection, even after browsing a category.
        category = null
        scope.launch { grid.scrollToItem(0) }
    }
    fun changeCategory(value: IconCategory?) {
        category = value; query = ""; categoryMenu = false
        focus.clearFocus(); keyboard?.hide()
        scope.launch { grid.scrollToItem(0) }
    }
    fun choose(icon: ShortcutIcon) {
        pending = icon
        focus.clearFocus(); keyboard?.hide()
    }
    Dialog(onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        val dialogView = LocalView.current
        val dialogWindow = (dialogView.parent as? DialogWindowProvider)?.window
        val lightBars = MaterialTheme.colorScheme.background.luminance() > 0.5f
        SideEffect {
            dialogWindow?.let { window ->
                WindowCompat.getInsetsController(window, dialogView).apply {
                    isAppearanceLightStatusBars = lightBars
                    isAppearanceLightNavigationBars = lightBars
                }
                window.isNavigationBarContrastEnforced = false
            }
        }
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding().imePadding(), contentAlignment = Alignment.TopCenter) {
                val compact = maxHeight < 650.dp || density.fontScale > 1.3f || imeVisible
                val tight = maxHeight < 420.dp || density.fontScale > 1.5f
                val showCategories = maxHeight >= 420.dp
                Column(Modifier.widthIn(max = 600.dp).fillMaxSize().testTag("shortcut-icon-picker")) {
                    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 24.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(if (tight) "Icons" else title, style = MaterialTheme.typography.titleLarge,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).semantics { heading() })
                        IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                            Icon(painterResource(R.drawable.ic_material_close), contentDescription = "Close icon picker")
                        }
                    }
                    if (!compact) Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 8.dp, bottom = 20.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(Modifier.size(64.dp).clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)), contentAlignment = Alignment.Center) {
                            ShortcutGlyph(pending, Modifier.size(36.dp))
                        }
                        Column(Modifier.weight(1f).testTag("selected-icon-preview")) {
                            Text(pending.label, style = MaterialTheme.typography.titleMedium)
                            SettingsDescription("${pending.category.label} · Preview")
                        }
                    }
                    OutlinedTextField(query, ::changeQuery, singleLine = true, label = { Text("Search icons") },
                        placeholder = { Text("Name or keyword") },
                        leadingIcon = { ShortcutGlyph(ShortcutIcon.SEARCH, Modifier.size(24.dp)) },
                        trailingIcon = if (query.isNotEmpty()) ({
                            IconButton(onClick = { changeQuery("") }) {
                                Icon(painterResource(R.drawable.ic_material_close), "Clear icon search")
                            }
                        }) else null,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).testTag("icon-search"))
                    if (showCategories) {
                        if (compact) Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Box(Modifier.weight(1f)) {
                                TextButton(onClick = { categoryMenu = true }) { Text(category?.label ?: "All icons") }
                                DropdownMenu(categoryMenu, onDismissRequest = { categoryMenu = false }) {
                                    DropdownMenuItem(text = { Text("All icons") }, onClick = { changeCategory(null) })
                                    IconCategory.entries.forEach { group ->
                                        DropdownMenuItem(text = { Text(group.label) }, onClick = { changeCategory(group) })
                                    }
                                }
                            }
                            IconResultCount(results.size)
                        } else {
                            LazyRow(Modifier.fillMaxWidth().padding(top = 12.dp).selectableGroup().testTag("icon-categories"),
                                contentPadding = PaddingValues(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                item { IconCategoryChip("All", category == null, { changeCategory(null) }) }
                                items(IconCategory.entries, key = { it.name }) { group ->
                                    IconCategoryChip(group.label, category == group, { changeCategory(group) })
                                }
                            }
                            IconResultCount(results.size, Modifier.padding(start = 24.dp, top = 8.dp, bottom = 4.dp))
                        }
                    }
                    if (results.isEmpty()) Column(Modifier.weight(1f).fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text("No icons found", style = MaterialTheme.typography.titleMedium)
                        SettingsDescription("Try a name or a word like music, travel or money.", Modifier.padding(top = 8.dp))
                        TextButton(onClick = { changeQuery(""); focus.clearFocus(); keyboard?.hide() }) { Text("Clear search") }
                    } else LazyVerticalGrid(columns = GridCells.Adaptive(if (density.fontScale > 1.3f) 128.dp else 104.dp),
                        state = grid, contentPadding = PaddingValues(16.dp),
                        modifier = Modifier.weight(1f).fillMaxWidth().selectableGroup().testTag("shortcut-icon-grid")) {
                        items(results, key = { it.name }) { icon ->
                            IconOption(icon, pending == icon, { choose(icon) })
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                    Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (!tight) TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text("Cancel") }
                        Button(onClick = { onSelect(pending) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                            .semantics { stateDescription = "Selected icon: ${pending.label}" }) {
                            ShortcutGlyph(pending, Modifier.size(22.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Use icon")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IconCategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val ink = MaterialTheme.colorScheme.onSurface
    FilterChip(selected, onClick, label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ink.copy(alpha = 0.1f), selectedLabelColor = ink),
        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = selected,
            borderColor = ink.copy(alpha = 0.25f), selectedBorderColor = ink.copy(alpha = 0.7f)))
}

@Composable
private fun IconResultCount(count: Int, modifier: Modifier = Modifier) {
    Text("$count ${if (count == 1) "icon" else "icons"}", style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite })
}

@Composable
private fun IconOption(icon: ShortcutIcon, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Column(Modifier.padding(4.dp).fillMaxWidth().heightIn(min = 96.dp).clip(shape)
        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = if (selected) 0.1f else 0f))
        .border(if (selected) 1.dp else 0.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = if (selected) 0.7f else 0f), shape)
        .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
        .semantics { contentDescription = icon.label }
        .padding(horizontal = 4.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            ShortcutGlyph(icon, Modifier.size(28.dp))
            if (selected) Icon(painterResource(R.drawable.ic_material_check), contentDescription = null,
                modifier = Modifier.align(Alignment.TopEnd).size(14.dp))
        }
        Text(icon.label, style = MaterialTheme.typography.labelSmall, maxLines = 2,
            overflow = TextOverflow.Ellipsis, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.clearAndSetSemantics {})
    }
}
