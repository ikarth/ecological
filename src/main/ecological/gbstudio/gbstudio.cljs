(ns ecological.gbstudio.gbstudio
  ;; (:require-macros [cljs.core.async.macros :refer [go]])
  ;; (:require [cljs-http.client :as http]
  ;;           [cljs.core.async :refer [<!]])
  (:require [datascript.core :as d])
  )


(def gbs-basic (js->clj (.parse js/JSON "{ \"author\": \"https://github.com/ikarth/ecological\", \"name\": \"Generated Game Boy ROM\", \"_version\": \"2.0.0\", \"scenes\": [], \"backgrounds\": [], \"variables\": [], \"spriteSheets\": [], \"music\": [], \"customEvents\": [], \"palettes\": [], \"settings\": { \"showCollisions\": true, \"showConnections\": true, \"worldScrollX\": 0, \"worldScrollY\": 0, \"zoom\": 100, \"customColorsWhite\": \"E8F8E0\", \"customColorsLight\": \"B0F088\", \"customColorsDark\": \"509878\", \"customColorsBlack\": \"202850\", \"defaultBackgroundPaletteIds\": [ \"default-bg-1\", \"default-bg-2\", \"default-bg-3\", \"default-bg-4\", \"default-bg-5\", \"default-bg-6\" ], \"defaultSpritePaletteId\": \"default-sprite\", \"defaultUIPaletteId\": \"default-ui\", \"startX\": 0, \"startY\": 0, \"startDirection\": 0, \"startSceneId\": 0, \"playerSpriteSheetId\": \"581d34d0-9591-4e6e-a609-1d94f203b0cd\" } }" ) :keywordize-keys true))

(.parse js/JSON "{\"author\": \"test\", \"music\": []}")


(def genboy-schema {:conn-end {:db.cardinality :db.cardinality/many
                               :db.type :db.type/ref}})

(def db-conn (d/create-conn genboy-schema))

(defn reset-the-database! []
  (d/reset-conn! db-conn (d/empty-db genboy-schema)))




(defn create-gbs-entity
  "create a new GBS entity"
  []
  [{:db/id -1 :type "scene"}])

;; (defn create-entity [db entity]
;;   (db-with db (update entity :db/id -1)))

(d/q '[:find ?element ?id ?name ?width ?height ?image-width ?image-height ?filename
               :in $
               :where
               [?element :type :background]
               [?element :name ?name]
               [?element :id ?id]
               [?element :width ?width]
               [?element :height ?height]
               [?element :imageWidth ?image-width]
               [?element :imageHeight ?image-height]
               [?element :filename ?filename]
               ]
             @db-conn)

(defn find-in-db [type property value]
  (d/q '[:find ?element
         :in $ ?element-type ?element-prop ?element-value
         :where
         [?element :type ?element-type]
         [?element ?element-prop ?element-value]]
       @db-conn
       type
       property
       value))

(defn exists-in-db? [exist-type exist-name]
  (< 0
     (count
      (d/q '[:find ?element
             :in $ ?name ?type
             :where
             [?element :type ?type]
             [?element :name ?name]]
           @db-conn
           exist-name
           exist-type))))

;; (defn update-db-property [eid propery value]
;;   (d/db-with @db-conn [[":db/add" eid property value]])  
;;   )


(def move-this-move-is-never-used
  {:name "if you see this, it is an error"
   :query '[:find ?scene :in $ % :where [?scene :type :invalid-action-for-debugging]]
   :exec (fn [])})

(def move-this-move-is-always-used
  {:name "if you see this, it is true"
   :query '[:find ?scene :in $ % :where [?scene :type :scene]]
   :exec (fn [])})

(def move-place-greenfield-scene
  {:name "place-greenfield-scene"
   :exec (fn [_] ; takes parameters but ignores them
           [{:db/id -1
               :type :scene
               :id (str (random-uuid))             
               :backgroundId ""
               :x 20
               :y 20}])}) ; todo: query db for empty editor space to place scene?

