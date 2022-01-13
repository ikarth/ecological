(ns ecological.generator.gencore
  (:require [datascript.core :as d]
            [ecological.generator.library :refer [current-database update-database setup-databases]]))

(defn random-from-range [low high]
  (let [[low high] (sort [low high])]
    (+  (* (rand) (- high low)) low)))

(defn timestamp []
  (js/parseInt (.now js/Date)))

(defn get-possible-design-move-from-moveset
   "Return a list of all possible design moves for the provided `moveset-db-conn`,
  as selected from the provided `design-moves` collection."
  ([moveset-db-conn design-moves]
   (get-possible-design-move-from-moveset moveset-db-conn design-moves nil)
   )
  ([moveset-db-conn design-moves move-type]
   (apply concat
          (map
           (fn [mov]
             (println "Move type:" (get mov :type nil))
             (let [q-map
                   (if-let [move-query (get mov :query false)]
                     ;; (if (fn? move-query) (move-query @db-conn)) ;; todo: properly handle functions for queries
                     (let [query-result (d/q move-query @moveset-db-conn nil)] ;; todo: pass additional context to query
                       ;; todo: check if result is a promise
                       (map (fn [v] {:move mov :vars v}) query-result))
                     {:move mov :vars nil})]
               (if (map? q-map) [q-map] q-map)))
           design-moves))))

(defn get-possible-design-moves
  "Return a list of all possible design moves for the provided `db`. 
   A _possible design move_ is an object with keys `move` and `vars`, 
   representing the abstract specification of this move type
   and a specific set of bindings for this move's logic variables respectively."
  [db-conn moveset]
  (let [m (get-possible-design-move-from-moveset db-conn moveset)]
    ;;(println m)
    m))

