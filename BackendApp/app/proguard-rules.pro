# Project-specific R8 rules.
#
# Intentionally minimal: Compose, Hilt/Dagger, Room and kotlinx.serialization all ship consumer
# rules via their artifacts, and this app uses no reflection of its own. Add keep rules here only
# if a release build is found to strip something it shouldn't.
