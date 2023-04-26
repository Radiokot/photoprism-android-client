# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.5.2-rc1] - 2023-04-26

### Fixed
- Duplication of gallery items, which happened to be at the edge of the data page
- Blinking of the empty view when the data is being loaded for the first time

### Added
- Infinite scrolling (swiping?) in the media viewer
- Clearing of the app internal downloads directory on startup, which frees "Data" storage

## [1.5.1] - 2023-04-22

### Fixed
- Non-functional video viewer when using mTLS authentication
- Displaying unmasked HTTP basic authentication credentials on the preferences screen

### Added
- Video preview caching – big videos and live photos are not be re-downloaded on each repeat

### Changed
- Lowered minimal video buffer duration requirements for faster start of playback
- Slightly improved look of video player controls

## [1.5.0] - 2023-04-20

### Added
- Video preview – now you can preview videos, animations and live photos without downloading them
- Empty view – when no media found, a corresponding view is shown instead of a blank screen

## [1.4.0] - 2023-04-12

### Added
- Preferences screen
- Bookmarks backup – export your bookmarks to restore them later or use on other device
- Disconnect from the library

## [1.3.0] - 2023-04-07

### Added
- Ability to include private content on the search screen
- Preview of RAW and vector images
- Support of mTLS (mutual TLS) authentication for Android 6 and higher
- Support of HTTP basic authentication with URL credentials (https://username:password@host.com)
- Showing an error when connecting to a private library without credentials

### Fixed
- Crash when there were too many months on the fast scroll bar

## [1.2.0] - 2023-04-03

### Added
- Month headers for the gallery items
- Chronological fast scroll – slide the scroll bar to quickly jump to a specific month
- Showing that the app is in the content selection mode

### Changed
- Made the viewer immersive – now it is dark and all the controls can be hidden by a tap
- Rearranged the action buttons on the viewer
- Made the status bar light

## [1.1.0] - 2023-03-28

### Added
- Search bookmarks – the search configuration can be bookmarked and applied later with a click
- Search filters guide shortcut to the search screen

### Changed
- "Clear text" button icon on the search screen made different from the "Reset search" button
icon on the main screen

## [1.0.2] - 2023-03-26

### Fixed
- Non-clickable files in the file selection dialog
- Search bar menu animation bug, when all the actions are visible during the animation

## [1.0.1] - 2023-03-25

### Changed
- Trust installed user CA so it is possible to use HTTPS with self-signed certificates
- Make env connection and content loading errors show the short summary of the cause

### Fixed
- "Library is not accessible" during the connection, when the entered root URL doesn't match the 
`PHOTOPRISM_SITE_URL` server config value 

[Unreleased]: https://github.com/Radiokot/photoprism-android-client/compare/1.5.2-rc1(10)...HEAD
[1.5.2-rc1]: https://github.com/Radiokot/photoprism-android-client/compare/1.5.1(9)...1.5.2-rc1(10)
[1.5.1]: https://github.com/Radiokot/photoprism-android-client/compare/1.5.0(8)...1.5.1(9)
[1.5.0]: https://github.com/Radiokot/photoprism-android-client/compare/1.4.0(7)...1.5.0(8)
[1.4.0]: https://github.com/Radiokot/photoprism-android-client/compare/1.3.0(6)...1.4.0(7)
[1.3.0]: https://github.com/Radiokot/photoprism-android-client/compare/1.2.0(5)...1.3.0(6)
[1.2.0]: https://github.com/Radiokot/photoprism-android-client/compare/1.1.0(4)...1.2.0(5)
[1.1.0]: https://github.com/Radiokot/photoprism-android-client/compare/1.0.2(3)...1.1.0(4)
[1.0.2]: https://github.com/Radiokot/photoprism-android-client/compare/1.0.1(2)...1.0.2(3)
[1.0.1]: https://github.com/Radiokot/photoprism-android-client/compare/1.0.0(1)...1.0.1(2)
