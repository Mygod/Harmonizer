import android.Keys._

android.Plugin.androidBuild

platformTarget := "android-24"

name := "Harmonizer"

scalaVersion := "2.11.8"

javacOptions ++= Seq("-source", "1.6", "-target", "1.6")

scalacOptions ++= Seq("-target:jvm-1.6", "-Xexperimental")

shrinkResources := true

typedViewHolders := false

resConfigs := Seq("zh")

useSupportVectors

resolvers += Resolver.sonatypeRepo("public")

libraryDependencies += "tk.mygod" %% "mygod-lib-android" % "2.0.1"

proguardVersion := "5.2.1"

proguardCache := Seq()
