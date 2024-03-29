(ns ecological.build
  (:require
    ;[shadow.cljs.devtools.api :as shadow]
    ;[clojure.java.shell :refer (sh)]
   [clojure.java.io :as io]
   [clojure.string :as string]
   [clojure.data]
   ;;[clojure.edn :as edn]
   ;;[clojure.core.match :as match]
   ;;[me.raynes.fs :as fs]
   [clojure.pprint :as pprint]
    ))


(defn copy-asset!
  "Copy an asset file to public resources. Returns a manifest of the asset, which can be included in an overall manifest. Overwrites the destination."
  [source source-prefix destination]
  ;(prn source source-prefix destination)

  (let [separator (System/getProperty "file.separator")
        source-file (str (.getPath source))
        source-file-vec (string/split source-file #"[\\/]")
        source-prefix-vec (string/split source-prefix #"[\\/]")
        destination-vec (string/split destination #"[\\/]")
        truncated-file-path (remove nil?
                                    (first
                                     (clojure.data/diff
                                      source-file-vec
                                      source-prefix-vec)))
        full-destination (io/file "."
                                  (string/join separator destination-vec)
                                  (string/join separator truncated-file-path))
        reference-destination (io/file "." (string/join separator (rest destination-vec))
                               (string/join separator truncated-file-path))

        ]
    (if (.exists source)
      (let [
            image-size
            (with-open [r (java.io.FileInputStream. source)]
              (let [image (javax.imageio.ImageIO/read r)]
                (if (nil? image)
                  (let []
                    (prn image)
                    (prn source)
                    [0 0])
                  [(.getWidth image) (.getHeight image)]              
                  )
                ))
            gb-size (mapv #(quot % 8) image-size)
            ]
         {:category (first truncated-file-path)
         :path (str (.getPath reference-destination))
         :file (last truncated-file-path)
         :success (if (.exists full-destination)
                    "exists"
                    (do
                      (clojure.java.io/make-parents full-destination)
                      (nil? (io/copy source full-destination))
                      ))
         :length (if (.exists full-destination)
                   (.length full-destination)
                   false)
         :size gb-size
         :image-size image-size
         }
        )
      {})))

(defn build-asset-data! [source-asset-path]                                      
  (let [resource-files
        (->> source-asset-path
             (io/file)
             (file-seq)
             (filter #(.isFile %))
             (filter #(or
                       (clojure.string/includes? (.getPath %) ".png") ; only get image files
                       (clojure.string/includes? (.getPath %) ".json") ; only get json files
                       )))]
    (let [assets (mapv
                  #(copy-asset! % source-asset-path "public\\data\\assets")
                  resource-files)]
      (with-open [writer-handle (io/writer "public\\data\\asset_manifest.edn")]
        (binding [*print-length* false
                  *out* writer-handle]
          (pprint/pprint assets))))))

(defn build-assets
  {:shadow.build/stage :flush}
  [build-state resource-folder & args]
  (build-asset-data! resource-folder)
  build-state)


