# <img src="app/src/main/res/mipmap-hdpi/ic_launcher.png" alt="Icon" style="vertical-align: bottom; height: 1.2em;"/> Gallery for PhotoPrism

[![GitHub discussions](https://img.shields.io/github/discussions/Radiokot/photoprism-android-client?label=Discussions&color=e2e0f6&style=flat-square)](https://github.com/Radiokot/photoprism-android-client/discussions) 
[![GitHub contributors](https://img.shields.io/github/contributors/Radiokot/photoprism-android-client?label=Contributors&color=e2e0f6&style=flat-square)](https://github.com/Radiokot/photoprism-android-client/graphs/contributors) 
[![Sponsors](https://img.shields.io/static/v1?label=Sponsors&message=4&color=e2e0f6&style=flat-square)](#sponsors) 

A user-friendly Android gallery that allows browsing [your library](https://www.photoprism.app/) content and sending it to other apps. 

<p float="left">
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/1.png" width=200 />
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/2.png" width=200 />
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/3.png" width=200 />
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/4.png" width=200 />
</p>

The official PhotoPrism PWA, despite being mobile-friendly, can't be integrated into the OS and, no matter what, [feels like a web page](https://blog.perryizgr8.com/photos/2023/03/20/google-photos-alt.html#photoprism).
This app does not (and probably will not) support all the web-client features, but nevertheless it can:
- Provide the library content to other Android apps – 
you can select PhotoPrism Gallery among other galleries when picking a photo or video
- Open the library content in suitable apps –
you can use your favorite image editor or video player instead of web viewer

## Other features
- Chronological grouping – the content is grouped by days
- Chronological fast scroll – slide the scroll bar to quickly jump to a specific month
- Search by media types and custom queries
- Search bookmarks – the search configuration can be bookmarked and applied later with a click
- Endless session – no need to re-enter password
- Connection to both private and public libraries
- Support of mTLS (mutual TLS) and HTTP basic authentication
- TV compatibility – easy browsing with a remote-control

The gallery is not intended to sync content with the library. 
I recommend using [Autosync app](https://play.google.com/store/apps/details?id=com.ttxapps.autosync).

## Compatibility
The gallery is confirmed to work with PhotoPrism versions from 
[June 7, 2023](https://github.com/photoprism/photoprism/releases/tag/230607-9e086c7eb) 
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
[<img src="https://avatars.githubusercontent.com/u/111684368?s=100&v=4" alt="C-Iaens" height=50 />](https://github.com/C-Iaens)
[<img src="https://avatars.githubusercontent.com/u/52239579?s=100&v=4" alt="ippocratis" height=50 />](https://github.com/ippocratis)
<br>
*I am very grateful to everyone supporting this project ♥ To join this public sponsors list, [make a donation](https://radiokot.com.ua/tip) with your preferred payment method and email me the transaction reference. &emsp;*

## Tech stack
- Kotlin
- RxJava
- Koin dependency injection
- OkHTTP + Retrofit
- ViewModel
- Room
- kotlin-logging with slf4j-handroid
- ExoPlayer
- FastAdapter
