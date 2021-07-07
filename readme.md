## To develop:

1. Install dependencies 
  - Install [node.js](https://nodejs.org/en/).
  - Install react via `npm install react react-dom`.
  - Install [shadow-cljs](https://github.com/thheller/shadow-cljs) via `npm install --save-dev shadow-cljs` and/or `npm install -g shadow-cljs`.

2. Run `npx shadow-cljs clj-run ecological.build/build-assets nil "resources/assets"` to build the asset resources directory.

3. Run `npx shadow-cljs server` to get a repl server.
  - Open [http://localhost:9630/](http://localhost:9630/) and enable `Build > Watch` to activate the compilation.
    - If it fails, click on the red dot to see the compile log.
  - Open a browser (to [http://localhost:8020](http://localhost:8020) by default)
    - And then use nREPL to connect to it, if you want. 

Can also run it with lein or deps but I haven't configured that here yet and you miss out on the shadow-server recompile watch. Might do it anyway to be able to use git-repo-versions of libraries.

### Connecting to REPL

In emacs: `M-x cider-connect-cljs`    
hostname: `localhost`    
socket: `3333`    
type: `shadow`    
build: `eco`    
