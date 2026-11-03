# Telangana Diagnostics (T-Diagnostics) Android Mobile App

A dedicated native Android mobile application wrapper for the **Government of Telangana Diagnostics Portal** (`tdiagnostics.telangana.gov.in`).

## Key Features
1. **Quick Login Switcher (4 Predefined Accounts)**:
   - `gadnew` / `abc1234`
   - `sidnew` / `abc1234`
   - `newdb2` / `abc1234`
   - `ntc` / `lims123`
   - One-tap account selection with automated ASP.NET JavaScript form injection.
2. **Patient Details Edit**:
   - Direct shortcut to `https://tdiagnostics.telangana.gov.in/LabUser/UniqueDataReportNew.aspx`
3. **Delete / Update Aadhaar**:
   - Direct shortcut to `https://tdiagnostics.telangana.gov.in/LabUser/UpdateDataReport.aspx`
4. **Enhanced Mobile WebView Experience**:
   - ASP.NET Session persistence with custom CookieManager.
   - Zoom controls and mobile viewport optimization.
   - Pull-to-refresh (`SwipeRefreshLayout`) and loading indicator.
   - Offline / VPN error handling and retry mechanism.

## How to Build the APK in Android Studio
1. Unzip `TDiagnostics_Android_App.zip`.
2. Open **Android Studio** (Hedgehog, Iguana, Koala, or Ladybug).
3. Select **File > Open** and choose the `TDiagnosticsApp` directory.
4. Let Gradle sync and download dependencies.
5. Connect your Android mobile device via USB (or start an Emulator).
6. Click **Run > Run 'app'** or **Build > Build Bundle(s) / APK(s) > Build APK(s)** to generate the installable `.apk` file.
7. Transfer and install the `.apk` on your Android phone.
