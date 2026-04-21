package cg.epilote.desktop

internal val desktopBackendBaseUrl: String
    get() = System.getProperty("epilote.backend.url")
        ?: System.getenv("EPILOTE_BACKEND_URL")
        ?: "http://localhost:8080"
