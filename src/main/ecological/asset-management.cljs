(ns ecological.asset-management
  (:require [shadow.resource :as resource]
            [cljs-node-io.core :as io]
            ))

(js/console.log "Ecological Generator, 2021")

;(js/alert "test")

;(def resource-manifest)

(defn hook
  {:shadow.build/stage :flush}
  [build-state & args]
  (prn [:hello-world args])
  (io/spit "node-asset-test.edn" [{:test "data"}])
  build-state)
