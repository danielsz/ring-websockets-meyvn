(ns websockets.routing-table)

(def routes [["/" {:name :root}]
             ["/faq" {:name :faq}]
             ["/about" {:name :about}]
             ["/wsck" {:name :websocket}]
             ["/support" {:name :support}]])
