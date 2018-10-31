(ns aidbox.sdk.core
  (:require [org.httpkit.client :as http]
            [cheshire.core :as json]))

(defn query
  "sql - sql string"
  [sql]

  )

(defn- init-manifest [{box :box app :app :as opts} m]
  (let [url (str (:scheme box) "://" (:host box) ":" (:port box) "/" "App")
        _ (println "URI" url)
        resp @(http/request
               {:url url
                :method :post
                :basic-auth [(:id app) (:secret app)]
                :headers {"content-type" "application/json"}
                :body (json/generate-string
                       (assoc m
                              :resourceType "App"
                              :apiVersion 1
                              :type (or (:type m) "app")))})]
    (println resp)
    resp))

(defmulti endpoint (fn [{id :id}] (keyword id)))

(defn start [{env :env :as m}]
  (init-manifest env (dissoc m :env))
  ;; post manifest - register app
  ;; start web server listen / for posts from aidbox 
  ;; dispatch

  )
