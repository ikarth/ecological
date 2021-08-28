(ns ecological.generator.library
  (:require [datascript.core :as d]
            [ecological.generator.image :as image]
            ;[ecological.generator.image.export]
            ;[ecological.generator.gbs   :as gbs]
            ))



(def database-records
  {:tab-image image/records
   ;:tab-gbs   gbs
   })


(defonce current-database
  (atom {:db-conn nil
         :db-schema {}
         :initial-transaction []
         :design-moves []
         :exporter (fn [_] (println "Exporter not implemented."))
         :export-most-recent (fn [_] (println "Most-Recent-Exporter not implemented."))
         }))


(defn update-database [database-id]
  ;(js/console.log database-id)
  (let [new-database (get database-records database-id :tab-image)]
    (js/console.log new-database)
    (if (and new-database (get new-database :db-conn false ))
      (let []
        (js/console.log new-database)
        (swap! current-database assoc-in [:db-conn]
               (get new-database :db-conn))
        (swap! current-database assoc-in [:db-schema]
               (get new-database :db-schema))
        (swap! current-database assoc-in [:design-moves]
               (get new-database :design-moves))
        (swap! current-database assoc-in [:exporter]
               (get new-database :exporter))
        (swap! current-database assoc-in [:export-most-recent]
               (get new-database :export-most-recent))
        (swap! current-database assoc-in [:intial-transaction]
               (get new-database :intitial-transaction))
        (println (get @current-database :design-moves))
        (println (get @current-database :exporter))
        
        )
      (println (str "Database not found: " database-id))
      )))
