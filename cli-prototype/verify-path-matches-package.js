#!/usr/bin/env node
const fs = require("fs");
const path = require("path");

const SOURCE_ROOT = path.join(
  __dirname,
  "..",
  "fake-target-project",
  "composeApp",
  "src",
  "commonMain",
  "kotlin"
);

function walk(dir, out = []) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) walk(full, out);
    else if (entry.name.endsWith(".kt")) out.push(full);
  }
  return out;
}

const files = walk(SOURCE_ROOT);
console.log(`Checking ${files.length} installed .kt files for path/package agreement...\n`);

let allOk = true;
for (const file of files) {
  const content = fs.readFileSync(file, "utf8");
  const match = content.match(/^package\s+([\w.]+)/m);
  if (!match) {
    console.log(`❌ ${file} — no package declaration found`);
    allOk = false;
    continue;
  }
  const declaredPackage = match[1];
  const expectedDir = path.join(SOURCE_ROOT, declaredPackage.replace(/\./g, "/"));
  const actualDir = path.dirname(file);

  const ok = expectedDir === actualDir;
  console.log(`${ok ? "✅" : "❌"} ${path.relative(SOURCE_ROOT, file)}`);
  console.log(`   package declares: ${declaredPackage}`);
  console.log(`   expected dir:     ${path.relative(SOURCE_ROOT, expectedDir)}`);
  console.log(`   actual dir:       ${path.relative(SOURCE_ROOT, actualDir)}`);
  if (!ok) allOk = false;
}

console.log(`\n${allOk ? "✅ ALL FILES: path matches package exactly" : "❌ MISMATCH FOUND — would fail to compile"}`);
process.exit(allOk ? 0 : 1);
