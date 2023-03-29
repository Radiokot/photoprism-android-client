# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Month headers for the gallery items

## [1.1.0] - 2022-03-28

### Added
- Search bookmarks – the search configuration can be bookmarked and applied later with a click
- Search filters guide shortcut to the search screen

### Changed
- "Clear text" button icon on the search screen made different from the "Reset search" button
icon on the main screen

## [1.0.2] - 2022-03-26

### Fixed
- Non-clickable files in the file selection dialog
- Search bar menu animation bug, when all the actions are visible during the animation

## [1.0.1] - 2022-03-25

### Changed
- Trust installed user CA so it is possible to use HTTPS with self-signed certificates
- Make env connection and content loading errors show the short summary of the cause

### Fixed
- "Library is not accessible" during the connection, when the entered root URL doesn't match the 
`PHOTOPRISM_SITE_URL` server config value 

[Unreleased]: https://github.com/Radiokot/photoprism-android-client/compare/1.1.0(4)...HEAD
[1.1.0]: https://github.com/Radiokot/photoprism-android-client/compare/1.0.2(3)...1.1.0(4)
[1.0.2]: https://github.com/Radiokot/photoprism-android-client/compare/1.0.1(2)...1.0.2(3)
[1.0.1]: https://github.com/Radiokot/photoprism-android-client/compare/1.0.0(1)...1.0.1(2)
