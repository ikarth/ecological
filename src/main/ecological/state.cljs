(ns ecological.state
  (:require [reagent.core :refer [atom]]
            ;[ecological.generator.gbs :as gbs]
            ;[ecological.imaging.imaging :as imaging]
            ))

;; (def database-interface
;;   {:tab-gbs {:fetch-data-output        gbs/fetch-gbs
;;              :fetch-database           gbs/fetch-database
;;              :fetch-possible-moves     gbs/fetch-possible-moves
;;              :make-empty-project       gbs/make-empty-project
;;              :execute-design-move!     gbs/execute-design-move!
;;              :fetch-generated-project! gbs/fetch-generated-project!
;;              :fetch-data-view                gbs/fetch-data-view}
;;    :tab-image
;;    {:fetch-data-output        imaging/fetch-data-output
;;     :fetch-database           imaging/fetch-database
;;     :fetch-possible-moves     imaging/fetch-possible-moves
;;     :make-empty-project       imaging/make-empty-project
;;     :execute-design-move!     imaging/execute-design-move!
;;     :fetch-generated-project! imaging/fetch-generated-project!
;;     :fetch-data-view          imaging/fetch-data-view}
;;    })

(defonce app-state
  (atom {:count 0
         :gbs-output nil;(interface-with-database :fetch-data-output)
         :data nil;(interface-with-database :fetch-database)
         :possible-moves nil;(interface-with-database :fetch-possible-moves)
         :all-moves nil;(interface-with-database :fetch-all-moves)
         :selected-moves nil
         ;:selected-tab nil
         :data-image nil
         :output-image nil
          }))

;; (defn interface-with-database [interface-func]
;;   (let [selected-tab (get @app-state :selected-tab :tab-gbs)]
;;     (if selected-tab
;;       (let [selected-tab (get @app-state :selected-tab {:id :tab-gbs})
;;             database-label (get selected-tab :id :tab-gbs)
;;             _ (assert (keyword? database-label) (str database-label " is not a recognized database name."))
;;             db-func (get-in database-interface [database-label interface-func] nil)
;;             _ (assert (fn? db-func) (str "(" database-label " " interface-func ") is not a known function"))]
;;         db-func))))


;; {:author "ecological generator 2021"
;; :name "Generated Game Boy ROM",
;; :_version "2.0.0",
;; :settings
;; {:showCollisions true
;; :showConnections true}
;; :scenes {}
;; }

