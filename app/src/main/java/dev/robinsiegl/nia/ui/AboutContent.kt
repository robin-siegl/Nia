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

internal object ProjectLinks {
    const val REPOSITORY = "https://github.com/robin-siegl/Nia"
    const val ISSUES = "$REPOSITORY/issues/new"
    const val PRIVACY = "$REPOSITORY/blob/main/docs/PRIVACY.md"
    const val NOTICES = "$REPOSITORY/blob/main/THIRD_PARTY_NOTICES.md"
}

@Composable
internal fun AboutContent(onOpenLink: (String) -> Unit) {
    Text("nia", fontSize = 48.sp, fontWeight = FontWeight.Light)
    Text("A quieter way home.", fontSize = 20.sp, modifier = Modifier.padding(top = 4.dp))
    Text("Version ${BuildConfig.VERSION_NAME}", fontSize = 14.sp, modifier = Modifier.padding(top = 16.dp))
    SectionLabel("PRIVACY")
    Text("No accounts, advertising or analytics. nia works offline.", fontSize = 16.sp)
    Text("App names are used on your device to show your apps and search results. Your assignments and preferences stay on your device.",
        fontSize = 14.sp, modifier = Modifier.padding(top = 12.dp))
    Text("Exported backups include your settings and assigned apps. You choose where to save them.",
        fontSize = 14.sp, modifier = Modifier.padding(top = 12.dp))
    TextButton(onClick = { onOpenLink(ProjectLinks.PRIVACY) }, modifier = Modifier.fillMaxWidth()) { Text("Privacy details") }
    SectionLabel("PROJECT")
    TextButton(onClick = { onOpenLink(ProjectLinks.REPOSITORY) }, modifier = Modifier.fillMaxWidth()) { Text("View on GitHub") }
    TextButton(onClick = { onOpenLink(ProjectLinks.ISSUES) }, modifier = Modifier.fillMaxWidth()) { Text("Report an issue") }
    SectionLabel("LICENSE INFORMATION")
    Text("A source license for nia has not been specified. Third-party components retain their own licenses.", fontSize = 14.sp)
    TextButton(onClick = { onOpenLink(ProjectLinks.NOTICES) }, modifier = Modifier.fillMaxWidth()) { Text("Third-party notices") }
}