(def move-create-background-from-image
  {:name "create-background-from-image"
   :query (fn [_]
            (not (exists-in-db? :background "autogen_logo.png")))
   :exec (fn [_ & {:keys [image-filename] :or {image-filename "autogen_logo.png"}}]
           (let [image-size [160 144] ;; todo: check actual size of image
                 tile-size 8  ;; gbstudio uses 8x8 tiles for its scene backgrounds
                 image-tiles (map #(/ % tile-size) image-size)]
             [{:db/id -1
               :type :background
               :name image-filename
               :id (str (random-uuid)) ; todo: use hash to speed comparisons?
               :width (first image-tiles)
               :height (second image-tiles)
               :imageWidth (first image-size)
               :imageHeight (second image-size)
               :filename image-filename}] ))})


(def move-add-existing-background-to-scene
  {:name "add-existing-background-to-scene"
   :comment "Adds an existing background (chosen at random) to a scene that doesn't have a background yet."
   :query '[:find ?scene ?background ?background_id ?bkg_width ?bkg_height
           :in $ %
           :where
            [?scene :type :scene]
            (not [?scene :db-background])
            [?scene :backgroundId ""]
           [?background :type :background]
           [?background :id ?background_id]
           [?background :width ?bkg_width]
           [?background :height ?bkg_height]]
   :exec
   (fn [_]
     ;; todo: I feel like "" isn't explicit enough for scenes without backgrounds yet...
     (let [scenes-sans-bkg (find-in-db :scene :backgroundId "")
           backgrounds (find-in-db :background :type :background)
           selected-background (first (rand-nth (seq backgrounds)))
           selected-scene-id (first (rand-nth (seq scenes-sans-bkg)))
           bkg-data (first (first (d/q '[:find (pull ?e [:id :width :height])
                                        :in $ ?e
                                        :where
                                        [?e :type :background]]
                                      @db-conn selected-background)))
           ;; todo: handle problem if there are multiple matches found for bkg-data...
           ]
       (cljs.pprint/pprint "XXX")       
       (cljs.pprint/pprint bkg-data)       
       [{:db/id selected-scene-id
         :backgroundId (get bkg-data :id "MISSING_BACKGROUND")
         :background selected-background
         :height (get bkg-data :height 0)
         :width  (get bkg-data :width 0)
         :test true
         }])
     )
   })

;; (def move-add-background-to-scene
;;   {:name "add-background-to-scene"
;;    :query '[:find ?scene ?background ?background_id ?bkg_width ?bkg_height
;;            :in $ %
;;            :where
;;             [?scene :type :scene]
;;             (not [?scene :background])
;;             [?scene :backgroundId ""]
;;            [?background :type :background]
;;            [?background :id ?background_id]
;;            [?background :width ?bkg_width]
;;            [?background :height ?bkg_height]]
;;    :exec
;;    ;; (fn [[scene background background-id width height]]
;;    ;;         [[scene :backgroundId background-id]
;;    ;;          [scene :width width]
;;    ;;          [scene :height height]]
;;    ;;         )
;;    (fn [_]
;;      (let [scenes-with-no-backgrounds (find-in-db :scene :backgroundId "")
;;            available-backgrounds (find-in-db :background :type :background)
;;            selected-background (if (< 0 (count available-backgrounds))
;;                                  (first (seq available-backgrounds))
;;                                  false)]
      
;;        (cljs.pprint/pprint "vvv")
;;        (cljs.pprint/pprint scenes-with-no-backgrounds)
;;        (if (and selected-background (< 0 (count scenes-with-no-backgrounds)))
;;          (let [selected-scene-id (first (rand-nth (seq scenes-with-no-backgrounds)))
;;                background-id 0
;;                background-uuid "X"
;;                ] ; todo: make determanistic
;;                   (cljs.pprint/pprint selected-scene-id)
;;            [{:db/id selected-scene-id
;;              :background background-id
;;              :backgroundId background-uuid}]
;;            )
;;          [])))})


(def move-place-possible-connection-point
  {:name "place-possible-connection-point"
   :query '[:find ?scene :in $ % :where [?scene :type :scene]]
   :exec (fn [_]
           [])})



(d/transact! db-conn ((:exec move-place-greenfield-scene) nil))

(def global-design-moves
  [move-place-greenfield-scene
   move-create-background-from-image
   move-add-existing-background-to-scene])

;(d/q '[:find ?o ?any :where [?o :type ?any]] @db-conn)

(defn export-backgrounds []
  (let [element-labels ["_datascript_internal_id" "id" "name" "width" "height" "imageWidth" "imageHeight" "filename"]
        elements
        (d/q '[:find ?element ?id ?name ?width ?height ?image-width ?image-height ?filename
               :in $
               :where
               [?element :type :background]
               [?element :name ?name]
               [?element :id ?id]
               [?element :width ?width]
               [?element :height ?height]
               [?element :imageWidth ?image-width]
               [?element :imageHeight ?image-height]
               [?element :filename ?filename]
               ]
             @db-conn)]
     (map #(zipmap element-labels %)
         elements)))

(defn export-scripts
  [scene-id]
  [])

(defn export-triggers
  "Finds and returns the scene triggers as exported Clojure data structures."
  [scene-id]
  (let [labels ["_datascript_internal_id"]
        scene-triggers
        (d/q '[:find ?trigger
               :in $ ?parent-scene
               :where
               [?trigger :type :trigger]
               [?trigger :parent-scene ?parent-scene]
               ]
             @db-conn
             scene-id)
        ]
    (map #(zipmap labels %)
         scene-triggers)))

