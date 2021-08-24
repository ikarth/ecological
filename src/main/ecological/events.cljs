(ns ecological.events
  (:require [ecological.state :refer [app-state]]
            [ecological.gbstudio.gbstudio :as gbs
             
             ;; :refer [fetch-gbs
             ;;                                      fetch-generated-project!
             ;;                                      fetch-database
             ;;                                      fetch-possible-moves
             ;;                                      make-empty-project
             ;;                                      execute-one-design-move!
             ;;                                      execute-design-move!
             ;;                                              ]
             ]
            [ecological.imaging.imaging :as imaging]
            ["clingo-wasm" :default clingo]
            
            ))


(def database-interface
  {:tab-gbs {:fetch-data-output        gbs/fetch-gbs
             :fetch-database           gbs/fetch-database
             :fetch-possible-moves     gbs/fetch-possible-moves
             :fetch-all-moves          gbs/fetch-all-moves
             :make-empty-project       gbs/make-empty-project
             :execute-design-move!     gbs/execute-design-move!
             :fetch-generated-project! gbs/fetch-generated-project!
             :fetch-data-view          gbs/fetch-data-view}
   :tab-image
   {:fetch-data-output        imaging/fetch-data-output
    :fetch-database           imaging/fetch-database
    :fetch-possible-moves     imaging/fetch-possible-moves
    :fetch-all-moves          imaging/fetch-all-moves
    :make-empty-project       imaging/make-empty-project
    :execute-design-move!     imaging/execute-design-move!
    :fetch-generated-project! imaging/fetch-generated-project!
    :fetch-data-view          imaging/fetch-data-view}
   })

(defn interface-with-database [interface-func]
  (let [selected-tab (get @app-state :selected-tab :tab-gbs)]
    
    (let [selected-tab (get @app-state :selected-tab {:id :tab-gbs})
          database-label (get selected-tab :id :tab-gbs)
          _ (assert (keyword? database-label) (str database-label " is not a recognized database name."))
          db-func (get-in database-interface [database-label interface-func] nil)
          _ (assert (fn? db-func) (str "(" database-label " " interface-func ") is not a known function"))]
      db-func)))

(defn set-active-sketch [active-sketch]
  (swap! app-state update-in [:active-sketch] active-sketch)
  )

(defn increment
  [event]
  (.preventDefault event)
  (swap! app-state update-in [:count] inc))

(defn decrement
  [event]
  (.preventDefault event)
  (swap! app-state update-in [:count] dec))


(defn select-main-database [event]
  (let [current-data (get @app-state :selected-tab false)]
    ))

(defn init-database [event]
  (if (some? event)
    (.preventDefault event))
  (let []
    (interface-with-database :make-empty-project)
    (swap! app-state update-in [:all-moves] (interface-with-database :fetch-all-moves))
    (swap! app-state update-in [:gbs-output] (interface-with-database :fetch-data-output))
    (swap! app-state update-in [:data] (interface-with-database :fetch-database))
    (swap! app-state update-in [:possible-moves] (interface-with-database :fetch-possible-moves))
    (swap! app-state update-in [:data-view] (interface-with-database :fetch-data-view))
    ))

(defn update-database-view [event]
  (if (some? event)
    (.preventDefault event))
  (swap! app-state update-in [:all-moves] (interface-with-database :fetch-all-moves))
  (swap! app-state update-in [:gbs-output] (interface-with-database :fetch-data-output))
  (swap! app-state update-in [:possible-moves] (interface-with-database :fetch-possible-moves))
  (swap! app-state update-in [:data] (interface-with-database :fetch-database))
  (swap! app-state update-in [:data-view] (interface-with-database :fetch-data-view)))

(defn select-tab
  [event tab]
  (if (some? event)
    (.preventDefault event))
  (if tab
    (let []
      (js/console.log "Selecting" tab)
      (swap! app-state assoc-in [:selected-tab] tab)
      (update-database-view nil)
      ;(swap! app-state update-in [:data] (interface-with-database :fetch-database))
      )))

