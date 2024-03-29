(ns ecological.generator.image.moves
    (:require [datascript.core :as d]
              [ecological.generator.image.ops :as ops]
              [ecological.generator.utilities :as util]
            ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(def move-generate-blank-raster-image
  {:name "generate-blank-raster-image"
   :comment "Create a raster image grid, initialized to 0"
   ;;:query nil
   :exec
   (fn [db & {:keys [size]}]
     (let [_ (assert (or (nil? size) (and (vector? size) (= (count size) 2)))
                     (str ":size should be nil or a vector of size 2, but instead it is " size))
           [w h] (if (nil? size) ops/default-image-size size)
           blank-image (ops/op-create-image [w h])]       
       [{:db/id -1
         :raster/image blank-image
         :raster/size [w h]
         :raster/uuid (str (random-uuid))        
         }]))})

(def move-generate-perlin-noise-raster
  {:name "generate-perlin-noise-raster"
   :comment "Create a raster grid of Perlin noise of the given size."
   ;;:query nil
   :parameters
   {
    :size
    {:default ops/default-image-size
     :form :vector2
     :intent :size
     :step 64
     :range [[(first  ops/default-image-size) (first  ops/default-image-size)]
             [(second ops/default-image-size) (second ops/default-image-size)]]
     }
    :noise-offset
    {:default [0 0]
     :form :vector2
     :intent :position
     :step 1
     :range [[-256 256] [-256 256]]
     }
    :noise-scale
    {
     :default [0.05 0.05]
     :form :vector2
     :intent :scale
     :step 0.01
     :precision 3
     :range [[0.01 0.28] [0.01 0.28]]
     }
    :noise-octaves
    {:default 4
     :form :scalar
     :intent :detail
     :step 1
     :range [3 6]
     }
    :noise-falloff
    {:default 0.5
     :form :scalar
     :intent :detail
     :precision 2
     :step 0.1
     :range [0.3 0.7]
     }}
   :exec
   (fn [db _ params]
     ;(println "(move-generate-perlin-noise-raster)")
     ;(println params)
     (let [noise-offset  (get params :noise-offset [0 0])
           noise-scale   (get params :noise-scale [0.05 0.05])
           noise-octaves (get params :noise-octaves 4)
           noise-falloff (get params :noise-falloff 0.5)
           size (:size params)
           _ (assert (or (nil? size) (and (vector? size) (= (count size) 2)))
                     (str ":size should be nil or a vector of size 2, but instead it is " size))
           [w h] (if (nil? size) ops/default-image-size size)
           perlin-image (ops/op-create-perlin-image
                         :size [w h]
                         :noise-offset  noise-offset
                         :noise-scale   noise-scale 
                         :noise-octaves (if (coll? noise-octaves)
                                          (apply Math/round noise-octaves)
                                          (Math/round noise-octaves))
                         :noise-falloff (if (coll? noise-falloff)
                                          (first noise-falloff)
                                          noise-falloff)
                         )]
       ;;(js/console.log noise-octaves)
       ;;(js/console.log noise-falloff)
       [{:db/id -1
         :raster/image perlin-image
         :raster/size [w h]
         :raster/uuid (str (random-uuid))         
         }]))})

(def move-generate-random-noise-raster
  {:name "generate-random-noise-raster"
   :comment "Create a raster grid filled with 0 to 255 based on UV -> RG coordinates."
   ;;:query nil
   :exec
   (fn [db _ params]
     (let [size (:size params)
           _ (assert (or (nil? size) (and (vector? size) (= (count size) 2)))
                     (str ":size should be nil or a vector of size 2, but instead it is " size))
           [w h] (if (nil? size) ops/default-image-size size)
           perlin-image (ops/op-create-random-noise [w h])]       
       [{:db/id -1
         :raster/image perlin-image
         :raster/size [w h]
         :raster/uuid (str (random-uuid))         
         }]))})

(def move-generate-uv-pattern-raster
  {:name "generate-uv-pattern-raster"
   :comment "Create a raster grid filled with 0 to 255 based on UV -> RG coordinates."
   ;;:query nil
   :exec
   (fn [db _ params]
     (let [size (:size params)
           _ (assert (or (nil? size) (and (vector? size) (= (count size) 2)))
                     (str ":size should be nil or a vector of size 2, but instead it is " size))
           [w h] (if (nil? size) ops/default-image-size size)
           perlin-image (ops/op-create-uv-pattern [w h])]       
       [{:db/id -1
         :raster/image perlin-image
         :raster/size [w h]
         :raster/uuid (str (random-uuid))         
         }]))})

(def move-generate-test-pattern-raster
  {:name "generate-test-pattern-raster"
   :comment "Create a raster grid filled with a repeating text pattern of the given size."
   ;;:query nil
   :exec
   (fn [db _ params]
     (let [size (:size params)
           _ (assert (or (nil? size) (and (vector? size) (= (count size) 2)))
                     (str ":size should be nil or a vector of size 2, but instead it is " size))
           [w h] (if (nil? size) ops/default-image-size size)
           perlin-image (ops/op-create-test-pattern [w h])]       
       [{:db/id -1
         :raster/image perlin-image
         :raster/size [w h]
         :raster/uuid (str (random-uuid))         
         }]))})

;image & {:keys [filter-mode filter-level] :or {filter-mode :threshold filter-level 0.5}}
(def move-filter-threshold
  {:name "move-filter-threshold"
   :comment "Converts an image to black and white pixels based on if they are above or below a threshold value."
   :parameters
   {:filter-mode
    {:default :threshold
     :form :enum
     :range [:threshold :gray :invert :erode :dilate]
     }
    :filter-level
    {:default 0.5
     :form :scalar
     :step 0.1
     :range [0.0 1.0]}}
    :query
   '[:find ?image-image ?image-size
     :in $ %
     :where
     [?element :raster/image ?image-image]
     [?element :raster/size   ?image-size]
     ]
   :exec
   (fn [db [image-data size] params]
     (let [_ (assert (or (nil? size) (and (vector? size) (= (count size) 2)))
                     (str ":size should be nil or a vector of size 2, but instead it is " size))
           [w h] (if (nil? size) ops/default-image-size size)
           filter-mode (util/string-to-keyword (get params :filter-mode :threshold))
           filter-level (get params :filter-level 0.7)
           filter-level (if (number? filter-level) filter-level (first filter-level))
           ;;_ (println filter-mode)
           ;;_ (println filter-level)
           result-image (ops/op-image-filter image-data :filter-mode filter-mode :filter-level filter-level)]       
       [{:db/id -1
         :raster/image result-image
         :raster/size [w h]
         :raster/uuid (str (random-uuid))         
         }]))})

(def move-filter-blur
  {:name "move-filter-blur"
   :comment "Performs a Gaussian blur on an image."
   :parameters {
    :filter-level
    {:default 1
     :form :scalar
     :step 0.25
     :range [0.0 5.0]}}
   :query
   '[:find ?image-image ?image-size
     :in $ %
     :where
     [?element :raster/image ?image-image]
     [?element :raster/size   ?image-size]
     ]
   :exec
   (fn [db [image-data size] params]
     (let [_ (assert (or (nil? size) (and (vector? size) (= (count size) 2)))
                     (str ":size should be nil or a vector of size 2, but instead it is " size))
           [w h] (if (nil? size) ops/default-image-size size)
           filter-level (get params :filter-level 1)
           result-image (ops/op-image-filter image-data :filter-mode :blur :filter-level filter-level)]
       ;;(println (str "blur: " filter-level))
       [{:db/id -1
         :raster/image result-image
         :raster/size [w h]
         :raster/uuid (str (random-uuid))         
         }]))})


