# Implementation Plan - Standard TIF HDMI Switching

Implement a robust, dynamic HDMI switching method using the official Android TV Input Framework (TIF) while ensuring the TV stays on the HDMI feed without returning to the Home screen unnecessarily.

## User Review Required

> [!IMPORTANT]
> **Dynamic Discovery**: The app will now scan your TV's system services to find the correct HDMI input ID automatically. This is much more reliable than hardcoding IDs.
> **No Browser/Home Hijacking**: By explicitly targeting the `com.google.android.tv.inputplayer` package and using the official `TvContract` URIs, we bypass the browser and ensure the TV switches directly to the hardware feed.
> **Stay on HDMI**: If the TV is already displaying the HDMI 1 feed, the app will avoid re-triggering a disruptive "full" switch and just focus on the KVM command.

## Proposed Changes

### Logic & Navigation

#### [MODIFY] [MainActivity.kt](file:///home/ali/AndroidStudioProjects/HDMIKVM/app/src/main/java/org/eu/john007/hdmikvm/MainActivity.kt)
- **New `switchToTvHdmi1()` using TIF**:
  1.  Get `TvInputManager` service.
  2.  Filter `tvInputList` for `TYPE_HDMI` and `isPassthroughInput`.
  3.  Find the correct HDMI 1 port dynamically.
  4.  Construct the URI via `TvContract.buildChannelUriForPassthroughInput(selectedInput.id)`.
  5.  Launch `Intent.ACTION_VIEW` with the URI, explicitly targeting the system's **Input Player** to prevent browser interception.
- **Smart Switching Logic**:
  - Add logic to send the KVM command first.
  - If the TV is already showing the HDMI feed (overlapped by our side-panel), use the TIF intent to "close" the Android UI and return focus to the HDMI hardware without going through the Home screen.

### Cleanup

#### [DELETE] [util/ChannelManager.kt](file:///home/ali/AndroidStudioProjects/HDMIKVM/app/src/main/java/org/eu/john007/hdmikvm/util/ChannelManager.kt)
- (Previously missed) Ensure legacy channel manager code is removed to keep the project clean.

## Verification Plan

### Manual Verification
- Deploy and verify that picking an HDMI source switches the TV feed **instantly**.
- Confirm that the browser no longer pops up.
- Verify that the app doesn't jump to the "Home" screen when you are already viewing an HDMI source.
- Check logs for `KVM_DEBUG` to see the discovered HDMI Input ID.
