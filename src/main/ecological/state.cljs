(ns ecological.state
  (:require [reagent.core :refer [atom]]))

(defonce app-state (atom {:count 0}))
