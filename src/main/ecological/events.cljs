(ns ecological.events
  (:require [ecological.state :refer [app-state]]))

(defn increment
  [event]
  (.preventDefault event)
  (swap! app-state update-in [:count] inc))

(defn decrement
  [event]
  (.preventDefault event)
  (swap! app-state update-in [:count] dec))









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
