To develop: 

Install [shadow-cljs](https://github.com/thheller/shadow-cljs) via `npm install --save-dev shadow-cljs` and/or `npm install -g shadow-cljs`.
Run `npx shadow-cljs server eco` to get a repl server, and then open a browser (to [http://localhost:8020](http://localhost:8020) by default) to activate the compilation and use nREPL to connect to it.

In emacs: `M-x cider-connect-cljs`
hostname: `localhost`
socket: `3333`
type: `shadow`
build: `eco`
