(ns build
  "build electric.jar library artifact and demos"
  (:require
   [clojure.tools.build.api :as b]
   [org.corfield.build :as bb]
   [babashka.fs :as fs]))

(def lib 'benjamin/ftlm-search)
(def version (b/git-process {:git-args "describe --tags --long --always --dirty"}))
(def basis (b/create-basis {:project "deps.edn"}))

(defn clean [opts]
  (bb/clean opts))

(def class-dir "target/classes")
(defn default-jar-name [{:keys [version] :or {version version}}]
  (format "target/%s-%s-standalone.jar" (name lib) version))

(defn uberjar [{:keys [jar-name version]
                :or   {version version}}]
  (println "Cleaning up before build")
  (clean nil)

  (println "Bundling sources")
  (b/copy-dir {:src-dirs   ["src" "resources"]
               :target-dir class-dir})

  (println "Compiling server. Version:" version)
  (b/compile-clj {:basis      basis
                  :src-dirs   ["src"]
                  :ns-compile '[ftlm.search.prod]
                  :class-dir  class-dir})

  (let [uber-file (str (or jar-name (default-jar-name {:version version})))]
    (println "Building uberjar")
    (b/uber {:class-dir class-dir
             :uber-file  uber-file
             :basis     basis
             :main      'ftlm.search.prod})

    (println "Setting up run scripts")
    (fs/delete-if-exists "release.jar")
    (fs/create-sym-link "release.jar" uber-file)))

(defn noop [_])                         ; run to preload mvn deps

(comment
  )
