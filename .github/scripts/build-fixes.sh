#!/usr/bin/env bash
set -euo pipefail

cd LiveBridge

python3 - <<'PY'
from pathlib import Path
root = Path(".")

p = root / "app/build.gradle.kts"
s = p.read_text()
needle = 'implementation("androidx.compose.material:material-icons-core")'
replacement = needle + '\n    implementation("androidx.compose.material:material-icons-extended")'
if 'material-icons-extended' not in s:
    if needle not in s:
        raise SystemExit("material-icons-core dependency not found")
    s = s.replace(needle, replacement, 1)
p.write_text(s)

p = root / "app/src/main/java/com/livebridge/ui/Common.kt"
s = p.read_text()
s = s.replace('import androidx.compose.ui.text.input.KeyboardOptions', 'import androidx.compose.foundation.text.KeyboardOptions')
p.write_text(s)

p = root / "app/src/main/java/com/livebridge/ui/LiveDashboardScreen.kt"
s = p.read_text()
if 'import androidx.compose.foundation.layout.RowScope' not in s:
    s = s.replace('import androidx.compose.foundation.layout.Row\n', 'import androidx.compose.foundation.layout.Row\nimport androidx.compose.foundation.layout.RowScope\n', 1)
s = s.replace('private fun QuickControl(icon:', 'private fun RowScope.QuickControl(icon:', 1)
p.write_text(s)

p = root / "app/src/main/java/com/livebridge/ui/ElementsScreen.kt"
s = p.read_text()
# Normalize Compose Alignment imports/usages deterministically.
lines = s.splitlines()
lines = [line for line in lines if not line.strip().startswith("import androidx.compose.ui.Alignment")]
s = "\n".join(lines) + ("\n" if s.endswith("\n") else "")
marker = s.find("\n", s.find("package "))
s = s[:marker+1] + "import androidx.compose.ui.Alignment\n" + s[marker+1:]
s = s.replace("androidx.compose.ui.Alignment.CenterHorizontally", "Alignment.CenterHorizontally")
s = s.replace("CenterHorizontally", "Alignment.CenterHorizontally")
s = s.replace("Alignment.Alignment.CenterHorizontally", "Alignment.CenterHorizontally")
p.write_text(s)

# Visual polish: modern dark studio palette, safe-area spacing, and touch-friendly selectors.
p = root / "app/src/main/java/com/livebridge/ui/Theme.kt"
s = p.read_text()
replacements = {
    "primary = Color(0xFF6EA8FF),": "primary = Color(0xFF7C8CFF),",
    "primaryContainer = Color(0xFF18365F),": "primaryContainer = Color(0xFF27215A),",
    "secondary = Color(0xFF8E9BB3),": "secondary = Color(0xFF9B8CFF),",
    "background = Color(0xFF080B10),": "background = Color(0xFF070910),",
    "surface = Color(0xFF10151D),": "surface = Color(0xFF0D1220),",
    "surfaceVariant = Color(0xFF171E28),": "surfaceVariant = Color(0xFF121A2A),",
    "outline = Color(0xFF2C3747),": "outline = Color(0xFF303B58),",
    "outlineVariant = Color(0xFF202936),": "outlineVariant = Color(0xFF202943),",
    "val info = Color(0xFF6EA8FF)": "val info = Color(0xFF6F8CFF)",
    "val live = Color(0xFFFF4D55)": "val live = Color(0xFFFF3D68)",
    "extraSmall = RoundedCornerShape(7.dp),": "extraSmall = RoundedCornerShape(8.dp),",
    "small = RoundedCornerShape(11.dp),": "small = RoundedCornerShape(14.dp),",
    "medium = RoundedCornerShape(16.dp),": "medium = RoundedCornerShape(18.dp),",
    "extraLarge = RoundedCornerShape(28.dp)": "extraLarge = RoundedCornerShape(30.dp)"
}
for a,b in replacements.items():
    s = s.replace(a,b)
p.write_text(s)

p = root / "app/src/main/java/com/livebridge/ui/StudioScreen.kt"
s = p.read_text()
if "import androidx.compose.ui.unit.dp" not in s:
    s = s.replace("import androidx.compose.ui.Modifier\n", "import androidx.compose.ui.Modifier\nimport androidx.compose.ui.unit.dp\n", 1)
s = s.replace("Box(Modifier.fillMaxSize().padding(top = 28.dp, bottom = 8.dp)) {", "Box(Modifier.fillMaxSize()) {")
p.write_text(s)