(defn log-db
  "Log a complete listing of the entities in the provided `db` to the console."
  [db]
  (d/q '[:find ?any ?obj :where [?obj :type ?any]] @db)) ;todo: log to console...


(defn default-parameters [design-move randomize-parameters]
  (let [params (get design-move :parameters {})
        ;;defaults (zipmap (keys params) (map :default (vals params)))
        ;;ranges (zipmap (keys params) (map :range (vals params)))
        ]
    (let [defaults
          (map (fn [[name param]]
                 {name
                  (cond
                    (and randomize-parameters (= :enum (:form param)))
                    (rand-nth (:range param))
                    (and randomize-parameters (= :vector2 (:form param)))
                    (mapv (fn [[low high]]
                            (random-from-range low high)) (:range param))
                    (and randomize-parameters (= :scalar (:form param)))
                    ((fn [[low high]]
                       (random-from-range low high)) (:range param))
                    :else
                    (:default param))})
               params)]
      (apply merge defaults))))

(defn assemble-exec-result [db design-move params]
  (let [_ (assert (map? design-move) "Need a design move before we can execute the design move function.")
        move-name (get-in design-move [:move :name])
        _ (assert (string? move-name) (str "Design move (" move-name ") not found in " design-move "."))
        exec-func (get-in design-move [:move :exec])
        _ (assert (fn? exec-func) (str move-name " has no :exec function!"))
        current-time (timestamp)
        ;; TODO: check if exec-func throws an exception or something, because we should
        ;;       cancel the transaction if that happens...
        result (exec-func db (:vars design-move) params)
        provenance-added (map (fn [transact]
                              (merge transact {:entity/timestamp current-time
                                               :provenance/cause move-name
                                              ;:provenance/bindings (:vars design-move)
                                               :provenance/params (str params)
                                                }))
                              result)
        history-record [{:db/id -999999 ; magic number to try and be unique...this will break if more than 1,000,000 changes are in the transaction. Which is unlikely.
                         ;:design/move-count current-design-move-count ; todo: count the actual number of moves that have been made by looking up the last one, instead of just using the loop counter
                         :design/move-record move-name
                                        ;:design/move-parameters (get design-move :vars [])
                         :design/timestamp current-time
                         }
                        ]
        tx-data (into [] (concat provenance-added history-record))
        tx-data result ;; Temporarily disable history recording...
        ]
    ;;(println tx-data)
    tx-data))

(defn execute-design-move!
  "Executes the supplied design move in the context of the current-database."
  [design-move params]
  (if-let [db-conn (get @current-database :db-conn)]
    (let []
      (assert (map? design-move) "Design move is missing, so can't be executed.")
      (println (str "executing move: " (:name (:move design-move))))
      (println
       (d/transact! db-conn (assemble-exec-result @db-conn design-move params)))
      )
    
    (println "Current database is missing somehow.")
    ))
 

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn setup []
  (setup-databases))

(defn switch-database [database-id]
  (update-database database-id))

(defn reset-the-database!
   "For when you want to start over. Returns an empty database that follows the schema."
  [database-id]
  (switch-database database-id)
  (if-let [db-conn (get @current-database :db-conn)]
    (let [db-schema (get @current-database :db-schema {})
          ]
      (println "Resetting DB connection...")
      (d/reset-conn! db-conn (d/empty-db db-schema))
      ;; TODO: if there is an initial transaction, it goes here...
      ;(js/console.log @db-conn)
      ;(js/console.log @current-database)
      (println "Performing initial transactions...")
      (let [initial-transaction (get @current-database
                                     :initial-transaction                                    
                                     [])]
        ;(js/console.log initial-transaction)
        ;(js/console.log @current-database)
        (doseq [act initial-transaction]
          (if (fn? act)
            (d/transact! db-conn (act))
            (d/transact! db-conn act))))
      ;; (doall
      ;;  (for [act initial-transaction]
      ;;    (let []
      ;;      (js/console.log act)
      ;;      (d/transact! db-conn (act)))))
     ;(js/console.log @db-conn)
      db-conn
      )))

(defn generate []
  ;;(reset-the-database! database-id)
  (js/console.log "generate...")
  (if-let [db-conn (get @current-database :db-conn)]
    (if-let [autonomous-generator (get @current-database :auto-generator)]
      (let []
        (js/console.log "attempting to run autonomous generator:" autonomous-generator)
        (autonomous-generator db-conn)
        (log-db db-conn)))))

(defn make-empty-project [database-id]
  (println (str "Switching to: " database-id))
  (assert (not (undefined? database-id)) "Tried to switch to an undefined generator database.")
  (switch-database database-id)
  (println "Resetting database...")
  (reset-the-database! database-id))


(defn fetch-data-output
  "Return the current state of the generated results"
  []
 
  (if-let [db-conn (get @current-database :db-conn)]
    (let [exporter-func (get @current-database :exporter (fn [_] (println "No exporter implemented for this database.")))]

      (exporter-func db-conn))))

(defn fetch-database []
  ;; TODO
  )

(defn fetch-possible-moves []
  (if-let [db-conn (get @current-database :db-conn)]
    (let [design-moves (get @current-database :design-moves [])
          moves (get-possible-design-move-from-moveset db-conn design-moves)]
      moves)))

(defn fetch-some-moves []
  (if-let [db-conn (get @current-database :db-conn)]
    (let [design-moves (get @current-database :design-moves [])
          moves (get-possible-design-moves db-conn design-moves)]
      moves)))

(defn fetch-all-moves
  "Return all of the moves we know about, valid or otherwise"
  []
  (if-let [db-conn (get @current-database :db-conn)]
    (let [moves (get @current-database :design-moves [])]
      moves)))


(defn fetch-generated-project!
  "Generate a new project and return it"
  []
  (if-let [db-conn (get @current-database :db-conn)]
    (let []
      (generate)
      (fetch-data-output))))

(defn fetch-data-view []
  ;(js/console.log @current-database)
  ;(switch-database )
  (if-let [db-conn (get @current-database :db-conn)]
    (vec (map (fn [dat]
                (let [[e a v tx add] dat]
                  [e a v tx add])) (d/datoms @db-conn :eavt)))
    (println "Database connection missing when trying to fetch a new view.")))

(defn fetch-most-recent-artifact
  "Returns the most recently-created artifact."
  []
  (if-let [db-conn (get @current-database :db-conn)]
    (let [exporter-func (get @current-database :export-most-recent (fn [_] (println "No most-recent-artifact exporter implemented for this database.")))]
      (exporter-func db-conn))))

(defn fetch-project-view
  "Returns a summarized list of the most signifigant artifacts, to make it easier to keep track of them."
  []
  (if-let [db-conn (get @current-database :db-conn)]
    (let [exporter-func (get @current-database :export-project-view (fn [_] (println "No (project-view) exporter implemented for this database.")))]
      (if (fn? exporter-func)
        (exporter-func db-conn)
        []))))