(defn select-move
  "Makes the given design move be the currently selected one."
  [event move]
  (.preventDefault event)
  (js/console.log "Selecting" (:name move))
  (swap! app-state assoc-in [:selected-move] move)
  )

(defn select-bound-move
  [event move]
  (.preventDefault event)
  (js/console.log "Selecting" move)
  (swap! app-state assoc-in [:selected-bound-move] move))

(defn select-random-bindings
  []
  (swap! app-state update-in [:possible-moves] (interface-with-database :fetch-possible-moves))
  (if (:selected-move @app-state)
    (let [valid-moves (:possible-moves @app-state)
          selected-move-name (get-in (:selected-move @app-state) [:name])
          possible-moves (filter #(= (get-in % [:move :name]) selected-move-name) valid-moves)
          chosen-move (rand-nth possible-moves)]
      (swap! app-state assoc-in [:selected-bound-move] [0 chosen-move])
      true)
    false))

(defn perform-bound-move
  [event]
  (if (some? event)
    (.preventDefault event))
  ((interface-with-database :execute-design-move!) (second (:selected-bound-move @app-state)))
  (update-database-view nil)
  ;; (js/console.log @app-state)
  ;; (select-tab nil (:selected-tab @app-state))
  ;(swap! app-state update-in [:data] (interface-with-database :fetch-database))
  )

(defn perform-random-move
  [event]
  (if (some? event)
    (.preventDefault event))
  (let [valid-moves (:possible-moves @app-state)
        chosen-move (if (< 0 (count valid-moves)) (rand-nth valid-moves) nil)
        ]
    (println chosen-move)
    (when chosen-move
      (swap! app-state assoc-in [:selected-bound-move] [0 chosen-move])
      (perform-bound-move nil))))

(defn run-generator [event]
  (.preventDefault event)
  (swap! app-state update-in [:all-moves] (interface-with-database :fetch-all-moves))
  (swap! app-state update-in [:gbs-output] (interface-with-database :fetch-generated-project!))
  (swap! app-state update-in [:data] (interface-with-database :fetch-database))
  (swap! app-state update-in [:possible-moves] (interface-with-database :fetch-possible-moves))
  )



;; from https://blog.klipse.tech/visualization/2021/02/16/graph-playground-cytoscape.html
(def ^:dynamic *default-graph-options* 
  {:style [{:selector "node"
            :style {:background-color "#666"
                    ;:label "data(label)"
                    }}
           {:selector "edge"
            :style {"width" 2
                    :line-color "#ccc"
                    :target-arrow-color "#ccc"
                    :curve-style "bezier"
                    :target-arrow-shape "triangle"
                    ;:label "data(label)"
                    }}]
   :layout {:name "circle"}
   :userZoomingEnabled false
   :userPanningEnabled false
   :boxSelectionEnabled false})


(def test-graph
  [{:data {:id "a"}}
   {:data {:id "b"}}
   {:data {:id "c"}}
   {:data {:id "d"}}
   {:data {:id "e"}}
   {:data {:id "ab" :source "a" :target "b"}}
   {:data {:id "bc" :source "b" :target "c"}}
   {:data {:id "ac" :source "a" :target "c"}}
   {:data {:id "cd" :source "c" :target "d"}}
   {:data {:id "da" :source "d" :target "a"}}
   
                 ])

;; TODO: Write a proper converter for scene data to a graph
;; TODO: Write a proper converter for project data to a graph
;; TODO: Write a react component so it automatically updates and renders, instead of pushing a button
(defn graph-display
  [event]
  (.preventDefault event)
  (let
      [data test-graph                          ;(:gbs-output @app-state)
       container-id "artifact-graph"]
    (js/cytoscape
     (clj->js (merge *default-graph-options*
                     {:container (js/document.getElementById container-id)
                      :elements data}))))
  nil)
