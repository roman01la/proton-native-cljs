- Proton Native — React Native like environment on top of Qt for building desktop apps in JS https://proton-native.js.org/#/
- Ported macOS calculator app example from JS to ClojureScript https://github.com/kusti8/proton-native/blob/master/examples/Calculator/app.js

## Build
```shell script
clojure -m cljs.main -co build.edn -c app.core
```

## Run
```shell script
node out/main.js
```

<img src="./screenshot.png" width="400" />
