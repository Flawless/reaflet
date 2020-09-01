(ns reaflet.core
  (:require [garden.core :as gc]
            [re-frame.core :as rf]
            [reaflet.util :refer [leaflet-component]]
            [reagent.dom :as rd]))

#_(defn index []
  [:html
   [:head
    [:style style]]
   [:body
    [:div
     [work-view]
     [:hr]
      [db-state]]]])

(rf/reg-event-db
 :init
 (fn [db _]
   {:geometries
    [{:type :polygon
      :coordinates [[65.1 25.2]
                    [65.15 25.2]
                    [65.125 25.3]]}

     {:type :line
      :coordinates [[65.3 25.0]
                    [65.4 25.5]]}]
    :view-position [65.1 25.2]
    :zoom-level 8
    :drawing false}))

(rf/reg-sub
 :geometries
 (fn [db _]
   (:geometries db)))

(rf/reg-sub
 :view-position
 (fn [db _]
   (:view-position db)))

(rf/reg-sub
 :zoom-level
 (fn [db _]
   (:zoom-level db)))

(rf/reg-sub
 :drawing
 (fn [db _]
   (:drawing db)))

(rf/reg-event-db
 :set-view-position
 (fn [db [_ pos]]
   (assoc db :view-position pos)))

(rf/reg-event-db
 :set-zoom-level
 (fn [db [_ lvl]]
   (assoc db :zoom-level lvl)))

(defn demo []
  (let [drawing (rf/subscribe [:drawing])]
    (fn []
      (let [view-position (rf/subscribe [:view-position])
            zoom-level (rf/subscribe [:zoom-level])
            geometries (rf/subscribe [:geometries])]
        [:span
         [:link {:rel "stylesheet"
                 :href "https://unpkg.com/leaflet@1.6.0/dist/leaflet.css"
                 :integrity "sha512-xwE/Az9zrjBIphAcBb3F6JVqxf46+CDLwfLMHloNu6KEQCAWi6HcDUbeOfBIptF7tcCzusKFjFw2yuvEpDL9wQ=="
                 :crossorigin ""}]
         [leaflet-component {:id "kartta"
                   :width "100%" :height "300px" ;; set width/height as CSS units
                   :view view-position ;; map center position
                   :zoom zoom-level ;; map zoom level

                   ;; The actual map data (tile layers from OpenStreetMap), also supported is
                   ;; :wms type
                   :layers [{:type :tile
                             :url "http://{s}.tile.osm.org/{z}/{x}/{y}.png"
                             :attribution "&copy; <a href=\"http://osm.org/copyright\">OpenStreetMap</a> contributors"}]

                   ;; Geometry shapes to draw to the map
                   :geometries geometries

                   ;; Add handler for map clicks
                   #_:on-click #_(when @drawing
                                   ;; if drawing, add point to polyline
                                   (swap! geometries
                                          (fn [geometries]
                                            (let [pos (dec (count geometries))]
                                              (assoc geometries pos
                                                     {:type :line
                                                      :coordinates (conj (:coordinates (nth geometries pos))
                                                                         %)})))))}
          ]
         [:div.actions
          "Control the map position/zoom by swap!ing the atoms"
          [:br]
          [:button
           {:on-click #(rf/dispatch [:set-view-position
                                     (update @view-position 1 - 0.2)])} "left"]
          [:button
           {:on-click #(rf/dispatch [:set-view-position
                                     (update @view-position 1 + 0.2)])} "right"]
          [:button
           {:on-click #(rf/dispatch [:set-view-position
                                     (update @view-position 0 + 0.2)])} "up"]
          [:button
           {:on-click #(rf/dispatch [:set-view-position
                                     (update @view-position 0 - 0.2)])} "down"]
          [:button {:on-click #(rf/dispatch [:set-zoom-level
                                             (inc @zoom-level)])} "zoom in"]
          [:button {:on-click #(rf/dispatch [:set-zoom-level
                                             (dec @zoom-level)])} "zoom out"]]

         #_(if @drawing
             [:span
              [:button {:on-click #(do
                                     (swap! geometries
                                            (fn [geometries]
                                              (let [pos (dec (count geometries))]
                                                (assoc geometries pos
                                                       {:type :polygon
                                                        :coordinates (:coordinates (nth geometries pos))}))))
                                     (reset! drawing false))}
               "done drawing"]
              "start clicking points on the map, click \"done drawing\" when finished"]

             [:button {:on-click #(do
                                    (.log js/console "drawing a poly")
                                    (reset! drawing true)
                                    (swap! geometries conj {:type :line
                                                            :coordinates []}))} "draw a polygon"])

         [:div.info
          [:b "current view pos: "] (pr-str @view-position) [:br]
          [:b "current zoom level: "] (pr-str @zoom-level)]

         ]))))

(defn init! []
  (rf/dispatch-sync [:init])
  #?(:cljs (rd/render [demo] (.getElementById js/document "app"))))
(init!)
