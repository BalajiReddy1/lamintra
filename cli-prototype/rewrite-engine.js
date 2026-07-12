#!/usr/bin/env node
/**
 * JetCompose rewrite engine — executable prototype.
 *
 * WHY THIS EXISTS AS JS AND NOT KOTLIN:
 * This sandbox has a JRE but no JDK (no javac) and no Kotlin compiler,
 * and no network access to install either. Node.js is the only runtime
 * available that can actually execute code here today. This file proves
 * the ALGORITHM is correct with real, executed test output. The proven
 * logic is then transcribed into the production Kotlin CLI (see
 * cli-kotlin/), which you will compile for real on your own machine
 * where a proper JDK + Kotlin + Gradle toolchain exists.
 *
 * This is not a shortcut — it's how you validate risky logic fast before
 * committing it to a slower compile/test loop.
 */

const fs = require("fs");
const path = require("path");

// ---------------------------------------------------------------------------
// Core rewrite primitive
// ---------------------------------------------------------------------------

/**
 * Boundary-safe replacement of a registry root package with a new root
 * package, anywhere it appears in file content (the `package` declaration
 * itself, and any `import` line referencing this component's own
 * sub-packages, e.g. its internal/<prefix> namespace).
 *
 * Boundary-safety matters: a naive global string replace of
 * "com.jetcompose.bottomsheet.glass" would ALSO corrupt an unrelated
 * import like "com.jetcompose.bottomsheet.glassy.Something" (note the
 * trailing 'y') by mangling it mid-identifier. We guard against that with
 * a negative lookahead for any character that could continue a Kotlin
 * identifier (letters, digits, underscore).
 */
function rewriteRootPackage(content, oldRoot, newRoot) {
  const escaped = oldRoot.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
  const boundarySafe = new RegExp(escaped + "(?![A-Za-z0-9_])", "g");
  return content.replace(boundarySafe, newRoot);
}

/**
 * Computes the new root package for a component once installed into a
 * target project, given the user's config and the component's
 * category/style. This is the SAME computation used for both rewriting
 * file contents and resolving file system paths — sharing one function
 * for both guarantees they can never drift out of sync with each other,
 * which matters because "file path doesn't match its package
 * declaration" is itself a compile error we're trying to eliminate.
 */
function computeNewRootPackage(config, category, style) {
  const componentPathDotted = config.componentPath
    .split("/")
    .filter(Boolean)
    .join(".");
  const parts = [config.packageName];
  if (componentPathDotted) parts.push(componentPathDotted);
  parts.push(category, style);
  return parts.join(".");
}

/**
 * Resolves the on-disk target path for a single file belonging to a
 * component, given the user's config, the component's manifest, and the
 * file's path as declared in the manifest (e.g.
 * "src/internal/bottomsheet_glass/DragHandle.kt").
 */
function resolveTargetPath(config, manifest, manifestRelativeFilePath) {
  const sourceRootKey = config.isKmp ? "common" : "android";
  const sourceRoot = config.sourceRoots[sourceRootKey];
  if (!sourceRoot) {
    throw new Error(
      `Config has no sourceRoots.${sourceRootKey} — cannot resolve install path`
    );
  }

  // Strip the registry's own "src/" convention prefix.
  const withoutSrc = manifestRelativeFilePath.replace(/^src\//, "");
  const segments = withoutSrc.split("/");
  const filename = segments.pop();

  const newRoot = computeNewRootPackage(config, manifest.category, manifest.style);

  let fullPackage;
  if (segments[0] === "internal") {
    const declaredPrefix = segments[1];
    if (declaredPrefix !== manifest.prefix) {
      // This is a real bug class: if a manifest's `prefix` field doesn't
      // match its own internal/ folder name, installs would silently
      // produce a package that doesn't match the folder structure.
      // Fail loudly instead of installing something broken.
      throw new Error(
        `Manifest prefix mismatch: manifest declares prefix "${manifest.prefix}" ` +
        `but file path uses "internal/${declaredPrefix}/". These must match exactly.`
      );
    }
    const extraSegments = segments.slice(2); // anything nested deeper than internal/<prefix>/
    fullPackage = [newRoot, "internal", manifest.prefix, ...extraSegments].join(".");
  } else {
    fullPackage = [newRoot, ...segments].join(".");
  }

  const packageAsPath = fullPackage.replace(/\./g, "/");
  // Relative to the target project's root — caller joins this with the
  // actual project directory. Keeping this function pure (no cwd
  // assumptions) makes it trivial to unit test in isolation.
  const relativePath = path.join(sourceRoot, packageAsPath, filename);
  return {
    fullPackage,
    relativePath,
    filename,
  };
}

// ---------------------------------------------------------------------------
// Manifest + install orchestration
// ---------------------------------------------------------------------------

function loadJson(filePath) {
  return JSON.parse(fs.readFileSync(filePath, "utf8"));
}

function installComponent(registryDir, componentName, targetProjectDir, log) {
  const componentDir = path.join(registryDir, componentName);
  const manifestPath = path.join(componentDir, "component.json");
  const manifest = loadJson(manifestPath);

  const config = loadJson(path.join(targetProjectDir, ".jetcompose", "config.json"));

  const newRoot = computeNewRootPackage(config, manifest.category, manifest.style);
  log(`\n=== Installing ${manifest.name} ===`);
  log(`registryPackage : ${manifest.registryPackage}`);
  log(`newRootPackage  : ${newRoot}`);

  const results = [];

  for (const relFile of manifest.files) {
    const sourcePath = path.join(componentDir, relFile);
    const originalContent = fs.readFileSync(sourcePath, "utf8");

    const rewritten = rewriteRootPackage(
      originalContent,
      manifest.registryPackage,
      newRoot
    );

    const target = resolveTargetPath(config, manifest, relFile);
    const targetAbsolute = path.join(targetProjectDir, target.relativePath);

    fs.mkdirSync(path.dirname(targetAbsolute), { recursive: true });
    fs.writeFileSync(targetAbsolute, rewritten, "utf8");

    log(`  wrote: ${target.relativePath}`);
    log(`    package -> ${target.fullPackage}`);

    results.push({
      relFile,
      targetAbsolute,
      originalContent,
      rewritten,
      fullPackage: target.fullPackage,
    });
  }

  return results;
}

module.exports = {
  rewriteRootPackage,
  computeNewRootPackage,
  resolveTargetPath,
  installComponent,
};

// ---------------------------------------------------------------------------
// If run directly, execute the full Day-1 verification suite
// ---------------------------------------------------------------------------
if (require.main === module) {
  require("./run-verification-suite.js");
}
