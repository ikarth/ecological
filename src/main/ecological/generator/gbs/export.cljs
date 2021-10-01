(ns ecological.generator.gbs.export
  (:require [datascript.core :as d]
            [clojure.string]
            [ecological.generator.utilities :as util]
            [cljs.pprint :as pprint]
            ;[goog.crypt :as crypt]
            ;[quil.core :as qc]
            ;[quil.middleware :as qm]
            ;[quil.applet :as qa]
            ))





(defn export-design-moves [db-conn]
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

(defn export-backgrounds [db-conn]
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

;; Get all of the stuff that will be added on top of the backgrounds and transform the background images accordingly...
(defn ground-backgrounds [db-conn]
  (let [backgrounds
        (d/q '[])
        connections
        (d/q '[])
        ]
    )  
  )

(defn export-triggers [db-conn scene]
  (pprint/pprint "(export-triggers)")
  (pprint/pprint scene)
  (pprint/pprint db-conn)
  (let [scene-id (first scene)
        triggers
        (d/q
         '[:find ?trigger-id ?target-scene ?target-uuid ?this-location ?target-location ?direction
           :in $ ?s-id
           :where
           [?trigger-id :type/gbs :gbs/trigger]
           [?trigger-id :trigger/scene ?s-id]
           [?trigger-id :trigger/parent ?end-id]
           [?end-id :entity/position ?this-location]
           [?trigger-id :trigger/target-location ?target-location]
           [?trigger-id :trigger/target ?target-scene]
           [?target-scene :scene/uuid ?target-uuid]
           [?trigger-id :trigger/direction ?direction]]     
         @db-conn scene-id)
        ]
    (pprint/pprint triggers)
    (map (fn [[id target target-uuid loc target-loc direction]]
           {"x" (first loc)
            "y" (second loc)
            "width" 2
            "height" 1
            "trigger" "walk"
            "script"
            [{"id" (str (random-uuid))
              "command" "EVENT_SWITCH_SCENE"
              "args" {"x" (first target-loc)
                      "y" (second target-loc)
                      "direction" (str direction)
                      "sceneId" target-uuid
                      "fadeSpeed" 2
                      }
              } 
             {"id" (str (random-uuid))
              "command" "EVENT_END"
              }
             ]
            "id" (str (random-uuid))
            }           )
         triggers)))

(defn export-scenes
  "Export all of the scenes from the database and return EDN that can eventually be interperted by GBS."
  [db-conn]
  ;; collisions and collisions-viz are separate because we want to have one for GBS and one for our internal display. TODO: However, we might be able to construct collisions-viz after the query.
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
             ;(js/console.log scene)
             ;(pprint/pprint scene)
             (let [s
                   (->
                    (zipmap scene-labels (concat
                                          scene
                                        ;(export-actors (first scene))
                                         ; (export-triggers db-conn (first scene))
                                        ;(export-scripts (first scene))
                                          ))
                    (update-in ["collisions-viz"]
                               (fn [colls]
                                 (comment
                                   [:collisions-viz
                                    (first (nth scene 8))
                                    (second (nth scene 8))
                                    (util/bytes-to-hex-string colls)
                                    (nth scene 6)
                                    ])
                                 (str "collisions-viz|" (first (nth scene 8)) "|" (second (nth scene 8)) "|" (util/bytes-to-hex-string colls) "|" (nth scene 6))
                                 
                                 ))
                    (assoc-in
                     ["triggers"]
                     (export-triggers db-conn scene)
                              
                              )

                    )]
               (pprint/pprint s)
               ()
               s
               ))
           scenes)))

(defn export-resources
  "Exports the collection of resources, mostly for debugging because GBS won't use this data."
  [db-conn]
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
  [basic-export db-conn]
  (-> basic-export
      (assoc :_design-moves (export-design-moves db-conn))
      (assoc :z_resources (export-resources db-conn))
      (assoc :backgrounds (export-backgrounds db-conn))
      (assoc :scenes (export-scenes db-conn))))

(defn export-most-recent-artifact
  [basic-export db-conn]
  (-> {}
      {}
      ))



