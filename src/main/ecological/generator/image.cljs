(ns ecological.generator.image
  (:require [datascript.core :as d]
            [ecological.generator.image.moves :as moves]
            [ecological.generator.image.export :as export]
            ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; (defn export-all-images [db-conn]
;;   (let [element-labels ["_datascript_internal_id"
;;                         "uuid"
;;                         "size"
;;                         "image-data"
;;                         "timestamp"
;;                         "cause"]
;;         elements
;;         (d/q '[:find ?element ?uuid ?size ?raster ?timestamp ?cause
;;                :in $
;;                :where
;;                [?element :raster/uuid ?uuid]
;;                [?element :raster/size ?size]
;;                [?element :raster/image ?raster]
;;                [?element :entity/timestamp ?timestamp]
;;                [?element :province/cause ?cause]
;;                ]
;;              @db-conn)]
;;     (js/console.log elements)
;;     (map #(zipmap element-labels %)
;;          elements)))

;; (defn export-output
;;   "Export the result after applying the design moves."
;;   [db-conn]
;;   (js/console.log @db-conn)
;;   (-> {}
;;       (update :raster #(export-all-images db-conn))
;;       ;;(update :output-image #(export-result-image db-conn))
;;       ))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def db-schema
  {:raster/image    {:db/cardinality :db.cardinality/one}
   :raster/size      {:db/cardinality :db.cardinality/one}
   :raster/uuid      {:db/cardinality :db.cardinality/one}
   :func/func-handle {:db/cardinality :db.cardinality/one}
   :func/parameters  {:db/cardinality :db.cardinality/one}
   :func/uuid        {:db/cardinality :db.cardinality/one}
   })

(def db-conn (d/create-conn db-schema))

(def design-moves
  [;move-generate-blank-image-function
   moves/move-generate-blank-raster-image
   moves/move-generate-perlin-noise-raster
   moves/move-generate-uv-pattern-raster
   moves/move-generate-test-pattern-raster
   moves/move-generate-random-noise-raster
   moves/move-filter-threshold
   moves/move-filter-blur
   moves/move-filter-posterize
   moves/move-blend-blend
   ])

(def initial-transaction
  [])


(def records
  {:db-conn db-conn
   :db-schema db-schema
   :design-moves design-moves
   :exporter #(export/export-output db-conn)
   :export-most-recent #(export/export-most-recent-artifact db-conn)
   :initial-transaction initial-transaction
   })
