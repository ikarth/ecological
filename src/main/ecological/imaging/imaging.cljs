(ns ecological.imaging.imaging
  (:require [datascript.core :as d]
            [clojure.string]
            [goog.crypt :as crypt]
            [quil.core :as qc]
            [quil.middleware :as qm]
            ;[quil.applet :as qa]
            ))

(def default-image-size [256 256])

;;;;;;;;;;;;;;;;;;;;;;;
;; Image operations


;(def graphics-sketchboard (qc/create-graphics 256 256))

(defn op-create-image [w h]
  (qc/with-sketch (qc/get-sketch-by-id "quil-canvas")
    (let [image-target (qc/create-image w h)]
      (dotimes [x w]
        (dotimes [y h]
          (qc/set-pixel image-target x y (qc/color 0 0 0))))
      (qc/update-pixels image-target)
      image-target)))

(defn op-create-test-graphics [w h]
  (let [graphics (qc/create-graphics w h)]
    (qc/with-graphics graphics
      (qc/background 127 127 255)
      (qc/ellipse 50 50 50 50))
    graphics))

(defn op-create-perlin-image [w h & {:keys [noise-offset noise-scale noise-octaves noise-falloff] :or {noise-offset [0 0] noise-scale [0.05 0.05] noise-octaves 4 noise-falloff 0.5}}]
  (qc/with-sketch (qc/get-sketch-by-id "quil-canvas")
    (let [image-target (qc/create-image w h)
          ;; noise-offset [0 0 0]
          ;; noise-scale [0.05 0.05 0.05]
          ;; noise-octaves 4
          ;; noise-falloff 0.5
          ]
      (qc/noise-detail noise-octaves noise-falloff)
      (dotimes [x w]
        (dotimes [y h]
          (let [intensity (* 255 (qc/noise (+ (first noise-offset) (* x (first noise-scale)))
                                           (+ (second noise-offset)(* y (second noise-scale)))))]
            (qc/set-pixel image-target x y (qc/color intensity intensity intensity))
            )))
      (qc/update-pixels image-target)
      image-target)))

(defn op-create-random-noise [w h]
  (qc/with-sketch (qc/get-sketch-by-id "quil-canvas")
    (let [image-target (qc/create-image w h)]
      (dotimes [x w]
        (dotimes [y h]
          (let [intensity (qc/random 1.0)]
            (qc/set-pixel image-target x y (qc/color (* 255 intensity) (* 255 intensity) (* 255 intensity))))))
      (qc/update-pixels image-target)
      image-target)))

(defn op-create-uv-pattern [w h]
  (qc/with-sketch (qc/get-sketch-by-id "quil-canvas")
    (let [image-target (qc/create-image w h)]
      (dotimes [x w]
        (dotimes [y h]
          (let [u (/ x w)
                v (/ y h)
                ]
            (qc/set-pixel image-target x y (qc/color (* 255 u) (* 255 v) 0)))))
      (qc/update-pixels image-target)
      image-target)))

(defn op-create-test-pattern [w h]
  (qc/with-sketch (qc/get-sketch-by-id "quil-canvas")
    (let [image-target (qc/create-image w h)]
      (dotimes [x w]
        (dotimes [y h]
          (qc/set-pixel image-target x y (qc/color (rem x 256) (rem y 256) 127))))
      (qc/update-pixels image-target)
      image-target)))

(defn op-image-filter [image & {:keys [filter-mode filter-level] :or {filter-mode :threshold filter-level 0.5}}]
  (qc/with-sketch (qc/get-sketch-by-id "quil-canvas")
    (qc/image-filter image filter-mode filter-level))
  image)

(defn op-image-blend [image-src image-dest & {:keys [filter-mode size] :or {filter-mode :blend size default-image-size}}]
  (js/console.log size)
  (qc/with-sketch (qc/get-sketch-by-id "quil-canvas")
    (let [image-final (qc/create-image (first size) (second size))] ;[graphics-obj (qc/create-graphics (first size) (second size) :p2d)]
      (qc/copy image-src image-final [0 0 (first size) (second size)] [0 0 (first size) (second size)])
      (qc/blend image-dest image-final 0 0 (first size) (second size) 0 0 (first size) (second size) filter-mode)
      image-final)))

