(ns ecological.views
  (:require [ecological.state :refer [app-state]]
            [ecological.events :refer [increment
                                       decrement
                                       graph-display
                                       run-generator
                                       select-move
                                       select-bound-move
                                       perform-bound-move
                                       perform-random-move
                                       select-random-bindings
                                       select-tab
                                       init-database
                                       set-active-sketch
                                       get-current-parameters
                                       alter-bound-move-parameters
                                       is-current-move-altered
                                       ]]
            [reagent.core :as r]
            [reagent.dom :as rd]
            [cljs.pprint]
            [json-html.core :refer [json->hiccup json->html edn->html]]
            [coll-pen.core]
            [clojure.walk]
            [clojure.string]
            [goog.crypt :as crypt]
            [re-com.core :as rc]
            [re-com.box :as rc-box]
            [re-com.util :as rc-util]
            [quil.core :as qc]
            [quil.middleware :as qm]
            [cljs.core.async :as a]
            ;;[clojure.core.matrix :as matrix]
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

(defn truncate-if-too-long
  "Return "
  [to-truncate max-length]     
  (let [trunc (clojure.string/join "" to-truncate)]
    (if (< (count trunc) max-length)
      trunc
      (concat "" (subs trunc 0 (min (count trunc) max-length)) "..." ))))

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
  "[bindings to be described here]"
  (if-let [params (get design-move :parameters)]
    (let [current-values (get-current-parameters design-move false)
          current-move-is-altered (is-current-move-altered)
          ]
      [:div.stripy
       (for [[val-name pram] params]
         (let [val-updated (get current-values val-name)
               val-default (if val-updated val-updated (:default pram))
               val-range   (:range   pram)
               val-step    (:step    pram)
               val-prec    (:precision pram)
               val-form    (:form    pram)]
           ^{:key val-name}
           [:div.stripy.pa1
            ;{:style {:background-color "#464646"}}
            ;;(println val-range)            
            (cond
              (= val-form :enum)
              [:select.pr2 {:default-value "";val-default
                            :on-change (fn [event] (alter-bound-move-parameters event
                                                                                [val-name 0]
                                                                                design-move val-range))}
               (for [[i p] (map-indexed vector (into [""] val-range))]
                 ^{:key i}
                 [:option (str p)]
                 )
               ]
              (or (= val-form :scalar) (= val-form :vector2))
              (for [[i p] (map-indexed vector val-range)]
                ^{:key i}
                [:div.dib.pa0.pl1.pr1
                 ;; (println "for")
                 ;; (println  (min (if (coll? val-range) (nth val-range i) val-range)))
                 ;; (println  (if (coll? val-range) (nth val-range i) val-range))
                 ;; (println  (if (coll? val-range) (nth val-range i) nil))
                 ;; (println  val-range)
                 ;; (js/console.log pram)
                 [:input.parameters
                  {:style {:width "6em" :height "1.8em"}
                   :type "number"
                   ;;:pattern "[0-9a-z]"
                   :step (if val-step val-step 1)
                   :precision (if val-prec val-prec 4)
                   ;;:min (apply min (if (coll? val-range) (nth val-range i) val-range))
                   ;;:max (apply max (if (coll? val-range) (nth val-range i) val-range))
                   :value (if (coll? val-default)
                                    (nth val-default i)
                                    val-default)
                   :on-change (fn [event]
                                (alter-bound-move-parameters event
                                                             [val-name i]
                                                             design-move val-range))}]])
              :else
              (str val-default))
            [:span.b
             (str (name val-name))]]))
       [:div.tc
        (if current-move-is-altered
          [:button.btn.grow.ma2.bg-green {:on-click (fn [event]
                                                      ;(select-move event design-move :clear-altered false)
                                                      (if (select-random-bindings :clear-altered false)
                                                        (perform-bound-move event)))} "Perform Move"]
          [:button.btn.grow.ma2.bg-grey "Perform Move"])
        [:button.btn.grow.ma2.bg-blue {:on-click #(if (select-random-bindings :clear-altered false) (perform-bound-move %))} "Randomize Move"]
        ]
       ])
    "No params?"
    ))



;; (def panel-style  (merge (flex-child-style "1")
                         ;; {:background-color "#fff4f4"
                          ;; :border           "1px solid lightgray"
                          ;; :border-radius    "4px"
                          ;; :padding          "0px"
;; :overflow         "hidden"}))

(defn valid-move-count [selected-move poss-moves]
  (let [move-name   (:name selected-move)
        valid-moves (filter #(= (get (get % :move) :name) move-name) poss-moves)]
    (count valid-moves)))

(defn operation-harness
  "The interface for the generative operation test harness. The user can select the operation to perform and the input to send to it and get a preview of the results."
  []
  (let [selected-move (:selected-move @app-state)
        bound-move (:selected-bound-move @app-state)
        possible-moves (:possible-moves @app-state)]
    [:div
     [:div.dt.dt--fixed
      [:div.dtc.tc.pa3.pv1.bg-black-10
       [:ul.list.pl0.ml0.center.mw6.ba.b--light--silver.br2
        (for [vecmove (map-indexed vector (:all-moves @app-state))]
          (let [move (second vecmove)
                move-index (first vecmove)]
            (let [move-li-key
                  (cond
                    (= (:name selected-move) (:name move))
                    :li.pv2.pointer.hover-bg-yellow.active-bg-gold.bg-orange
                    (odd? move-index)
                    :li.pv2.pointer.hover-bg-gold.bg-black-10
                    :else
                    :li.pv2.pointer.hover-bg-gold.bg-black-05
                    )]
              (let [vmc (valid-move-count move possible-moves)]
                ^{:key (:name move)}
                [move-li-key {:on-click #(select-move % move)}
                 (:name move)                 
                 " (" vmc ") "
                 (if (< 0 vmc)
                   [:div.dib.pa0.pointer.center.b--light--silver.hover-bg-red.shadow-hover.ba
                    {:style {:width "10%" :height "60%" :left "1em"} 
                     :on-click (fn [event]
                                 (select-move event move)
                                 (if (select-random-bindings)
                                   (perform-bound-move event)))}
                    "🎲"])]))))]]
      [:div.dtc.tc.pa3.pv2.bg-black-05.pa
       [:h3.f3.mt0 (if selected-move (:name selected-move) "Design Move")]
       [:p.tl-l
        (if selected-move (:comment selected-move) "")]
       [:div.tl-l
        (if selected-move (list-move-bindings selected-move) "")]]
      [:div.dtc.tc.pv4.bg-black-10
       [:p
        (if selected-move (pretty-print-query (:query selected-move)) "Query")]]]
     [:div.dt.dt--fixed
      [:div.dtc.tc.pv4.bg-black-05
       [:div.h5.overflow-auto
         (if selected-move
           (let [move-name   (:name selected-move)
                 valid-moves (filter #(= (get (get % :move) :name) move-name) possible-moves)
                ]
            [:div.ph3
             (if (< 0 (count valid-moves))
               [:ul.list.pl0.ml0.center.mw6.ba.b--list--silver.br2
                (for [vmove (map-indexed vector valid-moves)]
                  ^{:key (first vmove)}
                  [(if (and
                        (= (first vmove) (first bound-move))
                        (= (get-in (second vmove) [:move :name]) (get-in (second bound-move) [:move :name])))
                     :li.pointer.hover-bg-yellow.active-bg-gold.pv1.bg-orange
                     (if (odd? (first vmove))
                       :li.pointer.hover-bg-gold.active-bg-gold.pv1.bg-black-05
                       :li.pointer.hover-bg-gold.active-bg-gold.pv1.bg-black-10))
                   {:on-click #(select-bound-move % vmove)}
                   [:span.f7 (str (get-in (second vmove) [:move :name]))]
                   [:br]
                   (truncate-if-too-long (str (get (second vmove) :vars)) 61)])]
               "no matching possible choices")])
           "no move selected")]]
      [:div.dtc.tc.pv4.bg-black-10
       [:button.btn.grow.ma2 {:on-click #(perform-random-move %)} "Perform random design move" ]
       [:button.btn.grow.ma2 {:on-click #(perform-bound-move %)} "Perform selected move (with selected bindings)"]
       [:button.btn.grow.ma2 {:on-click #(if (select-random-bindings)
                                           (perform-bound-move %))}
        "Perform Selected Move (random bindings)"]
       ]
      [:div.dtc.tc.pv4.bg-black-05

       ]]]))



(defn constraint-solving-test-btn
  []
  [:div
   ;[:button.btn {:on-click #(test-constraint-solving %)} "test constraint solver"]
   ])

(defn image-generate-btn
  []
  [:div
   [:button.btn.ma2.grow.bg-green.white.bold.hover-bg-gold {:on-click #(init-database %)} "start empty project"]
   [:button.btn.ma2.grow.bg-light-yellow.hover-bg-gold {:on-click #(run-generator %)} "generate complete project"]])

(defn generate-btn
  []
  [:div
   [:button.btn.ma2.grow.bg-green.white.bold.hover-bg-gold {:on-click #(init-database %)} "start empty project"]
   [:button.btn.ma2.grow.bg-light-yellow.hover-bg-gold {:on-click #(run-generator %)} "generate complete project"]]
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
   [:button.btn.ma2.grow {:on-click run-gbs} "run GB Studio"]])

(defn download-btn
  []
  [:div
   [:button.btn.grow {:on-click download-gbs} "download"]])
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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Inspired by
;; https://github.com/simon-katz/nomisdraw/blob/master/src/cljs/nomisdraw/utils/nomis_quil_on_reagent.cljs
(defn q-sketch
  [& {:as sketch-args}]
  (let [host (if (not (contains? sketch-args :host))
               (str (random-uuid))
               (if (not (= nil (:host sketch-args)))
                 (:host sketch-args)
                 (str (random-uuid))))
        size (:size sketch-args)
        _ (assert (or (nil? size) (and (vector? size) (= (count size) 2)))
                  (str ":size should be nil or a vector of size 2, but instead it is " size))
        [w h] (if (nil? size) [600 600] size)
        canvas-id host 
        canvas-tag-&-id (keyword (str "div#" canvas-id))
        sketch-args* (merge sketch-args {:host canvas-id})
        saved-sketch-atom (atom ::not-set-yet)
        ]
    ^{:key canvas-id}
    [r/create-class
     {:reagent-render
      (fn []
        [canvas-tag-&-id {:style {:max-width w}
                          :width w
                          :height h}])
      :component-did-mount
      (fn []
        (a/go (reset! saved-sketch-atom (apply qc/sketch (apply concat sketch-args*)))))
      :component-will-unmount
      (fn []
        (a/go-loop [] ; will most likely not be reached
          (if (= @saved-sketch-atom ::not-set-yet)
            (do
              (a/<! (a/timeout 100))
              (recur))
            (qc/with-sketch @saved-sketch-atom
              (qc/exit)))))}]))

(defn visualize-image [img-data & {:keys [image-key use-canvas] :or {image-key (str (random-uuid)) use-canvas false}}]
  (if (undefined? img-data)
    (println "Tried to call (visualize-image) with undefined img-data.")
    (try
      (letfn [(img-size [] [(. img-data -width) (. img-data -height)])
              (initial-state []
                (let [[w h] (img-size)]
                  (qc/resize-sketch w h)
                  (qc/background-image img-data)
                  (qc/no-loop)
                  {:image img-data}))
              (update-state [state]
                state)
              (draw [state]
                (let [img (:image state)]))]
        (if (not use-canvas) ; can display as just the image or as an animated sketch
          (let [html-image-data (. (. img-data -canvas) toDataURL)]
            ^{:key image-key}
            [:img {:src html-image-data}])
          (q-sketch :setup initial-state
                    :update update-state
                    :draw draw
                    :host nil
                    :middleware [qm/fun-mode]
                    :size (img-size)
                    )))
      (catch :default e
        (js/console.log e)))))

(defn display-loose-images [g-state]
  (let [image (get "image-data" g-state)]
    (map (fn [i-entry]
           (if (map? i-entry)
             (let [idat (get i-entry "image-data")]
               (if (undefined? idat)
                 (println (str "Image data undefined for " i-entry))
                 (visualize-image idat {:key (get i-entry "uuid")})))))
         g-state)))

(defn visualize-generator-sketch
  ([w h]
   (visualize-generator-sketch w h nil))
  ([w h name]
   (letfn
       [(initial-state []
          (qc/pixel-density 1)
          {:time 0})
        (update-state [state] (update state :time inc))
        (draw [state]
          (let [t (:time state)]
            (qc/background 80 170 100)
            (qc/fill 100 120 230)
            (qc/ellipse (rem t w) (rem t h) 50 50)))]
     (q-sketch :setup initial-state
               :update update-state
               :draw draw
               :host name
               :middleware [qm/fun-mode]
               :size [w h]))))





;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


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

(defn vector-render-bits-viz
  "Given a vector of GBS collision data, turn it into a visualization"
  [data-segments]
  (let [height (nth data-segments 1)
        width (nth data-segments 0)
        just-data (nth data-segments 2)
        image-path (nth data-segments 3)
        bits (into []
                   (flatten (mapv hex-string-to-byte
                                  (mapv clojure.string/join (partition 2 just-data)))))
        draw-index (range (* width height))]
    [:div ;; TODO
     ])) 

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
        [:div      
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
               (into (list)))]])
      (str (count data-segments)))))

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
         (clojure.string/includes? data "image-data")
         [:div (str data)]
         :else data)
       (and (vector? data) (= (first data) :collisions-viz))
       (vector-render-bits-viz (rest data))
       (and (vector? data) (string? (second data)) (= "data:image" (subs (second data) 0 10)))
       (let []
         [:img {:src (second data)}]
         )
       :else
       (let []
         data)))
   hic))

;; (defn convert-viz-imaging
;;   "Takes a hiccup construct and converts it for display in-browser."
;;   [hic]
;;   (clojure.walk/postwalk
;;    (fn [data]
;;      (cond
;;        ;; (and (vector? data) (= (first data) :imaging))
;;        ;; data
;;        ;; false;(clojure.string/includes? data "image-data")
;;        ;; (let []
;;        ;;   (println (str "converting image data: " data))
;;        ;;   data
;;        ;;   )
;;        :else
;;        (let []
;;          ;(println (str "viz-data: " data))
;;          data)
;;        ))
;;    hic))

(defn filter-gen-state [g-state]
  (clojure.walk/postwalk
     (fn [node]
       (cond
         (and (map? node) (contains? node "collisions"))
         (dissoc node "collisions" "editor-position")
         :else node))
     g-state))

(defn filter-gen-state-img [g-state]
  (clojure.walk/postwalk
     (fn [node]
       (cond
         (and (map? node) (contains? node "collisions"))
         (dissoc node "collisions" "editor-position")
         (and (map-entry? node) (= (first node) "image-data"))
         (let [;;_ (js/console.log node)
               i-canvas (. (second node) -canvas)
               html-image-data (. i-canvas toDataURL)]
           [:image-data html-image-data])
         :else
         (let []           
           node)))
     g-state))

(defn display-imaging []
  (let [img-state (:gbs-output @app-state)]
    ;(js/console.log img-state)
    [:div
     (comment
       (display-loose-images img-state))
     [:div.self-center.content-center.items-center.justify-center.flex.bg-washed-green
      [:div.mw9.ma2
       ;;(println (str "image-state: " img-state))
       (convert-viz (json->hiccup (clj->js (filter-gen-state-img img-state))))]]]))


(defn display-project-element [element count]
  ;; (println "(display-project-element)")
  ;; (println (type element))
  ;; (println element)
  ;; (println count)
  (cond
    (string? element)
    [:li element]
    (coll? element)    
    (for [[ii el] (map-indexed vector element)]
      (let []
        ;; (println el)
        (if el
          ^{:key ii} [:li [:ul.ma0.ml1.mr1 (display-project-element el (if (nil? count) 1 (inc count)))]]))) ;[:div element]
    :else
    [:span (str element)]
           ))

(defn display-project-view
  "Display a view of the important artifacts in the project."
  []
  [:div.dib.pr3.project-view
   [:h4 "Project View"]
   (let [project-view (get @app-state :project-view [])]
     [:ul.pl3.selection-list
      ;[:li.hover-orange.pointer (str {:example 3}) ]
      (for [[i element] (map-indexed vector project-view)]
        ^{:key i}
        [:li.hover-orange.pointer
         (if (coll? element)
           (let []
             [:ul.ma0.ml3.mr3
              (str (first element))
              (display-project-element (second element) 0)])
           (str element))
         ]
        )]
     )])

(defn display-most-recent-artifact []
  [:div.dib
   (let [recent-artifact  (:recent-artifact @app-state)]
     (if (contains? recent-artifact "image-data")
       (let [img-data (get recent-artifact "image-data")]
         (if (undefined? img-data)
           (println "(display-most-recent-artifact) can't find recent-artifact image data")
           (visualize-image img-data {:key "most-recent-artifact"})))))])


(defn display-gbs []
  (let [gen-state (:gbs-output @app-state)]
    [:div.self-center.content-center.items-center.justify-center.flex.bg-lightest-blue
     [:div.mw9.ma2
      (convert-viz (json->hiccup (clj->js (filter-gen-state gen-state))))
      (comment)
      [:hr]
      "display gbs"
      (coll-pen.core/draw (convert-data-for-display gen-state)
                          {:el-per-page 30 :truncate false })
      [:hr]
      (.stringify js/JSON (clj->js gen-state))
      ]]))

(defn image-header []
  [:h1 "Ecological Generator Output Visualization"])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn app []
  (let [image-tab [:div
                   [image-header] ;; TODO: display most recent image here
                   [operation-harness]
                   [image-generate-btn]
                   [display-imaging]
                   [#(visualize-generator-sketch 400 300 "quil-canvas")] ;; NOTE: This is a load-bearing Quil/Processing sketch! Image generation ops don't work without some kind of canvas to use.                   
                   ]
        gbs-tab
        [:div
         [header]
         [operation-harness]
         [generate-btn]
         [constraint-solving-test-btn]
         [display-gbs]
         ]
        tab-defs [{:id nil :label "None" :contents [:div "Select A Generator Above..."]}
                  {:id :tab-image :label "Images" :contents image-tab}
                  {:id :tab-gbs :label "GBS" :contents gbs-tab}]
        selected-tab (get @app-state :selected-tab (first tab-defs))]
    [:div
     [:div.tabs.bg-light-blue      
      (for [tab tab-defs]
        (let []
          (if (:id tab)
            ^{:key (:id tab)}
            [(if (= (:id selected-tab) (:id tab))
               :div.pointer.dib.ma0.pa2.br.bl.bt.br3.br--top.bw2.b--black-60.hover-orange.bg-white
               :div.pointer.dib.ma0.pa2.br.bl.bt.br3.br--top.bw1.b--black-20.hover-orange.bg-black-30)
             {:on-click #(select-tab % tab)} (:label tab)])))]
     [:div
      [:div.dib.ma2.pa2.left-0.top-0.v-top
       [display-project-view]
       [display-most-recent-artifact]]
      [:div.dib.ma2.pa2.left-0.top-0.v-top; {:style "min-height: 200px"}
       (coll-pen.core/draw (:data-view @app-state) {:key :pen-data-display :el-per-page 10 :truncate false})]
      
      (get selected-tab :contents [:div])]]))
    
