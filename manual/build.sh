#!/bin/bash
# Manual APK build for ShizukuGameBooster (no Gradle)
set -euo pipefail
export JAVA_HOME=$HOME/jdk17
export PATH=$JAVA_HOME/bin:$PATH
export HOME=/home/hatch

SDK=~/android-sdk
BT=$SDK/build-tools/34.0.0
AAPT2=$BT/aapt2
D8=$BT/d8
ZIPALIGN=$BT/zipalign
APKSIGNER=$BT/apksigner
ANDROID_JAR=$SDK/platforms/android-34/android.jar

PROJ=~/workspace/shizuku-build
M=$PROJ/manual
OUT=$M/out
rm -rf $OUT && mkdir -p $OUT/compiled $OUT/aar_res $OUT/gen $OUT/classes $OUT/dex

echo "=== [1/8] Preparing manifest ==="
sed 's/${applicationId}/com.booster.shizuku/g' $PROJ/app/src/main/AndroidManifest.xml > $OUT/AndroidManifest.xml
grep -c "applicationId" $OUT/AndroidManifest.xml || echo "manifest placeholders replaced"

echo "=== [2/8] Compiling app resources ==="
$AAPT2 compile --dir $PROJ/app/src/main/res -o $OUT/compiled/app_res.zip

echo "=== [3/8] Compiling AAR resources ==="
EXTRA_PKGS=""
R_ZIPS="-R $OUT/compiled/app_res.zip"
for d in $M/aar/*/; do
  name=$(basename $d)
  case "$name" in *testing*) echo "skip $name (test lib)"; continue;; esac
  if [ -d "$d/res" ] && [ -n "$(ls -A $d/res 2>/dev/null)" ]; then
    $AAPT2 compile --dir $d/res -o $OUT/aar_res/$name.zip 2>/dev/null || echo "  (no resources compiled for $name)"
    if [ -f $OUT/aar_res/$name.zip ]; then
      R_ZIPS="$R_ZIPS -R $OUT/aar_res/$name.zip"
    fi
  fi
  if [ -f "$d/AndroidManifest.xml" ]; then
    pkg=$(grep -o 'package="[^"]*"' $d/AndroidManifest.xml | head -1 | cut -d'"' -f2)
    if [ -n "$pkg" ] && [ "$pkg" != "com.booster.shizuku" ]; then
      EXTRA_PKGS="$EXTRA_PKGS --extra-packages $pkg"
    fi
  fi
done
echo "extra packages: $(echo $EXTRA_PKGS | tr ' ' '\n' | grep -c extra-packages || true)"

echo "=== [4/8] Linking (aapt2 link) ==="
$AAPT2 link -o $OUT/base.apk \
  -I $ANDROID_JAR \
  --manifest $OUT/AndroidManifest.xml \
  $R_ZIPS \
  $EXTRA_PKGS \
  --auto-add-overlay \
  --min-sdk-version 26 \
  --target-sdk-version 34 \
  --version-code 34 \
  --version-name "3.1.1-remix" \
  --java $OUT/gen
echo "R.java files: $(find $OUT/gen -name 'R.java' | wc -l)"
ls $OUT/base.apk

echo "=== [5/8] Compiling Kotlin + Java ==="
CP="$ANDROID_JAR"
for j in $M/aar/*/classes.jar; do
  case "$j" in *testing*) continue;; esac
  CP="$CP:$j"
