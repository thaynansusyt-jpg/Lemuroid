# SEGA Edition checks

RulesTest.kt checks 128 unique medal IDs and reward colors, thresholds, persistent unlocks, the Super Sonic gate and core-specific layout values. Compile against KlAchievements.kt and KlPlaySettings.kt with Android SDK/CoreVariable types available. Tests do not award achievements to a real profile.

The signed Android build is validated by .github/workflows/build-klgba.yml. The website tests exercise real SQLite backup/restore validation for new rewards and existing profiles.
