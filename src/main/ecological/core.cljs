(ns ecological.core
  (:require [reagent.dom :as r]
            [ecological.views :as views]
            [ecological.events :as events]
            [ecological.generator.gencore]
            [shadow.resource :as resource]))

(js/console.log "Ecological Generator, 2021")

;(js/alert "test")

;(def resource-manifest)


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
                                        ;(spit (str "./test_start.txt") "Test text.")
  (let []
    (events/init-database nil)
    (ecological.generator.gencore/setup)
    (events/update-database-view nil)
    (r/render [views/app]
              (.getElementById js/document "app"))))

; This is the `ecological.core.init()` that's triggered in the html
(defn ^:export init []
  
  (start))

;; (defn ^:export main
;;   []
;;   (start))

;; (defn hook
;;   {:shadow.build/stage :flush}
;;   [build-state & args]
;;   (prn [:hello-world args])
;;   build-state)
