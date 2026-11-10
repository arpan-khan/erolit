package dev.erolit.reader.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.erolit.reader.AppContainer
import dev.erolit.reader.core.settings.AppSettings
import dev.erolit.reader.ui.components.Chip
import dev.erolit.reader.ui.components.ChipRow
import dev.erolit.reader.ui.reader.readerFontFamily
import dev.erolit.reader.ui.reader.readerFontOptions
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun ReadingSettingsPanel(container: AppContainer, s: AppSettings) {
    val scope = rememberCoroutineScope()
    var size by remember(s.fontSizeSp) { mutableStateOf(s.fontSizeSp.toFloat()) }
    var spacing by remember(s.lineHeightMultiplier) { mutableStateOf(s.lineHeightMultiplier) }

    Column {
        Text("Font size: ${size.roundToInt()} sp", style = MaterialTheme.typography.labelLarge)
        Slider(
            value = size,
            onValueChange = { size = it },
            onValueChangeFinished = { scope.launch { container.settings.setFontSize(size.roundToInt()) } },
            valueRange = 14f..26f,
            steps = 11,
        )
        Text(
            "Line spacing: ${String.format(Locale.US, "%.1f", spacing)}",
            style = MaterialTheme.typography.labelLarge,
        )
        Slider(
            value = spacing,
            onValueChange = { spacing = it },
            onValueChangeFinished = { scope.launch { container.settings.setLineHeight(spacing) } },
            valueRange = 1.2f..2.0f,
            steps = 7,
        )
        Text("Font", style = MaterialTheme.typography.labelLarge)
        ChipRow {
            readerFontOptions.forEach { option ->
                Chip(option.label, s.fontFamily == option.id) {
                    scope.launch { container.settings.setFontFamily(option.id) }
                }
            }
        }
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        ) {
            Text(
                text = "The right font makes every chapter feel like a book. " +
                    "Adjust the size and spacing until it reads just right.",
                fontFamily = readerFontFamily(s.fontFamily),
                fontSize = size.sp,
                lineHeight = (size * spacing).sp,
                textAlign = if (s.justifyText) TextAlign.Justify else TextAlign.Start,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(14.dp),
            )
        }
        SwitchRow("Justify text", s.justifyText) { scope.launch { container.settings.setJustify(it) } }
        SwitchRow("Keep screen on while reading", s.keepScreenOn) {
            scope.launch { container.settings.setKeepScreenOn(it) }
        }
        Text("Theme", style = MaterialTheme.typography.labelLarge)
        ChipRow {
            Chip("System", s.theme == "system") { scope.launch { container.settings.setTheme("system") } }
            Chip("Light", s.theme == "light") { scope.launch { container.settings.setTheme("light") } }
            Chip("Dark", s.theme == "dark") { scope.launch { container.settings.setTheme("dark") } }
        }
    }
}

@Composable
fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