(defn list-connections [db-conn]
  (let [data
        (d/q
         '[:find ?connection ?endA ?endB ?sceneA ?sceneB ?sceneAname ?sceneBname
           :in $
           :where
           [?connection :type/gbs :gbs/connection]
           [?endA :type/gbs :gbs/endpoint]
           [?endB :type/gbs :gbs/endpoint]
           [?sceneA :type/gbs :gbs/scene]
           [?sceneB :type/gbs :gbs/scene]
           [(not= ?endA ?endB)]
           [(not= ?sceneA ?sceneB)]
           ;;[(> ?endA ?endB)]
           [?endA :endpoint/scene ?sceneA]
           [?endB :endpoint/scene ?sceneB]
           [?connection :connection/left-end ?endA]
           [?connection :connection/right-end ?endB]
           [?sceneA :scene/name ?sceneAname]
           [?sceneB :scene/name ?sceneBname]
           ]
         @db-conn)
        ]
    ;;(println data)
    (mapv (fn [connect]
            ;; (-> (zipmap [:connection :endA :endB :sceneA :sceneB :sceneAname :sceneBname] connect)
            ;;     #([(str (:connection %) ": " (:sceneA %) " <-> " (:sceneB %))])
            ;;     )
            (let [c (zipmap [:connection :endA :endB :sceneA :sceneB :sceneAname :sceneBname] connect)]
              [(str "Edge " (:connection c) ": " (:sceneA c) " <-> " (:sceneB c))]
              )
            ;(str connect)
            )          
          data)))

(defn list-scenes [db-conn]
  (let [scene-labels []
        scenes
        (d/q '[:find ?scene ?name ?uuid; ?connections
               :in $ ?nameDefaultValue ?idDefaultValue
               :where
               [?scene :scene/editor-position ?editor-position]
               [(get-else $ ?scene :scene/name ?nameDefaultValue) ?name]
               [(get-else $ ?scene :scene/uuid ?idDefaultValue) ?uuid]
               ;[(get-else $ ?scene :scene/connections []) ?connections]
               ;[(get-else $ ?connections :connection/scene ?scene)]
               ]
             @db-conn
             "generated scene"
             "no id number")]
    ;(println scenes)
    (mapv (fn [scene]
            (-> (zipmap [:num :name :uuid] scene)
                ((fn [el]
                   [(str (:num el) ": " (:name el) " " (subs (:uuid el) 32))
                    ;;(into [] (:connections el))
                    ]       ))))
          scenes)))

(defn list-project [db-conn]
  (let [scene-labels []
        scenes
        (d/q '[:find ?scene ?name ?uuid; ?connections
               :in $ ?nameDefaultValue ?idDefaultValue
               :where
               [?scene :type/gbs :gbs/scene]
               [(get-else $ ?scene :scene/name ?nameDefaultValue) ?name]
               [(get-else $ ?scene :scene/uuid ?idDefaultValue) ?uuid]
               ]
             @db-conn
             "generated scene"
             "no id number")
        endpoints
        (d/q '[:find ?endpoint ?scene
               :in $
               :where
               [?endpoint :type/gbs :gbs/endpoint]
               [?endpoint :endpoint/scene ?scene]
               ] @db-conn)
        connections
        (d/q '[:find ?connection ?left ?right ?left-scene ?right-scene
              :in $
              :where
              [?connection :type/gbs :gbs/connection]
              [?connection :connection/left-end ?left]
              [?connection :connection/right-end ?right]
              [?left :endpoint/scene ?left-scene]
              [?right :endpoint/scene ?right-scene]
              ] @db-conn)]
    ;(println scenes)
    (mapv (fn [scene]
            (-> (zipmap [:num :name :uuid] scene)
                ((fn [el]
                   (let [ends
                         (filter
                          (fn [end]
                            (= (second end) (:num el))
                            )
                          endpoints)
                         cons (filter
                               (fn [con]
                                 (or (= (:num el) (nth con 3))
                                     (= (:num el) (nth con 4))))
                               connections)
                         ]
                     ;; (println "pview")
                     ;; (println ends)
                     ;; (println cons)
                     [(str (:num el) ": " (:name el) " " (subs (:uuid el) 32))
                      (for [end ends]
                        (let [left (filter  (fn [cn] (= (first end) (nth cn 1))) cons)
                              right (filter (fn [cn] (= (first end) (nth cn 2))) cons)
                              ]
                          ;; (println (str "end:" end))
                          ;; (println (str "cons: " cons))
                          ;; (println (str "left: " left))
                          ;; (println (str "right: " right))
                          (str (first end) " -> "
                               (cond
                                 (not (empty? left))
                                 (str (nth (first left) 4))
                                 (not (empty? right))                                                     
                                 (str (nth (first right) 3))
                                 :else
                                 "")
                                                         )
                                                  ;;      (catch Exception e (str "caught exception: " (.getMessage e)))
                               ;;      )
                              
                                                ))
                      ;;(str ends)
                      ;;(into [] (:connections el))
                      
                      ])       ))))
          scenes)))

(defn export-project-view
  [db-conn]
  ;;(println "(export-project-view)")
  [(list-connections db-conn)
   ;;(list-scenes db-conn)
   (list-project db-conn)
   ]
  )
