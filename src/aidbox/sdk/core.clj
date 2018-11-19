(ns aidbox.sdk.core
  (:require [org.httpkit.server :as server]
            [aidbox.sdk.crud :as crud]
            [aidbox.sdk.utils :refer [parse-json generate-json]]))

(defn- init-manifest [ctx]
  (crud/create
   ctx
   (assoc (dissoc ctx :env)
          :resourceType "App"
          :apiVersion 1
          :endpoint (assoc (select-keys (get-in ctx [:env :app]) [:host :port :scheme])
                           :type "http-rpc")
          :type (or (:type ctx) "app"))))

(defmulti endpoint (fn [ctx {id :id}] (keyword id)))

(defmethod endpoint
  :default
  [ctx {id :id}]
  {:status 404
   :body {:message (str "Endpoint [" id "] not found")}})

(defonce *server (atom nil?))

(defn dispatch [ctx req]
  (let [req (update req :body parse-json)]
    (let [a-req (:body req) 
          op-id (get-in a-req [:operation :id])]
      (let [resp (try (endpoint ctx (assoc a-req :id (keyword op-id)))
                      (catch Exception e
                        {:status 500 :body {:message (pr-str e)}}))]
        (-> resp
            (update :status (fn [x] (or x 200)))
            (update :headers (fn [x] (merge (or x {}) {"content-type" "applicaiton/json"})))
            (update :body (fn [x] (when x (generate-json x)))))))))

(defn stop []
  (when-let [s @*server]
    (try
      ((:server s))
      (catch Exception e (println "ups; can not stop server")))
    (reset! *server nil)))

(defn start [{{app :app :as env} :env :as m}]
  (stop)
  (let [_ (init-manifest m)]
    (let [server (server/run-server
                  (fn [req] (dispatch m req))
                  {:port (:port app)})]
      (reset! *server {:server server :config m}))))


;; Naming - FHIR like or custom ??
;; :entities vs :envtity
;; :attrs vs :attribute

;; CRUD
;; Raw resources (clients jobs ....)
