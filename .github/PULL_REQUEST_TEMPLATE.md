## What this changes

<!-- One or two sentences. What is different after this merges? -->

## Why

<!-- Link an issue if there is one. If this is a component, an issue first is
     strongly preferred - components carry design decisions from design/TOKENS.md
     and one that ignores them is more work to merge than to write. -->

## How it was verified

<!-- Delete what does not apply. Be specific: "tested" on its own is not useful. -->

- [ ] `cd cli-kotlin && ./gradlew test` passes
- [ ] All three workflows green: `release`, `verify-components`, `verify-ios-shell`
- [ ] Installed into a real project and compiled
- [ ] **Run on a real screen**, with the interactions exercised

<!-- A component is not verified by compiling. It counts as verified when it
     compiles on Android and desktop AND has been run on a device with its
     interactions exercised. That rule exists because a bottom sheet whose
     drag-to-dismiss compiled everywhere broke on the first human touch.

     If you cannot run it on a device, say so here. That is fine, and it is far
     better than an unstated assumption. -->

Device and OS version, if you ran it:

## Checklist for component changes

- [ ] `androidx.compose.foundation` only - `grep -r "material3"` returns nothing
- [ ] Uses the shared theme's tokens rather than inventing values
- [ ] Anything shared lives under `internal/<component>/`
- [ ] Interactive parts are reachable without the gesture (semantics actions)
