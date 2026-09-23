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
lines = s.splitlines()
clean = [line for line in lines if line.strip() != "import androidx.compose.ui.Alignment"]
s = "\n".join(clean) + "\n"
s = s.replace("Alignment.CenterHorizontally", "androidx.compose.ui.Alignment.CenterHorizontally")
s = s.replace("CenterHorizontally", "androidx.compose.ui.Alignment.CenterHorizontally")
# Normalize any generated duplicate qualification, then ensure the symbol is fully qualified.
s = s.replace("androidx.compose.ui.Alignment.androidx.compose.ui.Alignment.CenterHorizontally", "androidx.compose.ui.Alignment.CenterHorizontally")
s = s.replace("androidx.compose.ui.Alignment.androidx.compose.ui.Alignment.CenterHorizontally", "androidx.compose.ui.Alignment.CenterHorizontally")
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
