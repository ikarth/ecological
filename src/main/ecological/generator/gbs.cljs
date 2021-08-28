(ns ecological.generator.gbs
  (:require [datascript.core :as d]
            [ecological.generator.gbs-moves :as moves] ))


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
   moves/move-blend-blend
   ])

(def initial-transaction
  [])
