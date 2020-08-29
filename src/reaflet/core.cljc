(ns reaflet.core
  (:require [garden.core :as gc]
            [re-frame.core :as rf]
            [reaflet.util :refer [leaflet]]
            [reagent.dom :as rd]))

(def style
  (gc/css
   [:body
    [:.wrapper {:display "flex"
                :flex-wrap "wrap"}
     [:div.button {:flex "1 1 100px"
                   :border "none"
                   :padding "5px 20px"
                   :text-align "center"
                   :text-decoration "none"
                   :font-size "8px"}]]]))

(rf/reg-sub
 ::db
 (fn [db] db))

(rf/reg-sub
 ::model
 (fn [db] db))

(defn db-state []
  [:pre (pr-str @(rf/subscribe [::db]))])

(defn work-view
  []
  (let [m @(rf/subscribe [::model])]
    [:div
     [:pre (pr-str m)]]))

(defn index []
  [:html
   [:head
    [:script {:src "http://cdn.leafletjs.com/leaflet-0.7.3/leaflet.js"}]
    [:style style]]
   [:body
    [:div
     [work-view]
     [:hr]
      [db-state]]]])

(def geometries (atom [{:type :polygon
                        :coordinates [[65.1 25.2]
                                      [65.15 25.2]
                                      [65.125 25.3]]}

                       {:type :line
                        :coordinates [[65.3 25.0]
                                      [65.4 25.5]]}]))

(def view-position (atom [65.1 25.2]))
(def zoom-level (atom 8))

(defn demo []
  (let [drawing (atom false)]
    (fn []
    [:span
     [leaflet {:id "kartta"
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
               :on-click #(when @drawing
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
      [:button {:on-click #(swap! view-position update-in [1] - 0.2)} "left"]
      [:button {:on-click #(swap! view-position update-in [1] + 0.2)} "right"]
      [:button {:on-click #(swap! view-position update-in [0] + 0.2)} "up"]
      [:button {:on-click #(swap! view-position update-in [0] - 0.2)} "down"]
      [:button {:on-click #(swap! zoom-level inc)} "zoom in"]
      [:button {:on-click #(swap! zoom-level dec)} "zoom out"]]

     (if @drawing
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

   ])))

(defn init! []
  ;; (rf/dispatch [::init])
  #?(:cljs (rd/render [demo] (.getElementById js/document "app"))))
(init!)