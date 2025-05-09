# Grippo

Clear project
```
./gradlew clean      
cd iosApp
rm -rf iosApp/Pods
rm -rf iosApp/Podfile.lock
rm -rf build
rm -rf ~/Library/Developer/Xcode/DerivedData
pod deintegrate
pod install
```