(defn export-actors
  "Finds and returns the scene actors as exported Clojure data structures."
  [scene-id]
  (let [labels ["_datascript_internal_id"]
        scene-triggers
        (d/q '[:find ?element
               :in $ ?parent-scene
               :where
               [?element :type :actor]
               [?element :parent-scene ?parent-scene]
               ]
             @db-conn
             scene-id)
        ]
    (map #(zipmap labels %)
         scene-triggers)))

(defn export-scenes []
    (let [scene-labels
          ["_datascript_internal_id" "backgroundId" "name" "id" "width" "height" "x" "y" "collisions" "actors" "triggers" "script"]
          scenes
          (d/q '[:find ?scene ?backgroundId ?name ?uuid ?width ?height ?scene_x ?scene_y ?collisions
                 :in $ ?nameDefaultValue ?idDefaultValue ?collisionsDefaultValue
                 :where
                 [?scene :type :scene]
                 [?scene :backgroundId ?backgroundId] ;todo: backgrounds
                 [(get-else $ ?scene :width 10) ?width]
                 [(get-else $ ?scene :height 10) ?height]
                 [?scene :x ?scene_x]
                 [?scene :y ?scene_y]
                 [(get-else $ ?scene :name ?nameDefaultValue) ?name]
                 [(get-else $ ?scene :id ?idDefaultValue) ?uuid]
                 [(get-else $ ?scene :collisions ?collisionsDefaultValue) ?collisions]       
                 ] ; todo: actors and scripts/triggers
               @db-conn
                "generated scene"
               (random-uuid)
               [])]
      (map (fn [scene]
             (zipmap scene-labels (concat
                                   scene
                                   (export-actors (first scene))
                                   (export-triggers (first scene))
                                   (export-scripts (first scene))
                                   ))
             )
           scenes)
      ;(map #(zipmap scene-labels %) (concat scenes [(export-actors (first scenes)) (export-triggers (first scenes)) (export-scripts (first scenes))]) )
      ))

(defn export-gbs-project
  []
  (-> gbs-basic
      (update :backgrounds export-backgrounds)
      (update :scenes export-scenes)
      ))





;(defn load-project [])
;(defn render-project [])




(defn get-possible-design-move-from-moveset
  "Return a list of all possible design moves for the provides `db`,
  as selected from the provided `design-moves` collection."
  [design-moves]
  (filter
   (fn [mov]
     ;; (cljs.pprint/pprint mov)
     ;; (cljs.pprint/pprint "+++")
     (if-let [move-q (get mov :query false)]
       (if (fn? move-q)
         (move-q)
         (not (empty? (d/q move-q @db-conn nil))))
               true))
   design-moves))

(defn get-possible-design-moves
  "Return a list of all possible design moves for the provided `db`. 
   A _possible design move_ is an object with keys `move` and `vars`, 
   representing the abstract specification of this move type
   and a specific set of bindings for this move's logic variables respectively."
  []
  (get-possible-design-move-from-moveset global-design-moves))


(defn log-db
  "Log a complete listing of the entities in the provided `db` to the console."
  [db]
  (d/q '[:find ?any ?obj :where [?obj :type ?any]] @db)) ;todo: log to console...



;; todo: implement other generation heuristics.
(defn generate-level-random-heuristic
  "Generate a level in the provided `db` by performing a sequence of random
  design moves. The `budget` determines the number of design moves that can
  be performed. Takes a random `seed` for determanistic generation.
  TODO: determanistic generation is not implemented yet."
  [db budget seed]
  (dorun
   (for [i (range budget)]
     (let [moves (get-possible-design-move-from-moveset global-design-moves)]
       (cljs.pprint/pprint moves)
       (cljs.pprint/pprint (type moves))
       (cljs.pprint/pprint "---")
       (if (empty? moves)
         nil
         (let [poss-move (rand-nth moves)] ;; todo: make determanistic
           (cljs.pprint/pprint poss-move)
           (cljs.pprint/pprint (type poss-move))
           
           (if-let [exec-func (get poss-move :exec false)]
             (d/transact! db (exec-func db) nil) ; todo: do something with the transaction report, such as checking for errors
             nil))
           ))))) ;; todo: log which transaction was executed

(defn generate []
  (reset-the-database!)
  (generate-level-random-heuristic db-conn 8 0))

(reset-the-database!)
(generate)
(generate-level-random-heuristic db-conn 5 0)
(log-db db-conn)


(defn fetch-gbs []
  (generate)
  ;(clj->js (map clj->js (export-gbs-project)))
  (export-gbs-project))
