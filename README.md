# SourceHub - Smart HDMI KVM Controller for Android TV

**SourceHub** is a sleek, native-integrated Android TV side-panel that acts as a universal input selector for your smart home setup. It specifically targets hardware HDMI KVM switches controlled via ESPHome, allowing you to switch between physical devices (PC, PS5, Server, etc.) directly from your TV interface.

![Version](https://img.shields.io/badge/version-1.0.0-blue)
![Platform](https://img.shields.io/badge/platform-Android%20TV%20|%20Google%20TV-green)

---

## 🚀 Key Features

*   **Integrated Sidebar**: Appears as a modern, semi-transparent side-panel on the right side of your screen—just like native system menus.
*   **Direct ESPHome Control**: Communicates directly with your ESPHome-powered KVM device via REST API for instant switching.
*   **Active State Tracking**: Automatically polls your KVM to highlight the currently active HDMI input.
*   **Hardware-Optimized Switching**: Includes specific hardware signals for **JMGO**, Mediatek, and TCL projectors/TVs to force direct HDMI feed transitions and bypass browser hijacking.
*   **Standard TIF Support**: Uses the official Android TV Input Framework for dynamic HDMI port discovery and standard system switching.
*   **Customizable Sources**: Pre-configured for a HomeLab setup:
    *   **Google TV**: Instant jump back to Home.
    *   **HDMI 1**: Reserved port.
    *   **HDMI 2**: ali-monster-pc (Gaming PC).
    *   **HDMI 3**: TV Live (Setup Box/Receiver).
    *   **HDMI 4**: Minisforum HomeLab.
    *   **Input Settings**: Direct shortcut to system input management.

---

## 🛠 Hardware Integration

The app expects an ESPHome device running a `select` entity named **`KVM Input`** (Object ID: `kvm_input`).

### ESPHome Config Snippet Example:
```yaml
select:
  - platform: template
    name: "KVM Input"
    id: kvm_input
    options:
      - "Input 1"
      - "Input 2"
      - "Input 3"
      - "Input 4"
    initial_option: "Input 1"
    set_action:
      - logger.log:
          format: "Switching KVM to %s"
          args: [ 'item.c_str()' ]
      # Add your physical relay/pin logic here
```

---

## 📺 TV Integration

### Key Mapper Support
SourceHub is optimized to be used with a **Key Mapper** app. 
1.  Map a button on your TV remote (e.g., long-press Menu or a color button).
2.  Set the action to **Launch Activity**.
3.  Choose `org.eu.john007.hdmikvm/.MainActivity`.

### Native Feel
*   **Transparency**: Dark "glass" effect background keeps your video visible.
*   **Perfect Fit**: Slim 40dp items fit all sources vertically without scrolling.
*   **Dynamic Focus**: Remote focus automatically jumps to the currently active input when opened.

---

## 🔧 Installation & Setup

1.  Clone the repo and open in Android Studio.
2.  Update the `baseUrl` in `MainActivity.kt` to match your ESPHome device IP or `.local` address.
3.  Build and deploy to your Android TV.
4.  Open the app once to register background services.

---

## 📜 License

Distributed under the MIT License. See `LICENSE` for more information.
