(ns ecological.core)

(js/console.log "Hey from proto-repl!")

(js/alert "test")

; This is the :devtools {:before-load script
(defn stop []
  (js/console.log "Stopping..."))

; This is the :devtools {:after-load script
(defn start []
  (js/console.log "Starting..."))

; This is the `ecological.core.init()` that's triggered in the html
(defn ^:export init []
  (start))
