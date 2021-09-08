(ns ecological.generator.gbs.ops
  (:require ;[datascript.core :as d]
            ;[clojure.string]
            ;[goog.crypt :as crypt]
            [quil.core :as qc]
            [quil.middleware :as qm]
            ;[quil.applet :as qa]
            ))



;; Generative Operations
;; Create a scene
;; Create a scene with connections to specific scenes
;; Create a scene with connections in particular directions
;; Add a connection to a scene
;; Turn a finalized connection into a trigger + script
;; Update a connection to point to a specific scene
;; Draw a background (border + empty room?)
;; Draw a background (empty + indicators for connections?)
;; generate background based on scene properties
;; generate collisions based on background
;; ...and then we can move on to making graphs of scenes

(defn create-scene
  [db bindings parameters]
  [{:db/id -1
    :type/gbs :gbs/scene
    ;; :scene/uuid (str (random-uuid))
    :scene/editor-position :wish
    :scene/name "generated greenfield scene"
     }])

(defn create-connection
  [db bindings parameters]
  [{:db/id -1
    :type/gbs :gbs/connection
    :connection/direction :wish
    }])

(defn create-endpoint
  [db bindings parameters]
  (let [scene      (get bindings :scene      :wish)
        position   (get bindings :position   :wish)
        connection (get bindings :connection :wish)]
    [{:db/id -1
      :type/gbs :gbs/endpoint
      :parent/scene scene
      :parent/connection connection
      :entity/position position}]))

(defn link-endpoint-to-scene
  [& {:keys [scene endpoint position] :or {scene :wish endpoint :wish position :wish}}]
  [{:db/id endpoint
    :parent/scene scene
    :entity/position position ; position in scene
    }])

(defn link-endpoint-to-connection
  [& {:keys [connection endpoint] :or {connection :wish endpoint :wish}}]
  [{:db/id endpoint
    :parent/connection connection
    }])

(defn add-background-to-scene
  [db bindings parameters])

(defn create-background-from-scene
  [db bindings parameters])

(defn add-lock-to-scene
  [db bindings parameters])


