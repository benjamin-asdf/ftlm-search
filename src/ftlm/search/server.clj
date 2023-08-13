(ns ftlm.search.server
  (:require
   [integrant.core :as ig]

   [ring.adapter.jetty :as jetty]

   [ring.util.response :as resp]
   [muuntaja.core :as m]
   [ring.middleware.gzip :refer [wrap-gzip]]
   [ring.middleware.defaults :as ring-defaults]
   [reitit.coercion.malli]

   [reitit.ring :as ring]
   [reitit.coercion.spec]

   [reitit.ring.middleware.defaults]))

;; (defn search! [{:keys [params] :as req}]
;;   {:body (prn-str {:foo :bar})})

(defmethod ig/init-key :handler/handler [_ _]
  (ring/ring-handler
   (ring/router
    ["/search"
     {:post
      {:parameters {:body {:q [:string]}}
       :handler (fn
                  [e]
                  (->
                   {:body {:foo (-> e :body-params :q)}}
                   (resp/status 200)))}}]
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
  )
