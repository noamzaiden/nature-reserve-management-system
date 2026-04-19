# Java Coding Instructions

Use these instructions when generating or editing Java code in this repository.

These rules reflect my personal Java style. If the existing project already has a strong local convention, preserve the surrounding structure and adapt these rules to fit it. Otherwise, follow this file as the default.

## General Style

- Prefer straightforward, readable, class-based Java.
- Favor clear OOP structure over advanced patterns or clever abstractions.
- Keep logic explicit and easy to trace.
- Choose readability and maintainability over compactness.
- Keep code practical, student-friendly, and easy to debug.

## File And Class Structure

- Prefer one public class per file.
- Use package declarations only if the project already uses them.
- Put imports at the top.
- After imports, order members like this:
  1. constants
  2. fields
  3. constructors
  4. getters and setters
  5. main logic methods
  6. helper methods
  7. `main` method if needed
- In controller classes, keep UI fields near the top, then state fields, then `initialize()`, then handlers, then helpers.
- In `Driver` or `App` classes, keep the class focused on orchestration.

## Naming

- Use `PascalCase` for class names.
- Use `camelCase` for methods and most fields.
- Use boolean names that read naturally, such as `isMatched`, `isWar`, or `isBoardLocked`.
- Use `UPPER_CASE_WITH_UNDERSCORES` for constants.
- Prefer descriptive names, but short local names like `i`, `row`, `col`, `line`, `q`, or `p` are acceptable when they are obvious in context.
- Prefer `camelCase` over older snake_case field naming unless the surrounding code already uses snake_case.

## Formatting

- Use 4-space indentation.
- Keep opening braces on the same line.
- Leave blank lines between logical sections, but avoid excessive vertical spacing.
- Use spaces around operators and after commas.
- Short getters may stay on one line if they are still readable.
- Keep methods visually simple and easy to scan.

## Comments

- Prefer light `//` comments over heavy Javadoc.
- Use section comments when useful, such as:
  - `// Constructor`
  - `// Getters and Setters`
  - `// Helper methods`
  - `// class variables`
  - `// instance variables`
- Use short intent comments when they add value, such as:
  - `// deep copy`
  - `// call superclass constructor`
  - `// Remove terms with coefficient 0`
- Avoid over-commenting obvious code.
- Avoid long banner comments unless the file already uses them.

## Coding Habits

- Prefer explicit constructors and explicit field assignment.
- Use getters and setters freely for stateful classes.
- Prefer simple loops, conditionals, and direct state updates.
- Use standard library collections directly when they fit.
- Break non-trivial behavior into helper methods with descriptive names.
- Use guard clauses and validation for invalid input.
- When something fails, either:
  - throw a clear exception, or
  - print/show a direct user-facing error message
- Small debug prints are acceptable while building interactive or GUI features.

## What To Avoid

- Do not over-engineer simple tasks.
- Do not collapse readable logic into dense one-liners.
- Do not introduce heavy architectural patterns unless the project clearly needs them.
- Do not add Javadoc by default.
- Do not copy accidental formatting drift from older files if the cleaner version of the style is obvious.

## Codex Behavior

When editing this repository:

- Match the surrounding code first, then apply this style.
- Keep changes focused and avoid unrelated refactors.
- Prefer minimal, readable solutions over broad rewrites.
- If multiple valid implementations exist, choose the simplest one that fits the project.
