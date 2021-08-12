(ns ecological.events
  (:require [ecological.state :refer [app-state]]
            [ecological.gbstudio.gbstudio :refer [fetch-gbs fetch-database fetch-possible-moves]]
            ["clingo-wasm" :default clingo]
            ;; ["p5" :default p5]
            ))

(defn increment
  [event]
  (.preventDefault event)
  (swap! app-state update-in [:count] inc))

(defn decrement
  [event]
  (.preventDefault event)
  (swap! app-state update-in [:count] dec))


;; (defn test-constraint-solving [event]
;;   (.preventDefault event)
;;   (js/console.log "Running constraint solver...")
;;   (let [solution ;(fn [_] "TEST")
;;         (fn [data]
;;           ;(js/console.log clingo)
;;           ;(. clingo run data)
;;           (js/p5 5)
;;           )
;;         ]
;;     (js/console.log (solution "a. b :- a."))
;;     (swap! app-state update-in [:constraint-test] solution)
;;     )
;;   )

(defn run-generator [event]
  (.preventDefault event)
  (swap! app-state update-in [:gbs-output] fetch-gbs)
  (swap! app-state update-in [:data] fetch-database)
  (swap! app-state update-in [:moves] fetch-possible-moves)
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
