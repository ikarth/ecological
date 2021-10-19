(ns ecological.generator.gbs
  (:require [datascript.core :as d]
            [ecological.generator.gbs.moves :as moves]
            [ecological.generator.gbs.export :as export]
            [ecological.generator.gbs.assets :as assets]
            [shadow.resource :as rc]
            ))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;



;; gbs-basic is the boilerplate JSON for a GB Studio .proj file. It will probably have to be updated for future versions of GB Studio, though it's been fairly stable.

;; (def gbs-basic (js->clj (.parse js/JSON "{ \"author\": \"https://github.com/ikarth/ecological\", \"name\": \"Generated Game Boy ROM\", \"_version\": \"2.0.0\", \"_release\": \"2\", \"settings\": { \"showCollisions\": true, \"showConnections\": true, \"worldScrollX\": 0, \"worldScrollY\": 0, \"zoom\": 100, \"defaultSpritePaletteId\": \"default-sprite\", \"defaultUIPaletteId\": \"default-ui\", \"startX\": 0, \"startY\": 0, \"startDirection\": \"down\", \"startSceneId\": \"\", \"playerSpriteSheetId\": \"\" } }" ) :keywordize-keys true))

(def gbs-basic
  {:customEvents [],
   :palettes
   [{:id "default-bg-1",
     :name "Default BG 1",
     :colors ["F8E8C8" "D89048" "A82820" "301850"],
     :defaultName "Default BG 1",
     :defaultColors ["F8E8C8" "D89048" "A82820" "301850"]}
    {:id "default-bg-2",
     :name "Default BG 2",
     :colors ["E0F8A0" "78C838" "488818" "081800"],
     :defaultName "Default BG 2",
     :defaultColors ["E0F8A0" "78C838" "488818" "081800"]}
    {:id "default-bg-3",
     :name "Default BG 3",
     :colors ["F8D8A8" "E0A878" "785888" "002030"],
     :defaultName "Default BG 3",
     :defaultColors ["F8D8A8" "E0A878" "785888" "002030"]}
    {:id "default-bg-4",
     :name "Default BG 4",
     :colors ["B8D0D0" "D880D8" "8000A0" "380000"],
     :defaultName "Default BG 4",
     :defaultColors ["B8D0D0" "D880D8" "8000A0" "380000"]}
    {:id "default-bg-5",
     :name "Default BG 5",
     :colors ["F8F8B8" "90C8C8" "486878" "082048"],
     :defaultName "Default BG 5",
     :defaultColors ["F8F8B8" "90C8C8" "486878" "082048"]}
    {:id "default-bg-6",
     :name "Default BG 6",
     :colors ["F8D8B0" "78C078" "688840" "583820"],
     :defaultName "Default BG 6",
     :defaultColors ["F8D8B0" "78C078" "688840" "583820"]}
    {:id "default-sprite",
     :name "Default Sprites",
     :colors ["F8F0E0" "D88078" "B05010" "000000"],
     :defaultName "Default Sprites",
     :defaultColors ["F8F0E0" "D88078" "B05010" "000000"]}
    {:id "default-ui",
     :name "Default UI",
     :colors ["F8F8B8" "90C8C8" "486878" "082048"],
     :defaultName "Default UI",
     :defaultColors ["F8F8B8" "90C8C8" "486878" "082048"]}],
   :music
   [{:id "f50428ab-a084-4591-9bba-2ba10fe7b1c6",
     :name "template",
     :filename "template.mod",
     :settings {},
     :inode "42221246506897054",
     :_v 1633816999994}],
   :backgrounds
   [{:imageHeight 144,
     :_v 1633817000006,
     :name "placeholder",
     :width 20,
     :inode "112308515707846197",
     :filename "placeholder.png",
     :imageWidth 160,
     :id "1e5f9d6a-bdbc-43e6-8c34-22f5bb5dea05",
     :height 18}],
   :name "Example",
   :settings
   {:worldScrollX 708,
    :navigatorSplitSizes [647 214 214],
    :zoom 200,
    :startDirection "down",
    :defaultFadeStyle "white",
    :startAnimSpeed 3,
    :showCollisions true,
    :startMoveSpeed 1,
    :worldScrollY 0,
    :customColorsEnabled true,
    :playerPaletteId "",
    :defaultSpritePaletteId "default-sprite",
    :defaultUIPaletteId "default-ui",
    :defaultBackgroundPaletteIds
    ["default-bg-1"
     "default-bg-2"
     "default-bg-3"
     "default-bg-4"
     "default-bg-5"
     "default-bg-6"],
    :playerSpriteSheetId "581d34d0-9591-4e6e-a609-1d94f203b0cd",
    :startX 9,
    :startY 9,
    :customHead "",
    :showConnections true,
    :startSceneId "",
    :showNavigator true},
   :variables [],
   :author "User",
   :scenes [],
   :spriteSheets
   [{:id "11b5452b-187c-43a3-afb1-a1f4f74ffda2",
     :name "actor",
     :numFrames 3,
     :type "actor",
     :filename "actor.png",
     :inode "82190693199813398",
     :_v 1633817000006}
    {:id "581d34d0-9591-4e6e-a609-1d94f203b0cd",
     :name "actor_animated",
     :numFrames 6,
     :type "actor_animated",
     :filename "actor_animated.png",
     :inode "385057768140479545",
     :_v 1633817000006}
    {:id "daf95270-e30d-423b-9ee7-990ae29f57f6",
     :name "static",
     :numFrames 1,
     :type "static",
     :filename "static.png",
     :inode "141863388262405010",
     :_v 1633817000007}],
   :engineFieldValues [{:id "fade_style", :value 0}],
   :_version "2.0.0", 
   :_release "6"})


