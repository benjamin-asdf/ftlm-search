(ns ftlm.search.config
  (:require [clojure.java.io :as io]))

(def config
  (merge
   {:ftlm-search/public-dir "/home/benj/repos/faster-than-light-memes/public/"}
   (when (.exists (io/file "config.edn"))
     (read-string (slurp "config.edn")))))
