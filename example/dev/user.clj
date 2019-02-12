(ns user
  (:require [cider-nrepl.main]
            [spyscope.core]
            [app.core :as app]))

(defn start-nrepl []
  (println "Starting nrepl...")

  (cider-nrepl.main/start-nrepl {:port 55555 :bind "0.0.0.0" :middleware
                                 ["refactor-nrepl.middleware/wrap-refactor"
                                  "cider.nrepl/cider-middleware"]}))

(defn -main [& args]
  (start-nrepl)
  (app/-main))
