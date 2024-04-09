(ns websockets.handler
    (:require [reitit.ring :as ring]
              [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
              [ring.util.response :refer [response content-type]]
              [websockets.routing-table :refer [routes]]
              [websockets.html :as html]
              [ring.adapter.jetty9 :as jetty]
              [ring.websocket :as ringws]
              [ring.websocket.protocols :as ws]
              [clojure.core.async :as a :refer [thread <!!]]
              [clojure.tools.logging :as log]))

(defn keep-alive [socket]
  (thread
    (while (ws/-open? socket)
      (<!! (a/timeout 1000))
      (ws/-ping socket nil))))

(defn ws-handler [upgrade-request]
  {:ring.websocket/listener
   {:on-open (fn on-connect [ws]
               (log/info "connect" (:headers upgrade-request))
               (keep-alive ws))
    :on-message (fn on-text [ws text-message]
                  (log/info "received msg:" text-message)
                  (ringws/send ws (str "echo: " text-message)))
    :on-close (fn on-close [_ status-code reason]
                (log/info "closed" status-code reason))
    :on-pong (fn on-pong [_ _]
               (log/debug "pong"))
    :on-error (fn on-error [_ throwable]
                (.printStackTrace throwable)
                (log/error (.getMessage throwable)))}})

(defn bidi-routes [_]
  (let [index (fn [req] (if (jetty/ws-upgrade-request? req)
                         (ws-handler req)
                         (-> (html/index req)
                            response
                            (content-type "text/html"))))]
    (into [] (for [[r1 r2] routes] [r1 (assoc r2 :get index)]))))

(defn default-handler [_]
  (ring/routes
    (ring/create-resource-handler {:path "/" :root ""})
    (ring/create-default-handler)))

(def middleware {:middleware [[wrap-defaults site-defaults]]})
