(ns ecological.generator.image.ops
  (:require ;[datascript.core :as d]
            ;[clojure.string]
            ;[goog.crypt :as crypt]
            [quil.core :as qc]
            [quil.middleware :as qm]
            ;[quil.applet :as qa]
            ))


(def default-image-size [256 256])

;;;;;;;;;;;;;;;;;;;;;;;
;; Image operations


;(def graphics-sketchboard (qc/create-graphics 256 256))

(defn op-create-image
  "Create a blank raster image of a given size."
  [size]
  (let [_ (assert (or (nil? size) (and (vector? size) (= (count size) 2)))
                     (str ":size should be nil or a vector of size 2, but instead it is " size))
        [w h] (if (nil? size) default-image-size size)]
    (qc/with-sketch (qc/get-sketch-by-id "quil-canvas")
      (let [image-target (qc/create-image w h)]
        (dotimes [x w]
          (dotimes [y h]
            (qc/set-pixel image-target x y (qc/color 0 0 0))))
        (qc/update-pixels image-target)
        image-target))))

(defn op-create-test-graphics
  "Create an image and test that the drawing commands work.
  Note that quil.core/create-graphics causes a memory leak
  in ClojureScript and it'd be much better to have a central
  drawing surface that gets reused instead of creating a new
  one in an individual generative operation here.

  So don't use this in production."
  [w h]
  (let [graphics (qc/create-graphics w h)]
    (qc/with-graphics graphics
      (qc/background 127 127 255)
      (qc/ellipse 50 50 50 50))
    graphics))

(defn op-create-perlin-image
  "Create a raster matrix of Perlin noise.
  :size          the size of the raster image
  :noise-offset  Perlin noise offset
  :noise-scale   Perlin noise scaling
  :noise-octaves Perlin noise octaves
  :noise-falloff Perlin noise falloff
  Implemented via quil/noise"
  [& {:keys [size noise-offset noise-scale noise-octaves noise-falloff] :or {size default-image-size noise-offset [0 0] noise-scale [0.05 0.05] noise-octaves 4 noise-falloff 0.5}}]
  (qc/with-sketch (qc/get-sketch-by-id "quil-canvas")
    (let [_ (assert (or (nil? size) (and (vector? size) (= (count size) 2)))
                     (str ":size should be nil or a vector of size 2, but instead it is " size))
          [w h] (if (nil? size) default-image-size size)       
          image-target (qc/create-image w h)]
      (qc/noise-detail noise-octaves noise-falloff)
      (dotimes [x w]
        (dotimes [y h]
          (let [intensity (* 255 (qc/noise (+ (first noise-offset) (* x (first noise-scale)))
                                           (+ (second noise-offset)(* y (second noise-scale)))))]
            (qc/set-pixel image-target x y (qc/color intensity intensity intensity))
            )))
      (qc/update-pixels image-target)
      image-target)))

(defn op-create-random-noise
  "Create a raster image of random white noise."
  [size]
  (let [_ (assert (or (nil? size) (and (vector? size) (= (count size) 2)))
                  (str ":size should be nil or a vector of size 2, but instead it is " size))
        [w h] (if (nil? size) default-image-size size)]
    (qc/with-sketch (qc/get-sketch-by-id "quil-canvas")
      (let [image-target (qc/create-image w h)]
        (dotimes [x w]
          (dotimes [y h]
            (let [intensity (qc/random 1.0)]
              (qc/set-pixel image-target x y (qc/color (* 255 intensity) (* 255 intensity) (* 255 intensity))))))
        (qc/update-pixels image-target)
        image-target))))

(defn op-create-uv-pattern
  "Create a raster image with red corresponding to width and green corresponding to height."
  [size]
  (let [_ (assert (or (nil? size) (and (vector? size) (= (count size) 2)))
                  (str ":size should be nil or a vector of size 2, but instead it is " size))
        [w h] (if (nil? size) default-image-size size)]
    (qc/with-sketch (qc/get-sketch-by-id "quil-canvas")
      (let [image-target (qc/create-image w h)]
        (dotimes [x w]
          (dotimes [y h]
            (let [u (/ x w)
                  v (/ y h)
                  ]
              (qc/set-pixel image-target x y (qc/color (* 255 u) (* 255 v) 0)))))
        (qc/update-pixels image-target)
        image-target))))

(defn op-create-test-pattern
  "Create a raster image with repeating color ramps."
  [size]
  (let [_ (assert (or (nil? size) (and (vector? size) (= (count size) 2)))
                  (str ":size should be nil or a vector of size 2, but instead it is " size))
        [w h] (if (nil? size) default-image-size size)]
    (qc/with-sketch (qc/get-sketch-by-id "quil-canvas")
      (let [image-target (qc/create-image w h)]
        (dotimes [x w]
          (dotimes [y h]
            (qc/set-pixel image-target x y (qc/color (rem x 256) (rem y 256) 127))))
        (qc/update-pixels image-target)
        image-target))))

(defn op-image-filter
  "Given an image, run a filter on it.
  Implemented with quil.core/image-filter."
  [image & {:keys [filter-mode filter-level] :or {filter-mode :threshold filter-level 0.5}}]
  (qc/with-sketch (qc/get-sketch-by-id "quil-canvas")
    (qc/image-filter image filter-mode filter-level))
  image)

(defn op-image-blend
  "Create a new image that is a blend between the first image and the second image.
  Implemented via quil.core/blend."
  [image-src image-dest & {:keys [filter-mode size] :or {filter-mode :blend size default-image-size}}]
  ;(js/console.log size)
  (qc/with-sketch (qc/get-sketch-by-id "quil-canvas")
    (let [image-final (qc/create-image (first size) (second size))] ;[graphics-obj (qc/create-graphics (first size) (second size) :p2d)]
      (qc/copy image-src image-final [0 0 (first size) (second size)] [0 0 (first size) (second size)])
      (qc/blend image-dest image-final 0 0 (first size) (second size) 0 0 (first size) (second size) filter-mode)
      image-final)))

(defn process-operations
  "Apply the list of operations to the given input and return the result"
  [input operation-list]
  ;; TODO
  input)
