(ns ecological.views
  (:require [ecological.state :refer [app-state]]
            [ecological.events :refer [increment decrement]]
            [reagent.core :as r]
            [cljs.pprint]
            [json-html.core :refer [json->hiccup json->html edn->html]]
                                        ;[reagent-flowgraph.core :refer [flowgraph]]
            [coll-pen.core]
            ))

(defn header
  []
  [:div
   [:h1 "Ecological Generator Output Visualization"]])

(defn counter
  []
  [:div
   [:button.btn {:on-click #(decrement %)} "-"]
   [:button {:disabled true} (get @app-state :count)]
   [:button.btn {:on-click #(increment %)} "+"]])

; (.stringify js/JSON (:gbs-output @app-state))

(.stringify js/JSON (clj->js {:data "test data"}))



(defn download-gbs [_]
  (let [data-blob (js/Blob. (.stringify js/JSON (clj->js {:data "test data"})) #js {:type "application/json"})
        data-link (.createElement js/document "a")
        export-name "gbs-test-file.json"]
    (set! (.-href data-link) (.createObjectURL js/URL data-blob))
    (.setAttribute data-link "download" export-name)
    (.appendChild (.-body js/document) data-link)
    (.click data-link)
    (.removeChild (.-body js/document) data-link)
    ))

(defn download-btn
  []
  [:div
   [:button.btn {:on-click download-gbs} "download"]])
;; (defn gbs-graph []
;;   (let [flowgraph-data (get @app-state :flowgraph-demo)]
;;     [flowgraph flowgraph-data
;;      :layout-width 900
;;      :layout-height 900
;;      :branch-fn #(when (seq? %) %)
;;      :childs-fn #(when (seq? %) %)
;;      :render-fn (fn [n] [:div {:style {:border "1px solid black"
;;                                        :padding "8px"
;;                                        :border-radius "8px"}}
;;                          ])
;;      ]))


;; from https://blog.klipse.tech/visualization/2021/02/16/graph-playground-cytoscape.html
(def ^:dynamic *default-graph-options* 
  {:style [{:selector "node"
            :style {:background-color "#666"
                    :label "data(label)"}}
           {:selector "edge"
            :style {"width" 2
                    :line-color "#ccc"
                    :target-arrow-color "#ccc"
                    :curve-style "bezier"
                    :target-arrow-shape "triangle"
                    :label "data(label)"}}]
   :layout {:name "circle"}
   :userZoomingEnabled false
   :userPanningEnabled false
   :boxSelectionEnabled false})

(defn cytoscape-graph [data]
  
  data)

(defn convert-seq-to-vec-for-display [data]
  (cond
    (string? data) data
    (keyword? data) data
    (symbol? data) data
    (number? data) data
    (boolean? data) data
    (vector? data)
    (into (vector)
          (map (fn [val]
                 (convert-seq-to-vec-for-display val))
               data))
    (seq? data)
    (into (vector)
          (map (fn [val]
                 (convert-seq-to-vec-for-display val))
               data))
    (set? data)
    data
    (map? data)
    (into (hash-map)
           (map (fn [[key val]]
                  [key (convert-seq-to-vec-for-display val)])
                data))
    :else (type data)))

(defn display-gbs []
  (let [gen-state (:gbs-output @app-state)]
    [:div
     (cytoscape-graph gen-state)
     [:hr]
     (json->hiccup (clj->js gen-state))
     [:hr]
     (coll-pen.core/draw (convert-seq-to-vec-for-display gen-state)
                         {:el-per-page 30 :truncate false })
     [:hr]
     (.stringify js/JSON (clj->js gen-state))])
  
   ;; (with-out-str) (cljs.pprint/pprint)
   ;; (clj->js (:gbs-output @app-state))
   )
;(.stringify js/JSON)

(defn app []
  [:div
   [header]
   [download-btn]
   [counter]
   [display-gbs]
   ])
