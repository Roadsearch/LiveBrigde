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
if "import androidx.compose.foundation.layout.safeDrawingPadding" not in s:
    s = s.replace("import androidx.compose.animation.tween\n", "import androidx.compose.animation.tween\nimport androidx.compose.foundation.layout.safeDrawingPadding\n", 1)
s = s.replace("Box(Modifier.fillMaxSize()) {", "Box(Modifier.fillMaxSize().safeDrawingPadding()) {")
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

p = root / "app/src/main/java/com/livebridge/ui/PreviewCanvas.kt"
s = p.read_text()
old = ') {\n    Canvas(Modifier.fillMaxSize()) {\n'
if old in s and 'val primary = MaterialTheme.colorScheme.primary' not in s:
    s = s.replace(old, ') {\n    val primary = MaterialTheme.colorScheme.primary\n    Canvas(Modifier.fillMaxSize()) {\n', 1)
s = s.replace('color = MaterialTheme.colorScheme.primary,', 'color = primary,')
s = s.replace('drawCircle(MaterialTheme.colorScheme.primary, handle, point)', 'drawCircle(primary, handle, point)')
p.write_text(s)
PY
