# Harmonizer
It is the app where you can generate beep sounds to censor something weird like *BEEP* and *BEEEEEEP*, etc.

Because censorship is funny.

## Dependencies
* Android Support Repository
* SBT

## Building
First, create a `local.properties` following [this guide](https://github.com/pfn/android-sdk-plugin#usage). Then:

    sbt clean android:packageRelease
