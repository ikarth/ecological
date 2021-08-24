(ns ecological.imaging.imaging
  (:require [datascript.core :as d]
            [clojure.string]
            [goog.crypt :as crypt]
            ))



(defn imaging-moves []
  [])



(def imaging-schema
  {:image-matrix {:db/cardinality :db.cardinality/one}})

(def db-conn (d/create-conn imaging-schema))

(defn reset-the-database!
   "For when you want to start over. Returns an empty database that follows the schema."
  []
  (d/reset-conn! db-conn (d/empty-db imaging-schema))
  ;; if there is an initial transaction, it goes here...
  )

(defn export-image
  "Given an image in the database, export it in a way that can be shown in browser or saved to disk."
  []
  ;; TODO
  )


(defn fetch-data-output
  ""
  []
  [])

(defn fetch-database [] )

(defn fetch-possible-moves [])
(defn fetch-some-moves [])

(defn fetch-all-moves
  "Return all of the moves we know about, valid or otherwise"
  []
  (let [moves (imaging-moves)]
    moves))

(defn make-empty-project []
  (reset-the-database!))

(defn execute-design-move! [])
(defn fetch-generated-project! [])
(defn fetch-data-view [])
