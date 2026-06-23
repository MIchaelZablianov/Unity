# Android News App — Two-App Architecture Exercise

**Format:** Take-home, no time limit. Submit the result and we will review it with you.

## Mission

Build an Android news experience in Kotlin that shows news articles based on the user's preferences. The work is split across two separate Android applications that must communicate with each other:

1. **Backend app** - owns the data. Serves articles from a JSON file bundled in the app, and performs filtering on the article set. (The "backend" is still an Android app; filtering is implemented in Android code, but conceptually it plays the role of the server in this exercise).
2. **UI app** - owns the user experience. Renders the news list, exposes the filter controls, and requests filtered articles from the backend app.

The two apps must be installable as separate APKs with different applicationIds, and must communicate via a mechanism of your choice.

## Evaluation lens

We evaluate your submission primarily on **Architecture, Security, and Scalability**.

We expect the code itself to demonstrate these qualities - a formal design document is recommended but not required. Treat this as production code you would defend in a design review.

- Don't waste time on UI finishes. We are not evaluating visual polish.
- Write the code as you would for real production: clean, maintainable, testable.

---

## JSON

The "backend" app

### Responsibilities

1. Bundle the article dataset as a JSON file inside the app. No network calls at this point.
2. Expose an inter-app surface that lets a caller request articles, optionally with filter criteria.
3. Filter the article set per the request and return the filtered result to the caller. Filtering runs inside the backend app, not in the UI app.

### Article shape (provided as-is in the JSON)

```json
{
  "title": "string",
  "description": "string",
  "image_url": "string",
  "rating": 1,
  "placeholderColor": {
    "red": 195,
    "green": 186,
    "blue": 177
  }
}
```

### Constraints

- The backend app must be independently installable and launchable on its own.
- The backend app must not depend on the UI app at compile time or runtime.
- No third-party network library (Retrofit, OkHttp networking, etc.) for the data source - the JSON is local.
- The backend app owns the filter logic.

---

## The UI app

### Responsibilities

- Provide a news list screen that displays articles fetched from the backend app.
- Provide filter controls. At minimum:
  - A text filter on title
  - A filter on rating
  - An Apply/Save button that applies both filters together
- Send the filter criteria to the backend app and render the filtered list it returns.
- Show the article image with a placeholder color (from the article's `placeholderColor` field) visible until the image loads.

### Constraints

- The UI app must use the backend app as its content source.
- The UI app must be independently installable and launchable on its own.
- In the future, adding a new filter type should not require a structural rewrite of the UI app.

### Explicitly out of scope

- No login, no user accounts, no per-user persisted preferences.
- No pagination unless you specifically want to demonstrate it.
- No article detail screen - list view only is sufficient.

---

## AI tooling write-up

### What we want to know

We expect you to use AI tools while building these apps. We want a short, honest write-up of which tools were used and how you used them.

### We would like you to cover

- Which tools you used, and roughly how often.
- One or two examples of where AI clearly accelerated your work.
- One or two examples of where AI suggested something wrong, or off-architecture, and how you corrected it.
- Anything you would do differently next time.

---

## Submission

### What to send us

- A link to a git repo containing both apps. A single repo with two app modules is fine; two separate repos is also fine.
- The repo must build out of the box on a modern Android Studio / Gradle setup. If anything special is needed, say so in the README.
- Tests are part of production-quality code - we expect to see them. We do not prescribe a framework or coverage target.

### Recommended, not required

- A README per app: how to build, how to install both APKs, how to verify the UI app is talking to the backend app.
- A short architecture note explaining your choices, if you find that easier than letting the code speak.

### Required

- The AI tooling write-up (see above).

### After you submit

We will review the code on our own first, then book a session with you to walk through your result.
