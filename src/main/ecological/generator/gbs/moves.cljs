(ns ecological.generator.gbs.moves
  (:require [datascript.core :as d]
            [clojure.string]
            ;[goog.crypt :as crypt]
            ;[quil.core :as qc]
            ;[quil.middleware :as qm]
                                        ;[quil.applet :as qa]
            ["clingo-wasm" :as clingo]
            [ecological.generator.gbs.assets :refer [asset-manifest scene-manifest]]
            [ecological.generator.gbs.ops :as ops]
            [ecological.generator.gbs.query :as q]
            ))


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
   :comment "Given a GBS-derived template, create a new scene that resembles it."
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

;; (def move-place-greenfield-scene
;;   {:name "place-greenfield-scene"
;;    :exec (fn [db _] ; takes parameters but ignores them
;;            [{:db/id -1
;;              :scene/uuid (str (random-uuid))             
;;              :scene/editor-position [20 20]  ; todo: query db for empty editor space to place scene?
;;              :scene/name "generated greenfield scene"
;;                }])})


(def move-create-background-from-image
  {:name "create-background-from-image"
   :comment "Takes an image and turns it into a background that can be used in a Scene."
   :example
   [{:db/id -1 ;; note that this assumes that this file exists
     :resource/type :image
     :resource/filename :none
     :resource/filepath :none
     :resource/image-size [0 0]
     }]
   :query
   '[:find ?image-filename ?r ?image-size
     :in $ %
     :where
     [?r :resource/type :image]
     [?r :resource/filename ?image-filename]
     [?r :resource/image-size ?image-size]     
     (not-join [?image-filename]
               [?e :background/filename ?image-filename])]
   :exec (fn [db [image-filename resource-id gb-image-size] parameters]
           (let [image-size gb-image-size ;; todo: check actual size of image
                 tile-size 8  ;; gbstudio uses 8x8 tiles for its scene backgrounds
                 image-tiles (mapv #(quot % tile-size) image-size)]
             (js/console.log image-tiles)
             (println "*** move create background")
             (println image-tiles)
             (println image-size)
             [{:db/id -1
               :background/uuid (str (random-uuid)) ; todo: use hash to speed comparisons?
               :background/size image-tiles
               ;:background/imageSize image-size
               :background/filename image-filename
               :background/resource resource-id}] ))})


(def move-create-greenfield-image
  {:name "create-greenfield-image"
   :comment "Creates a new image in the database"
   :exec
   (fn [db bindings parameters]
     (ops/create-image db bindings parameters))})

(def move-create-speckled-background-image
  {:name "create-speckled-background-image"
   :comment "Creates new image with light speckle of dots"
   :exec
   (fn [db bindings parameters]
     (ops/create-speckled-background-image db bindings parameters))})

(def move-create-greenfield-background
  {:name "create-greenfield-background"
   :comment "Creates a new image to act as a background"
   :exec (fn [db bindings parameters]
           (ops/create-background db bindings parameters))})

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

;; (def move-apply-collision-to-scene
;;   {})

;; (def move-selection-collision-by-background
;;   {})

;; (def move-mixin-npc
;;   {})

;; (def move-connect-scenes
;;   {})

;; (def move-connect-scenes-by-region
;;   {})

;; (def move-connect-scenes-by-edge-tag
;;   {})


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



;; (def design-moves-finalizing
;;   [{:name "resolve-scene-backgrounds"}])


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



;; (def move-add-connection-to-scene
;;   {:name "add-connection-to-scene"
;;    :query
;;    '[:find ?sceneid ?connections
;;      :in $ %
;;      :where
;;      [?sceneid :scene/editor-position ?ed-pos]
;;      [(get-else $ ?sceneid :scene/connections []) ?connections]
;;      ]
;;    :exec
;;    (fn [db [scene-id connections] parameters]
;;      (println connections)
;;      (let [connection-limit 4
;;            existing-count (count connections)
;;            existing-directions []]
;;        (if (>= existing-count connection-limit)
;;          []
;;          (let []
;;            [
;;             ;; {:db/id scene-id
;;             ;;  :scene/connections (into connections [-1])
;;             ;;  }
;;             {:db/id -1
;;              :connection/scene scene-id
;;              :connection/position [:unknown :unknown]
;;              :connection/direction :unknown
;;              }
;;             ])
;;          )))})




(def move-place-greenfield-scene
  {:name "place-greenfield-scene"
   :exec (fn [db bindings parameters]
           (ops/create-scene db bindings parameters))})

;; (def move-place-greenfield-connection
;;   {:name "place-greenfield-connection"
;;    :exec (fn [db bindings parameters] ; takes parameters but ignores them
;;            (ops/create-connection db bindings parameters))})

(def move-place-greenfield-endpoint
  {:name "place-greenfield-endpoint"
   :exec (fn [db bindings parameters] ; takes parameters but ignores them
           (ops/create-endpoint db bindings parameters))})

(def move-add-endpoint-to-scene
  {:name "add-endpoint-to-scene"
   :comment "Add a linking endpoint to a scene, to be used for a future portal trigger."
   :query
   '[:find ?sceneA
     :in $ %
     :where
     [?sceneA :type/gbs :gbs/scene]]
   :exec
   (fn [db [scene] parameters]
     (ops/create-endpoint db {:scene scene} []))})

