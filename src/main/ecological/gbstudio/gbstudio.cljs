(ns ecological.gbstudio.gbstudio
  ;; (:require-macros [cljs.core.async.macros :refer [go]])
  ;; (:require [cljs-http.client :as http]
  ;;           [cljs.core.async :refer [<!]])
  ;;(:require-macros [cljs.core.async.macros :refer [go]])
  (:require [datascript.core :as d]
            ;[cljs.reader :as reader]
            ;;[cljs-http.client :as http]
            ;;[cljs.core.async :refer [<!]]
            [ecological.gbstudio.assets :refer [asset-manifest scene-example-projects]]
            [clojure.string]
            )
  )


(def gbs-basic (js->clj (.parse js/JSON "{ \"author\": \"https://github.com/ikarth/ecological\", \"name\": \"Generated Game Boy ROM\", \"_version\": \"2.0.0\", \"scenes\": [], \"backgrounds\": [], \"variables\": [], \"spriteSheets\": [], \"music\": [], \"customEvents\": [], \"palettes\": [], \"settings\": { \"showCollisions\": true, \"showConnections\": true, \"worldScrollX\": 0, \"worldScrollY\": 0, \"zoom\": 100, \"customColorsWhite\": \"E8F8E0\", \"customColorsLight\": \"B0F088\", \"customColorsDark\": \"509878\", \"customColorsBlack\": \"202850\", \"defaultBackgroundPaletteIds\": [ \"default-bg-1\", \"default-bg-2\", \"default-bg-3\", \"default-bg-4\", \"default-bg-5\", \"default-bg-6\" ], \"defaultSpritePaletteId\": \"default-sprite\", \"defaultUIPaletteId\": \"default-ui\", \"startX\": 0, \"startY\": 0, \"startDirection\": 0, \"startSceneId\": 0, \"playerSpriteSheetId\": \"581d34d0-9591-4e6e-a609-1d94f203b0cd\" } }" ) :keywordize-keys true))

;(.parse js/JSON "{\"author\": \"test\", \"music\": []}")

(def genboy-schema
  {:signal/signal             {:db/cardinality :db.cardinality/one}                 ; :db/valueType :db.type/keyword
   :scene/background          {:db/cardinality :db.cardinality/one  :db/valueType :db.type/ref}                               
   :scene/name                {:db/cardinality :db.cardinality/one}                 ; :db/valueType :db.type/string
   :scene/uuid                {:db/cardinality :db.cardinality/one  :db/unique :db.unique/identity}
   :scene/backgroundUUID      {:db/cardinality :db.cardinality/one}                 ; :db/valueType :db.type/string
   :scene/collisions          {:db/cardinality :db.cardinality/one}                 ; :db/valueType :db.type/tuple
   :scene/editor-position     {:db/cardinality :db.cardinality/one}                 ; :db/valueType :db.type/tuple
   :background/filename       {:db/cardinality :db.cardinality/one}                 ; :db/valueType :db.type/string
   :background/image          {:db/cardinality :db.cardinality/one  :db/valueType :db.type/ref}
   :background/size           {:db/cardinality :db.cardinality/one}                 ; :db/valueType :db.type/tuple
   :background/imageSize      {:db/cardinality :db.cardinality/one}                 ; :db/valueType :db.type/tuple
   :background/uuid           {:db/cardinality :db.cardinality/one  :db/unique :db.unique/identity}
   :resource/filename         {:db/cardinality :db.cardinality/one  :db/unique :db.unique/identity}
   :resource/type             {:db/cardinality :db.cardinality/one}                 ; :db/valueType :db.type/keyword
   })

(def db-conn (d/create-conn genboy-schema))

(defn reset-the-database! []
  (d/reset-conn! db-conn (d/empty-db genboy-schema))
  (d/transact! db-conn [{:db/id -1
                         :signal/signal :resources-not-loaded}
                        {:db/id -2
                         :signal/signal :gbs-examples-not-loaded}]))


(defn create-gbs-entity
  "create a new GBS entity"
  []
  [{:db/id -1 :type "scene"}])


;; (def move-viz-display-background-image
;;   {:name "viz-display-background-image"
;;    :query
;;    '[:find ?bkg-id ?fname
;;      :in $ %
;;      :where
;;      [?bkg-id :background/filename ?fname]
;;      [(missing? $ ?bkg-id :background/viz-image)]]
;;    :exec
;;    (fn [db [bkg-id fname]]
;;      [{:db/id bkg-id
;;        :background/filename fname
;;        :background/viz-image (clojure.string/join "./data/assets/backgrounds/" fname)
;;        }])})

