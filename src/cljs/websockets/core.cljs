(ns ^:figwheel-hooks websockets.core
    (:require [cljs-utils.core :as utils :refer [by-id]]
              [cljs.core.match :refer-macros [match]]
              [reitit.frontend :as rf]
              [reitit.frontend.easy :as rfe]
              [react :as react]
              [react-dom :as dom]
              [websockets.routing-table :as rt]
              [goog.string :as gstring])    
    (:require-macros [cljs-utils.compilers.hicada :refer [html]])
    (:import [goog Uri]))

;;;;
(defn dev-mode? []
  (let [url (.parse Uri js/window.location)
        domain (.getDomain url)]
    (or (= 3085 (.getPort url)) (gstring/startsWith domain "localhost"))))

(if (dev-mode?)
  (enable-console-print!)
  (set! *print-fn*
        (fn [& args]
          (do))))
;;;

(defn faq []
  (html [:p "FAQ"]))

(defn support []
  (html [:p "Support"]))

(defn about []
  (html [:p "About"]))

(defn banner []
  (html [:div
         [:a {:href "/"} "Home | "]
         [:a {:href "/faq"} "FAQ | "]
         [:a {:href "/about"} "About | "]
         [:a {:href "/support"} "Support"]]))

(defonce app-state (atom {}))

(defn ws []
  (let [[socket setSocket] (react/useState nil)]
    (html [:div
           [:p "Let's do some websocketing"]
           (when (seq @app-state) [:p "Global state" (pr-str @app-state)])
           (if socket             
             [:div
              [:button {:onClick (fn [_] (.send (:socket @app-state) "hello"))} "Say hi"]
              [:button {:onClick (fn [_] (.send (:socket @app-state) [(js/self.crypto.randomUUID)]))} "Say random"]]
             [:button {:onClick (fn [_] (let [socket (js/WebSocket. "ws://localhost:3085/wsck")]
                                         (.dir js/console socket)
                                         (.addEventListener socket "message" (fn [e] (.dir js/console e)
                                                                               (.log js/console "on message event")
                                                                               (println (.-data e))))
                                         (.addEventListener socket "open" (fn [e] (.dir js/console e)
                                                                            (.send socket "Hello server!")))
                                         (.addEventListener socket "error" (fn [e] (.dir js/console e)
                                                                             (.log js/concole "an error occured")))
                                         (.addEventListener socket "close" (fn [e] (.dir js/console e)
                                                                             (.log js/console "socket closed")))
                                         (swap! app-state assoc :socket socket)
                                         (setSocket socket)))} "Connect WS"])])))

(defn pages [props]
  (match [(:view (js->clj props :keywordize-keys true))]
         [:root] (html [:*
                        [:p "Home"]
                        [:> ws]])
         [:support] (html [:> support {}])
         [:faq] (html [:> faq {}])
         [:about] (html [:> about {}])))

(defn router []
  (let [[view setView] (react/useState :root)]
    (js/React.useEffect (fn []
                          (rfe/start!
                           (rf/router rt/routes)
                           (fn [m]
                             (setView (:name (:data m))))
                           {:use-fragment false})
                          (fn [] (println "router cleanup") #js [])))
    (html [:*
           [:> banner {}]
           [:> pages {:view view}]])))

(defn mount []
  (when-not (some? (:root @app-state))
    (let [tag (by-id "app")
          root (dom/createRoot tag)
          router (html [:> router {}])
          strict-mode (react/createElement react/StrictMode nil router)]
      (if (dev-mode?)
        (do (swap! app-state assoc :root root)
            (.render root strict-mode))
        (.render root router)))))

(defonce init (mount))

(defn ^:before-load my-before-reload-callback []
    (println "BEFORE reload!"))

(defn ^:after-load my-after-reload-callback []
  (println "AFTER reload!")
  (.render (:root @app-state) (react/createElement react/StrictMode nil (html [:> router {}]))))