(def move-connect-scenes
  {:name "connect-scenes"
   :comment "Connect two scenes via new connection."
   :query
   '[:find ?sceneA ?sceneB ?endA ?endB
     :in $ %
     :where
     [?sceneA :type/gbs :gbs/scene]
     [?sceneB :type/gbs :gbs/scene]
     [?endA   :type/gbs :gbs/endpoint]
     [?endB   :type/gbs :gbs/endpoint]
     [?endA   :endpoint/scene ?sceneA]
     [?endB   :endpoint/scene ?sceneB]
     [(> ?endA ?endB)] ;; break ties
     [(not= ?endA ?endB)]
     [(not= ?sceneA ?sceneB)]
     (or
      (and [?endA :entity/direction "up"]    [?endB :entity/direction "down"])
      (and [?endA :entity/direction "down"]  [?endB :entity/direction "up"])
      (and [?endA :entity/direction "left"]  [?endB :entity/direction "right"])
      (and [?endA :entity/direction "right"] [?endB :entity/direction "left"]))
     (not-join [?sceneA ?sceneB]
               [?endC :endpoint/scene ?sceneA]
               [?endD :endpoint/scene ?sceneB]
               [?existing-connection :type/gbs :gbs/connection]
               [?existing-connection :connection/left-end ?endC]
               [?existing-connection :connection/right-end ?endD])
     (not-join [?sceneA ?sceneB]
               [?endC :endpoint/scene ?sceneA]
               [?endD :endpoint/scene ?sceneB]
               [?existing-connection :type/gbs :gbs/connection]
               [?existing-connection :connection/left-end ?endD]
               [?existing-connection :connection/right-end ?endC])]
   :exec
   (fn [db [sceneA sceneB endA endB] parameters]
     (into
      []
      (concat
       (ops/create-connection db {:end-left endA :end-right endB} parameters)
       ;; (ops/link-endpoint-to-scene :scene sceneA :endpoint endA)
       ;; (ops/link-endpoint-to-scene :scene sceneB :endpoint endB)
       ;; (ops/link-left-endpoint-to-connection :endpoint endA :connection connection)
       ;; (ops/link-right-endpoint-to-connection :endpoint endB :connection connection)
       )))})

(def offsets-by-direction
  {"up"    [ 0 -1]
   "down"  [ 0  1]
   "left"  [-2  0]
   "right" [ 2  0]
   })

(def endpoint-assign-position
  {:name    "endpoint-assign-position"
   :comment "assigns a position (and direction) to an endpoint that doesn't have one"
   :query
   '[:find ?entity-id ?scene-id ?scene-size
     :in $ %
     :where
     [?entity-id :type/gbs :gbs/endpoint]
     [?entity-id :endpoint/scene ?scene-id]
     [?scene-id  :scene/background ?background-id]
     [?background-id  :background/size ?scene-size]
     (not-join [?entity-id]
               (or
                [?connection :connection/left-end ?entity-id]
                [?connection :connection/right-end ?entity-id]))
     (not-join [?entity-id]
               [?entity-id :entity/position  ?entity-pos])
     (not-join [?entity-id]
               [?entity-id :entity/direction ?entity-dir])
     ]
   :exec
   (fn [db [endpoint-id scene-id scene-size] parameters]
     (ops/place-endpoint-at-random-edge
      db
      {:endpoint-id endpoint-id
       :scene       scene-id
       :size        scene-size
       }
      parameters))})

(def ground-connection-into-trigger
  {:name    "ground-connnection-into-trigger"
   :comment "translate finished connections into scene triggers for eventual export"
   :query
   '[:find ?connection ?endA ?endB ?sceneA ?sceneB ?endApos ?endBpos ?directionA ?directionB
     :in $ %
     :where
     [?connection :type/gbs :gbs/connection]
     [?endA :type/gbs :gbs/endpoint]
     [?endB :type/gbs :gbs/endpoint]
     [?endA :entity/position ?endApos]
     [?endB :entity/position ?endBpos]
     [?sceneA :type/gbs :gbs/scene]
     [?sceneB :type/gbs :gbs/scene]
     [?endA :entity/direction ?directionA]
     [?endB :entity/direction ?directionB]
     [(not= ?endA ?endB)]
     [(not= ?sceneA ?sceneB)]
     ;;[(> ?endA ?endB)]
     [?endA :endpoint/scene ?sceneA]
     [?endB :endpoint/scene ?sceneB]
     [?connection :connection/left-end ?endA]
     [?connection :connection/right-end ?endB]
     ;;[?sceneA :scene/name ?sceneAname]
     ;;[?sceneB :scene/name ?sceneBname]
     (not-join [?endA ?connection]
               [?triggerA :type/gbs :gbs/trigger]
               [?triggerA :trigger/parent ?endA]
               (or
                [?connection :connection/left-end ?endA]
                [?connection :connection/right-end ?endA]))
     (not-join [?endB ?connection]
               [?triggerB :type/gbs :gbs/trigger]
               [?triggerB :trigger/parent ?endB]
               (or
                [?connection :connection/left-end ?endB]
                [?connection :connection/right-end ?endB]))]
   :exec
   (fn [db [connection endA endB sceneA sceneB posA posB dirA dirB] parameters]
     (let [offsetA (mapv + posA (get offsets-by-direction dirA))
           offsetB (mapv + posB (get offsets-by-direction dirB))]
       [{:db/id -1
         :type/gbs :gbs/trigger
         :trigger/scene  sceneA
         :trigger/parent endA
         :trigger/type   :trigger-type/connection
         :trigger/target sceneB
         :trigger/direction dirB
         :trigger/target-location offsetB
         }
        {:db/id -2
         :type/gbs :gbs/trigger
         :trigger/scene  sceneB
         :trigger/parent endB
         :trigger/type   :trigger-type/connection
         :trigger/target sceneA
         :trigger/direction dirA
         :trigger/target-location offsetA
         }      
        ])
     )})
