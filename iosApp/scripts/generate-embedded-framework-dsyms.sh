#!/bin/sh
set -eu

# Xcode replaces embedded static archives with codeless dylib stubs so their resource
# bundles can ship. Their code and debug information belong to Grippo.app.dSYM;
# the injected stubs still need matching dSYM UUIDs for App Store symbol upload.
for name in shared FirebaseAnalytics GoogleAdsOnDeviceConversion GoogleAppMeasurement GoogleAppMeasurementIdentitySupport; do
    source_binary="${BUILT_PRODUCTS_DIR}/${name}.framework/${name}"
    embedded_binary="${TARGET_BUILD_DIR}/${FRAMEWORKS_FOLDER_PATH}/${name}.framework/${name}"
    dsym="${DWARF_DSYM_FOLDER_PATH}/${name}.framework.dSYM"

    # Fail if a dependency becomes dynamic: deriving an empty dSYM from a real
    # stripped library would hide missing vendor symbols.
    case "$(/usr/bin/file -b "$source_binary")" in
        *"ar archive"*) ;;
        *) echo "error: Expected a static framework input for ${name}." >&2; exit 1 ;;
    esac
    sections=$(xcrun size -m "$embedded_binary")
    if ! printf '%s\n' "$sections" | awk '
        $1 == "Section" { found = 1; if ($3 != 0) exit 1 }
        END { if (!found) exit 1 }
    '; then
        echo "error: ${name} contains code or data; provide its original dSYM instead." >&2
        exit 1
    fi

    xcrun dsymutil --quiet "$embedded_binary" -o "$dsym"
    binary_uuids=$(xcrun dwarfdump --uuid "$embedded_binary")
    dsym_uuids=$(xcrun dwarfdump --uuid "$dsym")
    binary_uuids=$(printf '%s\n' "$binary_uuids" | awk '/^UUID:/ { print $2, $3 }' | LC_ALL=C sort)
    dsym_uuids=$(printf '%s\n' "$dsym_uuids" | awk '/^UUID:/ { print $2, $3 }' | LC_ALL=C sort)
    if [ -z "$binary_uuids" ] || [ "$binary_uuids" != "$dsym_uuids" ]; then
        echo "error: dSYM UUIDs do not match the embedded ${name} stub." >&2
        exit 1
    fi
    echo "Prepared dSYM for ${name} resource stub: ${binary_uuids}"
done
