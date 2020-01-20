(ns app.core
  (:require [uix.core.alpha :as uix.core]
            [clojure.string :as str]))

(set! *warn-on-infer* true)

(def react (js/require "react"))
(def proton-native (js/require "proton-native"))

(def App (.-App proton-native))
(def Window (.-Window proton-native))
(def View (.-View proton-native))
(def Text (.-Text proton-native))
(def TouchableOpacity (.-TouchableOpacity proton-native))

(defn register-component! [component-name component-instance]
  (.registerComponent
    (.-AppRegistry proton-native)
    component-name
    component-instance))

(defn update-proxy! [component]
  (.updateProxy (.-AppRegistry proton-native) component))

(def button-styles
  {:primary {:bg-color "#FC9E34"
             :text-color :white
             :text-size 40}
   :secondary {:bg-color "#A4A4A4"
               :text-color "#010101"
               :text-size 30}
   :number {:bg-color "#363636"
            :text-color :white
            :text-size 40}})

(defn button
  [{:keys [bg-color text-color text-size
           width start? on-press]}
   child]
  [:> TouchableOpacity
   {:on-press on-press
    :style {:background-color bg-color
            :border-radius 40
            :height 80
            :width (or width 80)
            :align-items (if start? :flex-start :center)
            :justify-content :center}}
   [:> Text {:style {:color text-color
                     :font-size text-size
                     :margin-left (if start? 25 0)}}
    child]])

(defn buttons-group [idx buttons]
  [:> View {:style {:flex 1
                    :flex-direction :row
                    :justify-content :space-evenly}}
   (map-indexed
     (fn [jdx btn]
       ^{:key (str jdx idx)}
       [button (merge (button-styles (:type btn))
                      (select-keys btn [:on-press :width :start?]))
        (:text btn)])
     buttons)])

(defn app-window [{:keys [width height background-color]} child]
  [:> App
   [:> Window {:style {:width width
                       :height height
                       :background-color background-color}}
    child]])

(defn update-operator [state next-operator]
  (let [{:keys [secondary primary operator]} @state
        update-state! #(swap! state merge {:secondary 0 :primary %
                                           :operator next-operator :changed? true})]
    (case operator
      + (update-state! (+ secondary primary))
      - (update-state! (- secondary primary))
      / (update-state! (/ secondary primary))
      * (update-state! (* secondary primary))
      (swap! state merge {:operator next-operator :changed? true}))))

(defn add-digit [state digit]
  (let [{:keys [changed? decimal? primary]} @state
        update-state! #(swap! state merge %)]
    (cond
      changed? (if decimal?
                 (update-state! {:secondary primary
                                 :primary (/ digit 10)
                                 :changed? false})
                 (update-state! {:secondary primary
                                 :primary digit
                                 :changed? false}))
      (not decimal?) (update-state! {:primary (+ digit (* 10 primary))})
      decimal? (if-not (str/includes? (str primary) ".")
                 (update-state! {:primary (-> (str primary "." digit)
                                              js/parseFloat)})
                 (update-state! {:primary (-> (str primary digit)
                                              js/parseFloat)})))))

(defn gen-buttons [state]
  (let [on-press #(swap! state merge %)
        {:keys [primary]} @state]
    [[{:text "AC"
       :type :secondary
       :on-press #(on-press {:primary 0 :secondary 0 :operator ""
                             :decimal? false :changed? false})}
      {:text "+/-"
       :type :secondary
       :on-press #(on-press {:primary (- primary)})}
      {:text "%"
       :type :secondary
       :on-press #(on-press {:primary (/ primary 100)})}
      {:text "÷"
       :type :primary
       :on-press #(update-operator state '/)}]
     [{:text "7"
       :type :number
       :on-press #(add-digit state 7)}
      {:text "8"
       :type :number
       :on-press #(add-digit state 8)}
      {:text "9"
       :type :number
       :on-press #(add-digit state 9)}
      {:text "×"
       :type :primary
       :on-press #(update-operator state '*)}]
     [{:text "4"
       :type :number
       :on-press #(add-digit state 4)}
      {:text "2"
       :type :number
       :on-press #(add-digit state 2)}
      {:text "6"
       :type :number
       :on-press #(add-digit state 6)}
      {:text "-"
       :type :primary
       :on-press #(update-operator state '-)}]
     [{:text "1"
       :type :number
       :on-press #(add-digit state 1)}
      {:text "2"
       :type :number
       :on-press #(add-digit state 2)}
      {:text "3"
       :type :number
       :on-press #(add-digit state 3)}
      {:text "+"
       :type :primary
       :on-press #(update-operator state '+)}]
     [{:text "0"
       :type :number
       :width 185
       :start? true
       :on-press #(add-digit state 0)}
      {:text "."
       :type :number
       :on-press #(on-press {:decimal? true})}
      {:text "3"
       :type :number
       :on-press #(add-digit state 3)}
      {:text "="
       :type :primary
       :on-press #(update-operator state '+)}]]))

(defn calculator []
  (let [state (uix.core/state {:secondary 0 :primary 0 :operator ""
                               :changed? false :decimal? false})]
    [:<>
     [:> View {:style {:width "100%"
                       :height "30%"
                       :justify-content :flex-end
                       :align-items :flex-end}}
      [:> Text {:style {:color :white
                        :font-size 80
                        :text-align :right
                        :margin-right 35
                        :margin-bottom 15
                        :font-weight 200}}
       (if (>= (count (str (:primary @state))) 7)
         (.toExponential ^js/Number (:primary @state) 4)
         (:primary @state))]]
     (->> (gen-buttons state)
          (map-indexed
            (fn [idx group]
              ^{:key idx}
              [buttons-group idx group])))]))

(defn app []
  [app-window {:width 450
               :height 900
               :background-color "#000"}
   [calculator]])

(defn root []
  (uix.core/as-element [app]))

(defonce __init
  (->> (.createElement react root)
       (register-component! "app")))

(defn ^:after-load update! []
  (update-proxy! root))
