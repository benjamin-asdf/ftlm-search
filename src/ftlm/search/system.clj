(ns ftlm.search.system
  (:require
   [integrant.core :as ig]))

(defonce system (atom nil))

(def config
  {:adapter/jetty {:port 8094
                   :handler (ig/ref :handler/handler)}
   :handler/handler {}})

(defn start! []
  (reset! system (ig/init config))
  (println "Started server on " (-> config :adapter/jetty :port)))

(defn halt! []
  (when-let [system @system] (ig/halt! system)))

(defn restart []
  (halt!)
  (start!))
