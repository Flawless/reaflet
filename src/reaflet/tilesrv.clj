(ns reaflet.tilesrv
  (:require [clojure.java.jdbc :as jdbc]
            [honeysql.core :as sql]
            [zlib-tiny.core :as zlib]
            [ring.adapter.jetty :as ring]))

(def db-spec
  {:classname   "org.sqlite.JDBC"
   :subprotocol "sqlite"
   :subname     "pngdb.db"})

(defn get-tile [x y z]
  (jdbc/with-db-connection [db-conn db-spec]
    (let [q (sql/format {:select [:value]
                         :from [:tiles_depth]
                         :where [:and [:= :zoom z] [:= :x x] [:= :y y]]})
          rows (jdbc/query db-conn q)
          data (-> rows
                   first
                   :value)]
      data)))

(defn tile-handler [rq]
  (let [path (:ring.request/path rq)
        matcher (re-matcher #"^\/(\d+)\/(\d+)\/(\d+).png$" path)
        [_ x y z] (re-find matcher)
        tile (get-tile x y z)
        not-found? (nil? tile)]
    (if not-found?
      {:status 404
       :headers {"Content-Type" "text/html"}
       :body (str x " " y " " z " not found")}
      {:status 200
       :headers {"Content-Type" "image/png"}
       :body (get-tile x y z)})))


(defonce srv
  (ring/run-jetty tile-handler {:port  3000}))
