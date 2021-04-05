## To develop:

Install react via `npm install react react-dom`.
Install [shadow-cljs](https://github.com/thheller/shadow-cljs) via `npm install --save-dev shadow-cljs` and/or `npm install -g shadow-cljs`.

Run `npx shadow-cljs server` to get a repl server, open a browser (to [http://localhost:8020](http://localhost:8020) by default) to activate the compilation, and then use nREPL to connect to it. Open [http://localhost:9630/](http://localhost:9630/) and enable `watch`.

Can also run it with lein or deps but I haven't configured that here yet and you miss out on the shadow-server recompile watch. Might do it anyway to be able to use git-repo-versions of libraries.

In emacs: `M-x cider-connect-cljs`
hostname: `localhost`
socket: `3333`
type: `shadow`
build: `eco`
