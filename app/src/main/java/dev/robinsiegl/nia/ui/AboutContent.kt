package dev.robinsiegl.nia.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.robinsiegl.nia.BuildConfig
import dev.robinsiegl.nia.model.AppRelease
import dev.robinsiegl.nia.model.ReleaseVersion
import dev.robinsiegl.nia.model.UpdateCheck

internal object ProjectLinks {
    const val REPOSITORY = AppRelease.REPOSITORY
    const val RELEASES = "$REPOSITORY/releases"
    const val ISSUES = "$REPOSITORY/issues/new"
    const val PRIVACY = "$REPOSITORY/blob/main/docs/PRIVACY.md"
    const val NOTICES = "$REPOSITORY/blob/main/THIRD_PARTY_NOTICES.md"
}

@Composable
internal fun AboutContent(onOpenLink: (String) -> Unit, updateCheck: UpdateCheck = UpdateCheck.Idle,
    onCheckUpdates: () -> Unit = {}) {
    Text("nia", fontSize = 48.sp, fontWeight = FontWeight.Light)
    Text("A quieter way home.", fontSize = 20.sp, modifier = Modifier.padding(top = 4.dp))
    Text("Version ${BuildConfig.VERSION_NAME}", fontSize = 14.sp, modifier = Modifier.padding(top = 16.dp))
    SectionLabel("UPDATES")
    UpdateContent(updateCheck, onCheckUpdates, onOpenLink)
    SectionLabel("PRIVACY")
    Text("No accounts, advertising or analytics. nia works offline.", fontSize = 16.sp)
    Text("App names are used on your device to show your apps and search results. Your assignments and preferences stay on your device.",
        fontSize = 14.sp, modifier = Modifier.padding(top = 12.dp))
    Text("Exported backups include your settings and assigned apps. You choose where to save them.",
        fontSize = 14.sp, modifier = Modifier.padding(top = 12.dp))
    Text("Checking for updates contacts GitHub only when you ask. Your app list and settings are never sent.",
        fontSize = 14.sp, modifier = Modifier.padding(top = 12.dp))
    TextButton(onClick = { onOpenLink(ProjectLinks.PRIVACY) }, modifier = Modifier.fillMaxWidth()) { Text("Privacy details") }
    SectionLabel("PROJECT")
    TextButton(onClick = { onOpenLink(ProjectLinks.REPOSITORY) }, modifier = Modifier.fillMaxWidth()) { Text("View on GitHub") }
    TextButton(onClick = { onOpenLink(ProjectLinks.ISSUES) }, modifier = Modifier.fillMaxWidth()) { Text("Report an issue") }
    SectionLabel("LICENSE INFORMATION")
    Text("A source license for nia has not been specified. Third-party components retain their own licenses.", fontSize = 14.sp)
    TextButton(onClick = { onOpenLink(ProjectLinks.NOTICES) }, modifier = Modifier.fillMaxWidth()) { Text("Third-party notices") }
}

@Composable
internal fun UpdateContent(state: UpdateCheck, onCheck: () -> Unit, onOpenLink: (String) -> Unit,
    installedVersion: String = BuildConfig.VERSION_NAME) {
    when (state) {
        UpdateCheck.Idle -> Text("Check GitHub for the latest public release. No automatic checks or downloads.", fontSize = 14.sp)
        UpdateCheck.Checking -> Text("Checking GitHub…", fontSize = 14.sp)
        UpdateCheck.NoRelease -> Text("No public release is available yet.", fontSize = 14.sp)
        is UpdateCheck.Failed -> Text(state.message, fontSize = 14.sp)
        is UpdateCheck.Result -> {
            val local = ReleaseVersion.parse(installedVersion.removeSuffix("-debug"))
            val latest = state.release
            Text(when {
                local == null -> "Latest release: ${latest.version}"
                latest.version > local -> "Update available: ${latest.version}"
                latest.version == local -> "You’re up to date. Latest release: ${latest.version}"
                else -> "Latest public release: ${latest.version}. Your installed version is newer."
            }, fontSize = 16.sp)
            latest.apkUrl?.let { url ->
                TextButton(onClick = { onOpenLink(url) }, modifier = Modifier.fillMaxWidth()) { Text("Download ${latest.version} APK") }
            }
            if (latest.apkUrl == null) Text("The APK hasn’t been attached to this release yet.", fontSize = 14.sp,
                modifier = Modifier.padding(top = 8.dp))
            TextButton(onClick = { onOpenLink(latest.pageUrl) }, modifier = Modifier.fillMaxWidth()) { Text("View release on GitHub") }
        }
    }
    TextButton(onClick = onCheck, enabled = state != UpdateCheck.Checking, modifier = Modifier.fillMaxWidth()) {
        Text(if (state == UpdateCheck.Idle) "Check for updates" else if (state == UpdateCheck.Checking) "Checking…" else "Check again")
    }
    if (state is UpdateCheck.Failed || state == UpdateCheck.NoRelease) {
        TextButton(onClick = { onOpenLink(ProjectLinks.RELEASES) }, modifier = Modifier.fillMaxWidth()) { Text("View releases on GitHub") }
    }
}
