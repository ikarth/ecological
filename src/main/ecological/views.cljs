(ns ecological.views
  (:require [ecological.state :refer [app-state]]
            [ecological.events :refer [increment decrement graph-display run-generator select-move]]
            [reagent.core :as r]
            [cljs.pprint]
            [json-html.core :refer [json->hiccup json->html edn->html]]
            [coll-pen.core]
            [clojure.walk]
            [clojure.string]
            [goog.crypt :as crypt]
            [re-com.core :as rc]
            [re-com.box :as rc-box]
            ;[clojure.core.matrix :as matrix]
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



(defn manual-operation
  "An interface for manually interacting with design moves."
  []
  [:div
   [:div.view-box.selection-list
    [:ul
     (for [move (distinct
                 (map (fn [m]
                        (:name m)) (:all-moves @app-state)))] 
       ^{:key move} [:li move])]]]
  [:div])

(defn add-unique-key-from-index [data]
  (for [element (map-indexed vector data)]
    (with-meta (second element) 
      {:key (first element)})))

(defn pretty-print-query [query]
  (pr-str query)
  ;; (map (fn [qelm]
  ;;        (cond 
  ;;              (keyword? qelm)
  ;;              [:h5.f5 (pr-str qelm)]
  ;;              :else
  ;;              (pr-str qelm)               
  ;;              )
  ;;        )
  ;;      query)
  (add-unique-key-from-index
   (for [qelm query]
     (cond
       (keyword? qelm)
       [:span.b [:br] (pr-str qelm) " "]
       (coll? qelm)
       [:span
        [:br]
        "[ "
        (map (fn [qe] ^{:key (pr-str qe)} [:span (pr-str qe) " "]) qelm)
        "]"]
       :else
       ^{:key (pr-str qelm)} [:span.i " " (pr-str qelm)]))))

(defn list-move-bindings [design-move]
  "bindings"
  )

;; (def panel-style  (merge (flex-child-style "1")
                         ;; {:background-color "#fff4f4"
                          ;; :border           "1px solid lightgray"
                          ;; :border-radius    "4px"
                          ;; :padding          "0px"
                          ;; :overflow         "hidden"}))
(defn operation-harness
  "The interface for the generative operation test harness. The user can select the operation to perform and the input to send to it and get a preview of the results."
  []
  (let [selected-move (:selected-move @app-state)]
    [:div
     [:div.dt.dt--fixed
      [:div.dtc.tc.pa3.pv1.bg-black-10
       [:ul.list.pl0.ml0.center.mw6.ba.b--light--silver.br2
        (for [move (:all-moves @app-state)]
          (if (= (:name selected-move) (:name move))
            ^{:key (:name move)} [:li.pv2.bg-orange.stripe-dark.hover-bg-gold.active-bg-gold.pointer {:on-click #(select-move % move)} (:name move)]
            ^{:key (:name move)} [:li.pv2.hover-bg-gold.active-bg-gold.pointer {:on-click #(select-move % move)} (:name move)]
            ))]]
      
      [:div.dtc.tc.pa3.pv2.bg-black-05.pa
       [:h3.f3.mt0 (if selected-move (:name selected-move) "Design Move")]
       [:p.tl-l
        (if selected-move (:comment selected-move) "")]
       [:p.tl-l
        (if selected-move (list-move-bindings selected-move) "")]]
      [:div.dtc.tc.pv4.bg-black-10
       [:p
        (if selected-move (pretty-print-query (:query selected-move)) "Query")]]]
     [:div.dt.dt--fixed
      [:div.dtc.tc.pv4.bg-black-05
       [:div.h5.overflow-auto
         (if selected-move
           (let [move-name   (:name selected-move)
                 valid-moves (filter #(= (get (get % :move) :name) move-name) (:possible-moves @app-state))
                 ]
            [:div.ph3
             (if (< 0 (count valid-moves))
               [:ul.list.pl0.ml0.center.mw6.ba.b--list--silver.br2
                (for [vmove (map-indexed vector valid-moves)]
                  ^{:key (first vmove)}
                  [(if (odd? (first vmove))
                     :li.pointer.hover-bg-gold.active-bg-gold.pv1.bg-black-05
                     :li.pointer.hover-bg-gold.active-bg-gold.pv1.bg-black-10)
                   [:span.f7 (str (get-in (second vmove) [:move :name]))]
                   [:br]
                   (str (get (second vmove) :vars))])]
               "no matching possible choices")])
           "no move selected")]]
      [:div.dtc.tc.pv4.bg-black-10
        [:button "Perform Move"]
       ]
      [:div.dtc.tc.pv4.bg-black-05

       ]]]))

; (.stringify js/JSON (:gbs-output @app-state))
;(.stringify js/JSON (clj->js {:data "test data"}))

(defn constraint-solving-test-btn
  []
  [:div
   ;[:button.btn {:on-click #(test-constraint-solving %)} "test constraint solver"]
   ])

(defn generate-btn
  []
  [:div
   [:button.btn {:on-click #(run-generator %)} "generate"]]
   )

(defn http-post! [path body cb]
  (let [req (js/XMLHttpRequest.)]
    (.addEventListener req "load" #(this-as this (cb this)))
    (.open req "POST" path)
    (.send req body)))

(defn run-gbs [_]
  (js/console.log "RUN GBS")
  (let [gen-state (:gbs-output @app-state)
        gbs-json (.stringify js/JSON (clj->js gen-state))
        ]
    (http-post! "http://localhost:8081/rungbs" gbs-json
                (fn [whatever] (js/console.log whatever)))))

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

(defn run-gbs-btn
  []
  [:div
   [:button.btn {:on-click run-gbs} "run GB Studio"]])

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

(defn bytes-to-bools [array-of-bytes]
  (clojure.string/join
   (mapv
    (fn [b-val]
      (cljs.pprint/cl-format nil (str "~" 8 ",'0d") (.toString b-val 2)))
    array-of-bytes)))

(defn hex-string-to-byte [h-val]
  (first (js->clj (crypt/hexToByteArray h-val))))

(defn hex-string-to-bools [h-val]
  (mapv #(js/parseInt (first %))
        (partition 1
                   (bytes-to-bools
                    [(hex-string-to-byte h-val)]))))


;; https://github.com/chrismaltby/gb-studio/blob/9cc3b4d341a6db6a6c10f7f35fb7f60e969f1d8c/src/consts.js#L50
;; export const COLLISION_TOP = 0x1;
;; export const COLLISION_BOTTOM = 0x2;
;; export const COLLISION_LEFT = 0x4;
;; export const COLLISION_RIGHT = 0x8;
;; export const COLLISION_ALL = 0xF;
;; export const TILE_PROP_LADDER = 0x10;
;; export const TILE_PROPS = 0xF0;

(def collision-conversion
  {0x1 :top
   0x2 :bottom
   0x4 :left
   0x8 :right
   0xF :all
   0x10 :ladder
   0xF0 :prop})

(def collision-visual
  {:top    {:width  8 :height  4  :fill "blue"   :x 0 :y 0}
   :bottom {:width  8 :height  4  :fill "yellow" :x 0 :y 4}
   :left   {:width  4 :height  8  :fill "pink"   :x 0 :y 0}
   :right  {:width  4 :height  8  :fill "cyan"   :x 4 :y 0}
   :all    {:width  8 :height  8  :fill "red"    :x 0 :y 0}
   :ladder {:width  4 :height  8  :fill "green"  :x 2 :y 0}
   :prop   {:width  4 :height  4  :fill "lime"   :x 2 :y 2}})

(def gbs-version 2.0)

;;; for GBS 2.0 beta v4
(defn collision-flip-mode-2
  ([index collision-map]
   (collision-flip-mode-2 index collision-map false))
  ([index collision-map return-map?]
   (if (= 0 (count collision-map))
     (let [] true)
     (let [cell (nth collision-map index)
           viz (get collision-visual (get collision-conversion cell false) false)]
       (if return-map?
         viz
         (map? viz))))))

;;; for GBS 1.2
(defn collision-flip-mode
  ([index collision-map]
   (collision-flip-mode index collision-map false))
  ([index collision-map return-map?]
   (if (= 0 (count collision-map))
     (let []
       true)
     (let [c-byte   (nth collision-map (unsigned-bit-shift-right index 3))
           c-offset (bit-and index 7)
           c-mask   (bit-shift-left 1 c-offset)
           c-val    (bit-and c-byte c-mask)
           ]
       (if return-map?
         (str c-byte " -> offset: " c-offset " -> mask:" c-mask " -> val:" c-val)
         (> c-val 0))))))

(defn render-bits-viz [data]
  (let [data-segments (clojure.string/split data #"\|")]
    ;;(js/console.log data-segments)
    (if (<= 4 (count data-segments))
      (let [height (nth data-segments 1)
            width (nth data-segments 0)
            just-data (nth data-segments 2)
            image-path (nth data-segments 3)
            bits (into [] (flatten (mapv hex-string-to-byte (mapv clojure.string/join (partition 2 just-data)))))
            draw-index (range (* width height))]
        ;(js/console.log bits)
        ;;(js/console.log image-path)
        ;"http://localhost:8020/collisions-viz%7C20%7C18%7Ce10000fe0770e000030c30cf81f118108903913030000f02e0207ee0e30302ff30f00780ff0ff8ffffffffffff%7C./data/assets/backgrounds/Forest_01_2c.png"
        [:div
        ;; [:p (str image-path)]
         ;;[:p (clojure.string/join " " bits)]
         [:svg {:style {:background "pink" :width (str (* 8 width) "px") :height (str (* 8 height) "px")}}
          [:image {:href image-path :width (str (* 8 width) "px") :height (str (* 8 height) "px") :preserveAspectRatio "xMinYMin" :x 0 :y 0 }]
          (->> draw-index
               (mapv
                (fn [index]
                  (if (>= gbs-version 2.0)
                    (let [c-state (collision-flip-mode-2 index bits true)] 
                      (if (map? c-state)
                        ^{:key index}
                        [:rect {:width (get c-state :width 8)
                                :height (get c-state :height 8)
                                :fill-opacity 0.4
                                :fill (get c-state :fill "purple")
                                :x (+ (get c-state :x 0) (* 8 (rem index width)))
                                :y (+ (get c-state :y 0) (* 8 (quot index width)))}]
                        nil
                        ))
                    (if (collision-flip-mode index bits) ; GBS 1.2
                      ^{:key index} [:rect {:r 8
                                            :width 8
                                            :height 8
                                            :fill-opacity 0.6
                                            :fill (if (collision-flip-mode index bits) "red" "blue")
                                            :x (+ 0 (* 8 (rem index width)))
                                            :y (+ 0 (* 8 (quot index width)))
                                            }]
                      nil))))
               (filter #(not (nil? %)))
               (into (list))
               )
          ]]
        )
      (str (count data-segments));data
      )))

(defn convert-viz [hic]
  (clojure.walk/postwalk
   (fn [data]
     (cond
       (string? data)
       (cond
         ;; fixme: send a sensibler data exchange format, instead of a weird string
         (clojure.string/includes? data "collisions-viz|")
         (let [just-data (subs data (count "collisions-viz|") (count data))]
           (if (< 0 (count just-data))
             (render-bits-viz just-data)
             data))
         (and (clojure.string/includes? data ".png")
              (or (clojure.string/includes? data "./")
                  (clojure.string/includes? data ".\\")))
         [:div [:img {:src data}]]
          :else data)
       :else data)
     )
   hic))

(defn filter-gen-state [g-state]
  (clojure.walk/postwalk
   (fn [node]
     (cond
       (and (map? node) (contains? node "collisions"))
       (dissoc node "collisions" "editor-position")
       :else node)
     )
   g-state))

(defn display-gbs []
  (let [gen-state (:gbs-output @app-state)]
    [:div
     ;[graph-display-button]
     ;[:div#artifact-graph {:style {:min-height "100px"}}]
     [:hr]
     (convert-viz (json->hiccup (clj->js (filter-gen-state gen-state))))
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
   [manual-operation]
   [operation-harness]
   [generate-btn]
   [constraint-solving-test-btn]
   [run-gbs-btn]
   ;[counter]
   [display-gbs]
   [:hr]
   ;; (coll-pen.core/draw (:data @app-state)
   ;;                       {:el-per-page 30 :truncate false })
   (js/console.log (:data @app-state))
   (js/console.log (:selected-move @app-state))
   (.stringify js/JSON (clj->js (:data @app-state)))
   [:hr]
   (.stringify js/JSON (clj->js (:possible-moves @app-state)))
   [:br]
   [:hr]
   ])
    
