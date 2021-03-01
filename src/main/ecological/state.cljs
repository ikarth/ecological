(ns ecological.state
  (:require [reagent.core :refer [atom]]))

(defonce app-state
  (atom {:count 0
         :gbs-output {:author "ecological generator 2021"
                      :name "Generated Game Boy ROM",
                      :_version "2.0.0",
                      :settings
                      {:showCollisions true
                       :showConnections true}
                      :scenes {}
                      }}))