(def move-load-resources-from-disk
  {:name "load-resources-from-disk"
   :query
   '[:find ?sig 
     :in $ %
     :where
     [?sig :signal/signal :resources-not-loaded]
     ]
   :exec
   (fn [db [signal-resources-not-loaded]]
     ;; TODO: actually get data from server
     (let [category-table {"ui" :ui "image" :image "sprites" :sprites "backgrounds" :image}
           manifest-transaction
           (concat
            [[:db/retractEntity signal-resources-not-loaded]]
            [{:db/id -1 :signal/signal :resources-are-loaded}]
            (mapv
             (fn [key asset]
               {:db/id key
                :resource/type (category-table (asset :category :none) :none)
                :resource/filename (asset :file :none)
                :resource/filepath (asset :path :none)
                ;:resource/size (asset :size [0 0])
                :resource/image-size (asset :image-size [0 0])})
             (iterate dec -2)
             (asset-manifest)))]
       (js/console.log manifest-transaction)
       manifest-transaction
       ))})

(def move-load-gbs-projects-from-disk
  {:name "load-gbs-projects-from-disk"
   :query '[:find ?sig :in $ %
            :where
            [?sig :signal/signal :gbs-examples-not-loaded]
            [?sig2 :signal/signal :resources-are-loaded]
            ;[(missing? $ ?sig2 :signal/signal :resources-not-loaded)]
            ]
   :exec
   (fn [db [signal-not-loaded]]
     (let [sep (first (scene-example-projects))
           transaction
           (concat
            [[:db/retractEntity signal-not-loaded]]
            (mapv (fn [key asset]
                    {:db/id key
                     :template/name (get asset :name "unnamed")
                     :template/triggers (get asset :triggers []) ; todo: translate triggers
                     :template/actors (get asset :actors []) ; todo: translate actors
                     :template/backgroundUUID (get asset :backgroundId "")
                     :template/collisions (get asset :collisions [])
                     :template/use-count 0
                     })
                  (iterate dec -3)
                  (get sep :scenes []))
            (mapv (fn [key asset]
                    {:db/id key
                     :gbs-input/type :background
                     :gbs-input/uuid (get asset :id "")
                     :gbs-input/name (get asset :name "")
                     :gbs-input/filename (get asset :filename "")
                     :gbs-input/width  (get asset :width 0)
                     :gbs-input/height (get asset :hieght 0)
                     })
                  (iterate dec (- 0 (+ 3 (count (get sep :scenes [])))))
                  (get sep :backgrounds []))
            )]
       (js/console.log transaction)
       transaction))})

(defn process-template-actors [actors]
  actors)

(defn process-template-triggers [triggers]
  triggers)

(def move-generate-scene-from-template
  {:name "generate-scene-from-template"
   :query
   '[:find ?template-id ?name ?bkg-id ?use-count ?collisions ?triggers ?actors
     :in $ %
     :where
     [?template-id :template/name ?name]
     [?template-id :template/backgroundUUID ?bkg-uuid]
     [?template-id :template/collisions ?collisions]     
     [?template-id :template/triggers ?triggers]
     [?template-id :template/actors ?actors]
     [?gbs-bkg-id :gbs-input/uuid ?bkg-uuid]
     [?gbs-bkg-id :gbs-input/filename ?bkg-filename]
     [?bkg-id :background/filename ?bkg-filename]
     [?template-id :template/use-count ?use-count]
     ]
   :exec
   (fn [db [template-id template-name bkg-id use-count collisions triggers actors
            ]]
     (let [transaction
           [{:db/id -1
             :scene/uuid (str (random-uuid))             
             :scene/editor-position [20 20]
             :scene/background bkg-id
             :scene/collisions collisions
             :scene/name (str template-name "_" (str (random-uuid)))
             :scene/triggers (process-template-triggers triggers)
             :scene/actors   (process-template-actors actors)
             }
            {:db/id template-id
             :template/use-count (inc use-count)}
            ]]
       ;(js/console.log template-id)
       ;(js/console.log transaction)
       transaction
       ))})

(def move-place-greenfield-scene
  {:name "place-greenfield-scene"
   :exec (fn [_] ; takes parameters but ignores them
           [{:db/id -1
             :scene/uuid (str (random-uuid))             
             :scene/editor-position [20 20]
             :scene/name "generated greenfield scene"
               }])}) ; todo: query db for empty editor space to place scene?

