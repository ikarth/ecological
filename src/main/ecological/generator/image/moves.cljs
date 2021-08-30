(ns ecological.generator.image.moves
    (:require [datascript.core :as d]
            [ecological.generator.image.ops :as ops]
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
     :range [[512 512] [512 512]]
     }
    :noise-offset
    {:default [0 0]
     :form :vector2
     :intent :position
     :range [[-256 256] [-256 256]]
     }
    :noise-scale
    {
     :default [0.05 0.05]
     :form :vector2
     :intent :scale
     :range [[0.01 0.9] [0.01 0.9]]
     }
    :noise-octaves
    {:default 4
     :form :scalar
     :intent :detail
     :range [[1 8]]
     }
    :noise-falloff
    {:default 0.5
     :form :scalar
     :intent :detail
     :range [[0.01 1.0]]
     }}
   :exec
   (fn [db & {:keys [size]}]
     (let [_ (assert (or (nil? size) (and (vector? size) (= (count size) 2)))
                     (str ":size should be nil or a vector of size 2, but instead it is " size))
           [w h] (if (nil? size) ops/default-image-size size)
           perlin-image (ops/op-create-perlin-image [w h])]       
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
   (fn [db & {:keys [size]}]
     (let [_ (assert (or (nil? size) (and (vector? size) (= (count size) 2)))
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
   (fn [db & {:keys [size]}]
     (let [_ (assert (or (nil? size) (and (vector? size) (= (count size) 2)))
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
   (fn [db & {:keys [size]}]
     (let [_ (assert (or (nil? size) (and (vector? size) (= (count size) 2)))
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
   :query
   '[:find ?image-image ?image-size
     :in $ %
     :where
     [?element :raster/image ?image-image]
     [?element :raster/size   ?image-size]
     ]
   :exec
   (fn [db [image-data size]]
     (let [_ (assert (or (nil? size) (and (vector? size) (= (count size) 2)))
                     (str ":size should be nil or a vector of size 2, but instead it is " size))
           [w h] (if (nil? size) ops/default-image-size size)
           result-image (ops/op-image-filter image-data :filter-mode :threshold :filter-level 0.5)]       
       [{:db/id -1
         :raster/image result-image
         :raster/size [w h]
         :raster/uuid (str (random-uuid))
         
         }]))})

(def move-blend-blend
  {:name "move-blend-blend"
   :comment "Converts an image to black and white pixels based on if they are above or below a threshold value."
   :query
   '[:find ?image-src ?image-src-size ?image-dest ?image-dest-size
     :in $ %
     :where
     [?e1 :raster/image ?image-src]
     [?e1 :raster/size  ?image-src-size]
     [?e2 :raster/image ?image-dest]
     [?e2 :raster/size  ?image-dest-size]
     [(not= ?e1 ?e2)]
     ]
   :exec
   (fn [db [image-src src-size image-dest size]]
     (let [_ (assert (or (nil? size) (and (vector? size) (= (count size) 2)))
                     (str ":size should be nil or a vector of size 2, but instead it is " size))
           [w h] (if (nil? size) ops/default-image-size size)
           result-image (ops/op-image-blend image-src image-dest :size size :filter-mode :overlay)]       
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
   (fn [db & {:keys [size]}]
     (let [_ (assert (or (nil? size) (and (vector? size) (= (count size) 2)))
                     (str ":size should be nil or a vector of size 2, but instead it is " size))
           [w h] (if (nil? size) ops/default-image-size size)]       
       [{:db/id -1
         :func/func-handle :op-create-image
         :func/parameters [w h :rgb]
         :func/uuid (str (random-uuid))         
         }]))})



