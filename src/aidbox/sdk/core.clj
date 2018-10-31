(ns aidbox.sdk.core
  (:require [org.httpkit.client :as http]
            [org.httpkit.server :as server]
            [cheshire.core :as json]))

(defn-  parse-json [x]
  (when x
    (if (string? x)
      (json/parse-string x keyword)
      (json/parse-string (slurp x) keyword))))

(defn- box-request [ctx opts]
  (let [ctx (:env ctx)
        app (:app ctx)
        box (:box ctx)
        url (str (:scheme box) "://" (:host box) ":" (:port box) (:url opts))
        res @(http/request
              {:url url
               :method :post
               :basic-auth [(:id app) (:secret app)]
               :headers {"content-type" "application/json"}
               :body (when (:body opts) (json/generate-string (:body opts)))})]
    (update res :body parse-json)))

(defn query
  "sql - sql string"
  [ctx sql]
  (let [res (box-request ctx {:url "/$sql" :body sql})]
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


(defn- init-manifest [ctx]
  (let [resp (box-request
              ctx
              {:url "/App"
               :method :post
               :body (assoc (dissoc ctx :env)
                            :resourceType "App"
                            :apiVersion 1
                            :endpoint (assoc (select-keys (get-in ctx [:env :app]) [:host :port :scheme])
                                             :type "http-rpc")
                            :type (or (:type ctx) "app"))})]
    resp))

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
            (update :body (fn [x] (when x (json/generate-string x)))))))))

(defn stop []
  (when-let [s @*server]
    (try
      ((:server s))
      (catch Exception e (println "ups; can not stop server")))
    (reset! *server nil)))

(defn start [{{app :app :as env} :env :as m}]
  (stop)
  (let [res (init-manifest m)]
    (if (and (:status res) (< (:status res) 300))
      (let [server (server/run-server
                    (fn [req] (dispatch m req))
                    {:port (:port app)})]
        (reset! *server {:server server :config m}))
      (throw (Exception. (str "Failed to start app: " res))))))
