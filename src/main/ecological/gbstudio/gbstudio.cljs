(ns ecological.gbstudio.gbstudio
  ;; (:require-macros [cljs.core.async.macros :refer [go]])
  ;; (:require [cljs-http.client :as http]
  ;;           [cljs.core.async :refer [<!]])
  ;;(:require-macros [cljs.core.async.macros :refer [go]])
  (:require [datascript.core :as d]
            ;[cljs.reader :as reader]
            ;;[cljs-http.client :as http]
            ;;[cljs.core.async :refer [<!]]
            [clojure.string]
            [goog.crypt :as crypt]
            [ecological.gbstudio.gbsmoves :as gbs-moves]
            [ecological.gbstudio.assets :refer [asset-manifest scene-manifest]]
            ))


(def gbs-basic (js->clj (.parse js/JSON "{ \"author\": \"https://github.com/ikarth/ecological\", \"name\": \"Generated Game Boy ROM\", \"_version\": \"2.0.0\", \"scenes\": [], \"backgrounds\": [], \"variables\": [], \"spriteSheets\": [], \"music\": [], \"customEvents\": [], \"palettes\": [], \"settings\": { \"showCollisions\": true, \"showConnections\": true, \"worldScrollX\": 0, \"worldScrollY\": 0, \"zoom\": 100, \"customColorsWhite\": \"E8F8E0\", \"customColorsLight\": \"B0F088\", \"customColorsDark\": \"509878\", \"customColorsBlack\": \"202850\", \"defaultBackgroundPaletteIds\": [ \"default-bg-1\", \"default-bg-2\", \"default-bg-3\", \"default-bg-4\", \"default-bg-5\", \"default-bg-6\" ], \"defaultSpritePaletteId\": \"default-sprite\", \"defaultUIPaletteId\": \"default-ui\", \"startX\": 0, \"startY\": 0, \"startDirection\": 0, \"startSceneId\": 0, \"playerSpriteSheetId\": \"581d34d0-9591-4e6e-a609-1d94f203b0cd\" } }" ) :keywordize-keys true))

;(.parse js/JSON "{\"author\": \"test\", \"music\": []}")

(def genboy-schema
  {:signal/signal             {:db/cardinality :db.cardinality/one} ; :db/valueType :db.type/keyword
   :scene/background          {:db/cardinality :db.cardinality/one  :db/valueType :db.type/ref}
   :scene/name                {:db/cardinality :db.cardinality/one} ; :db/valueType :db.type/string
   :scene/uuid                {:db/cardinality :db.cardinality/one  :db/unique :db.unique/identity}
   :scene/backgroundUUID      {:db/cardinality :db.cardinality/one} ; :db/valueType :db.type/string
   :scene/collisions          {:db/cardinality :db.cardinality/one} ; :db/valueType :db.type/tuple
   :scene/editor-position     {:db/cardinality :db.cardinality/one} ; :db/valueType :db.type/tuple
   :background/filename       {:db/cardinality :db.cardinality/one} ; :db/valueType :db.type/string
   :background/image          {:db/cardinality :db.cardinality/one  :db/valueType :db.type/ref}
   :background/size           {:db/cardinality :db.cardinality/one} ; :db/valueType :db.type/tuple
   :background/imageSize      {:db/cardinality :db.cardinality/one} ; :db/valueType :db.type/tuple
   :background/uuid           {:db/cardinality :db.cardinality/one  :db/unique :db.unique/identity}
   :resource/filename         {:db/cardinality :db.cardinality/one  :db/unique :db.unique/identity}
   :resource/type             {:db/cardinality :db.cardinality/one} ; :db/valueType :db.type/keyword
   })

(def db-conn (d/create-conn genboy-schema))

(defn reset-the-database! []
  (d/reset-conn! db-conn (d/empty-db genboy-schema))
  (d/transact! db-conn [;; {:db/id -1
                        ;;  :signal/signal :resources-not-loaded}
                        ;; {:db/id -2
                        ;;  :signal/signal :gbs-examples-not-loaded}
                        ]))


(defn create-gbs-entity
  "create a new GBS entity"
  []
  [{:db/id -1 :type "scene"}])