p = root / "app/src/main/java/com/livebridge/ui/SetupScreen.kt"
s = p.read_text()
if "import androidx.compose.ui.text.style.TextOverflow" not in s:
    s = s.replace("import androidx.compose.ui.platform.LocalConfiguration\n", "import androidx.compose.ui.platform.LocalConfiguration\nimport androidx.compose.ui.text.style.TextOverflow\n", 1)
s = s.replace('StepHeader(1, 3, "Configurer", "Préparez votre diffusion")', 'StepHeader(1, 3, "Prêt à diffuser", "Préparez votre diffusion")')
old_platform = """PLATFORMS.forEach { name ->
                        FilterChip(selected = platform.value == name, onClick = { platform.value = name }, label = { Text(name) })
                    }"""
new_platform = """PLATFORMS.forEach { name ->
                        FilterChip(
                            modifier = Modifier.weight(1f),
                            selected = platform.value == name,
                            onClick = { platform.value = name },
                            label = { Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        )
                    }"""
s = s.replace(old_platform, new_platform)
p.write_text(s)

# OBS-inspired mobile studio layout.
p = root / "app/src/main/java/com/livebridge/ui/SetupScreen.kt"
s = p.read_text()

imports = [
    "import androidx.compose.material.icons.Icons",
    "import androidx.compose.foundation.layout.Box",
    "import androidx.compose.foundation.layout.size",
    "import androidx.compose.material3.Icon",
    "import androidx.compose.material3.IconButton",
    "import androidx.compose.foundation.background\nimport androidx.compose.foundation.horizontalScroll",
    "import androidx.compose.material.icons.filled.Add",
    "import androidx.compose.material.icons.filled.GraphicEq",
    "import androidx.compose.material.icons.filled.Layers",
    "import androidx.compose.ui.text.style.TextOverflow",
]
for imp in imports:
    if imp not in s:
        s = s.replace("\n", "\n" + imp + "\n", 1)

portrait_old = """    } else {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
            StepHeader(1, 3, "Configurer", "Préparez votre diffusion")
            form()
            Spacer(Modifier.height(Spacing.s))
            preview(Modifier.fillMaxWidth())
        }
    }
}"""
portrait_new = """    } else {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Spacing.l, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StudioTopBar()
            preview(Modifier.fillMaxWidth())
            SceneStrip(studio, onOpenElements)
            SourceAudioStrip(studio, ui, onOpenElements)
            form()
        }
    }
}"""
if portrait_old in s:
    s = s.replace(portrait_old, portrait_new, 1)

obs = """@Composable
private fun StudioTopBar() {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Studio", style = MaterialTheme.typography.headlineSmall)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(7.dp).background(SemanticColors.success, CircleShape))
                Text(" Prêt", style = MaterialTheme.typography.bodySmall, color = SemanticColors.success)
            }
        }
        IconButton(onClick = {}) { Icon(Icons.Filled.Tune, contentDescription = "Réglages") }
    }
}

@Composable
private fun SceneStrip(studio: com.livebridge.studio.StudioState, onOpenElements: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Scènes", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            TextButton(onClick = onOpenElements) { Text("Gérer") }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            studio.scenes.forEach { scene ->
                FilterChip(selected = scene.id == studio.current.id, onClick = onOpenElements, label = { Text(scene.name, maxLines = 1, overflow = TextOverflow.Ellipsis) })
            }
            IconButton(onClick = onOpenElements) { Icon(Icons.Filled.Add, contentDescription = "Ajouter") }
        }
    }
}

@Composable
private fun SourceAudioStrip(
    studio: com.livebridge.studio.StudioState,
    ui: RtmpUi,
    onOpenElements: () -> Unit
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.weight(1.3f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("Sources", style = MaterialTheme.typography.titleSmall)
            studio.current.sources.take(4).forEach { source ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(source.name, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(if (source.visible) "●" else "○", color = if (source.visible) SemanticColors.success else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            TextButton(onClick = onOpenElements) { Text("Modifier") }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Audio", style = MaterialTheme.typography.titleSmall)
            Text("Micro  ${if (ui.micMuted) "OFF" else "ON"}", style = MaterialTheme.typography.bodySmall)
            Text("Système  ON", style = MaterialTheme.typography.bodySmall)
        }
    }
}

"""
if "private fun StudioTopBar()" not in s:
    insert_at = s.rfind("\n}") + 2
    s = s[:insert_at] + obs + s[insert_at:]

# Add imports used by the studio strip if they are not already present.
for imp in ["import androidx.compose.foundation.shape.CircleShape", "import androidx.compose.material.icons.filled.Tune"]:
    if imp not in s:
        s = s.replace("\n", "\n" + imp + "\n", 1)

