package dev.robinsiegl.nia.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.CircularProgressIndicator
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
    SectionLabel("Updates")
    UpdateContent(updateCheck, onCheckUpdates, onOpenLink)
    SectionLabel("Privacy")
    Text("No accounts, ads or analytics.", fontSize = 16.sp)
    SettingsDescription("Your app list and preferences stay on your device. You choose where to save backup files.",
        Modifier.padding(top = 12.dp))
    SettingsDescription("nia works offline. Checking for updates contacts GitHub only when you ask. Your app list and settings are never sent.",
        Modifier.padding(top = 12.dp))
    SettingsAction("Privacy policy", "", { onOpenLink(ProjectLinks.PRIVACY) })
    SectionLabel("Project")
    SettingsAction("View on GitHub", "", { onOpenLink(ProjectLinks.REPOSITORY) })
    SettingsAction("Report an issue", "", { onOpenLink(ProjectLinks.ISSUES) })
    SectionLabel("Licenses")
    SettingsDescription("A source license for nia has not been specified. Third-party components retain their own licenses.")
    SettingsAction("Third-party notices", "", { onOpenLink(ProjectLinks.NOTICES) })
}

@Composable
internal fun UpdateContent(state: UpdateCheck, onCheck: () -> Unit, onOpenLink: (String) -> Unit,
    installedVersion: String = BuildConfig.VERSION_NAME) {
    when (state) {
        UpdateCheck.Idle -> SettingsDescription("Check GitHub for the latest release. Updates are checked and downloaded only when you choose.")
        UpdateCheck.Checking -> SettingsDescription("Looking for the latest GitHub release…")
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
    OutlinedButton(onClick = onCheck, enabled = state != UpdateCheck.Checking,
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp).heightIn(min = 48.dp)) {
        if (state == UpdateCheck.Checking) {
            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(12.dp))
        }
        Text(if (state == UpdateCheck.Idle) "Check for updates" else if (state == UpdateCheck.Checking) "Checking…" else "Check again")
    }
    if (state is UpdateCheck.Failed || state == UpdateCheck.NoRelease) {
        TextButton(onClick = { onOpenLink(ProjectLinks.RELEASES) }, modifier = Modifier.fillMaxWidth()) { Text("View releases on GitHub") }
    }
}
