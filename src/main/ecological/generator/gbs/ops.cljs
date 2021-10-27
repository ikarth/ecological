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
      ;:background/resource resource-id
      }]))

(defn create-image
  [db bindings parameters]
  [{:db/id -1
    :type/gbs :gbs/resource
    :resource/type :image
    :resource/filename "test.png" ;(str (random-uuid) ".png")
    :resource/filepath ""
    :resource/image-size [160 160]}])

(defn copy-image
  [db bindings parameters]
  
  )

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
                (let [;;intensity (if (> speckle-density (qc/random 100)) 0 1)
                      ]
                  (qc/set-pixel image-target
                                x
                                y
                                ;;(qc/color 200 100 0)
                                (if (> speckle-density (qc/random 100))
                                  (qc/color 0x86 0xc0 0x6c)
                                  (qc/color 0xe0 0xf8 0xcf))
                                ;(nth palette intensity (qc/color 20 30 20))
                                ))))
            
            ;;(js/console.log image-target)
            (qc/update-pixels image-target)
            image-target))]
    ;;(js/console.log image-tile-size)
    ;;(println image-tile-size)
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
        edge        (or (:edge bindings) (stable-hash-choice endpoint-id
                                                             ["edge-top"
                                                              "edge-bottom"
                                                              "edge-left"
                                                              "edge-right"]))
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

(defn draw-endpoints-on-background
  "Creates a new background (old background + transition graphics) and makes that the background for the scene in question."
  [db [scene image-resource background image-tiles image-data image-size image-tile-size endpoint direction position] [temp-id-resource temp-id-background]]
  ;;(js/console.log image-data)
  ;;(js/console.log (qc/pixels image-data))
  (let [img-id (random-uuid)
        image-filename (str img-id ".png")

        new-image-data       
        (qc/with-sketch (qc/get-sketch-by-id "quil-canvas")
          (let [tile-x 8
                tile-y 8
                pos-x (* tile-x (+ 0.0 (first position)))
                pos-y (* tile-y (+ 0.0 (second position)))
                pos-x (case direction
                        "left" (+ pos-x tile-x) pos-x) ; start higher when on right edge
                image-target (qc/create-image (first image-size) (second image-size))
                size-x (* tile-x (case direction "down" 2 "up" 2 1))
                size-y (* tile-y (case direction "down" 1 "up" 1 1))
                ]
            (qc/pixels image-target)
            (println [pos-x pos-y direction])
            (dotimes [x (first image-size)]
              (dotimes [y (second image-size)]
                (qc/set-pixel image-target
                              x
                              y
                              (if (and (= 0 (rem (+ x y) 2))
                                       (<= pos-x x (+ pos-x size-x))
                                       (<= pos-y y (+ pos-y size-y)))
                                (let [length-y
                                      (case direction
                                        "down" (* 2 tile-y)
                                        "up" (* 2 tile-y)
                                        tile-y
                                        )
                                      axis 
                                      (case direction
                                        "down" (- y pos-y) 
                                        "up" (.abs js/Math (- size-y (- y pos-y))) ;(- (* tile-y 2) y)
                                        "left" (.abs js/Math (- tile-x (- x pos-x)));(- (* tile-x 2) x)
                                        (- x pos-x))
                                      
                                      ]
                                  (println [direction [x y] [pos-x pos-y] [(- x pos-x)(- y pos-y)] axis])
                                  (case (int (+ (* (rem (+ x y) 4) 0.48) (* axis 0.25)))
                                    ;;(qc/color 0x1f 0xf3 0xff)
                                    0 (qc/color 0x07 0x18 0x21)
                                    1 (qc/color 0x30 0x68 0x50)
                                    2 (qc/color 0x86 0xc0 0x6c)
                                    (qc/color 0xe0 0xf8 0xcf)
                                    ;;(qc/color 0x1f 0x03 0xff)
                                    ))
                                (qc/get-pixel image-data x y)
                                ;; (if (> 0.3 (qc/random 100))
                                ;;   (qc/color 0xff 0x03 0x30)
                                ;;   (qc/get-pixel image-data x y))
                                
                              ))))
            ;;(qc/update-pixels image-target)
            ;;(println direction)
            ;;(println position)
            ;;(js/console.log image-target)
            ;; (dotimes [x (* 3 tile-x)]
            ;;   (dotimes [y (* 3 tile-y)]                
            ;;     (if (> 0 (rem (+ x y) 2))
            ;;       (let [axis (case direction
            ;;                   "up" y
            ;;                   "down" (- 0 y)
            ;;                   "right" (- 0 x)
            ;;                   x
            ;;                   )
            ;;             pixel-color
            ;;             (qc/color 0x00 0x08 0xff)
            ;;             ;; other-pixel-color
            ;;             ;; (case (int (.floor js/Math(/ axis 4.0)))
            ;;             ;;   0 (qc/color 0x07 0x18 0x21)
            ;;             ;;   1 (qc/color 0x30 0x68 0x50)
            ;;             ;;   2 (qc/color 0x86 0xc0 0x6c)
            ;;             ;;   (qc/color 0xe0 0xf8 0xcf))
            ;;             ]
            ;;         (qc/set-pixel image-target (+ pos-x x) (+ pos-y y) pixel-color)))))
            ;;(println image-target)
            ;;(js/console.log image-target)
            (qc/update-pixels image-target)
            image-target))]
    [; create new image (with added transition)
     {:db/id -1
      :type/gbs :gbs/resource
      :resource/type :image
      :resource/uuid img-id
      :resource/filename image-filename
      :resource/image-data new-image-data
      :resource/image-size image-size
      :resource/image-tile-size image-tile-size
      :resource/filepath :memory}
     ;; ;; create new background (with same size)
     {:db/id -2
      ;;:type/gbs :gbs/background
      :background/uuid (str (random-uuid))
      :background/size image-tiles
      :background/filename image-filename
      :background/resource -1
      :background/filepath :memory
      } 
      
     ;; ;; assign new background to scene
     [:db/add scene :scene/background -2]
     ;; ;; alter endpoint to indicate that it has been drawn on the background
     [:db/add endpoint :endpoint/background -2]
     ]))
