# Harmonizer

[<img alt='Get it on Google Play' src='https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png' height='80'/>](https://play.google.com/store/apps/details?id=tk.mygod.harmonizer&utm_source=global_co&utm_medium=prtnr&utm_content=Mar2515&utm_campaign=PartBadge&pcampaignid=MKT-Other-global-all-co-prtnr-py-PartBadge-Mar2515-1)

[![Build Status](https://api.travis-ci.org/Mygod/Harmonizer.svg)](https://travis-ci.org/Mygod/Harmonizer)

It is the app where you can generate beep sounds to censor something weird like *BEEP* and *BEEEEEEP*, etc.

Because censorship is funny.

## Dependencies

* Android Support Repository
* SBT

## Building

First, create a `local.properties` following [this guide](https://github.com/pfn/android-sdk-plugin#usage). Then:

    sbt clean android:packageRelease