(defn process-operations
  "Apply the list of operations to the given input and return the result"
  [input operation-list]
  ;; TODO
  input)

;;;;;;;;;;;;;;;;;;;;;;;;
;; Design moves

(defn timestamp []
  (js/parseInt (.now js/Date)))

(def move-generate-blank-raster-image
  {:name "generate-blank-raster-image"
   :comment "Create a raster image grid, initialized to 0"
   ;;:query nil
   :exec
   (fn [db & {:keys [size]}]
     (let [_ (assert (or (nil? size) (and (vector? size) (= (count size) 2)))
                     (str ":size should be nil or a vector of size 2, but instead it is " size))
           [w h] (if (nil? size) default-image-size size)
           blank-image (op-create-image w h)]       
       [{:db/id -1
         :raster/image blank-image
         :raster/size [w h]
         :raster/uuid (str (random-uuid))
         
         }]))})

(def move-generate-perlin-noise-raster
  {:name "generate-perlin-noise-raster"
   :comment "Create a raster grid of Perlin noise of the given size."
   ;;:query nil
   :exec
   (fn [db & {:keys [size]}]
     (let [_ (assert (or (nil? size) (and (vector? size) (= (count size) 2)))
                     (str ":size should be nil or a vector of size 2, but instead it is " size))
           [w h] (if (nil? size) default-image-size size)
           perlin-image (op-create-perlin-image w h)]       
       [{:db/id -1
         :raster/image perlin-image
         :raster/size [w h]
         :raster/uuid (str (random-uuid))
         
         }]))})

(def move-generate-random-noise-raster
  {:name "generate-random-noise-raster"
   :comment "Create a raster grid filled with 0 to 255 based on UV -> RG coordinates."
   ;;:query nil
   :exec
   (fn [db & {:keys [size]}]
     (let [_ (assert (or (nil? size) (and (vector? size) (= (count size) 2)))
                     (str ":size should be nil or a vector of size 2, but instead it is " size))
           [w h] (if (nil? size) default-image-size size)
           perlin-image (op-create-random-noise w h)]       
       [{:db/id -1
         :raster/image perlin-image
         :raster/size [w h]
         :raster/uuid (str (random-uuid))
         
         }]))})

(def move-generate-uv-pattern-raster
  {:name "generate-uv-pattern-raster"
   :comment "Create a raster grid filled with 0 to 255 based on UV -> RG coordinates."
   ;;:query nil
   :exec
   (fn [db & {:keys [size]}]
     (let [_ (assert (or (nil? size) (and (vector? size) (= (count size) 2)))
                     (str ":size should be nil or a vector of size 2, but instead it is " size))
           [w h] (if (nil? size) default-image-size size)
           perlin-image (op-create-uv-pattern w h)]       
       [{:db/id -1
         :raster/image perlin-image
         :raster/size [w h]
         :raster/uuid (str (random-uuid))
         
         }]))})

(def move-generate-test-pattern-raster
  {:name "generate-test-pattern-raster"
   :comment "Create a raster grid filled with a repeating text pattern of the given size."
   ;;:query nil
   :exec
   (fn [db & {:keys [size]}]
     (let [_ (assert (or (nil? size) (and (vector? size) (= (count size) 2)))
                     (str ":size should be nil or a vector of size 2, but instead it is " size))
           [w h] (if (nil? size) default-image-size size)
           perlin-image (op-create-test-pattern w h)]       
       [{:db/id -1
         :raster/image perlin-image
         :raster/size [w h]
         :raster/uuid (str (random-uuid))
         
         }]))})

;image & {:keys [filter-mode filter-level] :or {filter-mode :threshold filter-level 0.5}}
(def move-filter-threshold
  {:name "move-filter-threshold"
   :comment "Converts an image to black and white pixels based on if they are above or below a threshold value."
   :query
   '[:find ?image-image ?image-size
     :in $ %
     :where
     [?element :raster/image ?image-image]
     [?element :raster/size   ?image-size]
     ]
   :exec
   (fn [db [image-data size]]
     (let [_ (assert (or (nil? size) (and (vector? size) (= (count size) 2)))
                     (str ":size should be nil or a vector of size 2, but instead it is " size))
           [w h] (if (nil? size) default-image-size size)
           result-image (op-image-filter image-data :filter-mode :threshold :filter-level 0.5)]       
       [{:db/id -1
         :raster/image result-image
         :raster/size [w h]
         :raster/uuid (str (random-uuid))
         
         }]))})

