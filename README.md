# <img src="app/src/main/res/mipmap-hdpi/ic_launcher.png" alt="Icon" style="vertical-align: bottom; height: 36px;"/> Gallery for PhotoPrism

[![GitHub discussions](https://img.shields.io/github/discussions/Radiokot/photoprism-android-client?label=Discussions&color=e2e0f6&style=flat-square)](https://github.com/Radiokot/photoprism-android-client/discussions) 
[![GitHub contributors](https://img.shields.io/github/contributors/Radiokot/photoprism-android-client?label=Contributors&color=e2e0f6&style=flat-square)](https://github.com/Radiokot/photoprism-android-client/graphs/contributors) 
[![Sponsors](https://img.shields.io/static/v1?label=Sponsors&message=54&color=e2e0f6&style=flat-square)](#sponsors) 

This Android app brings a convenient mobile gallery experience for [PhotoPrism](https://www.photoprism.app/).

<p float="left">
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/1.png" width=200 />
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/2.png" width=200 />
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/3.png" width=200 />
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/4.png" width=200 />
</p>

It does not support all the official web app functionality, but nevertheless has plenty of useful features:
- Sending photos and videos to Gmail, Telegram or any other app
- Content timeline with 5 grid size options, grouped by days and months
- Timeline scroll that lets you quickly jump to a specific month
- [Configurable search](https://github.com/Radiokot/photoprism-android-client/wiki/How-to-search-the-library)
- [Search bookmarks](https://github.com/Radiokot/photoprism-android-client/wiki/How-to-use-search-bookmarks) that let you save search configurations and apply them later
- Enhanced live photo viewer, which works best with Samsung and Apple shots
- Full-screen slideshow with 5 speed options
- Deleting items without archiving them first
- Importing photos and videos trough sharing
- [Connection to both private and public libraries](https://github.com/Radiokot/photoprism-android-client/wiki/Connection-guide)
- Endless session without the need to re-enter the password
- Support for [mTLS (mutual TLS)](https://github.com/Radiokot/photoprism-android-client/wiki/How-to-connect-to-a-library-with-mTLS-(mutual-TLS)-auth%3F), [HTTP basic auth](https://github.com/Radiokot/photoprism-android-client/wiki/Connection-guide#examples-of-valid-urls) and [SSO](https://github.com/Radiokot/photoprism-android-client/wiki/Connection-guide#sso) like Authelia, Cloudflare Access, etc.
- Basic TV compatibility that lets you browse and search the timeline with a remote-control
  (Not available in Google Play on TV, [install as APK](https://github.com/Radiokot/photoprism-android-client/issues/66#issuecomment-1667426238))
- ⭐ Extensions:
  - [Memories](https://github.com/Radiokot/photoprism-android-client/wiki/Memories-extension) – get a daily collection of photos and videos from the same day in past years
  - [Photo frame widget](https://github.com/Radiokot/photoprism-android-client/wiki/Photo-frame-widget-extension) – see random photos from your library on the home screen

The gallery is not intended to sync content with the library. 
For this, I recommend [Autosync](https://play.google.com/store/apps/details?id=com.ttxapps.autosync).

## Compatibility
The gallery runs on Android 5.0+ and it is confirmed to work with the PhotoPrism release of 
[November 30, 2025](https://github.com/photoprism/photoprism/releases/tag/251130-b3068414c).
Compatibility with older PhotoPrism versions may be partial.

## Download
[<img src="repository-assets/icon-github.svg" alt="APK" style="height: 1em;"/> APK from the latest release](https://github.com/Radiokot/photoprism-android-client/releases/latest)

[<img src="repository-assets/icon-obtainium.svg" alt="Obtainium" style="height: 1em;"/> Obtainium](https://apps.obtainium.imranr.dev/redirect?r=obtainium://app/%7B%22id%22%3A%22ua.com.radiokot.photoprism%22%2C%22url%22%3A%22https%3A%2F%2Fgithub.com%2FRadiokot%2Fphotoprism-android-client%22%2C%22author%22%3A%22Radiokot%22%2C%22name%22%3A%22PhotoPrism%20Gallery%22%2C%22preferredApkIndex%22%3A0%2C%22additionalSettings%22%3A%22%7B%5C%22includePrereleases%5C%22%3Afalse%2C%5C%22fallbackToOlderReleases%5C%22%3Atrue%2C%5C%22filterReleaseTitlesByRegEx%5C%22%3A%5C%22%5C%22%2C%5C%22filterReleaseNotesByRegEx%5C%22%3A%5C%22%5C%22%2C%5C%22verifyLatestTag%5C%22%3Afalse%2C%5C%22dontSortReleasesList%5C%22%3Afalse%2C%5C%22useLatestAssetDateAsReleaseDate%5C%22%3Afalse%2C%5C%22releaseTitleAsVersion%5C%22%3Afalse%2C%5C%22trackOnly%5C%22%3Afalse%2C%5C%22versionExtractionRegEx%5C%22%3A%5C%22%5C%22%2C%5C%22matchGroupToUse%5C%22%3A%5C%22%5C%22%2C%5C%22versionDetection%5C%22%3Atrue%2C%5C%22releaseDateAsVersion%5C%22%3Afalse%2C%5C%22useVersionCodeAsOSVersion%5C%22%3Afalse%2C%5C%22apkFilterRegEx%5C%22%3A%5C%22%5C%22%2C%5C%22invertAPKFilter%5C%22%3Afalse%2C%5C%22autoApkFilterByArch%5C%22%3Afalse%2C%5C%22appName%5C%22%3A%5C%22Gallery%20for%20PhotoPrism%5C%22%2C%5C%22shizukuPretendToBeGooglePlay%5C%22%3Afalse%2C%5C%22allowInsecure%5C%22%3Afalse%2C%5C%22exemptFromBackgroundUpdates%5C%22%3Afalse%2C%5C%22skipUpdateNotifications%5C%22%3Afalse%2C%5C%22about%5C%22%3A%5C%22A%20convenient%20gallery%20for%20PhotoPrism%20library%20with%20plenty%20of%20useful%20features%5C%22%2C%5C%22refreshBeforeDownload%5C%22%3Atrue%7D%22%2C%22overrideSource%22%3A%22GitHub%22%7D)

[<img src="repository-assets/icon-fdroid.png" alt="F-Droid" style="height: 1em;"/> F-Droid](https://f-droid.org/packages/ua.com.radiokot.photoprism)


[<img src="repository-assets/icon-gplay.svg" alt="Google Play" style="height: 1em;"/> Google Play](https://play.google.com/store/apps/details?id=ua.com.radiokot.photoprism) (has no [extension catalog](https://github.com/Radiokot/photoprism-android-client/wiki/Gallery-extensions))

## Sponsors
[<img src="https://avatars.githubusercontent.com/u/110915809?s=100" alt="Stefan" title="Stefan" height=70 />](https://github.com/mousey85)
[<img src="https://avatars.githubusercontent.com/u/257511385?s=100" alt="MagCris79" title="MagCris79" height=70 />](https://github.com/MagCris79)
[<img src="https://avatars.githubusercontent.com/u/35317168?s=100" alt="Piotr Icikowski" title="Piotr Icikowski" height=70 />](https://github.com/Icikowski)
[<img src="https://avatars.githubusercontent.com/u/664171?s=100" alt="bokahrij" title="bokahrij" height=70 />](https://github.com/bokahrij)
[<img src="https://avatars.githubusercontent.com/u/74513001?s=100" alt="FirefoxNL" title="FirefoxNL" height=70 />](https://github.com/FirefoxNL)
[<img src="https://avatars.githubusercontent.com/u/76539837?s=100" alt="boognish43" title="boognish43" height=70 />](https://github.com/boognish43)
[<img src="https://avatars.githubusercontent.com/u/17031473?s=100" alt="James" title="James" height=70 />](https://github.com/JameZUK)
[<img src="https://avatars.githubusercontent.com/u/20790694?s=100" alt="Niko Theiner" title="Niko Theiner" height=70 />](https://github.com/nikotheiner)
[<img src="https://avatars.githubusercontent.com/u/315648?s=100" alt="GDR!" title="GDR!" height=70 />](https://github.com/gjedeer)
[<img src="https://avatars.githubusercontent.com/u/14095706?s=100" alt="Jans Rautenbach" title="Jans Rautenbach" height=70 />](https://github.com/J4NS-R)
[<img src="https://avatars.githubusercontent.com/u/58886740?s=100" alt="james856" title="james856" height=70 />](https://github.com/james856)
[<img src="https://avatars.githubusercontent.com/u/38279393?s=100" alt="Beet4" title="Beet4" height=70 />](https://github.com/Beet4)
[<img src="https://avatars.githubusercontent.com/u/31888202?s=100" alt="bleepblub" title="bleepblub" height=70 />](https://github.com/bleepblub)
[<img src="https://avatars.githubusercontent.com/u/5235732?s=100" alt="Tim Omer" title="Tim Omer" height=70 />](https://github.com/timomer)
[<img src="https://github.com/user-attachments/assets/0ea9bd10-05d3-4cbf-bb44-f4b26d299f74" alt="Pablo Martín Ferraro" title="Pablo Martín Ferraro" height=70 />](https://www.instagram.com/pmferraro/)
[<img src="https://avatars.githubusercontent.com/u/75267371?s=100" alt="raghubhaskar" title="raghubhaskar" height=70 />](https://github.com/raghubhaskar)
[<img src="https://avatars.githubusercontent.com/u/9728953?s=100" alt="reverendj1" title="reverendj1" height=70 />](https://github.com/reverendj1)
[<img src="https://avatars.githubusercontent.com/u/103765434?s=100" alt="₿logging₿itcoin" title="₿logging₿itcoin" height=70 />](https://github.com/BrutusBondBTC)
[<img src="https://avatars.githubusercontent.com/u/104279101?s=100" alt="PitRejection2359" title="PitRejection2359" height=70 />](https://github.com/PitRejection2359)
[<img src="https://avatars.githubusercontent.com/u/35728385?s=100" alt="Sayantan Santra" title="Sayantan Santra" height=70 />](https://github.com/SinTan1729)
[<img src="https://avatars.githubusercontent.com/u/136379342?s=100" alt="Philipp" title="Philipp" height=70 />](https://github.com/Blendan1)
[<img src="https://avatars.githubusercontent.com/u/28670365?s=100" alt="Daniel Fuchs" title="Daniel Fuchs" height=70 />](https://github.com/dfoxg)
[<img src="https://avatars.githubusercontent.com/u/12165268?s=100" alt="Hulmgulm" title="Hulmgulm" height=70 />](https://github.com/hulmgulm)
[<img src="https://avatars.githubusercontent.com/u/36690764?s=100" alt="Jonas Gustavsson" title="Jonas Gustavsson" height=70 />](https://github.com/jonasgustavsson)
[<img src="https://avatars.githubusercontent.com/u/5047127?s=100" alt="Florian Voswinkel" title="Florian Voswinkel" height=70 />](https://github.com/FlorentBrianFoxcorner)
[<img src="https://avatars.githubusercontent.com/u/9214215?s=100" alt="Koen Koppens" title="Koen Koppens" height=70 />](https://github.com/koen81)
[<img src="https://avatars.githubusercontent.com/u/6559064?s=100" alt="Radon Rosborough" title="Radon Rosborough" height=70 />](https://github.com/raxod502)
[<img src="https://avatars.githubusercontent.com/u/301686?s=100" alt="Michael Mayer" title="Michael Mayer" height=70 />](https://github.com/lastzero)
[<img src="https://avatars.githubusercontent.com/u/15210372?s=100" alt="Theresa Gresch" title="Theresa Gresch" height=70 />](https://github.com/graciousgrey)
[<img src="https://avatars.githubusercontent.com/u/3181318?s=100" alt="Juha Lehtiranta" title="Juha Lehtiranta" height=70 />](https://github.com/yatzy)
[<img src="https://avatars.githubusercontent.com/u/2885748?s=100" alt="Neil Castelino" title="Neil Castelino" height=70 />](https://github.com/TwistTheNeil)
[<img src="https://avatars.githubusercontent.com/u/40500387?s=100" alt="Seth For Privacy" title="Seth For Privacy" height=70 />](https://github.com/sethforprivacy)
[<img src="https://avatars.githubusercontent.com/u/111684368?s=100" alt="C-Iaens" title="C-Iaens" height=70 />](https://github.com/C-Iaens)
[<img src="https://avatars.githubusercontent.com/u/6351543?s=100" alt="Tobias Fiechter" title="Tobias Fiechter" height=70 />](https://github.com/tobiasfiechter)
[<img src="https://avatars.githubusercontent.com/u/52239579?s=100" alt="ippocratis" title="ippocratis" height=70 />](https://github.com/ippocratis)
<br>
…and 19 anonymous sponsors.

*I am very grateful to everyone [supporting this project](https://radiokot.com.ua/tip) ❤️ To join this public list, email me the transaction reference once it is complete. By the way, sponsors get [extensions](https://github.com/Radiokot/photoprism-android-client/wiki/Gallery-extensions) for free.*

## License
I reject the concept of intellectual property. Claiming ownership over information that can be replicated perfectly and endlessly is inherently flawed. Consequently, any efforts to uphold such form of ownership inevitably result in some people gaining unjustifiable control over other's tangible resources, such as computers, printing equipment, construction materials, etc. <sup>[1](repository-assets/kinsella_against_intellectual_property.pdf)</sup>
When talking specifically about source code licensing – without a state violently enforcing [copyright monopolies](https://torrentfreak.com/language-matters-framing-the-copyright-monopoly-so-we-can-keep-our-liberties-130714/), it would be ludicrous to assume that a mere text file in a directory enables someone to restrict processing copies of this information by others on their very own computers. 
However, there is [such a file](LICENSE) in this repository bearing the GPLv3 license. Why?

One would expect someone with such an attitude to not use the license at all, use a permissive license, or [explicitly unlicense](https://unlicense.org/).
But for me, to do so is to voluntarily limit my means of defense. To act as a gentleman with those who readily exploit state violence against you is to lose.
In a world where copyright monopolies are violently enforced, I choose GPLv3 for the software I really care for, because under the current circumstances this license is a tool that:
- Allows **others** to freely use, modify and distribute this software, without the risk of being sued;
- Enables **me** to pull all the valuable changes from public forks back to the trunk, also without the risk of being sued;
- **Knocks down a peg** individuals or companies willing to monopolize their use case or modifications of this software.

## Tech stack
- Kotlin
- Classic views & ViewModel
- RxJava for concurrency
- Koin for dependency injection
- OkHTTP & Retrofit for networking
- Room database
- kotlin-logging & slf4j-handroid for logging
- Picasso for images & ExoPlayer for videos
- FastAdapter for lists
- Offline License Key for extensions activation
- MapLibre for maps
