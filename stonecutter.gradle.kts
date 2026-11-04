plugins {
    id("dev.kikugie.stonecutter")
}

// Version whose code is visible in src/ (switch with the "Set active project to ..." tasks).
stonecutter active "26.3"

stonecutter parameters {
    replacements {
        string(current.parsed >= "1.21.11") {
            replace("ResourceLocation", "Identifier")
        }
    }
}
