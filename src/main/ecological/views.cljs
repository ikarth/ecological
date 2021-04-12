(ns ecological.views
  (:require [ecological.state :refer [app-state]]
            [ecological.events :refer [increment decrement graph-display run-generator]]
            [reagent.core :as r]
            [cljs.pprint]
            [json-html.core :refer [json->hiccup json->html edn->html]]
            [coll-pen.core]
            [clojure.walk]
            [clojure.string]
            [goog.crypt :as crypt]
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

; (.stringify js/JSON (:gbs-output @app-state))
;(.stringify js/JSON (clj->js {:data "test data"}))

 (defn generate-btn
  []
  [:div
   [:button.btn {:on-click #(run-generator %)} "generate"]]
  )

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
               (mapv (fn [index]
                       (if (collision-flip-mode index bits)
                           ^{:key index}
                           [:rect {:r 8
                                   :width 8
                                   :height 8
                                   :fill-opacity 0.6
                                   :fill (if (collision-flip-mode index bits) "red" "blue")
                                   :x (+ 0 (* 8 (rem index width)))
                                   :y (+ 0 (* 8 (quot index width)))
                                   }
                            ;;[:title (str index " | " (collision-flip-mode index bits true))]
                            ]
                           nil)
                       ))
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
   [generate-btn]
   ;[counter]
   [display-gbs]
   [:hr]
   ;; (coll-pen.core/draw (:data @app-state)
   ;;                       {:el-per-page 30 :truncate false })   
   ])
