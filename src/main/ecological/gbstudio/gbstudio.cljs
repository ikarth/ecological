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

(defn reset-the-database []
  (d/reset-conn! db-conn (d/empty-db genboy-schema)))




(defn create-gbs-entity
  "create a new GBS entity"
  []
  [{:db/id -1 :type "scene"}])

;; (defn create-entity [db entity]
;;   (db-with db (update entity :db/id -1)))

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
             :id (random-uuid)
             :backgroundId ""
             :x 20
             :y 20}])}) ; todo: query db for empty editor space to place scene?

(def move-place-possible-connection-point
  {:name "place-possible-connection-point"
   :query '[:find ?scene :in $ % :where [?scene :type :scene]]
   :exec (fn [_]
           [])})



(d/transact! db-conn ((:exec move-place-greenfield-scene) nil))

(def global-design-moves [move-place-greenfield-scene
                   ])

;(d/q '[:find ?o ?any :where [?o :type ?any]] @db-conn)

(defn export-backgrounds []
  {})

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
      (map #(zipmap scene-labels %) (concat scenes [(export-actors (first scenes)) (export-triggers (first scenes)) (export-scripts (first scenes))]) )))

(defn export-gbs-project
  []
  (-> gbs-basic
      (update :backgrounds export-backgrounds)
      (update :scenes export-scenes)
      ))


(defn fetch-gbs []
  (clj->js (map clj->js (export-gbs-project)))
  (export-gbs-project))


;(defn load-project [])
;(defn render-project [])




(defn get-possible-design-move-from-moveset
  "Return a list of all possible design moves for the provides `db`,
  as selected from the provided `design-moves` collection."
  [db design-moves]
  (filter
   (fn [mov] (if-let [move-q (get mov :query )]
               (not (empty? (d/q move-q @db-conn nil)))
               true))
   design-moves))

(defn get-possible-design-moves
  "Return a list of all possible design moves for the provided `db`. 
   A _possible design move_ is an object with keys `move` and `vars`, 
   representing the abstract specification of this move type
   and a specific set of bindings for this move's logic variables respectively."
  [db]
  (get-possible-design-move-from-moveset db global-design-moves))


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
     (let [moves (get-possible-design-move-from-moveset db global-design-moves)]
       (if (empty? moves)
         nil
         (let [poss-move (rand-nth moves)] ;; todo: make determanistic
           (if-let [exec-func (get poss-move :exec false)]
             (d/transact! db (exec-func db) nil) ; todo: do something with the transaction report, such as checking for errors
             nil)))))))

(reset-the-database)
(generate-level-random-heuristic db-conn 3 0)
;; (log-db db-conn)

