(ns aidbox.sdk.core
  (:require [clojure.tools.logging :as log]
            [org.httpkit.server :as server]
            [org.httpkit.client :as http]
            [aidbox.sdk.crud :as crud]
            [aidbox.sdk.utils :refer [parse-json generate-json]]
            [cheshire.core :as json]))

(defn build-manifest [{m :manifest env :env :as ctx}]
  (assoc (:manifest ctx)
         :resourceType "App"
         :apiVersion 1
         :endpoint {:url (:app-url env)
                    :type "http-rpc"
                    :secret (:app-secret env)}
         :type (or (:type ctx) "app")))

(defn- init [{env :env :as ctx}]
  ;; send init request
  ;; (crud/create ctx (build-manifest ctx))
  (let [resp @(http/request
              {:url (str (:init-url env) "/App/$init")
               :method :post
               :basic-auth [(:init-client-id env) (:init-client-secret env)]
               :headers {"content-type" "application/json"}
               :body (generate-json {:url (:app-url env)
                                     :app_id (:app-id env)
                                     :secret (:app-secret env)})})]
    (println (:body resp))
    resp))

(defmulti endpoint (fn [ctx {id :id}] (keyword id)))
(defmulti subscription (fn [ctx {handler :handler}] (keyword handler)))

(defmethod endpoint
  :default
  [ctx {id :id}]
  {:status 404
   :body {:message (str "Endpoint [" id "] not found")}})

(defmethod subscription
  :default
  [ctx {handler :handler}]
  (println "Subscription handler [" (keyword handler) "] is not registered"))

(defonce *server (atom nil?))

(defn dispatch [ctx req]
  (let [req (update req :body parse-json)]
    (let [a-req (:body req)]
      (cond (= "operation" (:type a-req))
            (let [op-id (get-in a-req [:operation :id])
                  box (:box a-req)
                  client (get-in @(:state ctx) [:boxes (:base-url box) :client])
                  resp (try (endpoint (assoc ctx :box box :client client)
                                      (assoc (:request a-req) :id (keyword op-id)))
                            (catch Exception e
                              {:status 500 :body {:message (pr-str e)}}))]
              (-> resp
                  (update :status (fn [x] (or x 200)))
                  (update :headers (fn [x] (merge (or x {}) {"content-type" "application/json"})))
                  (update :body (fn [x] (when x (generate-json x))))))

            (= "manifest" (:type a-req))
            {:status 200
             :headers {"content-type" "application/json"}
             :body (json/generate-string {:manifest (build-manifest ctx)})}

            (= "config" (:type a-req))
            (do
              (swap! (:state ctx) (fn [s]
                                    (println "Config" a-req)
                                    (assoc-in s [:boxes (get-in a-req [:box :base-url])] a-req)))
              {:status 200
               :headers {"content-type" "application/json"}
               :body (json/generate-string {})})
            (= "subscription" (:type a-req))
            (let [e (:event a-req)]
              (println "Subscription: " a-req)
              (subscription ctx a-req)
              {:status 200
               :headers {"content-type" "application/json"}
               :body (json/generate-string {})})
            :else
            {:status 422
             :headers {"content-type" "application/json"}
             :body (json/generate-string {:message (str "Unknown message type [" (:type a-req) "]")})}))))

(defn stop [state]
  (when-let [s @state]
    (try
      ((:server s))
      (catch Exception e (log/info "ups; can not stop server")))
    (swap! state dissoc :server)))

(defn start* [state {env :env :as ctx}]
  (stop state)
  (let [port (when-let [p (:app-port env)]
               (if (int? p) p (Integer/parseInt p)))
        ctx (assoc ctx :state state)
        server (server/run-server (fn [req] (dispatch ctx req)) {:port port})]
    (log/info (str "Listening port " port "..."))
    (init ctx)
    (swap! state assoc :server server)
    ctx))
