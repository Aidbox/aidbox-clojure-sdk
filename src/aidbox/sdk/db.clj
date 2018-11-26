(ns aidbox.sdk.db
  (:require [aidbox.sdk.crud :as crud]))

(defn query
  "sql - sql string"
  [ctx sql]
  (let [res (crud/request ctx {:url "/$sql" :method :post :body sql})]
    (if (and (:status res) (< (:status res) 300))
      (:body res)
      (do (println "ERROR: " res)
          (throw (Exception. (pr-str res)))))))

(defn query-first
  "sql - sql string"
  [ctx sql]
  (let [res (query ctx sql)
        first-row (first res)]
    first-row))

;; TODO: what is that?
(defn query-value
  "sql - sql string"
  [ctx sql]
  (let [res (query ctx sql)
        row (first res)]
    (when row (first (vals row)))))

(defn row-to-resource [{id :id st :status rt :resource_type ts :ts  txid :txid resource :resource :as row}]
  (when (and row (or resource {}))
    (merge resource
           (dissoc row :ts :id :status :txid :resource_type :resource)
           {:id id :resourceType rt
            :meta {:lastUpdated ts
                   :versionId (str txid)
                   :tag [{:system "https://aidbox.app" :code st}]}})))
