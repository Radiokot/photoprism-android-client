# <img src="app/src/main/res/mipmap-hdpi/ic_launcher.png" alt="Icon" style="vertical-align: bottom; height: 1.2em;"/> PhotoPrism Gallery

A native Android gallery and content provider for your [PhotoPrism library](https://www.photoprism.app/). 

<p float="left">
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/1.png" width=200 />
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/2.png" width=200 />
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/3.png" width=200 />
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/4.png" width=200 />
</p>

The official PhotoPrism PWA, despite being mobile-friendly, can't be integrated into the OS and has other drawbacks.
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

## Content sync
The gallery is not intended to sync content with the library. 
I recommend using [Autosync app](https://play.google.com/store/apps/details?id=com.ttxapps.autosync).

## Compatibility
The gallery is confirmed to work with PhotoPrism versions from 
[November 18, 2022](https://github.com/photoprism/photoprism/releases/tag/221118-e58fee0fb) 
down to [October 9, 2021](https://github.com/photoprism/photoprism/releases/tag/211009-d6cc8df5). 
It may work with older ones though, I just haven't tested it.
The app uses [PhotoPrism Web Service API](https://docs.photoprism.app/developer-guide/api/), 
which serves only the original frontend needs and doesn't guarantee backward compatibility. 
When a new version of PhotoPrism comes out, the app may break.

## Download
- [APK from the latest release](https://github.com/Radiokot/photoprism-android-client/releases/latest)
- [F-Droid](https://f-droid.org/packages/ua.com.radiokot.photoprism)
- [Google Play](https://play.google.com/store/apps/details?id=ua.com.radiokot.photoprism)

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

---
🙌🏻 [Send me a tip](https://radiokot.com.ua/tip)&emsp;&emsp;🏠 [Visit my homepage](https://radiokot.com.ua/tip)
