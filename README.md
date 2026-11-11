# Code Sharing & Learning Platform

 CodeShare is an Android application designed as a social learning platform centered around programming. It allows users to share posts that can include text, images, and code snippets, with robust code display and syntax highlighting. The platform also integrates educational components like lessons and exercises, alongside community features such as a friend system and messaging.

## Key Features:

*   **Social Posting:** Share posts with text, images, and code.
*   **Code Display:** View code snippets with syntax highlighting in posts and a dedicated dialog.
*   **Learning Modules:** Access lessons and practice exercises.
*   **Community Interaction:** Connect with friends and exchange messages.
*   **AI Assistance:** Integrated AI to help with coding and learning.
*   **User Authentication:** Secure login, registration, and profile management.

## Core Technologies:

*   Android (Java)
*   Firebase (Authentication, Realtime Database, Storage)
*   Glide (Image Loading)
*   highlight.js (Syntax Highlighting)
*   Chaquopy (Python Integration)
*   Gemini AI API

## Firebase security

The included Realtime Database rules require authentication, restrict private user records to their owners, limit conversations to participants, and reserve exercise publishing for Firebase Auth users with the trusted `admin` custom claim. The client-side `isAdmin` profile field does not grant admin access. User score fields cannot be changed by clients; score updates and admin claims need a trusted backend.

Storage access is denied until uploads are implemented with a user-scoped path and matching rules. The Firebase project currently requires a Blaze billing plan to enable Storage, so deploy the database rules separately. Post writes are limited to their authors, so the current client-side like transaction on other users' posts is denied until likes are moved to separately validated per-user records.

Deploy the Realtime Database rules to the configured Firebase project with `firebase deploy --only database`. Once Storage is enabled, deploy its deny-all rules with `firebase deploy --only storage`. Username-based login and friend lookup still use broad `/users` queries in the app and will be denied by these rules; use email login until those flows are migrated to a non-sensitive public username index.