(def move-blend-blend
  {:name "move-blend-blend"
   :comment "Converts an image to black and white pixels based on if they are above or below a threshold value."
   :query
   '[:find ?image-src ?image-src-size ?image-dest ?image-dest-size
     :in $ %
     :where
     [?e1 :raster/image ?image-src]
     [?e1 :raster/size  ?image-src-size]
     [?e2 :raster/image ?image-dest]
     [?e2 :raster/size  ?image-dest-size]
     [(not= ?e1 ?e2)]
     ]
   :exec
   (fn [db [image-src src-size image-dest size]]
     (let [_ (assert (or (nil? size) (and (vector? size) (= (count size) 2)))
                     (str ":size should be nil or a vector of size 2, but instead it is " size))
           [w h] (if (nil? size) default-image-size size)
           result-image (op-image-blend image-src image-dest :size size :filter-mode :overlay)]       
       [{:db/id -1
         :raster/image result-image
         :raster/size [w h]
         :raster/uuid (str (random-uuid))
         
         }]))})

(def move-generate-blank-image-function
  {:name "generate-blank-image-function"
   :comment "Create a function that returns a blank image of the given size."
   ;:query nil
   :exec
   (fn [db & {:keys [size]}]
     (let [_ (assert (or (nil? size) (and (vector? size) (= (count size) 2)))
                     (str ":size should be nil or a vector of size 2, but instead it is " size))
           [w h] (if (nil? size) default-image-size size)]       
       [{:db/id -1
         :func/func-handle :op-create-image
         :func/parameters [w h :rgb]
         :func/uuid (str (random-uuid))
         
         }]))})




(def image-design-moves
  [;move-generate-blank-image-function
   move-generate-blank-raster-image
   move-generate-perlin-noise-raster
   move-generate-uv-pattern-raster
   move-generate-test-pattern-raster
   move-generate-random-noise-raster
   move-filter-threshold
   move-blend-blend
   ])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def imaging-schema
  {:raster/image    {:db/cardinality :db.cardinality/one}
   :raster/size      {:db/cardinality :db.cardinality/one}
   :raster/uuid      {:db/cardinality :db.cardinality/one}
   :func/func-handle {:db/cardinality :db.cardinality/one}
   :func/parameters  {:db/cardinality :db.cardinality/one}
   :func/uuid        {:db/cardinality :db.cardinality/one}
   })

(def db-conn (d/create-conn imaging-schema))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn export-image
  "Given an image in the database, export it in a way that can be shown in browser or saved to disk."
  []
  ;; TODO
  )

(defn export-result-image
  []
  ;; TODO
  []
  )

(defn export-all-images []
  (let [element-labels ["_datascript_internal_id"
                        "uuid"
                        "size"
                        "image-data"
                        "timestamp"]
        elements
        (d/q '[:find ?element ?uuid ?size ?raster ?timestamp
               :in $
               :where
               [?element :raster/uuid ?uuid]
               [?element :raster/size ?size]
               [?element :raster/image ?raster]
               [?element :entity/timestamp ?timestamp]
               ]
             @db-conn)]
    (map #(zipmap element-labels %)
         elements)))

(defn export-most-recent-image []
  (let [element-labels ["_datascript_internal_id"
                        "uuid"
                        "size"
                        "image-data"
                        "timestamp"]
        elements
        (d/q '[:find ?element ?uuid ?size ?raster (max ?timestamp) ;; this is only supposed to return one result but it is returning all instead of max, suggesting that my understanding of this is flawed
               :in $
               :where
               [?element :raster/uuid ?uuid]
               [?element :raster/size ?size]
               [?element :raster/image ?raster]
               [?element :entity/timestamp ?timestamp]
               ]
             @db-conn)]
    (map #(zipmap element-labels %)
         elements))) 



