#!/usr/bin/env bash
set -euo pipefail

# Asset text is split into small chunks to keep source-file uploads robust.
# Reassemble the original binary WebP before Android resource compilation.
asset_dir=".github/assets"
output="app/src/main/res/drawable-nodpi/mascot_crystal.webp"
mkdir -p "$(dirname "$output")"
cat "$asset_dir/mascot.00.b64" "$asset_dir/mascot.01.b64" "$asset_dir/mascot.02.b64" \
  | tr -d '\r\n ' \
  | base64 --decode > "$output"

# Fail early if the resource is not a genuine WebP file.
file "$output" | grep -qi 'web/p image'
test "$(wc -c < "$output")" -gt 6000
echo "Reassembled crystal mascot: $(wc -c < "$output") bytes"
