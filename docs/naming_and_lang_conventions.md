# TYPE-MOON-WORLD Naming And Lang Conventions

## 1. Item Naming (Code vs Registry)
- Keep registry IDs stable to avoid world/save/recipe breakage.
- Use clear code identifiers for readability and AI tooling.

Applied in this pass:
- `redsword` item (registry id unchanged) -> code/display name standardized to `Muramasa`.
- `tsumukari_muramasa` display name standardized to `TsumukariMuramasa` in `en_us`.

Recommended pattern:
- Registry ID: snake_case (stable, backward-compatible).
- Java class: PascalCase, domain-meaningful (`MuramasaItem`).
- Constant name: UPPER_SNAKE_CASE (`ModItems.MURAMASA`).

## 2. Lang Key Prefix Categories
Use prefix-first organization for consistency:
1. `item.*`
2. `block.*`
3. `effect.*`
4. `attribute.*`
5. `key.*`
6. `gui.*`
7. `magic.*`
8. `message.*`
9. `entity.*`
10. `tooltip.*`
11. `command.*`
12. `adv.*`
13. `creativetab.*`

## 3. zh/en Synchronization Rules
- Every user-facing key should exist in both `zh_cn.json` and `en_us.json`.
- Prefer same placeholders across locales (`%s`, `%%`, `\n`).
- Rename by semantics, not by literal translation.

## 4. Safe Refactor Strategy
- Rename classes/constants first, keep registry IDs stable.
- Update translation keys used in code.
- Run compile checks after each rename batch.
- For large lang formatting updates, validate with tooling before writeback.

## 5. Tooling
- Sort lang keys by category:
  - `powershell -NoProfile -ExecutionPolicy Bypass -File tools/sort_lang_keys.ps1`
- Validate zh/en key parity:
  - `powershell -NoProfile -ExecutionPolicy Bypass -File tools/validate_lang_keys.ps1`