p.write_text(s)
p = root / "app/src/main/java/com/livebridge/ui/PreviewCanvas.kt"
s = p.read_text()
old = ') {\n    Canvas(Modifier.fillMaxSize()) {\n'
if old in s and 'val primary = MaterialTheme.colorScheme.primary' not in s:
    s = s.replace(old, ') {\n    val primary = MaterialTheme.colorScheme.primary\n    Canvas(Modifier.fillMaxSize()) {\n', 1)
s = s.replace('color = MaterialTheme.colorScheme.primary,', 'color = primary,')
s = s.replace('drawCircle(MaterialTheme.colorScheme.primary, handle, point)', 'drawCircle(primary, handle, point)')
p.write_text(s)
# Replace the actual live dashboard UI as well. The setup screen is not the screen
# users see after pressing Start; keep the runtime screen OBS-like too.
p = root / "app/src/main/java/com/livebridge/ui/LiveDashboardScreen.kt"
s = p.read_text()
for imp in [
    "import androidx.compose.foundation.shape.CircleShape",
    "import androidx.compose.material3.FilterChip",
    "import androidx.compose.material3.Icon",
    "import androidx.compose.foundation.horizontalScroll",
    "import androidx.compose.foundation.rememberScrollState",
    "import androidx.compose.foundation.shape.RoundedCornerShape",
    "import androidx.compose.material.icons.filled.GraphicEq",
    "import androidx.compose.material.icons.filled.Layers",
    "import androidx.compose.material.icons.filled.Videocam",
    "import com.livebridge.studio.SourceType"
]:
    if imp not in s:
        s = s.replace("\n", "\n" + imp + "\n", 1)

# Add a real mobile-studio strip immediately before the quick controls.
studio_helpers = r'''
@Composable
private fun LiveStudioStrip(store: com.livebridge.studio.StudioStore, scene: com.livebridge.studio.Scene, ui: RtmpUi) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Scènes", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Text("LIVE", style = MaterialTheme.typography.labelSmall, color = LiveRed, fontWeight = FontWeight.Bold)
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            store.state.value.scenes.forEach { item ->
                FilterChip(
                    selected = item.id == scene.id,
                    onClick = { store.selectScene(item.id) },
                    label = { Text(item.name, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) }
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Column(
                Modifier.weight(1.25f).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(11.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(androidx.compose.material.icons.Icons.Filled.Layers, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Text("Sources", Modifier.padding(start = 7.dp).weight(1f), style = MaterialTheme.typography.titleSmall)
                }
                scene.sources.take(4).forEach { source ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (source.type == SourceType.CAMERA) androidx.compose.material.icons.Icons.Filled.Videocam else androidx.compose.material.icons.Icons.Filled.Layers,
                            null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(source.name, Modifier.padding(start = 7.dp).weight(1f), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        Box(Modifier.size(7.dp).background(if (source.visible) SemanticColors.success else MaterialTheme.colorScheme.outline, CircleShape))
                    }
                }
            }
            Column(
                Modifier.weight(.95f).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(11.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(androidx.compose.material.icons.Icons.Filled.GraphicEq, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Text("Mixeur", Modifier.padding(start = 7.dp), style = MaterialTheme.typography.titleSmall)
                }
                LiveMeter("Micro", !ui.micMuted)
                LiveMeter("Système", true)
            }
        }
    }
}

@Composable
private fun LiveMeter(label: String, active: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            repeat(14) { i ->
                Box(
                    Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(2.dp))
                        .background(if (active && i < 10) SemanticColors.success else MaterialTheme.colorScheme.outline)
                )
            }
        }
    }
}
'''
if "private fun LiveStudioStrip(" not in s:
    insert_at = s.rfind("\n}") + 2
    s = s[:insert_at] + "\n" + studio_helpers + s[insert_at:]

# Inject the studio controls into both portrait and landscape layouts.
s = s.replace(
    """                LiveHeader(ui)
                preview()
            }
            Column(Modifier.weight(.9f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(Spacing.m)) {""",
    """                LiveHeader(ui)
                preview()
                LiveStudioStrip(store, scene, ui)
            }
            Column(Modifier.weight(.9f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(Spacing.m)) {""",
    1
)
s = s.replace(
    """            LiveHeader(ui)
            preview()
            StatsGrid(ui)
            controls()""",
    """            LiveHeader(ui)
            preview()
            LiveStudioStrip(store, scene, ui)
            StatsGrid(ui)
            controls()""",
    1
)
p.write_text(s)

PY


# Final source-of-truth UI: overwrite the launcher setup screen with the mobile OBS studio layout.
python3 - <<'PY'
from pathlib import Path
import base64, gzip
data = """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"""
Path("app/src/main/java/com/livebridge/ui/SetupScreen.kt").write_bytes(gzip.decompress(base64.b64decode(data)))
PY
