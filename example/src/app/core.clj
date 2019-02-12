(ns app.core
  (:require [aidbox.sdk.core :as aidbox]))

(def env
  {:init-url "http://devbox:8080"
   :init-client-id (or (System/getenv "APP_INIT_CLIENT_ID") "root")
   :init-client-secret (or (System/getenv "APP_INIT_CLIENT_SECRET") "secret")

   :app-id (or (System/getenv "APP_ID") "app-example")
   :app-url (or (System/getenv "APP_URL") "http://app:8989")
   :app-port (Integer/parseInt (or (System/getenv "APP_PORT") "8989"))
   :app-secret (or (System/getenv "APP_SECRET") "secret")})

(def ctx
  {:env env
   :manifest {:id "app-example"
              :type "app"
              :subscriptions {:User {:handler ::on-user-change-callback}}
              }})

(defmethod aidbox/subscription
  ::on-user-change-callback
  [ctx sub]
  (println "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!" sub))

(defonce state (atom {}))

(defn -main []
  (println "Starting with")
  (clojure.pprint/pprint ctx)
  (aidbox/start* state ctx)
  )

(comment
 (-main)
 (aidbox/stop state)
 (reset! state {})
 (keys (:boxes @state))
 )

