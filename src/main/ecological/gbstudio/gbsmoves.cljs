
(ns ecological.gbstudio.gbsmoves
  (:require [datascript.core :as d]
            ["clingo-wasm" :as clingo]
             ;[ecological.gbstudio.assets :refer [asset-manifest scene-manifest]]
              ))

;; (def move-load-resources-from-disk
;;   {:name "load-resources-from-disk"
;;    :query
;;    '[:find ?sig 
;;      :in $ %
;;      :where
;;      [?sig :signal/signal :resources-not-loaded]
;;      ]
;;    :exec
;;    (fn [db [signal-resources-not-loaded]]
;;      ;; TODO: actually get data from server
;;      (let [manifest (asset-manifest)]
;;        (if (empty? manifest)
;;          (let []
;;            (js/console.log "Missing asset manifest")
;;            [])  ; todo: more elaborate file missing error handling?
;;          (let [category-table {"ui" :ui "image" :image "sprites" :sprites "backgrounds" :image}
;;                manifest-transaction
;;                (concat
;;                 [[:db/retractEntity signal-resources-not-loaded]]
;;                 [{:db/id -1 :signal/signal :resources-are-loaded}]
;;                 (mapv
;;                  (fn [key asset]
;;                    {:db/id key
;;                     :resource/type (category-table (asset :category :none) :none)
;;                     :resource/filename (asset :file :none)
;;                     :resource/filepath (asset :path :none)
;;                                         ;:resource/size (asset :size [0 0])
;;                     :resource/image-size (asset :image-size [0 0])})
;;                  (iterate dec -2)
;;                  manifest
;;                  ))]
;;              manifest-transaction))))})

;; (def move-load-gbs-projects-from-disk
;;   {:name "load-gbs-projects-from-disk"
;;    :query '[:find ?sig ?sig2 :in $ %
;;             :where
;;             [?sig :signal/signal :gbs-examples-not-loaded]
;;             [?sig2 :signal/signal :resources-are-loaded]
;;             ;[(missing? $ ?sig2 :signal/signal :resources-not-loaded)]
;;             ]
;;    :exec
;;    (fn [db [signal-not-loaded signal-resources-are-loaded]]
;;      (let [sep (scene-manifest)
;;            scenes-to-add
;;            (mapv (fn [key asset]
;;                     {:db/id key
;;                      :template/name (get asset :name "unnamed")
;;                      :template/triggers (get asset :triggers []) ; todo: translate triggers
;;                      :template/actors (get asset :actors []) ; todo: translate actors
;;                      :template/backgroundUUID (get asset :backgroundId "")
;;                      :template/collisions (get asset :collisions [])
;;                      :template/originaluuid (get asset :id "")
;;                      :template/use-count 0
;;                      })
;;                   (iterate dec -3)
;;                   (get sep :scenes []))
;;            backgrounds-to-add
;;            (mapv (fn [key asset]
;;                     {:db/id key
;;                      :gbs-input/type :background
;;                      :gbs-input/uuid (get asset :id "")
;;                      :gbs-input/name (get asset :name "")
;;                      :gbs-input/filename (get asset :filename "")
;;                      :gbs-input/width  (get asset :width 0)
;;                      :gbs-input/height (get asset :hieght 0)
;;                      })
;;                   (iterate dec (- 0 (+ 3 (count (get sep :scenes [])))))
;;                   (get sep :backgrounds []))
;;            transaction
;;            (into []
;;                  (concat
;;                   [[:db/retractEntity signal-not-loaded]]
;;                   scenes-to-add
;;                   backgrounds-to-add
;;                   ))]
;;        ;; (js/console.log sep)
;;        ;; (js/console.log (get sep :scenes :no-scenes-in-sep))
;;        ;; (js/console.log scenes-to-add)
;;        ;; (js/console.log (count scenes-to-add))
;;        ;; (js/console.log (> (count scenes-to-add) 0))
;;        ;; (js/console.log transaction)
;;        ;; (js/console.log [{:db/id -999 :signal/signal :successfully-loaded-scenes}])
;;        (if (and (> (count scenes-to-add) 0) (> (count backgrounds-to-add) 0))
;;          transaction
;;          [{:db/id -999 :signal/signal :failed-to-load-scenes}]
;;          ;; TODO: handle error if we can't manage to load the template scenes...
;;          )))})

(defn process-template-actors [actors original-uuid]
  actors)

;; TODO: verify this still works
(defn is-trigger-a-connection? [trigger original-uuid]
  (let [scripts (get trigger :script [])
        scripts-switch (filter #(-> % (get :command "") (= "EVENT_SWITCH_SCENE")) scripts)
        matching-scripts
        (filter (fn [scrpt]
               ;(js/console.log (into {} (get scrpt :args [])))
               (= original-uuid (get (into {} (get scrpt :args [])) :sceneId ""))
                )
                scripts-switch)]
    ;(js/console.log matching-scripts)
    (> (count matching-scripts) 0))
  false)

(defn process-template-triggers [triggers original-uuid]
  ;;(js/console.log triggers)
  (mapv
   (fn [trigger-to-process]
     ;(js/console.log trigger-to-process)
     (cond      
       (is-trigger-a-connection? trigger-to-process original-uuid)        
       trigger-to-process ;; todo: process the connection
       :else trigger-to-process))
   triggers)
  [triggers []])

;; (def move-run-clingo
;;   {:name "run-clingo"
;;    :query
;;    '[:find ?clingo-command
;;      :in $ %
;;      :where
;;      [?e :constraint/asp ?clingo-command]]
;;    :exec
;;    (fn [db [clingo-command]]
;;      (. js/clingo run)
;;      )})

(def move-generate-scene-from-template
  {:name "generate-scene-from-template"
   :query
   '[:find ?template-id ?name ?bkg-id ?use-count ?collisions ?triggers ?actors ?original-template-uuid
     :in $ %
     :where
     [?template-id :template/name ?name]
     [?template-id :template/backgroundUUID ?bkg-uuid]
     [?template-id :template/collisions ?collisions]     
     [?template-id :template/triggers ?triggers]
     [?template-id :template/actors ?actors]
     [?template-id :template/originaluuid ?original-template-uuid]
     [?gbs-bkg-id :gbs-input/uuid ?bkg-uuid]
     [?gbs-bkg-id :gbs-input/filename ?bkg-filename]
     [?bkg-id :background/filename ?bkg-filename]
     [?template-id :template/use-count ?use-count]
     ]
   :exec
   (fn [db [template-id template-name bkg-id use-count collisions triggers actors original-template-uuid
            ]]
     (let [triggers (process-template-triggers triggers original-template-uuid)
           actors (process-template-actors actors original-template-uuid)
           transaction
           [{:db/id -1
             :scene/uuid (str (random-uuid))             
             :scene/editor-position [20 20]
             :scene/background bkg-id
             :scene/collisions collisions
             :scene/name (str template-name "_" (str (random-uuid)))
             :scene/triggers (first triggers)
             :scene/connections (second triggers)
             :scene/actors actors
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

(def design-moves
  [
   ;move-load-resources-from-disk
   move-create-background-from-image
   ;move-load-gbs-projects-from-disk
   move-generate-scene-from-template
   ;move-place-greenfield-scene
   ;move-add-existing-background-to-scene
   ])

(def design-moves-finalizing
  [{:name "resolve-scene-backgrounds"}])
