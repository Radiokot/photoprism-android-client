# <img src="app/src/main/res/mipmap-hdpi/ic_launcher.png" alt="Icon" style="vertical-align: bottom; height: 1.2em;"/> Gallery for PhotoPrism

[![GitHub discussions](https://img.shields.io/github/discussions/Radiokot/photoprism-android-client?label=Discussions&color=e2e0f6&style=flat-square)](https://github.com/Radiokot/photoprism-android-client/discussions) 
[![GitHub contributors](https://img.shields.io/github/contributors/Radiokot/photoprism-android-client?label=Contributors&color=e2e0f6&style=flat-square)](https://github.com/Radiokot/photoprism-android-client/graphs/contributors) 
[![Sponsors](https://img.shields.io/static/v1?label=Sponsors&message=6&color=e2e0f6&style=flat-square)](#sponsors) 

This Android app brings a convenient mobile gallery experience for [PhotoPrism](https://www.photoprism.app/).

<p float="left">
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/1.png" width=200 />
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/2.png" width=200 />
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/3.png" width=200 />
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/4.png" width=200 />
</p>

It does not support all the official web app functionality, but nevertheless has plenty of useful features:
- Sending photos or videos to Gmail, Telegram or any other app
- Grouping the content by days and months
- Chronological scroll bar that lets you quickly jump to a specific month
- [Configurable search](https://github.com/Radiokot/photoprism-android-client/wiki/How-to-search-the-library)
- [Search bookmarks](https://github.com/Radiokot/photoprism-android-client/wiki/How-to-use-search-bookmarks) that let you save search configurations and apply them later
- Endless session without the need to re-enter the password
- [Connection to both private and public libraries](https://github.com/Radiokot/photoprism-android-client/wiki/Connection-guide)
- Support of [mTLS (mutual TLS)](https://github.com/Radiokot/photoprism-android-client/wiki/How-to-connect-to-a-library-with-mTLS-(mutual-TLS)-auth%3F) and [HTTP basic authentication](https://github.com/Radiokot/photoprism-android-client/wiki/Connection-guide)
- TV compatibility to easily browse your library with a remote-control

The gallery is not intended to sync content with the library. 
I recommend using [Autosync app](https://play.google.com/store/apps/details?id=com.ttxapps.autosync).

## Compatibility
The gallery is confirmed to work with PhotoPrism versions from 
[July 19, 2023](https://github.com/photoprism/photoprism/releases/tag/230719-73fa7bbe8) 
down to [October 9, 2021](https://github.com/photoprism/photoprism/releases/tag/211009-d6cc8df5). 
It may work with older ones though, I just haven't tested it.
The app uses [PhotoPrism Web Service API](https://docs.photoprism.app/developer-guide/api/), 
which serves only the original frontend needs and doesn't guarantee backward compatibility. 
When a new version of PhotoPrism comes out, the app may break.

## Download
[<img src="repository-assets/icon-github.svg" alt="APK" style="height: 1em;"/> APK from the latest release](https://github.com/Radiokot/photoprism-android-client/releases/latest)


[<img src="repository-assets/icon-fdroid.png" alt="F-Droid" style="height: 1em;"/> F-Droid](https://f-droid.org/packages/ua.com.radiokot.photoprism)


[<img src="repository-assets/icon-gplay.svg" alt="Google Play" style="height: 1em;"/> Google Play](https://play.google.com/store/apps/details?id=ua.com.radiokot.photoprism)

## Sponsors
[<img src="https://avatars.githubusercontent.com/u/6351543?s=100&v=4" alt="Tobias Fiechter" height=70 />](https://github.com/tobiasfiechter)
[<img src="https://avatars.githubusercontent.com/u/15210372?s=100&v=4" alt="Theresa Gresch" height=70 />](https://github.com/graciousgrey)
[<img src="https://avatars.githubusercontent.com/u/111684368?s=100&v=4" alt="C-Iaens" height=70 />](https://github.com/C-Iaens)
[<img src="https://avatars.githubusercontent.com/u/52239579?s=100&v=4" alt="ippocratis" height=70 />](https://github.com/ippocratis)
<br>
*I am very grateful to everyone supporting this project ♥ To join this public sponsors list, [make a donation](https://radiokot.com.ua/tip) with your preferred payment method and email me the transaction reference. &emsp;*

## Tech stack
- Kotlin
- RxJava
- Koin dependency injection
- OkHTTP networking + Retrofit
- Room database
- kotlin-logging with slf4j-handroid
- ExoPlayer
- FastAdapter
- ViewModel
