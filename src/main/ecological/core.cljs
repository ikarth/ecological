(ns ecological.core
  (:require [reagent.dom :as r]
            [ecological.views :as views]))

(js/console.log "Hey from proto-repl!")

(js/alert "test")


;; (defn ^:dev/after-load start
;;   []
;;   (r/render-component [views/app]
;;                       (.getElementById js/document "app")))

; This is the :devtools {:before-load script
(defn stop []
  (js/console.log "Stopping..."))

; This is the :devtools {:after-load script
(defn start []
  (js/console.log "Starting...")
  (r/render [views/app]
                      (.getElementById js/document "app")))

; This is the `ecological.core.init()` that's triggered in the html
(defn ^:export init []
  (start))

;; (defn ^:export main
;;   []
;;   (start))
