(ns aidbox.sdk.crud
  (:require [org.httpkit.client :as http]
            [aidbox.sdk.utils :refer [parse-json generate-json]]))

(defn request [ctx opts]
  (let [ctx (:env ctx)
        app (:app ctx)
        box (:box ctx)
        url (str (:scheme box) "://" (:host box) ":" (:port box) (:url opts))
        res @(http/request
              {:url url
               :method (or (:method opts) :get)
               :basic-auth [(:id app) (:secret app)]
               :headers {"content-type" "application/json"}
               :body (when (:body opts) (generate-json (:body opts)))})]
    (update res :body parse-json)))

(defn read
  "res - resource"
  [ctx res]
  (let [res (request ctx {:url (str "/" (:resourceType res) "/" (:id res))})]
    (if (and (:status res) (< (:status res) 300))
      (:body res)
      (do (println "ERROR: " res)
          (throw (Exception. (pr-str res)))))))

(defn create
  [ctx res]
  (let [res (request ctx {:url (str "/" (:resourceType res))
                              :method :post
                              :body res})]
    (if (and (:status res) (< (:status res) 300))
      (:body res)
      (do (println "ERROR: " res)
          (throw (Exception. (pr-str res)))))))

(defn update
  [ctx res]
  (let [res (request ctx {:url (str "/" (:resourceType res) "/" (:id res))
                              :method :put
                              :body res})]
    (if (and (:status res) (< (:status res) 300))
      (:body res)
      (do (println "ERROR: " res)
          (throw (Exception. (pr-str res)))))))
