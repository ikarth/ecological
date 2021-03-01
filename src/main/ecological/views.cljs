(ns ecological.views
  (:require [ecological.state :refer [app-state]]
            [ecological.events :refer [increment decrement]]
            [reagent.core :as r]
            [cljs.pprint]
            ;[reagent-flowgraph.core :refer [flowgraph]]
            ))

(defn header
  []
  [:div
   [:h1 "A template for reagent apps"]])

(defn counter
  []
  [:div
   [:button.btn {:on-click #(decrement %)} "-"]
   [:button {:disabled true} (get @app-state :count)]
   [:button.btn {:on-click #(increment %)} "+"]])

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

(defn display-gbs []
  [:p
   (with-out-str (cljs.pprint/pprint (:gbs-output @app-state)))
   ])

(defn app []
  [:div
   [header]
   [counter]
   [display-gbs]
   ])
