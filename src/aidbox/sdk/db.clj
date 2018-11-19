(ns aidbox.sdk.db
  (:require [aidbox.sdk.crud :as crud))

(defn query
  "sql - sql string"
  [ctx sql]
  (let [res (crud/request ctx {:url "/$sql" :method :post :body sql})]
    (if (and (:status res) (< (:status res) 300))
      (:body res)
      (do (println "ERROR: " res)
          (throw (Exception. (pr-str res)))))))

(defn query-value
  "sql - sql string"
  [ctx sql]
  (let [res (query ctx sql)
        row (first res)]
    (when row (first (vals row)))))
