# CodeShare

CodeShare is an Android app for learning Python, solving programming exercises, and sharing code with a community. It combines guided lessons and practice with a social feed, friends, and messaging.

## Features

- **Guided learning:** Python foundations and data-structures-and-algorithms lessons, with bundled syntax highlighting and runnable examples.
- **Python playground:** Edit code with CodeMirror 6 and run Python 3.12 on-device.
- **Practice workspace:** Browse official and community exercises; switch between problem descriptions and a full-screen editor.
- **Personal progress:** Track solved exercises, topics, and streaks without points or leaderboards.
- **Community:** Create posts with titles, descriptions, images, and inline expandable code; connect with friends and exchange messages.
- **Authentication and data:** Firebase Authentication and Realtime Database support sign-in, profiles, posts, exercises, progress, and conversations.
- **Navigation:** Swipe between main sections and use Android Back to return through open screens.

## Tech stack

- Android application written in Java (Android Gradle Plugin 8.7.2)
- Firebase Authentication and Realtime Database
- CodeMirror 6 with the Lezer Python parser, bundled locally for offline editing and highlighting
- Chaquopy with Python 3.12 for on-device execution
- Material Components, AndroidX, and Glide
- Optional Gemini-powered AI assistance

## Getting started

### Requirements

- Android Studio with Android SDK 34 and Build Tools 34
- JDK 17
- Python 3.12 for the Chaquopy Android build
- A 64-bit Android device or emulator: `arm64-v8a` or `x86_64`
- Node.js and npm only when rebuilding the editor bundle

The app's minimum Android SDK is 24. The configured Python runtime supports only the 64-bit ABIs listed above.

### Firebase configuration

The Android app uses the Firebase configuration in `app/google-services.json`. For a different Firebase project, replace it with that project's Android configuration and make sure its application ID matches `com.code_share.finalapp`.

The repository includes Realtime Database and Storage rules in `database.rules.json` and `storage.rules`. Review them against your Firebase project before deployment. To deploy the database rules with the Firebase CLI:

```shell
firebase use <your-project-id>
firebase deploy --only database
```

The included Storage rules deny all access. Image uploads require a separate, user-scoped implementation and matching rules before Storage is enabled.

### Build and test

From the project root on Windows:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

Run the Python helper tests independently with:

```shell
python -m unittest discover -s tests/python -p "test_*.py"
```

### Rebuild the CodeMirror bundle

The editor source is in `editor/src/main.js`; the generated app asset is `app/src/main/assets/code-editor.js`.

```shell
npm --prefix editor ci
npm --prefix editor run build
```

The editor and lesson highlighter use bundled dependencies and do not load scripts from a CDN. Third-party notices are in `app/src/main/assets/third_party_licenses.txt`.

### Optional Gemini assistance

AI assistance is disabled when no key is configured. Set `GEMINI_API_KEY` in the environment used to launch Gradle or Android Studio before building. The key is included in the client build and can be extracted from an APK; do not treat a mobile client key as a secret. For production, use a protected backend or a provider-supported restricted key.

## Security and runtime notes

- **Run only code you trust.** Python runs on-device in the app process; it is not sandboxed. The runner currently bundles the Python standard library, not arbitrary pip packages.
- **Client-recorded progress is not tamper-proof.** Solved-exercise history and streaks are personal tracking, not trusted credentials or rewards.
- Database rules use Firebase Auth identity and the `admin` custom claim for privileged exercise operations. A profile field such as `isAdmin` does not grant administrator access.
- Some existing flows need migration to fit the included restrictive rules: username lookup still queries user records broadly, post likes are not separately modeled as per-user writes, and Storage uploads are blocked by the included rules. Test these behaviors against a non-production Firebase project before deployment.
- The Gemini key is packaged into the app when configured; API keys embedded in client applications are recoverable.

## Project layout

```text
app/                 Android application, lessons, editor assets, and Python runtime
editor/              CodeMirror editor source and npm build configuration
tests/python/        Python helper tests
database.rules.json  Firebase Realtime Database security rules
storage.rules        Firebase Storage security rules
```
