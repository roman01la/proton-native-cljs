- Proton Native — React Native like environment on top of Qt for building desktop apps in JS https://proton-native.js.org/#/
- Ported macOS calculator app example from JS to ClojureScript https://github.com/kusti8/proton-native/blob/master/examples/Calculator/app.js

1. Build `clojure -m cljs.main -co build.edn -c app.core`
2. Install NPM deps `yarn install`
3. Run in dev `node out/main.js`
4. Package executable `npx nexe out/main.js -o calculator`
5. Run `calculator` executable

<img src="./screenshot.png" width="400" />
