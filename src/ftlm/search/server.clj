(ns ftlm.search.server
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]

   [integrant.core :as ig]
   [ring.adapter.jetty :as jetty]

   [muuntaja.core :as m]
   [ring.middleware.gzip :refer [wrap-gzip]]
   [ring.middleware.defaults :as ring-defaults]
   [ring.util.response :as resp]
   [reitit.coercion.malli]

   [reitit.ring :as ring]
   [reitit.coercion.spec]
   [ftlm.search.config :refer [config]]

   [reitit.ring.middleware.defaults]))

(set! *warn-on-reflection* true)

(defn find-content! [file search-str]
  (with-open [rdr (io/reader file)]
    (doall
     (->>
      (line-seq rdr)
      (sequence
       (comp
        (remove #(re-find #"^\s*#" %))
        (filter
         (fn [s]
           (or (str/index-of s search-str)
               (str/index-of s (str/lower-case search-str)))))))))))

(defn file? [^java.io.File file] (.isFile file))

(defn ->filename [^java.io.File file]
  (let [p (.toPath file)]
    (.getFileName p)))

(defn search!-1 [q]
  (let [q (str/lower-case q)]
    (doall
     (sequence
      (let [dir (io/file (:ftlm-search/public-dir config) "search-index")]
        (->>
         (file-seq dir)
         (sequence
          (comp
           (filter file?)
           (keep (fn [file]
                   (when-let [lines (seq (find-content! file q))]
                     {:lines lines :path (str (->filename file) ".html")})))))))))))

(defn ->result [results q]
  {:results results
   :q q})

(defn ->no-result [q]
  {:no-result? true
   :q q})

(def sane? (comp #(< 3 % 256) count))

(defn search! [{{q :q} :body-params}]
  (if-not (sane? q)
    (resp/bad-request "Query string is not sane.")
    (let [r (search!-1 q)]
      {:status 200
       :body
       (if (seq r)
         (->result r q)
         (->no-result q))})))

(defmethod ig/init-key :handler/handler [_ _]
  (ring/ring-handler
   (ring/router
    ["/search"
     {:post
      {:parameters {:body {:q [:string]}}
       :handler search!}}]
    {:data
     {:coercion reitit.coercion.malli/coercion
      :muuntaja m/instance
      :defaults ring-defaults/api-defaults
      :middleware
      (concat
       [{:wrap wrap-gzip}]
       reitit.ring.middleware.defaults/defaults-middleware)}})
   (ring/routes (ring/create-default-handler))))

(defmethod ig/init-key :adapter/jetty [_ {:keys [handler] :as opts}]
  (jetty/run-jetty handler (-> opts (dissoc :handler) (assoc :join? false))))

(defmethod ig/halt-key! :adapter/jetty [_ ^org.eclipse.jetty.server.Server server]
  (.stop server))

(comment
  (search!-1 "Alternatively")
  (search!-1 "Some"))
