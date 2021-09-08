(ns ecological.generator.library
  (:require [datascript.core :as d]
            [ecological.generator.image :as image]
            ;[ecological.generator.image.export]
            [ecological.generator.gbs   :as gbs]
            ))



(def database-records
  {:tab-image image/records
   :tab-gbs   gbs/records
   })


(defonce current-database
  (atom {:db-conn nil
         :db-schema {}
         :initial-transaction [(fn [_] (println "No initial transaction.") [])]
         :design-moves []
         :exporter (fn [_] (println "Exporter not implemented."))
         :export-most-recent (fn [_] (println "Most-Recent-Exporter not implemented."))
         :setup []
         :project-view (fn [_] (println "Project view not implemented."))
         }))



(defn update-database [database-id]

  (let [new-database (get database-records database-id :tab-image)]
    (if (and new-database (get new-database :db-conn false ))
      (let []
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
        (swap! current-database assoc-in [:export-project-view]
               (get new-database :export-project-view))
        (swap! current-database assoc-in [:initial-transaction]
               (get new-database :initial-transaction)))
       (println (str "Database not found: " database-id)))))

(defn setup-databases []
  (doseq [id (keys database-records)]
    (let []
      (update-database id)
      (if-let [setup (get current-database :setup)]
        (doseq [setup-func setup]
          (if (fn? setup-func)
            (setup-func)))))))
