# SIM Stock Checker App

This application automates checking SIM stock/balance using an Accessibility Service to navigate the SIM Toolkit (STK) and an SMS Receiver to parse the response.

## Features
- **One-Tap Check**: Just press "Check Stock" and the app does the rest.
- **Accessibility Automation**: Navigates through STK menus automatically.
- **SMS Parsing**: Automatically detects and parses the reply SMS from the provider.
- **MVVM Architecture**: Clean and maintainable code structure.
- **Material 3 UI**: Modern design with dark mode support.

## Flow
1. User taps "Check Stock".
2. Accessibility Service launches the SIM Toolkit (`com.android.stk`).
3. The service navigates through the menus (e.g., "Dialog" -> "Stock Inquiry").
4. STK sends an SMS.
5. The app waits for the incoming SMS.
6. `SmsReceiver` parses the balance and commission using Regex.
7. UI updates immediately.

## Setup Instructions
1. **Enable Accessibility Service**:
   - Go to Settings > Accessibility.
   - Find "Reloader" or "SIM Stock Checker".
   - Turn it ON.
2. **Permissions**:
   - Grant SMS permissions when prompted.
3. **Customize STK Navigation**:
   - The STK menu layout varies by carrier and country.
   - To customize the flow, edit `StkAccessibilityService.kt` and modify the `flow` list in `startStockCheck()`.

## Customizing STK Navigation Flow
The `AutomationStep` model allows you to define steps easily:
- `WAIT_FOR_TEXT`: Waits for specific text to appear on screen.
- `CLICK_TEXT`: Clicks on a UI element with the given text.
- `CLICK_ID`: Clicks on a UI element with a specific resource ID.
- `WAIT`: Pauses for a specified duration (in ms).
- `BACK`: Performs the back action.
- `HOME`: Returns to the home screen.

Example flow customization in `StkAccessibilityService.kt`:
```kotlin
val flow = listOf(
    AutomationStep(StepType.WAIT_FOR_TEXT, "MenuName"),
    AutomationStep(StepType.CLICK_TEXT, "MenuName"),
    AutomationStep(StepType.WAIT_FOR_TEXT, "InquiryOption"),
    AutomationStep(StepType.CLICK_TEXT, "InquiryOption"),
    AutomationStep(StepType.HOME)
)
```

## Parsing Different SMS Formats
Edit `SmsReceiver.kt` to update the Regex patterns in `parseAndUpdateStock` if your carrier sends SMS in a different format.
