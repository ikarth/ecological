(ns ecological.views
  (:require [ecological.state :refer [app-state]]
            [ecological.events :refer [increment decrement graph-display]]
            [reagent.core :as r]
            [cljs.pprint]
            [json-html.core :refer [json->hiccup json->html edn->html]]
                                        ;[reagent-flowgraph.core :refer [flowgraph]]
            [coll-pen.core]
            [erinite.template.core :as t]
            ;; [cljs.core.match :refer-macros [match]]
            [clojure.walk]
            [clojure.string]
             
            ))

(defn header
  []
  [:div
   [:h1 "Ecological Generator Output Visualization"]])

(defn graph-display-button []
  [:div
   [:button.btn {:on-click #(graph-display %)} "display"]])

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

(def tranformations
  {[:div :td.jh-value :span.jh-type-string] [:append-content :test]})

(defn augment-project-viz [proj]
  (js/console.log proj)
  (let [template proj]
    ((t/compile-template proj tranformations)
     {:test "text"})))


(defn convert-data-for-display
  "Converts data structures to a format that can be completely explored via coll-pen."
  [data]
  (cond
    (string? data) data
    (keyword? data) data
    (symbol? data) data
    (number? data) data
    (boolean? data) data
    (vector? data)
    (into (vector)
          (map (fn [val]
                 (convert-data-for-display val))
               data))
    (seq? data)
    (into (vector)
          (map (fn [val]
                 (convert-data-for-display val))
               data))
    (set? data)
    data ; TODO: also handle sets
    (map? data)
    (into (hash-map)
           (map (fn [[key val]]
                  [key (convert-data-for-display val)])
                data))
    :else (type data)))

(defn add-viz [proj]
  (js/console.log proj)
  (let []
    (-> proj
        (update-in
         [:backgrounds]
         (fn [elements]
           (map
            (fn [elmt]
              (assoc-in
               elmt
               [:viz-image]
               (get elmt "filename" "no image found")))
            elements)
           )))))

(defn convert-viz [hic]
  (clojure.walk/postwalk
   (fn [data]
     (cond
       (string? data)
       (if (and
            (clojure.string/includes? data ".png")
            (or (clojure.string/includes? data "./")
                (clojure.string/includes? data ".\\")))
         [:div
          ;data [:br]
          [:img {:src data}]]
         data)
       :else data)
     )
   hic))

(defn display-gbs []
  (let [gen-state (:gbs-output @app-state)]
    [:div
     ;[graph-display-button]
     ;[:div#artifact-graph {:style {:min-height "100px"}}]
     [:hr]
     (convert-viz (json->hiccup (clj->js gen-state)))
     [:hr]
     (coll-pen.core/draw (convert-data-for-display gen-state)
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
   [:hr]
   (coll-pen.core/draw (:data @app-state)
                         {:el-per-page 30 :truncate false })   
   ])
