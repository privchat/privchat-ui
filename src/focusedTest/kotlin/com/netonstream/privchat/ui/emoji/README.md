# Apple Emoji JVM Regression Gate

From the `privchat-ui` repository root:

```sh
bash scripts/test-apple-emoji-focused.sh
```

Expected result: `OK (19 tests)`. The tracked sources are:

- `src/commonTest/kotlin/com/netonstream/privchat/ui/emoji/AppleEmojiTest.kt`: 6 catalog, Unicode editing, category and identifier tests.
- `src/commonTest/kotlin/com/netonstream/privchat/ui/emoji/EmojiTextEditBoundaryTest.kt`: 5 emoji/non-emoji ZWJ and grapheme-boundary tests.
- `src/focusedTest/kotlin/com/netonstream/privchat/ui/emoji/AppleEmojiEditorFocusedTest.kt`: 8 JVM-specific controller regressions.

The 8 controller tests cover same-frame callback deduplication, insertion at the
caret and reversed selection replacement, deletion before the caret/selection,
IME marked-text protection without deferred edits, controlled echoes preserving
selection/composition, raw Unicode/output-transformation preservation,
surrogate-safe programmatic carets, and external reset before native echo.

These tests use the real Kuikly TextFieldState and TextFieldBuffer. JVM reflection
only accesses the library's internal state constructor; it does not substitute a
state implementation. The script uses the real desktop Compose runtime because
the Android runtime requires Looper. It compiles the checked-out production
controller and generated catalog, not copies under `/tmp` or `build`.

The 11 common tests are 6 + 5; the 8 controller tests live in `focusedTest`, not
`commonTest`. The command compiles and executes all three test classes.

Requirements: JDK, curl, unzip, shasum, and the existing Gradle cache for the
explicit versions in the script. The only uncached download is the 1.3 MiB
desktop Compose runtime, pinned by version and SHA-256. No Gradle tasks, iOS
linking, simulator, native rendering or IM end-to-end acceptance is run.

Results and source hashes are written to `build/apple-emoji-focused/`.
