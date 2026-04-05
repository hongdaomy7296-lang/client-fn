# CURRENT_ISSUES

Generated: 2026-03-05 00:22

## [FIXED][P0] Dynamic FOV + Fog did not apply
- Evidence: `InvalidInjectionException` on `MixinEntityRenderer` for `func_78480_b/updateCameraAndRender` descriptor mismatch.
- Cause: tail inject targeted `(FJ)V`, while this 1.7.10 runtime exposes `func_78480_b(F)V`.
- Fix:
  - Updated inject target to `func_78480_b(F)V` / `updateCameraAndRender(F)V`.
  - Updated handler signature to `(float, CallbackInfo)`.
  - Rebuilt and redeployed runtime jar.
- Status: FIXED in source and deployed jar.

## [FIXED][P1] K reset trigger reliability
- Cause: key handling depended on `dispatchKeypresses` event redirect.
- Fix: moved to `runTick` HEAD using `Keyboard.isKeyDown(KEY_K)` + 200ms cooldown.
- Status: FIXED in source and deployed jar.

## [CLOSED][P1] Premium auth suspicion
- Evidence: runtime log now shows `Setting user: CNE9` and valid session token.
- Conclusion: auth chain is currently working.
- Status: CLOSED.

## [OPEN][P1] Missing asset index / unknown sound events
- Evidence:
  - `Can't find the resource index file: .\\assets\\indexes\\1.7.10.json`
  - repeated `Unable to play unknown soundEvent: minecraft:...`
- Impact: sound warnings and missing sounds; not a config persistence issue.
- Next action: verify launcher `assetsDir` and `assetIndex` for `1.7.10-ClientFN`.

## [OPEN][P2] Runtime verification pending after latest hotfix
- Required action:
  - Launch once with current jar.
  - Confirm no `MixinEntityRenderer` / `MixinGuiIngameHud` apply errors.
  - Validate in game: Dynamic FOV lock and Fog slider live update.