;; TODO: populate the database with existing image files
(def move-create-background-from-image
  {:name "create-background-from-image"
   :query
   '[:find ?image-filename ?r ?image-size
     :in $ %
     :where
     [?r :resource/type :image]
     [?r :resource/filename ?image-filename]
     [?r :resource/image-size ?image-size]     
     (not-join [?image-filename]
               [?e :background/filename ?image-filename])]
   :exec (fn [db [image-filename resource-id gb-image-size]]
           (let [image-size gb-image-size ;; todo: check actual size of image
                 tile-size 8  ;; gbstudio uses 8x8 tiles for its scene backgrounds
                 image-tiles (map #(quot % tile-size) image-size)]
             [{:db/id -1
               :background/uuid (str (random-uuid)) ; todo: use hash to speed comparisons?
               :background/size image-tiles
               :background/imageSize image-size
               :background/filename image-filename
               :background/resource resource-id}] ))})


(def move-add-existing-background-to-scene
  {:name "add-existing-background-to-scene"
   :comment "Adds an existing background (chosen at random) to a scene that doesn't have a background yet."
   :query  '[:find ?scene ?e ?background-id ?bkg-size
             :in $ %
             :where
             [?scene :scene/uuid ?uuid]
             [(missing? $ ?scene :scene/background)]
             [?e :background/uuid ?background-id]
             [?e :background/size ?bkg-size]
             ]
   :exec
   (fn [db [scene bkg bkg-uuid bkg-size]]
     [{:db/id scene
       :scene/background bkg
       }])})

(def move-make-template-from-gbs
  {:name "make-template-from-gbs"
   :query '[:find ?]
   :exec
   (fn [db]
     []
     )})

(def move-apply-collision-to-scene
  {})

(def move-selection-collision-by-background
  {})

(def move-mixin-npc
  {})

(def move-connect-scenes
  {})

(def move-connect-scenes-by-region
  {})

(def move-connect-scenes-by-edge-tag
  {})


;; Design moves to write:
;; - Grab a GBS project from a file
;; - Convert a GBS project to a list of proto-scene-templates
;; - Convert a proto-template to a scene-template
;; - Load assets from an asset directory (backgrounds and sprites)
;; - Get collision data associated with scene/background
;; - Detect connection points
;; - Edit background graphics to visually show where there's a connection
;; - compsite one image on top of another image (with transparency)
;; - scrunch image with seam carving
;; - detect the number of unique tiles in an image
;; - place NPC in scene
;; - generate an NPC
;; - generate a connection lock
;; - generate a key for a lock
;; - pair templates together so you have to either have both or neither
;; - pathfinding to make sure level is completeable (entrance->key->door->victory)
;; - pathfinding to make sure door is seen before key (problem/solution ordering)
;; - graph grammar/rewriting to generate level layout
;; - decorate a plain scene background with WFC



;; (def move-place-possible-connection-point
;;   {:name "place-possible-connection-point"
;;    :query '[:find ?scene :in $ :where [?scene :type :scene]]
;;    :exec (fn [_]
;;            [])})

(def global-design-moves
  [
   move-load-resources-from-disk
   move-create-background-from-image
   move-load-gbs-projects-from-disk
   move-generate-scene-from-template
   ;move-place-greenfield-scene
   ;move-add-existing-background-to-scene
   ])

(def design-moves-finalizing
  [{:name "resolve-scene-backgrounds"}])


(defn export-design-moves []
  (let [elements
        (d/q '[:find ?move-count ?move-name ?move-parameters
               :in $
               :where
               [?element :design/move-record ?move-name]
               [?element :design/move-parameters ?move-parameters]
               [?element :design/move-count  ?move-count]]
             @db-conn )]
    ; we could return all of the parameters...
    (mapv #(zipmap [:order :name :parameters] %) (sort-by first elements))
    ; ...but for now just return the names
    (mapv second (sort-by first elements)) 
    ))

(defn export-backgrounds []
  (let [element-labels ["_datascript_internal_id" "id" "size" "filename" "image"]
        elements
        (d/q '[:find ?element ?uuid ?size ?filename ?path
               :in $
               :where
               [?element :background/uuid ?uuid]
               [?element :background/size ?size]
               [?element :background/filename ?filename]
               [?element :background/resource ?resource]
               [?resource :resource/filepath ?path]
               ]
             @db-conn)]
     (map #(zipmap element-labels %)
         elements)))

;; (defn export-scripts
;;   [scene-id]
;;   [])

;; (defn export-triggers
;;   "Finds and returns the scene triggers as exported Clojure data structures."
;;   [scene-id]
;;   (let [labels ["_datascript_internal_id"]
;;         scene-triggers
;;         (d/q '[:find ?trigger
;;                :in $ ?parent-scene
;;                :where
;;                [?trigger :type :trigger]
;;                [?trigger :parent-scene ?parent-scene]
;;                ]
;;              @db-conn
;;              scene-id)
;;         ]
;;     (map #(zipmap labels %)
;;          scene-triggers)))