(def move-filter-posterize
  {:name "move-filter-posterize"
   :comment "Reduces the number of colors in an image."
   :parameters {
    :filter-level
    {:default 4
     :form :scalar
     :range [[2 16]]}}
   :query
   '[:find ?image-image ?image-size
     :in $ %
     :where
     [?element :raster/image ?image-image]
     [?element :raster/size   ?image-size]
     ]
   :exec
   (fn [db [image-data size] params]
     (let [_ (assert (or (nil? size) (and (vector? size) (= (count size) 2)))
                     (str ":size should be nil or a vector of size 2, but instead it is " size))
           [w h] (if (nil? size) ops/default-image-size size)
           filter-level (get params :filter-level 4)
           result-image (ops/op-image-filter image-data :filter-mode :posterize :filter-level filter-level)]
       ;;(println (str "posterize: " filter-level))
       [{:db/id -1
         :raster/image result-image
         :raster/size [w h]
         :raster/uuid (str (random-uuid))         
         }]))})

(def move-blend-blend
  {:name "move-blend-blend"
   :comment "Blends two images together."
   :parameters
   {:filter-mode
    {:default :overlay
     :form :enum
     :range [:blend :add :darkest :lightest :difference :exclusion :multiply :screen :overlay :hard-light :soft-light :dodge :burn]
     }}
   :query
   '[:find ?image-src ?image-src-size ?image-dest ?image-dest-size
     :in $ %
     :where
     [?e1 :raster/image ?image-src]
     [?e1 :raster/size  ?image-src-size]
     [?e2 :raster/image ?image-dest]
     [?e2 :raster/size  ?image-dest-size]
     [(not= ?e1 ?e2)]]   
   :exec
   (fn [db [image-src src-size image-dest size] params]
     (let [;size (:size params) ; TODO: get from image size
           filter-mode (util/string-to-keyword (get params :filter-mode :overlay))
           ;;_ (println (str "filter-mode in (move-blend-blend): " filter-mode))
           ;;_ (println (type filter-mode))
           ;;_ (js/console.log filter-mode)
           _ (assert (or (nil? size) (and (vector? size) (= (count size) 2)))
                     (str ":size should be nil or a vector of size 2, but instead it is " size))
           [w h] (if (nil? size) ops/default-image-size size)
           result-image (ops/op-image-blend image-src image-dest :size size :filter-mode filter-mode)]       
       [{:db/id -1
         :raster/image result-image
         :raster/size [w h]
         :raster/uuid (str (random-uuid))         
         }]))})

(def move-generate-blank-image-function
  {:name "generate-blank-image-function"
   :comment "Create a function that returns a blank image of the given size."
   ;:query nil
   :exec
   (fn [db bindings params]
     (let [size (:size params)
           _ (assert (or (nil? size) (and (vector? size) (= (count size) 2)))
                     (str ":size should be nil or a vector of size 2, but instead it is " size))
           [w h] (if (nil? size) ops/default-image-size size)]       
       [{:db/id -1
         :func/func-handle :op-create-image
         :func/parameters [w h :rgb]
         :func/uuid (str (random-uuid))         
         }]))})



