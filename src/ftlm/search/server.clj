(ns ftlm.search.server
  (:require
   [integrant.core :as ig]
   [ring.adapter.jetty :as jetty]

   [muuntaja.core :as m]
   [ring.middleware.gzip :refer [wrap-gzip]]
   [ring.middleware.defaults :refer [api-defaults] :as ring-defaults]
   [ring.middleware.session.memory :as memory]
   [ring.util.response :as resp]

   [reitit.ring :as ring]
   [reitit.coercion.spec]
   [reitit.ring.coercion :as rrc]

   [reitit.ring.middleware.defaults :refer [ring-defaults-middleware]]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.ring.middleware.parameters :as parameters]))

(defn search! [{:keys [params] :as req}]
  {:body (prn-str {:foo :bar})})

(defmethod ig/init-key :handler/handler [_ _]
  (ring/ring-handler
   (ring/router
    ["/" {:post search!}]
    {:data {:coercion reitit.coercion.spec/coercion
            :muuntaja m/instance
            :middleware [parameters/parameters-middleware
                         rrc/coerce-request-middleware
                         muuntaja/format-response-middleware]}})
   (ring/routes (ring/create-default-handler))
   {:middleware
    [{:wrap wrap-gzip}
     ring-defaults-middleware]
    :defaults
    api-defaults}))

(defmethod ig/init-key :adapter/jetty [_ {:keys [handler] :as opts}]
  (jetty/run-jetty handler (-> opts (dissoc :handler) (assoc :join? false))))

(defmethod ig/halt-key! :adapter/jetty [_ server]
  (.stop server))

(comment
  )
