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


# Replace the actual launcher SetupScreen with the mobile OBS-style studio UI.
python3 - <<'PY'
from pathlib import Path
import base64, gzip
payload = "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"
payload += "=" * (-len(payload) % 4)
target = Path("app/src/main/java/com/livebridge/ui/SetupScreen.kt")
target.parent.mkdir(parents=True, exist_ok=True)
target.write_bytes(gzip.decompress(base64.b64decode(payload)))
PY
