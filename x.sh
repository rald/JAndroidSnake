#!/bin/bash

export ANDROID=$HOME/android-sdk/platforms/android-29/android.jar

rm -rf bin obj gen output game.apk

mkdir bin
mkdir obj
mkdir gen
mkdir output

aapt package -f -m \
	-J gen \
	-M AndroidManifest.xml \
	-S res \
	-I $ANDROID

javac -cp $ANDROID -d obj $(find src -name '*.java') -Xlint:deprecation

d8 --lib $ANDROID --output output $(find obj -name '*.class')

aapt package -f -m \
	-J gen \
    -S res \
    -M AndroidManifest.xml \
    -I $ANDROID \
    -F bin/game.apk \
    output

zipalign -v 4 bin/game.apk bin/game-aligned.apk

apksigner sign --ks android.keystore --ks-key-alias android --ks-pass pass:android --key-pass pass:android bin/game-aligned.apk

cp bin/game-aligned.apk game.apk
