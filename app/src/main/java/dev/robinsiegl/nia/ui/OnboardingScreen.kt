package dev.robinsiegl.nia.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.robinsiegl.nia.model.HomeLayout

@Composable
fun OnboardingScreen(layout: HomeLayout, isDefault: Boolean, onLayout: (HomeLayout) -> Unit,
    onSettings: () -> Unit, onDefault: () -> Unit, onStart: () -> Unit, onApps: () -> Unit, onImport: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp)) {
        Spacer(Modifier.height(24.dp))
        Text("nia", fontSize = 56.sp, fontWeight = FontWeight.Light)
        Text("A quieter way home.", fontSize = 21.sp, modifier = Modifier.padding(top = 8.dp, bottom = 24.dp))
        Text("Five everyday shortcuts. Everything else is a name away.", fontSize = 16.sp)
        SectionLabel("YOUR HOME")
        ChoiceRow(listOf(HomeLayout.GRID to "Icon grid", HomeLayout.LIST to "Text list"), layout, onLayout)
        Spacer(Modifier.height(20.dp))
        OutlinedButton(onClick = onSettings, modifier = Modifier.fillMaxWidth()) { Text("Choose apps and appearance") }
        Spacer(Modifier.height(16.dp))
        Text("To open nia when you press Home, choose it as your default launcher.", fontSize = 14.sp)
        Spacer(Modifier.height(12.dp))
        if (isDefault) Text("nia is your default launcher.", fontSize = 14.sp)
        else Button(onClick = onDefault, modifier = Modifier.fillMaxWidth()) { Text("Set as default launcher") }
        OutlinedButton(onClick = onStart, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) { Text("Start using nia") }
        TextButton(onClick = onImport, modifier = Modifier.fillMaxWidth()) { Text("Restore a settings backup") }
        TextButton(onClick = onApps, modifier = Modifier.fillMaxWidth()) { Text("All apps") }
    }
}