done
for j in $M/libs/*.jar; do CP="$CP:$j"; done
CP="$CP:$HOME/kotlinc/lib/kotlin-stdlib.jar"
echo "$CP" > $OUT/classpath.txt
$HOME/kotlinc/bin/kotlinc \
  -cp "$CP" \
  -d $OUT/classes \
  -jvm-target 17 \
  -no-reflect \
  $PROJ/app/src/main/java/com/booster/shizuku/MainActivity.kt \
  $PROJ/app/src/main/java/com/booster/shizuku/ShizukuBooster.kt \
  $PROJ/app/src/main/java/com/booster/shizuku/Prefs.kt \
  $PROJ/app/src/main/java/com/booster/shizuku/ScriptStore.kt \
  $PROJ/app/src/main/java/com/booster/shizuku/SettingsActivity.kt \
  $PROJ/app/src/main/java/com/booster/shizuku/ScriptsActivity.kt \
  $PROJ/app/src/main/java/com/booster/shizuku/AboutActivity.kt \
  $PROJ/app/src/main/java/com/booster/shizuku/BoostBubbleService.kt \
  $PROJ/app/src/main/java/com/booster/shizuku/QuickBoostReceiver.kt \
  $PROJ/app/src/main/java/com/booster/shizuku/GameList.kt \
  $PROJ/app/src/main/java/com/booster/shizuku/GamesActivity.kt \
  $M/gen/com/booster/shizuku/databinding/ActivityMainBinding.java \
  $(find $OUT/gen -name 'R.java')
echo "compiled classes: $(find $OUT/classes -name '*.class' | wc -l)"

echo "=== [5b/8] Compiling Java sources with javac (kotlinc skips .java files) ==="
$JAVA_HOME/bin/javac -encoding UTF-8 -nowarn \
  -cp "$OUT/classes:$CP" \
  -d $OUT/classes \
  $M/gen/com/booster/shizuku/databinding/ActivityMainBinding.java \
  $(find $OUT/gen -name 'R.java')
echo "classes after javac: $(find $OUT/classes -name '*.class' | wc -l)"
[ -f $OUT/classes/com/booster/shizuku/R.class ] || { echo "FATAL: app R.class missing after javac"; exit 1; }

echo "=== [6/8] Dexing (d8) ==="
mkdir -p $OUT/allclasses
# merge dependency classes first-wins (dedupe); some jars are intentionally
# empty (relocated artifacts) so ignore unzip's "no files" exit code
for j in $M/aar/*/classes.jar; do
  case "$j" in *testing*) continue;; esac
  unzip -q -n "$j" -d $OUT/allclasses -x 'META-INF/*' || true
done
for j in $M/libs/*.jar $HOME/kotlinc/lib/kotlin-stdlib.jar; do
  unzip -q -n "$j" -d $OUT/allclasses -x 'META-INF/*' || true
done
# app classes last (overwrite with our own)
cp -r $OUT/classes/. $OUT/allclasses/
[ -f $OUT/allclasses/com/booster/shizuku/MainActivity.class ] || { echo "FATAL: app classes missing"; exit 1; }
(cd $OUT/allclasses && zip -q -r $OUT/allclasses.jar . -x 'META-INF/*')
[ -f $OUT/allclasses.jar ] || { echo "FATAL: allclasses.jar not created"; exit 1; }
$D8 --min-api 26 --output $OUT/dex $OUT/allclasses.jar
ls -la $OUT/dex/

echo "=== [7/8] Packaging APK ==="
cp $OUT/base.apk $OUT/unsigned.apk
cd $OUT/dex && zip -q -j $OUT/unsigned.apk classes*.dex
# NOTE: must be classes*.dex (not just classes.dex) — D8 splits into
# classes2.dex etc. past the 64K limit; packaging only classes.dex drops
# classes (incl. all R classes) and the app crashes on launch.
# add native libs / assets from AARs if any
for d in $M/aar/*/; do
  if [ -d "$d/jni" ]; then
    (cd $d && for abi in jni/*; do an=$(basename $abi); for so in $abi/*.so; do
      mkdir -p $OUT/libtmp/lib/$an; cp $so $OUT/libtmp/lib/$an/
    done; done)
  fi
  if [ -d "$d/assets" ] && [ -n "$(ls -A $d/assets 2>/dev/null)" ]; then
    mkdir -p $OUT/libtmp/assets && cp -r $d/assets/* $OUT/libtmp/assets/
  fi
done
# add the app's own assets (pre-installed script pack in assets/scripts/)
if [ -d "$PROJ/app/src/main/assets" ] && [ -n "$(ls -A $PROJ/app/src/main/assets 2>/dev/null)" ]; then
  mkdir -p $OUT/libtmp/assets && cp -r $PROJ/app/src/main/assets/* $OUT/libtmp/assets/
fi
if [ -d $OUT/libtmp ]; then (cd $OUT/libtmp && zip -q -r $OUT/unsigned.apk .); fi
$ZIPALIGN -f 4 $OUT/unsigned.apk $OUT/aligned.apk

echo "=== [8/8] Signing ==="
if [ ! -f $M/debug.keystore ]; then
  keytool -genkeypair -keystore $M/debug.keystore -storepass android \
    -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 -validity 10950 \
    -dname "CN=Android Debug,O=Android,C=US" 2>/dev/null
  echo "debug keystore created"
fi
$APKSIGNER sign --ks $M/debug.keystore --ks-pass pass:android --key-pass pass:android \
  --out $OUT/ShizukuGameBooster-remix.apk $OUT/aligned.apk
$APKSIGNER verify --print-certs $OUT/ShizukuGameBooster-remix.apk | head -5
ls -la $OUT/ShizukuGameBooster-remix.apk
echo "BUILD SUCCESS"