;; (def gbs-basic
;;   (js->clj (.parse js/JSON
;;                    "")
;;            :keywordize-keys true)
;;   )

;; (def gbs-basic
;;   (json/read-str (rc/inline "gbs_basic.json"))

(def genboy-schema
  {:signal/signal             {:db/cardinality :db.cardinality/one} ; :db/valueType :db.type/keyword
   :scene/background          {:db/cardinality :db.cardinality/one  :db/valueType :db.type/ref}
   :scene/name                {:db/cardinality :db.cardinality/one} ; :db/valueType :db.type/string
   :scene/uuid                {:db/cardinality :db.cardinality/one  :db/unique :db.unique/identity}
   :scene/backgroundUUID      {:db/cardinality :db.cardinality/one} ; :db/valueType :db.type/string
   :scene/collisions          {:db/cardinality :db.cardinality/one} ; :db/valueType :db.type/tuple
   ;:scene/connections         {:db/cardinality :db.cardinality/many}
   :scene/editor-position     {:db/cardinality :db.cardinality/one} ; :db/valueType :db.type/tuple
   :background/filename       {:db/cardinality :db.cardinality/one} ; :db/valueType :db.type/string
   :background/image          {:db/cardinality :db.cardinality/one  :db/valueType :db.type/ref}
   :background/size           {:db/cardinality :db.cardinality/one} ; :db/valueType :db.type/tuple
   ;:background/imageSize      {:db/cardinality :db.cardinality/one} ; :db/valueType :db.type/tuple
   :background/uuid           {:db/cardinality :db.cardinality/one  :db/unique :db.unique/identity}
   :resource/filename         {:db/cardinality :db.cardinality/one  :db/unique :db.unique/identity}
   :resource/type             {:db/cardinality :db.cardinality/one} ; :db/valueType :db.type/keyword
   :constraint/asp            {:db/cardinality :db.cardinality/one}
   :endpoint/scene            {:db/cardinality :db.cardinality/one  :db/valueType :db.type/ref}
   :connection/left-end       {:db/cardinality :db.cardinality/one  :db/valueType :db.type/ref}
   :connection/direction      {:db/cardinality :db.cardinality/one}
   :connection/right-end      {:db/cardinality :db.cardinality/one  :db/valueType :db.type/ref}
   :entity/position           {:db/cardinality :db.cardinality/one}
   :type/gbs                  {:db/cardinality :db.cardinality/one}
   })

(def db-conn (d/create-conn genboy-schema))


(defn wish
  []
  [{:db/id -1
    :type :wish
    }]
  )

(defn load-resources
  "Load resources from disk that we need to generate things..."
  []

  (let [manifest (assets/asset-manifest)]
      (println "(load-resources)")
       (if (empty? manifest)
         (let []
           (js/console.log "Missing asset manifest")
           [])  ; todo: more elaborate file missing error handling?
         (let [category-table {"ui" :ui "image" :image "sprites" :sprites "backgrounds" :image}
               manifest-transaction
               (concat
                []
                ;[[:db/retractEntity signal-resources-not-loaded]]
                ;[{:db/id -1 :signal/signal :resources-are-loaded}]
                (mapv
                 (fn [key asset]
                   {:db/id key
                    :resource/type (category-table (asset :category :none) :none)
                    :resource/filename (asset :file :none)
                    :resource/filepath (asset :path :none)
                                        ;:resource/size (asset :size [0 0])
                    :resource/image-size (asset :image-size [0 0])})
                 (iterate dec -2)
                 manifest
                 ))]
             manifest-transaction))))

(defn load-gbs-projects
  "Load projects and convert them into a databse format to eventually use to create templates."
  []
  (println "(load-gbs-projects)")
  (let [sep (assets/scene-manifest)
           scenes-to-add
           (mapv (fn [key asset]
                    {:db/id key
                     :template/name (get asset :name "unnamed")
                     :template/triggers (get asset :triggers []) ; todo: translate triggers
                     :template/actors (get asset :actors []) ; todo: translate actors
                     :template/backgroundUUID (get asset :backgroundId "")
                     :template/collisions (get asset :collisions [])
                     :template/originaluuid (get asset :id "")
                     :template/use-count 0
                     })
                  (iterate dec -3)
                  (get sep :scenes []))
           backgrounds-to-add
           (mapv (fn [key asset]
                    {:db/id key
                     :gbs-input/type :background
                     :gbs-input/uuid (get asset :id "")
                     :gbs-input/name (get asset :name "")
                     :gbs-input/filename (get asset :filename "")
                     :gbs-input/width  (get asset :width 0)
                     :gbs-input/height (get asset :hieght 0)
                     })
                  (iterate dec (- 0 (+ 3 (count (get sep :scenes [])))))
                  (get sep :backgrounds []))
           transaction
           (into []
                 (concat
                  ;[[:db/retractEntity signal-not-loaded]]
                  scenes-to-add
                  backgrounds-to-add
                  ))]
       (if (and (> (count scenes-to-add) 0) (> (count backgrounds-to-add) 0))
         transaction
         [{:db/id -999 :signal/signal :failed-to-load-scenes}]
         ;; TODO: handle error if we can't manage to load the template scenes...
         )))



(def design-moves
  [
   ;;move-load-resources-from-disk
   ;;move-load-gbs-projects-from-disk
   moves/move-create-background-from-image   
   ;;moves/move-generate-scene-from-template
   moves/move-add-existing-background-to-scene
   ;;moves/move-add-connection-to-scene
   moves/move-place-greenfield-scene
   moves/move-create-greenfield-image
   ;moves/move-place-greenfield-connection
   ;moves/move-place-greenfield-endpoint
   moves/move-connect-scenes
   moves/move-add-endpoint-to-scene
   moves/ground-connection-into-trigger
   moves/move-create-speckled-background-image
   ])

(def initial-transaction
  [;;load-resources
   ;;load-gbs-projects
   wish
   ]
  )

(def records
  {:db-conn db-conn
   :db-schema genboy-schema
   :design-moves design-moves
   :exporter #(export/export-gbs-project gbs-basic db-conn)
   :export-most-recent #(export/export-most-recent-artifact gbs-basic db-conn)
   :initial-transaction initial-transaction
   :setup [(assets/load-manifest) (assets/load-scene-sources)]
   :export-project-view #(export/export-project-view db-conn)
   })
