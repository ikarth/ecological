(ns ecological.generator.gbs.ops
  (:require ;[datascript.core :as d]
            ;[clojure.string]
            ;[goog.crypt :as crypt]
            [quil.core :as qc :include-macros true]
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
    :scene/uuid (str (random-uuid))
    :scene/editor-position [20 20] ;;(refer-wish) ; TODO: find empty editor position
    :scene/name "generated greenfield scene"
    }])

(defn create-background
  [db bindings parameters]
  (let [image-size [160 160] ;; todo: check actual size of image
        tile-size 8  ;; gbstudio uses 8x8 tiles for its scene backgrounds
        image-tiles (map #(quot % tile-size) image-size)
        resource-id :generated
        ]
    [{:db/id -1
      :type/gbs :gbs/background
      :background/uuid (str (random-uuid))
      :background/size image-tiles
      :background/filename "GENERATED"
      :background/resource resource-id
      }]))

(defn create-image
  [db bindings parameters]
  [{:db/id -1
    :type/gbs :gbs/resource
    :resource/type :image
    :resource/filename "test.png" ;(str (random-uuid) ".png")
    :resource/filepath ""
    :resource/image-size [160 160]}])

(defn create-speckled-background-image
  [db bindings parameters]
  (let [id (random-uuid)
        image-size [160 160]
        speckle-density 0.18
        ;palette [(qc/color 200 100 0) (qc/color 0 100 200)]  ;0x86c06cff 0xe0f8cfff
        image-data
        (qc/with-sketch (qc/get-sketch-by-id "quil-canvas")
          (let [image-target (qc/create-image (first image-size) (second image-size))]
            (dotimes [x (first image-size)]
              (dotimes [y (second image-size)]
                (let [intensity (if (> speckle-density (qc/random 100)) 0 1)]
                  (qc/set-pixel image-target
                                x
                                y
                                ;;(qc/color 200 100 0)
                                (if (> speckle-density (qc/random 100))
                                  (qc/color 0x86 0xc0 0x6c)
                                  (qc/color 0xe0 0xf8 0xcf))
                                ;(nth palette intensity (qc/color 20 30 20))
                                          ))))
            (qc/update-pixels image-target)
            image-target))]
    [{:db/id -1
      :type/gbs :gbs/resource
      :resource/type :image
      :resource/uuid id
      :resource/filename (str id ".png")
      :resource/filepath :memory
      :resource/image-size image-size
      :resource/image-data image-data
      }]))

(defn create-connection
  [db bindings parameters]
  (let [left  (get-or-wish db bindings :end-left)
        right (get-or-wish db bindings :end-right)]
    [{:db/id -1
      :type/gbs :gbs/connection
      :connection/direction (refer-wish)
      :connection/left-end left
      :connection/right-end right
      :connection/left-graphic (refer-wish)
      :connection/right-graphic (refer-wish)
      }]))

(defn create-endpoint
  [db bindings parameters]
  (let [scene      (get-or-wish db bindings :scene)
        position   [0 0] ;(get-or-wish db bindings :position)
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

;; (defn convert-connection-to-trigger
;;   [db bindings parameters]
;;   (let [connection-id    (get bindings :db/id)
;;         connection-left  (get bindings :connection/left-end)
;;         connection-right (get bindings :connection/right-end)


;;         ])
;;   ;[& {:keys [] :or {}}]
;;   )



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


