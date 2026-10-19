# LMS Mobile (Library Management System, Android app)

Companion mobile app for the Library Management System website. It implements the screens designed in Task 1
(splash/login, member home, book search, loans, notifications, profile and settings, librarian console).

* Language: Kotlin. UI is built in code (no XML layouts), using only the Android framework.
* Database (DBMS): SQLite, six tables, every table seeded with at least 10 records on first launch.
* Security: salted PBKDF2-HMAC-SHA256 password hashes, parameterised SQL, login lock-out after 5 failures.
* Single sign-on: "Continue with Google" through the Android Credential Manager.
* Minimum Android version: 8.0 (API 26). Compiled with SDK 34.

## Run it

1. Install Android Studio (Koala or newer) and open this folder.
2. Let Gradle sync, choose an emulator or a phone with USB debugging, and press Run.
3. Log in with a demo account (password for all: `Password@123`):

| Username  | Role      | Notes                                                    |
|-----------|-----------|----------------------------------------------------------|
| `naledi`  | Member    | 3 loans (one overdue), fines due R45, matches the Figma screens |
| `thabo.m` | Member    | 1 loan                                                   |
| `ahomani` | Librarian | Issue loans, look up members, mark fines paid            |

You can also use **Create a new account** to register. The database is created the first time the app opens.
To reset the demo data, uninstall the app (or clear its storage) and start it again.

## Single sign-on (Google)

The button needs an OAuth client ID that only you can create:

1. Open Google Cloud Console, create a project, then APIs & Services > OAuth consent screen (External, add your Google account as a test user).
2. Credentials > Create credentials > OAuth client ID > **Android**. Package name `com.lms.mobile` and the SHA-1 of your debug keystore
   (`./gradlew signingReport`).
3. Credentials > Create credentials > OAuth client ID > **Web application**. Copy this client ID.
4. Paste the Web client ID into `GOOGLE_WEB_CLIENT_ID` in `app/src/main/java/com/lms/mobile/Config.kt`.
5. Run on a device or emulator image that has Google Play services and a signed-in Google account.

Until step 4 is done, the button shows a message instead of opening the account chooser.

## Tests

```
./gradlew testDebugUnitTest
```

Unit tests cover password hashing, validation rules, fine calculation, the login lock-out, search query building and the
seed data (10+ records per table, valid relationships). The same command runs in GitHub Actions on every push
(`.github/workflows/android-ci.yml`), followed by `./gradlew assembleDebug`. The debug APK and the test report are uploaded as build artifacts.

## Project layout

```
app/src/main/java/com/lms/mobile/
  Config.kt, SessionManager.kt
  data/      Schema, SeedData, LmsDatabase (SQLiteOpenHelper), Repo (queries and rules), Models
  security/  PasswordHasher
  logic/     Validators, FineCalculator, LoginThrottle, BookSearch
  ui/        Ui (widgets and palette), BaseActivity (theme, bottom tabs), one Activity per screen, GoogleSso
app/src/test/  JUnit tests
```

## Known limitations

* Data is stored on the device only; accounts and loans are not shared between phones.
* The Google ID token is accepted on the device and is not verified by a server.
* "Forgot password" is simulated (username plus registered email); there is no email service.
* The librarian screen takes book codes by typing; camera barcode scanning is not included.
 
## Demo video 
https://youtu.be/rzTa-nEdiJM 