(defn load-resources
  "Load resources from disk that we need to generate things..."
  []
  (let [manifest (asset-manifest)]
       (if (empty? manifest)
         (let []
           (js/console.log "Missing asset manifest")
           [])  ; todo: more elaborate file missing error handling?
         (let [category-table {"ui" :ui "image" :image "sprites" :sprites "backgrounds" :image}
               manifest-transaction
               (concat
                []
                ;[[:db/retractEntity signal-resources-not-loaded]]
                ;[{:db/id -1 :signal/signal :resources-are-loaded}]
                (mapv
                 (fn [key asset]
                   {:db/id key
                    :resource/type (category-table (asset :category :none) :none)
                    :resource/filename (asset :file :none)
                    :resource/filepath (asset :path :none)
                                        ;:resource/size (asset :size [0 0])
                    :resource/image-size (asset :image-size [0 0])})
                 (iterate dec -2)
                 manifest
                 ))]
             manifest-transaction))))

(defn load-gbs-projects
  []
  (let [sep (scene-manifest)
           scenes-to-add
           (mapv (fn [key asset]
                    {:db/id key
                     :template/name (get asset :name "unnamed")
                     :template/triggers (get asset :triggers []) ; todo: translate triggers
                     :template/actors (get asset :actors []) ; todo: translate actors
                     :template/backgroundUUID (get asset :backgroundId "")
                     :template/collisions (get asset :collisions [])
                     :template/originaluuid (get asset :id "")
                     :template/use-count 0
                     })
                  (iterate dec -3)
                  (get sep :scenes []))
           backgrounds-to-add
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
           transaction
           (into []
                 (concat
                  ;[[:db/retractEntity signal-not-loaded]]
                  scenes-to-add
                  backgrounds-to-add
                  ))]
       (if (and (> (count scenes-to-add) 0) (> (count backgrounds-to-add) 0))
         transaction
         [{:db/id -999 :signal/signal :failed-to-load-scenes}]
         ;; TODO: handle error if we can't manage to load the template scenes...
         )))


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

(defn byte-to-hex [b-val]
  (let [hex-str (.toString b-val 16)]
    (case (count hex-str)
      0 "00"
      1 (str "0" hex-str)
      (subs hex-str (- (count hex-str) 2) (count hex-str)))))

;; (defn hex-to-byte [h-val]
;;   ;(js/console.log (str h-val))
;;   ;(js/console.log (crypt/stringToUtf8ByteArray (str h-val)))
;;   ;(into [] (crypt/stringToUtf8ByteArray (str h-val)))
;;   (apply str h-val))
;; ;utf8ByteArrayToString

(defn bytes-to-hex-string [array-of-bytes]
  (clojure.string/join
   (mapv
    (fn [b-val]
      (byte-to-hex b-val))
    array-of-bytes)))

;; (defn hex-string-to-bytes [hex-string]
;;   (mapv
;;    hex-to-byte
;;    (partition 2 hex-string)))

(defn bytes-to-bools [array-of-bytes]
  (clojure.string/join
   (mapv
    (fn [b-val]
      (cljs.pprint/cl-format nil (str "~" 8 ",'0d") (.toString b-val 2)))
    array-of-bytes)))


(defn export-scenes
  "Export all of the scenes from the database and return EDN that can eventually be interperted by GBS."
  []
    (let [scene-labels
          ["_datascript_internal_id" "backgroundId" "editor-position" "name" "id" "collisions" "background-image" "collisions-viz" "size"]
          scenes
          (d/q '[:find ?scene ?backgroundUUID ?editor-position ?name ?uuid ?collisions ?bkg-resource-path ?collisions ?size
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
                 [?bkg :background/size ?size]
                 ] ;; todo: size, actors and scripts/triggers
               @db-conn
                "generated scene"
               (random-uuid)
               [])]
      (map (fn [scene]
             ;;(js/console.log scene)
             (->
              (zipmap scene-labels (concat
                                    scene
                                        ;(export-actors (first scene))
                                        ;(export-triggers (first scene))
                                        ;(export-scripts (first scene))
                                    ))
              (update-in ["collisions-viz"] (fn [colls] (str "collisions-viz|" (first (nth scene 8)) "|" (second (nth scene 8)) "|" (bytes-to-hex-string colls) "|" (nth scene 6))))))
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
  (get-possible-design-move-from-moveset gbs-moves/design-moves))


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
     (let [moves (get-possible-design-move-from-moveset gbs-moves/design-moves)]
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
                             [{:db/id -999999 ; magic number to try and be unique...this will break if more than 1,000,000 changes are in the transaction. Which is unlikely.
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
  (d/transact! db-conn (load-resources) nil)
  (d/transact! db-conn (load-gbs-projects) nil)
  (generate-level-random-heuristic db-conn 128 0)
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
