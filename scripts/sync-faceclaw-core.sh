#!/bin/sh
# Copies Faceclaw's shared Kotlin core and its tests into faceclaw-core/ and its Android GATT
# adapters into faceclaw-android/, all unchanged. Left out: the iOS sources and FontTest, which
# needs a 6 MB font from Faceclaw's app/ directory.
# Usage: scripts/sync-faceclaw-core.sh /path/to/faceclaw-checkout
set -eu

src="${1:?usage: $0 /path/to/faceclaw}"
root="$(cd "$(dirname "$0")/.." && pwd)"
shared="$src/native/kotlin/shared/src"

[ -d "$shared/commonMain" ] || { echo "not a Faceclaw checkout: $src" >&2; exit 1; }

for set in commonMain androidMain; do
    rm -rf "$root/faceclaw-core/src/$set"
    cp -R "$shared/$set" "$root/faceclaw-core/src/$set"
done

tests="$src/tests/kotlin/src"
for set in commonTest androidHostTest; do
    rm -rf "$root/faceclaw-core/src/$set"
    cp -R "$tests/$set" "$root/faceclaw-core/src/$set"
done
rm -f "$root/faceclaw-core/src/commonTest/kotlin/com/faceclaw/app/FontTest.kt"

# The Android GATT adapters (faceclaw-android) come from the same commit.
android="$src/App_Resources/Android/src/main/java/com/faceclaw/app"
for file in FaceclawBleManager.kt AndroidSessionLink.kt AndroidStockLink.kt FaceclawDeviceInfoProbe.kt; do
    cp "$android/$file" "$root/faceclaw-android/src/main/java/com/faceclaw/app/$file"
done

commit="$(git -C "$src" rev-parse HEAD)"
revision="$(sed -n 's/.*REQUIRED_FACECLAW_FIRMWARE_VERSION = \([0-9]*\).*/\1/p' "$src/app/g2/firmware-compat.ts")"
echo "Copied Faceclaw $commit (needs CFW revision $revision)."
echo "Update faceclaw-core/UPSTREAM.md and FirmwareRequirement.REQUIRED_REVISION in the app."
