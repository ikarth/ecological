(ns ecological.generator.gbs.assets
  ;; (:require-macros [cljs.core.async.macros :refer [go]])
  ;; (:require [cljs-http.client :as http]
  ;;           [cljs.core.async :refer [<!]])
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
              ))



(defn load-file! [path callback]
  (let [req (js/XMLHttpRequest.)]
    (.addEventListener req "load" #(this-as this (callback this)))
    (.open req "GET" path)
    (.send req)))

(defonce asset-state (atom {}))

;; (load-file! ".\\data\\assets\\asset_manifest.edn"
;;             (fn [response]
;;               (js/console.log "loaded asset manifest")
;;               (swap! asset-state assoc :asset-manifest (edn/read-string (.responseText response)))))

(defn load-manifest []
  (load-file! ".\\data\\asset_manifest.edn"
              (fn [response]
          ;; TODO: should do more error checking               
                (js/console.log "loaded asset manifest")
                (swap! asset-state assoc :asset-manifest (edn/read-string (.-responseText response))))))

(defn load-scene-sources []
  (load-file! ".\\data\\scenes\\Forest.gbsproj"
        (fn [response]
          ;; TODO: should do more error checking
          (js/console.log "loaded forest scenes for templates")
          (let [res-text (.-responseText response)]
            ;(js/console.log res-text)
            (swap! asset-state assoc :scene-manifest (js->clj (js/JSON.parse res-text) :keywordize-keys true)))
          )))

(defn asset-manifest []
  (get @asset-state :asset-manifest []))


(defn scene-manifest []
  (get @asset-state :scene-manifest []))

