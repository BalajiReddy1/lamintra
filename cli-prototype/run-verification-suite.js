#!/usr/bin/env node
const fs = require("fs");
const path = require("path");
const {
  rewriteRootPackage,
  installComponent,
} = require("./rewrite-engine.js");

const REGISTRY_DIR = path.join(__dirname, "..", "registry");
const TARGET_DIR = path.join(__dirname, "..", "fake-target-project");

let passCount = 0;
let failCount = 0;
function check(label, condition, detail) {
  if (condition) {
    console.log(`  ✅ PASS  ${label}`);
    passCount++;
  } else {
    console.log(`  ❌ FAIL  ${label}${detail ? " — " + detail : ""}`);
    failCount++;
  }
}

console.log("################################################################");
console.log("# JetCompose rewrite engine — Day 1 verification suite");
console.log("################################################################");

console.log("\n--- Target project config ---");
console.log(fs.readFileSync(path.join(TARGET_DIR, ".jetcompose", "config.json"), "utf8"));

// ---------------------------------------------------------------------------
// Test 1: install bottomsheet/glass
// ---------------------------------------------------------------------------
const bottomsheetResults = installComponent(
  REGISTRY_DIR,
  "bottomsheet/glass",
  TARGET_DIR,
  console.log
);

// ---------------------------------------------------------------------------
// Test 2: install button/neon (shares a filename with test 1's internal file)
// ---------------------------------------------------------------------------
const neonResults = installComponent(
  REGISTRY_DIR,
  "button/neon",
  TARGET_DIR,
  console.log
);

console.log("\n################################################################");
console.log("# Correctness checks against real written output");
console.log("################################################################\n");

// --- Check 1: package declarations were actually rewritten, not left as-is
const bottomSheetFile = bottomsheetResults.find(r => r.relFile.endsWith("BottomSheet.kt"));
check(
  "BottomSheet.kt package line rewritten to target namespace",
  bottomSheetFile.rewritten.includes(
    "package com.testapp.myapp.features.shared.widgets.bottomsheet.glass"
  ),
  `got: ${bottomSheetFile.rewritten.split("\n")[0]}`
);

// --- Check 2: cross-file internal import correctly rewritten
check(
  "BottomSheet.kt's import of DragHandle points to the new namespace",
  bottomSheetFile.rewritten.includes(
    "import com.testapp.myapp.features.shared.widgets.bottomsheet.glass.internal.bottomsheet_glass.DragHandle"
  )
);
check(
  "BottomSheet.kt's import of glassSurface points to the new namespace",
  bottomSheetFile.rewritten.includes(
    "import com.testapp.myapp.features.shared.widgets.bottomsheet.glass.internal.bottomsheet_glass.glassSurface"
  )
);

// --- Check 3: non-registry imports (androidx.*, kotlinx.*) were left untouched
check(
  "Unrelated androidx.compose imports were NOT touched",
  bottomSheetFile.rewritten.includes("import androidx.compose.foundation.background") &&
  bottomSheetFile.originalContent.includes("import androidx.compose.foundation.background")
);
check(
  "Unrelated kotlinx.coroutines import was NOT touched",
  bottomSheetFile.rewritten.includes("import kotlinx.coroutines.launch")
);

// --- Check 4: no leftover occurrences of the old registry package anywhere
const allWrittenContent = [...bottomsheetResults, ...neonResults]
  .map(r => r.rewritten)
  .join("\n");
check(
  "No leftover 'com.jetcompose' references in any installed file",
  !allWrittenContent.includes("com.jetcompose"),
  allWrittenContent.includes("com.jetcompose") ? "found a leftover occurrence!" : ""
);

// --- Check 5: THE COLLISION TEST — two components both ship ModifierExtensions.kt
const bottomsheetModifierFile = bottomsheetResults.find(r =>
  r.relFile.endsWith("ModifierExtensions.kt")
);
const neonModifierFile = neonResults.find(r =>
  r.relFile.endsWith("ModifierExtensions.kt")
);
check(
  "The two same-named ModifierExtensions.kt files landed at DIFFERENT paths",
  bottomsheetModifierFile.targetAbsolute !== neonModifierFile.targetAbsolute,
  `bottomsheet: ${bottomsheetModifierFile.targetAbsolute}\n           neon:       ${neonModifierFile.targetAbsolute}`
);
check(
  "The two same-named ModifierExtensions.kt files have DIFFERENT package declarations",
  bottomsheetModifierFile.fullPackage !== neonModifierFile.fullPackage,
  `bottomsheet pkg: ${bottomsheetModifierFile.fullPackage}\n           neon pkg:       ${neonModifierFile.fullPackage}`
);
check(
  "Both files physically exist on disk simultaneously (no overwrite happened)",
  fs.existsSync(bottomsheetModifierFile.targetAbsolute) &&
  fs.existsSync(neonModifierFile.targetAbsolute)
);

// ---------------------------------------------------------------------------
// Test 3: boundary-safety regex test — the decoy case, run in isolation
// ---------------------------------------------------------------------------
console.log("\n--- Boundary-safety regex test (isolated, not part of shipped files) ---");

const decoyOriginal = [
  "package com.jetcompose.bottomsheet.glass",
  "",
  "// Decoy 1: a DIFFERENT, unrelated package that happens to start with",
  "// the same characters — must NOT be rewritten by a naive string replace.",
  "import com.jetcompose.bottomsheet.glassy.SomeUnrelatedThing",
  "",
  "// Decoy 2: same idea, but the continuation is a digit instead of a letter.",
  "import com.jetcompose.bottomsheet.glass2.AnotherUnrelatedThing",
  "",
  "// The real thing that SHOULD be rewritten:",
  "import com.jetcompose.bottomsheet.glass.internal.bottomsheet_glass.DragHandle",
].join("\n");

const decoyRewritten = rewriteRootPackage(
  decoyOriginal,
  "com.jetcompose.bottomsheet.glass",
  "com.testapp.myapp.features.shared.widgets.bottomsheet.glass"
);

console.log(decoyRewritten);

check(
  "Decoy 'glassy' package left completely untouched",
  decoyRewritten.includes("import com.jetcompose.bottomsheet.glassy.SomeUnrelatedThing")
);
check(
  "Decoy 'glass2' package left completely untouched",
  decoyRewritten.includes("import com.jetcompose.bottomsheet.glass2.AnotherUnrelatedThing")
);
check(
  "The real internal import WAS correctly rewritten",
  decoyRewritten.includes(
    "import com.testapp.myapp.features.shared.widgets.bottomsheet.glass.internal.bottomsheet_glass.DragHandle"
  )
);
check(
  "The package declaration line WAS correctly rewritten",
  decoyRewritten.startsWith("package com.testapp.myapp.features.shared.widgets.bottomsheet.glass")
);

// ---------------------------------------------------------------------------
console.log("\n################################################################");
console.log(`# RESULT: ${passCount} passed, ${failCount} failed`);
console.log("################################################################");

if (failCount > 0) {
  process.exit(1);
}
