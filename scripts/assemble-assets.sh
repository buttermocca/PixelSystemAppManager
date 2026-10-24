#!/usr/bin/env bash
set -euo pipefail

# The art is stored as small base64 text pieces so every change is reviewable.
# Rebuild the real WebP artwork before Android resource compilation.
asset_dir=".github/assets"
out_dir="app/src/main/res/drawable-nodpi"
mkdir -p "$out_dir"

assemble_webp() {
  local output="$1"
  shift
  cat "$@" | tr -d '\r\n ' | base64 --decode > "$output"
  file "$output" | grep -qi 'web/p image'
  echo "Created $output ($(wc -c < "$output") bytes)"
}

assemble_webp "$out_dir/hero_art.webp" \
  "$asset_dir/hero-real.00.b64" \
  "$asset_dir/hero-real.01.b64" \
  "$asset_dir/hero-real.02.b64"
test "$(wc -c < "$out_dir/hero_art.webp")" -gt 5000

assemble_webp "$out_dir/splash_art.webp" \
  "$asset_dir/splash-real.00.b64" \
  "$asset_dir/splash-real.01.b64" \
  "$asset_dir/splash-real.02.b64" \
  "$asset_dir/splash-real.03.b64" \
  "$asset_dir/splash-real.04.b64" \
  "$asset_dir/splash-real.05.b64" \
  "$asset_dir/splash-real.06.b64" \
  "$asset_dir/splash-real.07.b64" \
  "$asset_dir/splash-real.08.b64" \
  "$asset_dir/splash-real.09.b64" \
  "$asset_dir/splash-real.10.b64" \
  "$asset_dir/splash-real.11.b64"
test "$(wc -c < "$out_dir/splash_art.webp")" -gt 20000

assemble_webp "$out_dir/icon_art.webp" \
  "$asset_dir/icon-real.00.b64" \
  "$asset_dir/icon-real.01a.b64" \
  "$asset_dir/icon-real.01b.b64" \
  "$asset_dir/icon-real.01c.b64" \
  "$asset_dir/icon-real.02.b64" \
  "$asset_dir/icon-real.03.b64"
test "$(wc -c < "$out_dir/icon_art.webp")" -gt 8000

# Smoothly prepare the artwork for modern high-density Android screens.
# The icon source contains a simulated gray/white checkerboard (not alpha);
# remove connected neutral background pixels, then enlarge it with a protected margin.
python3 scripts/process-art-assets.py "$out_dir"
