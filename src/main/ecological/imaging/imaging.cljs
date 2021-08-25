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
    (qc/create-image w h)))

(defn op-create-random-noise [w h]
  (let [graphics (qc/create-graphics w h)]
    (qc/with-graphics graphics
      (qc/background 127 127 255)
      (qc/ellipse 50 50 50 50))
    graphics))

(defn op-create-perlin-image [w h]
  ;; (let [graphics (qc/create-graphics w h)]
  ;;   (qc/with-graphics graphics
  ;;     (qc/background 127 127 255)
  ;;     (qc/ellipse 50 50 50 50))
  ;;   graphics)
  (qc/with-sketch (qc/get-sketch-by-id "quil-canvas")
    (let [image-target (qc/create-image w h)
          ;;image-pixels (qc/pixels image-target)
          ]
      (dotimes [x w]
        (dotimes [y h]
          (qc/set-pixel image-target x y (qc/color (rem x 255) (rem y 255) 127))))
      (qc/update-pixels image-target)
      image-target)))

(defn process-operations
  "Apply the list of operations to the given input and return the result"
  [input operation-list]
  input)

;;;;;;;;;;;;;;;;;;;;;;;;
;; Design moves



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
         :raster/matrix blank-image
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
         :raster/matrix perlin-image
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
   ;move-generate-blank-raster-image
   move-generate-perlin-noise-raster
   ])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def imaging-schema
  {:raster/matrix    {:db/cardinality :db.cardinality/one}
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
                        "image-data"]
        elements
        (d/q '[:find ?element ?uuid ?size ?raster
               :in $
               :where
               [?element :raster/uuid ?uuid]
               [?element :raster/size ?size]
               [?element :raster/matrix ?raster]]
             @db-conn)]
    (map #(zipmap element-labels %)
         elements)))

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
        result (exec-func db (:vars design-move))
        history-record [{:db/id -999999 ; magic number to try and be unique...this will break if more than 1,000,000 changes are in the transaction. Which is unlikely.
                         ;:design/move-count current-design-move-count ; todo: count the actual number of moves that have been made by looking up the last one, instead of just using the loop counter
                         :design/move-record move-name
                         ;:design/move-parameters (get design-move :vars [])
                         }
                        ]
        tx-data (into [] (concat result history-record))]
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