(defn export-most-recent-artifact []
  (let [all-images (export-all-images)
        ;most-recent (export-most-recent-image)
        images-sorted (sort-by #(get-in % ["timestamp"]) > all-images)]
    ;(js/console.log most-recent)
    ;(js/console.log images-sorted)
    ;(assert (= (first most-recent) (first images-sorted)) (str "Mismatch between " (first most-recent) " and " (first images-sorted)))
    ;(first most-recent)
    (first images-sorted)))

(defn export-output
  "Export the result after applying the design moves."
  []
  (-> {}
      (update :raster export-all-images)
      (update :output-image export-result-image)))



(defn get-possible-design-move-from-moveset
  "Return a list of all possible design moves for the provides `db`,
  as selected from the provided `design-moves` collection."
  [design-moves]
  (apply concat
   (map
    (fn [mov]
      (let [q-map
            (if-let [move-query (get mov :query false)]
              ;; (if (fn? move-query) (move-query @db-conn)) ;; todo: properly handle functions for queries
              (let [query-result (d/q move-query @db-conn nil)] ;; todo: pass additional context to query
                ;; todo: check if result is a promise
                (map (fn [v] {:move mov :vars v}) query-result))
              {:move mov :vars nil})]
        (if (map? q-map) [q-map] q-map)))
    design-moves)))

(defn get-possible-design-moves
  "Return a list of all possible design moves for the provided `db`. 
   A _possible design move_ is an object with keys `move` and `vars`, 
   representing the abstract specification of this move type
   and a specific set of bindings for this move's logic variables respectively."
  []
  (let [m (get-possible-design-move-from-moveset image-design-moves)]
    ;;(println m)
    m))

(defn log-db
  "Log a complete listing of the entities in the provided `db` to the console."
  [db]
  (d/q '[:find ?any ?obj :where [?obj :type ?any]] @db)) ;todo: log to console...


(defn assemble-exec-result [db design-move]
  (let [_ (assert (map? design-move) "Need a design move before we can execute the design move function.")
        move-name (get-in design-move [:move :name])
        _ (assert (string? move-name) (str "Design move (" move-name ") not found in " design-move "."))
        exec-func (get-in design-move [:move :exec])
        _ (assert (fn? exec-func) (str move-name " has no :exec function!"))
        current-time (timestamp)
        result (exec-func db (:vars design-move))
        province-added (map (fn [transact]
                              (merge transact {:entity/timestamp current-time
                                                :province/cause move-name
                                                ;:province/bindings (:vars design-move)
                                                })
                              )
                            result)
        history-record [{:db/id -999999 ; magic number to try and be unique...this will break if more than 1,000,000 changes are in the transaction. Which is unlikely.
                         ;:design/move-count current-design-move-count ; todo: count the actual number of moves that have been made by looking up the last one, instead of just using the loop counter
                         :design/move-record move-name
                                        ;:design/move-parameters (get design-move :vars [])
                         :design/timestamp current-time
                         }
                        ]
        tx-data (into [] (concat province-added history-record))]
    ;;(println tx-data)
    tx-data))

(defn execute-design-move! [design-move]
  (assert (map? design-move) "Design move is missing, so can't be executed.")
  ;(println db-conn)
  ;(println design-move)
  (d/transact! db-conn (assemble-exec-result @db-conn design-move)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn reset-the-database!
   "For when you want to start over. Returns an empty database that follows the schema."
  []
  (d/reset-conn! db-conn (d/empty-db imaging-schema))
  ;; if there is an initial transaction, it goes here...
  )

(defn make-empty-project []
  ;(println "Make Empty Project: Imaging")
  (reset-the-database!))


(defn fetch-data-output
  "Return the current state of the generated results"
  []
  (export-all-images))

(defn fetch-database [])

(defn fetch-possible-moves []
  (let [moves (get-possible-design-move-from-moveset image-design-moves)]
    ;(js/console.log moves)
    moves))

(defn fetch-some-moves []
  (let [moves (get-possible-design-moves)]
    moves))

(defn fetch-all-moves
  "Return all of the moves we know about, valid or otherwise"
  []
  (let [moves image-design-moves]
    moves))


(defn fetch-generated-project!
  "Generate a new project and return it"
  []
  )

(defn fetch-data-view []
  (vec (map (fn [dat]
              (let [[e a v tx add] dat]
                [e a v tx add])) (d/datoms @db-conn :eavt))))

(defn fetch-most-recent-artifact
  "Returns the most recently-created artifact."
  []
  (export-most-recent-artifact))
