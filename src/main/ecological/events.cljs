(ns ecological.events
  (:require [ecological.state :refer [app-state]]
            ;; [ecological.gbstudio.gbstudio :as gbs
             
            ;;  ;; :refer [fetch-gbs
            ;;  ;;                                      fetch-generated-project!
            ;;  ;;                                      fetch-database
            ;;  ;;                                      fetch-possible-moves
            ;;  ;;                                      make-empty-project
            ;;  ;;                                      execute-one-design-move!
            ;;  ;;                                      execute-design-move!
            ;;  ;;                                              ]
            ;;  ]
            ;; [ecological.imaging.imaging :as imaging]
            [ecological.generator.gencore :as generator]
            [ecological.generator.utilities :as util]
            ["clingo-wasm" :default clingo]
            
            ))


;; (def database-interface
;;   {:tab-gbs {:fetch-data-output        gbs/fetch-gbs
;;              :fetch-database           gbs/fetch-database
;;              :fetch-possible-moves     gbs/fetch-possible-moves
;;              :fetch-all-moves          gbs/fetch-all-moves
;;              :make-empty-project       gbs/make-empty-project
;;              :execute-design-move!     gbs/execute-design-move!
;;              :fetch-generated-project! gbs/fetch-generated-project!
;;              :fetch-data-view          gbs/fetch-data-view
;;              :fetch-most-recent-artifact gbs/fetch-most-recent-artifact
;;              }
;;    :tab-image
;;    {:fetch-data-output          imaging/fetch-data-output
;;     :fetch-database             imaging/fetch-database
;;     :fetch-possible-moves       imaging/fetch-possible-moves
;;     :fetch-all-moves            imaging/fetch-all-moves
;;     :make-empty-project         imaging/make-empty-project
;;     :execute-design-move!       imaging/execute-design-move!
;;     :fetch-generated-project!   imaging/fetch-generated-project!
;;     :fetch-data-view            imaging/fetch-data-view
;;     :fetch-most-recent-artifact imaging/fetch-most-recent-artifact
;;     }
;;    })

;; (def generator-interface
;;   {:fetch-data-output          generator/fetch-data-output
;;    :fetch-database             generator/fetch-database
;;    :fetch-possible-moves       generator/fetch-possible-moves
;;    :fetch-all-moves            generator/fetch-all-moves
;;    :make-empty-project         generator/make-empty-project
;;    :execute-design-move!       generator/execute-design-move!
;;    :fetch-generated-project!   generator/fetch-generated-project!
;;    :fetch-data-view            generator/fetch-data-view
;;    :fetch-most-recent-artifact generator/fetch-most-recent-artifact
;;    })

;; (defn interface-with-database [interface-func]
;;   (let [selected-tab (get @app-state :selected-tab {:id :tab-gbs})
;;         _ (assert (not (:map? selected-tab)) (str "No valid tab selected: " selected-tab))
;;         database-label (get selected-tab :id)
;;         _ (assert (keyword? database-label) (str database-label " is not a recognized database name."))
;;         _ (generator/switch-database database-label)        
;;         db-func (get-in generator-interface [interface-func] nil)
;;         ;_ (println (str "db function: " database-label " " interface-func " " db-func))
;;         _ (assert (fn? db-func) (str "(" database-label " " interface-func ") is not a known function"))]
;;     db-func))
 
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
  (let [selected-tab (get @app-state :selected-tab {:id :tab-gbs})
        _ (assert (not (:map? selected-tab)) (str "No valid tab selected: " selected-tab))
        database-label (get selected-tab :id)
        _ (assert (keyword? database-label) (str database-label " is not a recognized database name."))
        _ (generator/switch-database database-label)]
    (generator/make-empty-project database-label)
    ;(println "...")
    ;(js/console.log @app-state)
    (if (not
         (some? (:data @app-state)))
      (println "No data found in app-state?"))
    ;;(println "---")
    (swap! app-state assoc-in [:all-moves] (generator/fetch-all-moves))
    (swap! app-state assoc-in [:gbs-output] (generator/fetch-data-output))
    (swap! app-state assoc-in [:data] (generator/fetch-database))
    (swap! app-state assoc-in [:possible-moves] (generator/fetch-possible-moves))
    (swap! app-state assoc-in [:data-view] (generator/fetch-data-view))
    (swap! app-state assoc-in [:recent-artifact] (generator/fetch-most-recent-artifact))
    
    ))

(defn update-database-view [event]
  (if (some? event)
    (.preventDefault event))
  (let [selected-tab (get @app-state :selected-tab {:id :tab-gbs})
        _ (assert (not (:map? selected-tab)) (str "No valid tab selected: " selected-tab))
        database-label (get selected-tab :id)
        _ (assert (keyword? database-label) (str database-label " is not a recognized database name."))
        _ (generator/switch-database database-label)])
  (swap! app-state assoc-in [:all-moves] (generator/fetch-all-moves))
  (swap! app-state assoc-in [:gbs-output] (generator/fetch-data-output))
  (swap! app-state assoc-in [:possible-moves] (generator/fetch-possible-moves))
  (swap! app-state assoc-in [:data] (generator/fetch-database))
  (swap! app-state assoc-in [:data-view] (generator/fetch-data-view))
  (swap! app-state assoc-in [:recent-artifact] (generator/fetch-most-recent-artifact))
  ;(js/console.log (:data-view @app-state))
  )

(defn select-tab
  [event tab]
  (if (some? event)
    (.preventDefault event))
  (if tab
    (let []
      ;(js/console.log "Selecting" tab)
      (swap! app-state assoc-in [:selected-tab] tab)
      (update-database-view nil)
      ;(swap! app-state update-in [:data] (generator/-with-database :fetch-database))
      )))

