(ns ecological.generator.gbs.query
  (:require [datascript.core :as d]))



(defn scenes-connected?
  "Return true if an existing connection links the two scenes."
  [sceneA sceneB]
  false ;; TODO
  )

(defn connection-empty?
  "True if neither of the endpoints are linked."
  [connection-id]
  ;;(d/query '[])
  true
  )