;; (defn export-actors
;;   "Finds and returns the scene actors as exported Clojure data structures."
;;   [scene-id]
;;   (let [labels ["_datascript_internal_id"]
;;         scene-triggers
;;         (d/q '[:find ?element
;;                :in $ ?parent-scene
;;                :where
;;                [?element :type :actor]
;;                [?element :parent-scene ?parent-scene]
;;                ]
;;              @db-conn
;;              scene-id)
;;         ]
;;     (map #(zipmap labels %)
;;          scene-triggers)))

(defn export-scenes
  "Export all of the scenes from the database and return EDN that can eventually be interperted by GBS."
  []
    (let [scene-labels
          ["_datascript_internal_id" "backgroundId" "editor-position" "name" "id" "collisions" "background-image"]
          scenes
          (d/q '[:find ?scene ?backgroundUUID ?editor-position ?name ?uuid ?collisions ?bkg-resource-path
                 :in $ ?nameDefaultValue ?idDefaultValue ?collisionsDefaultValue
                 :where
                 [?scene :scene/editor-position ?editor-position]
                 [(get-else $ ?scene :scene/name ?nameDefaultValue) ?name]
                 [(get-else $ ?scene :scene/uuid ?idDefaultValue) ?uuid]
                 [(get-else $ ?scene :scene/collisions ?collisionsDefaultValue) ?collisions]
                 [?scene :scene/background ?bkg]
                 [?bkg :background/uuid ?backgroundUUID]
                 [?bkg :background/resource ?bkg-resource]
                 [?bkg-resource :resource/filepath ?bkg-resource-path]
                 ] ;; todo: size, actors and scripts/triggers
               @db-conn
                "generated scene"
               (random-uuid)
               [])]
      (map (fn [scene]
             (zipmap scene-labels (concat
                                   scene
                                   ;(export-actors (first scene))
                                   ;(export-triggers (first scene))
                                   ;(export-scripts (first scene))
                                   )))
           scenes)))

(defn export-resources
  "Exports the collection of resources, mostly for debugging because GBS won't use this data."
  []
  (let [resource-labels ["_datascript_internal_id" "type" "filename" "path"]
        resources
        (d/q '[:find ?id ?rtype ?rfname ?rfpath
               :in $
               :where 
               [?id :resource/type ?rtype]
               [?id :resource/filename ?rfname]
               [?id :resource/filepath ?rfpath]]
             @db-conn)
        ]
    (map (fn [element]
           (zipmap resource-labels element))
         resources)))



(defn export-gbs-project
  "Export the entire project as a GBS-compatible EDN."
  []
  (-> gbs-basic
      (update :z_design-moves export-design-moves)
      ;;(update :resources export-resources)
      (update :backgrounds export-backgrounds)
      (update :scenes export-scenes)))

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
        (if (map? q-map)
          [q-map]
          q-map)
       ))
    design-moves)))

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
  (cljs.pprint/pprint (get-in @db [:val :datoms]))
  (dorun
   (for [i (range budget)]
     (let [moves (get-possible-design-move-from-moveset global-design-moves)]
       (if false ;; set to true for debugging in the console
         (cljs.pprint/pprint @db))
       (if (empty? moves)
         nil
         (let [poss-move (rand-nth moves)] ;; todo: make determanistic
           (if-let [exec-func (get (get poss-move :move) :exec false)]
             (let [move-name (get (get poss-move :move) :name)]
               (cljs.pprint/pprint move-name)
               (let [result (concat
                             (exec-func db (:vars poss-move))
                             [{:db/id -999999 ; magic number to try and be unique...
                               :design/move-count (inc i) ; todo: count the actual number of moves that have been made by looking up the last one, instead of just using the loop counter
                               :design/move-record move-name
                               :design/move-parameters (get poss-move :vars [])}])]
                 ;; todo: do something to check for errors when executing the move...
                 ;(cljs.pprint/pprint result)
                 (d/transact! db result nil))) ; todo: do something with the transaction report, such as checking for errors
             nil)

           ))
         )))) ;; todo: log which transaction was executed


(defn generate []
  (reset-the-database!)
  (generate-level-random-heuristic db-conn 180 0)
  )

(comment (reset-the-database!)
         (generate)
         (generate-level-random-heuristic db-conn 5 0)
         (log-db db-conn))


(defn fetch-database []
  @db-conn
  )

(defn fetch-gbs []
  (generate)
  ;(clj->js (map clj->js (export-gbs-project)))
  (export-gbs-project))
