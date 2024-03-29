(ns ecological.generator.utilities
  (:require ;[datascript.core :as d]
   [clojure.string]
   [clojure.pprint]
            ;[goog.crypt :as crypt]
            ;[quil.core :as qc]
            ;[quil.middleware :as qm]
            ;[quil.applet :as qa]
            ))


(defn byte-to-hex [b-val]
  (let [hex-str (.toString b-val 16)]
    (case (count hex-str)
      0 "00"
      1 (str "0" hex-str)
      (subs hex-str (- (count hex-str) 2) (count hex-str)))))

;; (defn hex-to-byte [h-val]
;;   ;(js/console.log (str h-val))
;;   ;(js/console.log (crypt/stringToUtf8ByteArray (str h-val)))
;;   ;(into [] (crypt/stringToUtf8ByteArray (str h-val)))
;;   (apply str h-val))
;; ;utf8ByteArrayToString

(defn bytes-to-hex-string [array-of-bytes]
  (clojure.string/join
   (mapv
    (fn [b-val]
      (byte-to-hex b-val))
    array-of-bytes)))

;; (defn hex-string-to-bytes [hex-string]
;;   (mapv
;;    hex-to-byte
;;    (partition 2 hex-string)))

(defn bytes-to-bools [array-of-bytes]
  (clojure.string/join
   (mapv
    (fn [b-val]
      (cljs.pprint/cl-format nil (str "~" 8 ",'0d") (.toString b-val 2)))
    array-of-bytes)))

(defn string-to-int [val]
  (js/parseInt val))

(defn string-to-float [val]
  (js/parseFloat val))

(defn remove-leading-colons [val]
  (if (string? val)
    (if (clojure.string/starts-with? val ":")
      (remove-leading-colons (subs val 1))
      val)
    val))

(defn string-to-keyword [val]
  (if (keyword? val)
    val
    (if (string? val)
      (keyword (remove-leading-colons val))
      val)))
