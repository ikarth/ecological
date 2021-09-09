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

(defn refer-wish
  ([] 1)
  ([db] 1))

(defn get-or-wish [db bindings key]
  (let [binding (get bindings key :wish)]
    (if (= binding :wish)
      (refer-wish db)
      binding)))

(defn create-scene
  [db bindings parameters]
  [{:db/id -1
    :type/gbs :gbs/scene
    ;; :scene/uuid (str (random-uuid))
    :scene/editor-position (refer-wish)
    :scene/name "generated greenfield scene"
     }])

(defn create-connection
  [db bindings parameters]
  (let [left  (get-or-wish db bindings :end-left)
        right (get-or-wish db bindings :end-right)]
    [{:db/id -1
      :type/gbs :gbs/connection
      :connection/direction (refer-wish)
      :connection/left-end left
      :connection/right-end right
      }]))

(defn create-endpoint
  [db bindings parameters]
  (let [scene      (get-or-wish db bindings :scene)
        position   (get-or-wish db bindings :position)
        ;connection (get-or-wish db bindings :connection)
        ]
    [{:db/id -1
      :type/gbs :gbs/endpoint
      :endpoint/scene scene
      ;:endpoint/connection connection
      :entity/position position}]))

(defn link-endpoint-to-scene
  [& {:keys [scene endpoint position] :or {scene (refer-wish) endpoint (refer-wish) position (refer-wish)}}]
  [{:db/id endpoint
    :endpoint/scene scene
    :entity/position position ; position in scene
    }])

;; (defn link-any-endpoint-to-connection
;;   [& {:keys [connection endpoint] :or {connection (refer-wish) endpoint (refer-wish)}}]
;;   []
;;   )

;; (defn link-left-endpoint-to-connection
;;   [& {:keys [connection endpoint] :or {connection (refer-wish) endpoint (refer-wish)}}]
;;   [{:db/id connection
;;     :connection/left-end endpoint
;;     }])

;; (defn link-right-endpoint-to-connection
;;   [& {:keys [connection endpoint] :or {connection (refer-wish) endpoint (refer-wish)}}]
;;   [{:db/id connection
;;     :connection/right-end endpoint
;;     }])

;; (defn add-background-to-scene
;;   [db bindings parameters])

;; (defn create-background-from-scene
;;   [db bindings parameters])

;; (defn add-lock-to-scene
;;   [db bindings parameters])


