(ns ecological.generator.image.export
  (:require [datascript.core :as d]
            [clojure.string]
            ))


;; (defn export-image
;;   "Given an image in the database, export it in a way that can be shown in browser or saved to disk."
;;   []
;;   ;; TODO
;;   )

;; (defn export-result-image
;;   [db-conn]
;;   ;; TODO
;;   []
;;   )

(defn export-all-images [db-conn]
  (let [element-labels ["_datascript_internal_id"
                        "uuid"
                        "size"
                        "image-data"
                        "timestamp"
                        "cause"]
        elements
        (d/q '[:find ?element ?uuid ?size ?raster ?timestamp ?cause
               :in $
               :where
               [?element :raster/uuid ?uuid]
               [?element :raster/size ?size]
               [?element :raster/image ?raster]
               [?element :entity/timestamp ?timestamp]
               [?element :province/cause ?cause]
               ]
             @db-conn)]
    (map #(zipmap element-labels %)
         elements)))

(defn export-most-recent-image [db-conn]
  (let [element-labels ["_datascript_internal_id"
                        "uuid"
                        "size"
                        "image-data"
                        "timestamp"
                        "cause"
                        ]
        elements
        (d/q '[:find ?element ?uuid ?size ?raster (max ?timestamp) ?cause ;; this is only supposed to return one result but it is returning all instead of max, suggesting that my understanding of this is flawed
               :in $
               :where
               [?element :raster/uuid ?uuid]
               [?element :raster/size ?size]
               [?element :raster/image ?raster]
               [?element :entity/timestamp ?timestamp]
               [?element :province/cause ?cause]
               ]
             @db-conn)]
    (map #(zipmap element-labels %)
         elements)))



(defn export-most-recent-artifact [db-conn]
  (let [all-images (export-all-images db-conn)
        ;most-recent (export-most-recent-image)
        images-sorted (sort-by #(get-in % ["timestamp"]) > all-images)]
    ;(js/console.log most-recent)
    ;(js/console.log images-sorted)
    ;(assert (= (first most-recent) (first images-sorted)) (str "Mismatch between " (first most-recent) " and " (first images-sorted)))
    ;(first most-recent)
    (first images-sorted)))

(defn export-output
  "Export the result after applying the design moves."
  [db-conn]
  (-> {}
      (update :raster #(export-all-images db-conn))
      ;;(update :output-image #(export-result-image db-conn))
      ))