(defn get-current-parameters [move randomize-parameters]
  (let [default-state (generator/default-parameters move randomize-parameters)
        altered-state (get @app-state :altered-parameters {})
        total-state (merge default-state   
                           altered-state)]
    ;(println "(get-current-parameters)")
    ;; (println default-state)
    ;(println altered-state)    
    ;(println total-state)
    ;; (println @app-state)
    total-state
    ))

(defn select-move
  "Makes the given design move be the currently selected one."
  [event move]
  (.preventDefault event)
  ;(js/console.log "Selecting" (:name move))
  (swap! app-state assoc-in [:selected-move] move)
  (swap! app-state assoc-in [:altered-parameters] {})
  (swap! app-state assoc-in [:selected-parameters] (get-current-parameters move false))
  (swap! app-state assoc-in [:data-view] (generator/fetch-data-view))
  )


(defn is-current-move-altered []
  (let [cur-alter (get @app-state :altered-parameters)]
    (not (empty? cur-alter))))

(defn alter-bound-move-parameters
  [event [param-name param-index] current-move]
  ;; (if (some? event)
  ;;   (.preventDefault event))
  (js/console.log event)
  (let [current-state (get-current-parameters current-move false)
        old-values (get current-state param-name [-1 -1])
        form (get-in current-move [:parameters param-name :form] :form-not-found)
        new-value-raw (-> event .-target .-value)
        new-value
        (cond
          (not (== new-value-raw new-value-raw))
          (nth old-values param-index)
          (or (= :scalar form) (= :vector2 form))
          (util/string-to-float new-value-raw)
          :else
          new-value-raw
          )]
    ;(println "(alter)")
    ;(println current-move)
    ;(println current-state)
    ;(println @app-state)
    ;(js/console.log form)
    ;(js/console.log new-value)
    (swap! app-state update-in [:altered-parameters]
           (fn [old-parameters]
             ;(js/console.log param-index)
             ;(js/console.log old-parameters)
             (let [new-param
                   (cond
                     (= :scalar form)
                     {param-name [new-value]}
                     (= :vector2 form)
                     {param-name (assoc old-values param-index new-value)}
                     :else
                     {param-name new-value})
                   ]
               ;(println )
               (merge old-parameters new-param)
               )
             )
           )
    ;;(println (:altered-parameters @app-state))
      ;; (swap! app-state update-in [:altered-parameters]
      ;;    (fn [old-parameters]
    ;;      (merge old-parameters new-parameter)))
    (:altered-parameters @app-state)
    )
)

(defn select-bound-move
  [event move]
  (.preventDefault event)
  (js/console.log "Selecting" move)
  (swap! app-state assoc-in [:selected-bound-move] move)
  (swap! app-state assoc-in [:altered-parameters] {})
  (swap! app-state assoc-in [:selected-parameters] (get-current-parameters move false)))

(defn select-random-bindings
  []
  (swap! app-state assoc-in [:possible-moves] (generator/fetch-possible-moves))
  (if (:selected-move @app-state)
    (let [valid-moves (:possible-moves @app-state)
          selected-move-name (get-in (:selected-move @app-state) [:name])
          possible-moves (filter #(= (get-in % [:move :name]) selected-move-name) valid-moves)
          chosen-move (rand-nth possible-moves)]
      (swap! app-state assoc-in [:selected-bound-move] [0 chosen-move])
      (swap! app-state assoc-in [:altered-parameters] {})
      (swap! app-state assoc-in [:selected-parameters] (get-current-parameters chosen-move true))
      true)
    false))

(defn perform-bound-move
  [event]
  ;(println "(perform-bound-move)")
  (if (some? event)
    (.preventDefault event))
                                        ;(js/console.log (:selected-bound-move @app-state))
  (let [randomize-parameters (empty? (get @app-state :altered-parameters))]
    (swap! app-state assoc-in [:selected-parameters] (get-current-parameters (second (:selected-bound-move @app-state))
                                                                             randomize-parameters))
    ;; (println (:selected-bound-move @app-state))
    ;; (println (str "Selected Parameters: " (:selected-parameters @app-state)))
    (let [params (get-current-parameters (:move (second (:selected-bound-move @app-state))) randomize-parameters)]
      ;;(println params)
      (generator/execute-design-move!
       (second (:selected-bound-move @app-state))
       ;;(:selected-parameters @app-state)    ; TODO: get params
       params ;(generator/default-parameters (second (:selected-bound-move @app-state)))
       ))
    (update-database-view nil)
    ;; (js/console.log @app-state)
    ;; (select-tab nil (:selected-tab @app-state))
                                        ;(swap! app-state update-in [:data] (generator/-with-database :fetch-database))
    ))

(defn perform-random-move
    [event]
    (if (some? event)
      (.preventDefault event))
    (let [valid-moves (:possible-moves @app-state)
          chosen-move (if (< 0 (count valid-moves)) (rand-nth valid-moves) nil)
          ]
      ;(println chosen-move)
      (when chosen-move
        (swap! app-state assoc-in [:selected-bound-move] [0 chosen-move])
        (swap! app-state assoc-in [:altered-parameters] {})
        (swap! app-state assoc-in [:selected-parameters] (get-current-parameters chosen-move true))
        (perform-bound-move nil))))

(defn run-generator [event]
  (.preventDefault event)
  (swap! app-state assoc-in [:all-moves] (generator/fetch-all-moves))
  (swap! app-state assoc-in [:gbs-output] (generator/fetch-generated-project!))
  (swap! app-state assoc-in [:data] (generator/fetch-database))
  (swap! app-state assoc-in [:possible-moves] (generator/fetch-possible-moves))
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
