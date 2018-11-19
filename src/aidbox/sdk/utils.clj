(ns aidbox.sdk.utils
  (:require [cheshire.core :as json]))

(defn parse-json [x]
  (when x
    (if (string? x)
      (json/parse-string x keyword)
      (json/parse-string (slurp x) keyword))))

(defn generate-json [x]
  (json/generate-string x))
