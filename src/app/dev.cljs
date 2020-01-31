(ns app.dev
  (:require [clojure.string :as str]
            [goog.string :as gstring]
            [uix.core.alpha :as uix.core]))

(def proton-native (js/require "proton-native"))

(def View (.-View proton-native))
(def Text (.-Text proton-native))

(defonce dev-warnings (atom nil))
(defonce dev-errors (atom nil))
(defonce dev-runtime-errors (atom nil))

(defn use-atom [ref key]
  (uix.core/subscribe
    (uix.core/memo
      (fn []
        {:get-current-value (fn [] @ref)
         :subscribe (fn [schedule-update!]
                      (add-watch ref key
                                 (fn [_ _ old-v new-v]
                                   (when (not= old-v new-v)
                                     (schedule-update!))))
                      #(remove-watch ref key))})
      [ref])))

(def sep-length 80)

(defn sep-line
  ([]
   (sep-line "" 0))
  ([label offset]
   (let [sep-len (Math/max sep-length offset)
         len (count label)

         sep
         (fn [c]
           (->> (repeat c "-")
                (str/join "")))]
     (str (sep offset) label (sep (- sep-len (+ offset len)))))))

(defn source-line [start-idx lines]
  (for [[idx text] (map-indexed vector lines)]
    [:> Text (gstring/format "%4d | %s" (+ 1 idx start-idx) text)]))

(defn warning-view [{:keys [resource-name source-excerpt msg] :as warning}]
  [:> View {:style {:background-color "yellow"
                    :padding 8}}
   [:> Text {:style {:font-weight 700
                     :margin-bottom 8}}
    (str "WARNING in " resource-name)]
   (when source-excerpt
     (let [{:keys [start-idx before line column after]} source-excerpt
           arrow-idx (+ 6 (or column 1))]
       [:> View {:style {:background-color "#fff"
                         :padding 4}}
        [source-line start-idx before]
        [source-line (+ start-idx (count before)) [line]]
        [:> Text (sep-line "^" arrow-idx)]
        [:> Text msg]
        [source-line (+ start-idx (count before) 1) after]]))])

;(defn x)
;(kashd)

(defn error-view [{:keys [report]}]
  [:> View {:style {:background-color "red"
                    :padding 8}}
   [:> View {:style {:background-color "#fff"
                     :padding 4}}
    (for [line (str/split report "\n")]
      [:> Text line])]])

(defn runtime-error-view [^js err]
  (let [msg (.-message err)
        stack (.-stack err)]
    [:> View {:style {:background-color "red"
                      :padding 8}}
     [:> Text {:style {:font-weight 700
                       :margin-bottom 8}}
      (str "Runtime error: " msg)]
     [:> View {:style {:background-color "#fff"
                       :padding 4}}
      (for [line (str/split stack "\n")]
        [:> Text line])]]))

(defn heads-up-display []
  (let [warnings (use-atom dev-warnings ::dev-warnings)
        errors (use-atom dev-errors ::dev-errors)
        runtime-errors (use-atom dev-runtime-errors ::dev-runtime-errors)]
    (when (or (seq warnings) errors runtime-errors)
      [:> View {:style {:position :absolute
                        :bottom 0
                        :left 0
                        :width "100%"}}
       (cond
         (seq warnings)
         (for [{:keys [warnings]} warnings
               warning warnings]
           [warning-view warning])

         errors
         [error-view errors]

         runtime-errors
         [runtime-error-view runtime-errors])])))

(defn on-build-complete [type msg]
  (case type
    :warning
    (->> msg
         :info
         :sources
         (remove :from-jar)
         (filter #(seq (:warnings %)))
         (into [])
         (reset! dev-warnings))

    :error
    (reset! dev-errors msg)

    nil))

(defonce original-process-message
  shadow.cljs.devtools.client.node/process-message)

(defn process-message [msg done]
  (case (:type msg)
    :build-complete (on-build-complete :warning msg)
    :build-failure (on-build-complete :error msg)
    nil)
  (original-process-message msg done))

(set! (.. js/shadow -cljs -devtools -client -node -process-message) process-message)

(defonce __init-error-handler
  (.on js/process "uncaughtException"
       #(reset! dev-runtime-errors %)))

(defn ^:dev/before-load before-load! []
  (reset! dev-warnings nil)
  (reset! dev-errors nil)
  (reset! dev-runtime-errors nil))

