(ns ecological.generator.gbs.ops
  (:require ;[datascript.core :as d]
            ;[clojure.string]
            ;[goog.crypt :as crypt]
            [quil.core :as qc :include-macros true]
            [quil.middleware :as qm]
            ;;[quil.applet :as qa]
            ["js-xxhash" :as xxhash]
            ["prando" :as prando]
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

(defn stable-hash-number
  ([source-string]
   (stable-hash-number source-string 42))
  ([source-string seed]
   (let [encoded (.encode (js/TextEncoder.) source-string)
         hash    (js/xxHash32. encoded seed)]
     (js/ParseInt
      (.slice (.toString hash 10) -4 -2)
      10))))

(defn stable-random-value
  "Returns a determanistically random value between min and max"
  ([hash-input min max]
   (stable-random-value hash-input min max 42))
  ([hash-input min max salt-count]
   (comment
    ;; TODO: figure out interpo with Prando so we can use it for determanistic randomness     
     (let [rng (. js/prando Prando hash-input)
           skipped (.skip rng salt-count)
           num (.nextInt rng min max)]
       num))
   (println min)
   (println max)
   (+ min (rand-int (- max min)))))

(defn stable-hash-choice
  [hash-input choices]
  (comment
    ;; TODO: figure out interpo with Prando so we can use it for determanistic randomness
    (let [rng (. js/prando Prando hash-input)]
      (.nextArrayItem rng (apply array choices))))
  (rand-nth choices)
  )

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
        image-tiles (mapv #(quot % tile-size) image-size)
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
        tile-size 8  ;; gbstudio uses 8x8 tiles for its scene backgrounds
        image-tile-size (mapv #(quot % tile-size) image-size)
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
    (js/console.log image-tile-size)
    (println image-tile-size)
    [{:db/id -1
      :type/gbs :gbs/resource
      :resource/type :image
      :resource/uuid id
      :resource/filename (str id ".png")
      :resource/filepath :memory
      :resource/image-size image-size
      :resource/image-tile-size image-tile-size
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
        ]
    [{:db/id -1
      :type/gbs :gbs/endpoint
      :endpoint/scene scene
      }]))

(defn place-endpoint-at-edge
  [db bindings parameters]
  (let [endpoint-id (get bindings :endpoint-id)
        scene       (get bindings :scene)
        ;; use a random edge if it isn't specified...
        edge        (or (:edge bindings) (stable-hash-choice endpoint-id ["edge-top" "edge-bottom" "edge-left" "edge-right"]))
        size-x      (first  (or (:size bindings) [0 0]))
        size-y      (second (or (:size bindings) [0 0]))
        hashed-seed 42 ;;TODO: make this work (stable-hash-number endpoint-id)
        direction   (case edge
                          "edge-bottom" "up"
                          "edge-top"    "down"
                          "edge-right"  "left"
                          "edge-left"   "right"
                          :default     "up"
                          )
        x           (case edge
                      "edge-top"    (+ 2 (rand-int (- size-x 6)))
                      "edge-bottom" (+ 2 (rand-int (- size-x 6)))
                      "edge-left"   0
                      "edge-right"  (- size-x 2)
                      :default      0)
        y           (case edge
                          "edge-top"    0
                          "edge-bottom" (- size-y 1)
                          "edge-left"   (+ 1 (rand-int (- size-y 3)))
                          "edge-right"  (+ 1 (rand-int (- size-y 3)))
                          :default
                          0)]
    (println [x y size-x size-y edge bindings])
    [{:db/id endpoint-id
      :entity/position [x y]
      :entity/direction direction}]))

(defn link-endpoint-to-scene
  [& {:keys [scene endpoint position] :or {scene (refer-wish) endpoint (refer-wish) position (refer-wish)}}]
  [{:db/id endpoint
    :endpoint/scene scene
    :entity/position position ; position in scene
    }])
