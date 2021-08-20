(ns ecological.events
  (:require [ecological.state :refer [app-state]]
            [ecological.gbstudio.gbstudio :refer [fetch-gbs
                                                  fetch-generated-project!
                                                  fetch-database
                                                  fetch-possible-moves
                                                  make-empty-project
                                                  execute-one-design-move!
                                                  ]]
            ["clingo-wasm" :default clingo]))

(defn increment
  [event]
  (.preventDefault event)
  (swap! app-state update-in [:count] inc))

(defn decrement
  [event]
  (.preventDefault event)
  (swap! app-state update-in [:count] dec))

(defn init-database [event]
  (if (some? event)
    (.preventDefault event))
  (let []
    (make-empty-project)
    (swap! app-state update-in [:gbs-output] fetch-gbs)
    (swap! app-state update-in [:data] fetch-database)
    (swap! app-state update-in [:possible-moves] fetch-possible-moves)))

(defn update-database-view [event]
  (if (some? event)
    (.preventDefault event))
  (swap! app-state update-in [:gbs-output] fetch-gbs)
  (swap! app-state update-in [:data] fetch-database)
  (swap! app-state update-in [:possible-moves] fetch-possible-moves))

(defn select-tab
  [event tab]
  (.preventDefault event)
  (js/console.log "Selecting" tab)
  (swap! app-state assoc-in [:selected-tab] tab) 
  )

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
  (swap! app-state assoc-in [:selected-bound-move] move)
  )

(defn select-random-bindings
  []
  (swap! app-state update-in [:possible-moves] fetch-possible-moves)
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
  (js/console.log (:selected-bound-move @app-state))
  ;; TODO
  (execute-one-design-move! (second (:selected-bound-move @app-state)))
  (update-database-view nil))

(defn perform-random-move
  [event]
  (if (some? event)
    (.preventDefault event))
  (let [valid-moves (:possible-moves @app-state)
        chosen-move (if (< 0 (count valid-moves)) (rand-nth valid-moves) nil)]
    (if (not (nil? chosen-move))
      (swap! app-state assoc-in [:selected-bound-move] [0 chosen-move])
      (perform-bound-move nil))))

(defn run-generator [event]
  (.preventDefault event)
  (swap! app-state update-in [:gbs-output] fetch-generated-project!)
  (swap! app-state update-in [:data] fetch-database)
  (swap! app-state update-in [:possible-moves] fetch-possible-moves)
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
