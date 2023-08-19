(ns ftlm.search.prod
  (:gen-class)
  (:require
   [ftlm.search.server]
   [ftlm.search.system]))

(defn -main [& _]
  (ftlm.search.system/start!))

(comment
  (ftlm.search.system/restart)
  ;; http://localhost:8094
  )
