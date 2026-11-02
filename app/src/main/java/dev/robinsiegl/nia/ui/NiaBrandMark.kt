package dev.robinsiegl.nia.ui

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import dev.robinsiegl.nia.R

/** Decorative alongside the visible app name; follows nia's current foreground. */
@Composable
internal fun NiaBrandMark(modifier: Modifier = Modifier) {
    Icon(painterResource(R.drawable.ic_nia_mark), contentDescription = null,
        modifier = modifier, tint = MaterialTheme.colorScheme.onBackground)
}
