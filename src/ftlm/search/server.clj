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

(defn find-content! [file search-str]
  (with-open [rdr (io/reader file)]
    (doall
     (sequence
      (comp
       (drop-while
        (fn [line] (str/index-of line "div id=\"content\"" )))
       (take-while
        (complement (fn [line] (str/index-of line "div id=\"postamble\""))))
       (filter #(str/index-of % search-str)))
      (line-seq rdr)))))

(defn search!-1 [q]
  (doall
   (sequence
    (comp
     (map (fn [{:keys [path]}]
            {:file (io/file (:ftlm-search/public-dir config) path)
             :path path}))
     (keep (fn [{:keys [file path]}]
             (when-let [lines (seq (find-content! file q))]
               {:lines lines :path path}))))
    (read-string (slurp (io/file (:ftlm-search/public-dir config) "posts-list.edn"))))))

(defn ->result [results q]
  {:results results
   :q q})

(defn ->no-result [q]
  {:no-result? true
   :q q})

(def sane? (comp #(< 3  256) count))

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

(defmethod ig/halt-key! :adapter/jetty [_ server]
  (.stop server))

(comment
  (search!-1 "Alternatively")
  '({:lines ("Alternatively, if you have cider")
    :path "jacking-nbb.html"}
   {:lines ("Alternatively, I count every vertex twice and do the front face + 1 cubie on the right.")
    :path "dreams.html"}
   {:lines ("Alternatively, I would set <code>show-paren-match</code> to white. It makes it blink pretty!")
    :path "faq.html"}))
