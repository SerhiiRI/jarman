(jarman.logic.view-manager/defview user
  (table
    :id :user
    :name "Pracownicy GZWM"
    :permission :ekka-all
    :tables [:user]
    :model-configurations
    {:insert
     {:fields
      [{:field :first_name,
        :field-qualified :user.first_name
        :component
        {:type :jsgl-text, :value ""
         :on-change
         (fn [e]
           (jarman.lang/exception-> (seesaw.core/text e)
             (jarman.gui.gui-components2/check-field-re #"[^\s]{3,}" "Żadnych pustych symboli i imie dluższe niż 3 symbole")
             (clojure.string/upper-case)))

         :completion-list
         (vec (concat
                (with-open [reader (clojure.java.io/reader "female_names_pl.csv")]
                  (->> (clojure.data.csv/read-csv reader)
                    (doall) (pmap first)))
                (with-open [reader (clojure.java.io/reader "male_names_pl.csv")]
                  (->> (clojure.data.csv/read-csv reader)
                    (doall) (pmap first)))))},
        :representation "Imie"
        :description "No kurwa jak ciebie mamka z tatkiem nazwali"}
       {:field :last_name,
        :field-qualified :user.last_name
        :representation "Nazwisko"
        :description "Drugie slowo co musisz pamiętać w życiu"
        :component
        {:type :jsgl-text, :value "", :char-limit 50
         :on-change
         (fn [e]
           (jarman.lang/exception-> (seesaw.core/text e)
             (jarman.gui.gui-components2/check-field-re #"[^\s]{3,}" "Żadnych pustych symboli i nazwisko dluższe niż 3 symbole")
             (clojure.string/upper-case)))}}
       {:field :work_type,
        :field-qualified :user.work_type
        :representation "Typ pracy"
        :description "To taki przełącznik trudności gry"
        :component
        {:type :jsgl-combobox, :value "Fizyczny", :model ["Fizyczny" "Umysłowy"]
         :on-select (fn [e] e)}}
       {:field :section,
        :field-qualified :user.section
        :representation "Section",
        :description "Wybierz pokemona",
        :component
        {:type :jsgl-combobox, :value "TME", :on-select (fn [e] e)
         :model #{"TME" "HMS" "EK" "HSM" "WP" "DTP" "BHP" "EP" "TKT" "HE" "DE" "NKJ" "CHWDP"}}}
       {:field :act,
        :field-qualified :user.act
        :representation "Akt"
        :description "musi być formatu '<4-3xliczby>/<U/F>', przykład 1234/U"
        :component
        {:type :jsgl-text, :value "",
         :on-change
         (fn [e]
           (jarman.lang/exception-> (seesaw.core/text e)
             (jarman.gui.gui-components2/check-field-re #"\d{3,4}/[UF]"
               "Pattern dla aktu nie odpowiada [0-9]{3,4}/[UF]")))}}
       {:field :teta_nr,
        :field-qualified :user.teta_nr,
        :representation "Teta nr",
        :description "Musi być koniecznie liczba calkowita bez spacji!"
        :column-type {:stype "varchar(10)", :tname :varchar, :tspec nil, :tlimit 10, :tnull true, :textra "", :tdefault nil, :tkey ""},
        :component
        {:type :jsgl-text,
         :value ""
         :on-change
         (fn [e] (jarman.lang/exception-> (seesaw.core/text e)
                   (jarman.gui.gui-components2/check-field-re #"\d{3,30}"
                     "Liczba tety musi odpowiadać patternu [0-9]{3,30}")))}}]
      :additional
      [{:type :jsgl-button, :value "Dodaj nowego pracownika", :on-click :insert-action}]}
     :update
     {:fields
      [{:field :first_name,
        :field-qualified :user.first_name
        :component
        {:type :jsgl-text, :value ""
         :on-change
         (fn [e]
           (jarman.lang/exception-> (seesaw.core/text e)
             (jarman.gui.gui-components2/check-field-re #"[^\s]{3,}" "Żadnych pustych symboli i imie dluższe niż 3 symbole")
             (clojure.string/upper-case)))

         :completion-list
         (vec (concat
                (with-open [reader (clojure.java.io/reader "female_names_pl.csv")]
                  (->> (clojure.data.csv/read-csv reader)
                    (doall) (pmap first)))
                (with-open [reader (clojure.java.io/reader "male_names_pl.csv")]
                  (->> (clojure.data.csv/read-csv reader)
                    (doall) (pmap first)))))},
        :representation "Imie"
        :description "No kurwa jak ciebie mamka z tatkiem nazwali"}
       {:field :last_name,
        :field-qualified :user.last_name
        :representation "Nazwisko"
        :description "Drugie slowo co musisz pamiętać w życiu"
        :component
        {:type :jsgl-text, :value "", :char-limit 50
         :on-change
         (fn [e]
           (jarman.lang/exception-> (seesaw.core/text e)
             (jarman.gui.gui-components2/check-field-re #"[^\s]{3,}" "Żadnych pustych symboli i nazwisko dluższe niż 3 symbole")
             (clojure.string/upper-case)))}}
       {:field :work_type,
        :field-qualified :user.work_type
        :representation "Typ pracy"
        :description "To taki przełącznik trudności gry"
        :component
        {:type :jsgl-combobox, :value "Fizyczny", :model ["Fizyczny" "Umysłowy"]
         :on-select (fn [e] e)}}
       {:field :section,
        :field-qualified :user.section
        :representation "Section",
        :description "Wybierz pokemona",
        :component
        {:type :jsgl-combobox, :value "TME", :on-select (fn [e] e)
         :model #{"TME" "HMS" "EK" "HSM" "WP" "DTP" "BHP" "EP" "TKT" "HE" "DE" "NKJ" "CHWDP"}}}
       {:field :act,
        :field-qualified :user.act
        :representation "Akt"
        :description "musi być formatu '<4-3xliczby>/<U/F>', przykład 1234/U"
        :component
        {:type :jsgl-text, :value "",
         :on-change
         (fn [e]
           (jarman.lang/exception-> (seesaw.core/text e)
             (jarman.gui.gui-components2/check-field-re #"\d{3,4}/[UF]"
               "Pattern dla aktu nie odpowiada [0-9]{3,4}/[UF]")))}}
       {:field :teta_nr,
        :field-qualified :user.teta_nr,
        :representation "Teta nr",
        :description "Musi być koniecznie liczba calkowita bez spacji!"
        :column-type {:stype "varchar(10)", :tname :varchar, :tspec nil, :tlimit 10, :tnull true, :textra "", :tdefault nil, :tkey ""},
        :component
        {:type :jsgl-text,
         :value ""
         :on-change
         (fn [e] (jarman.lang/exception-> (seesaw.core/text e)
                   (jarman.gui.gui-components2/check-field-re #"\d{3,30}"
                     "Liczba tety musi odpowiadać patternu [0-9]{3,30}")))}}]
      :additional
      [{:type :jsgl-button :value "Usuń wybranego" :on-click :delete-action}
       {:type :jsgl-button :value "Aktualizuj wybranego" :on-click :update-action}]}
     :export
     {:filters
      [{:field :first_name
        :field-qualified :user.first_name
        :component
        {:type :jsgl-text :value ""
         :on-change
         (fn [e]
           (jarman.lang/exception-> (seesaw.core/text e)
             (jarman.gui.gui-components2/check-field-re #"[^\s]{3,}" "Żadnych pustych symboli i imie dluższe niż 3 symbole")
             (clojure.string/upper-case)))
         :completion-list
         (vec (concat
                (with-open [reader (clojure.java.io/reader "female_names_pl.csv")]
                  (->> (clojure.data.csv/read-csv reader)
                    (doall) (pmap first)))
                (with-open [reader (clojure.java.io/reader "male_names_pl.csv")]
                  (->> (clojure.data.csv/read-csv reader)
                    (doall) (pmap first)))))}
        :representation "Imie"
        :description "No kurwa jak ciebie mamka z tatkiem nazwali"}]}}
    :actions
    ;; ---------------------------------
    {:insert-action
     (fn [state! dispatch!]
       (jarman.org/print-line "tworzenie nowego pacownika")
       (if-let [exceptions (not-empty (filter (partial instance? Exception) (vals (:model-changes (state!)))))]
         (doall (map jarman.interaction/error-to-alert exceptions))
         (do
           (jarman.logic.connection/exec
             (jarman.logic.sql-tool/insert!
               {:table_name :user
                :set (jarman.logic.metadata/convert_model->flattfields
                       (:model-changes (state!)) :user)}))
           (dispatch! {:route :main-table :action :refresh-to-bottom})
           (dispatch! {:action :switch-plugin-ui-mode :debug true :plugin-ui-mode :insert :erease-selected? true
                       :model (jarman.logic.metadata/convert_metadata->model
                                (.return-table_name (-> (state!) :plugin-toolkit :meta-obj)))})
           (jarman.interaction/success "Table. User" "Successfully created new user"))))
     :delete-action
     (fn [state! dispatch!]
       (jarman.org/print-line "usuwanie pacownika")
       (if (get-in (state!) [:model-changes :user.id])
         (do
           (jarman.logic.connection/exec
             (jarman.logic.sql-tool/delete!
               {:table_name :user
                :where [:= :user.id (get-in (state!) [:model-changes :user.id])]}))
           (dispatch! {:route :main-table :action :refresh})
           (dispatch! {:action :switch-plugin-ui-mode :debug true :plugin-ui-mode :insert :erease-selected? true
                       :model (jarman.logic.metadata/convert_metadata->model :user)}))
         (jarman.interaction/danger "Delete user action" "ID is empty for model")))
     :update-action
     (fn [state! dispatch!]
       (jarman.org/print-line "aktualizacja pacownika")
       (if-let [user-id (get-in (state!) [:model-changes :user.id])]
         (if-let [exceptions (not-empty (filter (partial instance? Exception) (vals (:model-changes (state!)))))]
           (doall (map jarman.interaction/error-to-alert exceptions))
           (do
             (jarman.logic.connection/exec
               (jarman.logic.sql-tool/update!
                 {:table_name :user
                  :where [:= :user.id user-id]
                  :set (jarman.logic.metadata/convert_model->flattfields
                         (:model-changes (state!)) :user)}))
             (dispatch! {:route :main-table :action :refresh})
             (dispatch! {:action :switch-plugin-ui-mode :debug true :plugin-ui-mode :insert :erease-selected? true
                         :model (jarman.logic.metadata/convert_metadata->model
                                  (.return-table_name (-> (state!) :plugin-toolkit :meta-obj)))})
             (jarman.interaction/success "Table. Pracownicy GZWM" "Successfully created new user")))
         (jarman.interaction/danger "Update user action" "ID is empty for model")))}
    ;; -------------------------------
    :view-configurations
    {:socket-id :main-table
     :type :jsgl-database-table
     :tables [:user]
     :columns
     [:user.first_name
      :user.last_name
      :user.work_type
      :user.act
      :user.section
      :user.teta_nr]
     :socket-refreshable-query
     {:table_name :user,
      :column
      [:#as_is
       :user.id
       :user.first_name
       :user.last_name
       :user.work_type
       :user.act
       :user.section
       :user.teta_nr]}
     :on-select
     (fn [state! dispatch! e]
       (println (jarman.logic.metadata/convert_flattfields->model e :user))
       (dispatch!
         {:action :switch-plugin-ui-mode :plugin-ui-mode :update
          :debug? true :message "TABLE CLICK UPDATE"
          :model (jarman.logic.metadata/convert_flattfields->model e :user)}))})
  (dialog-table
    :id :user-dialog
    :name "Profile dialog"
    :permission :ekka-all
    :tables [:user]
    :view-configurations
    {:type :jsgl-database-table
     :tables  [:user]
     :columns [:user.last_name :user.first_name]
     :data
     (fn []
       (vec
         (jarman.logic.connection/query
           (jarman.logic.sql-tool/select!
             {:table_name :user
              :column
              [:#as_is
               :user.id
               :user.first_name
               :user.last_name]
              :order [:user.last_name :asc]}))))}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(jarman.logic.view-manager/defview card
  (table
    :id :card
    :name "Pracownicy GZWM"
    :permission :ekka-all
    :tables [:card :user]
    :model-configurations
    {:insert
     {:fields
      [{:field :rfid,
        :field-qualified :card.rfid
        :representation "RFID"
        :description "Radio Identyfikator dla karty"
        :component
        {:type :jsgl-text, :value ""
         :on-change
         (fn [e]
           (jarman.lang/exception-> (seesaw.core/text e)
             (jarman.gui.gui-components2/check-field-re #"[^\s]{12}" "RFID musi zawierać 12 znaków")
             (clojure.string/upper-case)))}}
       {:field :card_nr,
        :field-qualified :card.card_nr
        :representation "Numer karty"
        :description "Numer napisany zgóry dla identyfikacji karty"
        :component
        {:type :jsgl-text, :value "", :char-limit 50
         :on-change
         (fn [e]
           (jarman.lang/exception-> (seesaw.core/text e)
             (jarman.gui.gui-components2/check-field-re #"[^\s]{5,7}" "5-7 ")
             (clojure.string/upper-case)))}}
       {:field :id_user
        :field-qualified :card.id_user
        :representation "Użytkownik"
        :description "Posiadacz karty"
        :key-table :user
        :foreign-keys [{:id_user :user} {:delete :null, :update :null}]
        :component {:type :dialog-external-table
                    :expand-table :user
                    :expand-columns [:user.first_name :user.last_name]
                    :dialog-plugin-path [:user :dialog-table :user-dialog]
                    :on-select (fn [e] e)}}]
      :additional [{:type :jsgl-button, :value "Utworz nową kartę", :on-click :insert-action}]}
     :update
     {:fields
      [{:field :rfid,
        :field-qualified :card.rfid
        :representation "RFID"
        :description "Radio Identyfikator dla karty"
        :component
        {:type :jsgl-text, :value ""
         :on-change
         (fn [e]
           (jarman.lang/exception-> (seesaw.core/text e)
             (jarman.gui.gui-components2/check-field-re #"[^\s]{12}" "RFID musi zawierać 12 znaków")
             (clojure.string/upper-case)))}}
       {:field :card_nr,
        :field-qualified :card.card_nr
        :representation "Numer karty"
        :description "Numer napisany zgóry dla identyfikacji karty"
        :component
        {:type :jsgl-text, :value "", :char-limit 50
         :on-change
         (fn [e]
           (jarman.lang/exception-> (seesaw.core/text e)
             (jarman.gui.gui-components2/check-field-re #"[^\s]{5,7}" "5-7 ")
             (clojure.string/upper-case)))}}
       {:field :id_user
        :field-qualified :card.id_user
        :representation "Użytkownik"
        :description "Posiadacz karty"
        :key-table :user
        :foreign-keys [{:id_user :user} {:delete :null, :update :null}]
        :component {:type :dialog-external-table
                    :expand-table :user
                    :expand-columns [:user.first_name :user.last_name]
                    :dialog-plugin-path [:user :dialog-table :user-dialog]
                    :on-select (fn [e] e)}}]
      :additional
      [{:type :jsgl-button :value "Usuń wybraną kartę" :on-click :delete-action}
       {:type :jsgl-button :value "Aktualizuj wybraną kartę" :on-click :update-action}]}}
    :actions
    ;; ---------------------------------
    {:insert-action
     (fn [state! dispatch!]
       (jarman.org/print-line "dodawanie nowej karty...")
       (if-let [exceptions (not-empty (filter (partial instance? Exception) (vals (:model-changes (state!)))))]
         (doall (map jarman.interaction/error-to-alert exceptions))
         (do
           (jarman.logic.connection/exec
             (jarman.logic.sql-tool/insert!
               {:table_name :card
                :set (jarman.logic.metadata/convert_model->flattfields (:model-changes (state!)) :card)}))
           (dispatch! {:route :main-table :action :refresh-to-bottom})
           (dispatch! {:action :switch-plugin-ui-mode :debug true :plugin-ui-mode :insert :erease-selected? true
                       :model (jarman.logic.metadata/convert_metadata->model :card)})
           (jarman.interaction/success "Table. Card" "Successfully created new card"))))
     :delete-action
     (fn [state! dispatch!]
       (jarman.org/print-line "usuwanie pacownika")
       (if-let [card-id (get-in (state!) [:model-changes :card.id])]
         (do
           (jarman.logic.connection/exec
             (jarman.logic.sql-tool/delete!
               {:table_name :card
                :where [:= :card.id card-id]}))
           (dispatch! {:route :main-table :action :refresh})
           (dispatch! {:action :switch-plugin-ui-mode :debug true :plugin-ui-mode :insert :erease-selected? true
                       :model (jarman.logic.metadata/convert_metadata->model :card)}))
         (jarman.interaction/danger "Delete user action" "ID is empty for model")))
     :update-action
     (fn [state! dispatch!]
       (jarman.org/print-line "aktualizacja pacownika")
       (if-let [card-id (get-in (state!) [:model-changes :card.id])]
         (if-let [exceptions (not-empty (filter (partial instance? Exception) (vals (:model-changes (state!)))))]
           (doall (map jarman.interaction/error-to-alert exceptions))
           (do
             (jarman.logic.connection/exec
               (jarman.logic.sql-tool/update!
                 {:table_name :card :where [:= :card.id card-id]
                  :set (jarman.logic.metadata/convert_model->flattfields
                         (:model-changes (state!)) :card)}))
             (dispatch! {:route :main-table :action :refresh})
             (dispatch! {:action :switch-plugin-ui-mode :debug true :plugin-ui-mode :insert :erease-selected? true
                         :model (jarman.logic.metadata/convert_metadata->model
                                  (.return-table_name (-> (state!) :plugin-toolkit :meta-obj)))})
             (jarman.interaction/success "Table. Pracownicy GZWM" "Successfully created new card")))
         (jarman.interaction/danger "Update card action" "ID is empty for model")))}
    ;; -------------------------------
    :view-configurations
    {:socket-id :main-table
     :type :jsgl-database-table
     :tables [:card :user]
     :columns
     [:card.rfid
      :card.card_nr
      :user.first_name
      :user.last_name]
     :socket-refreshable-query
     {:table_name :card,
      :column
      [:#as_is
       :card.id
       :card.id_user
       :card.rfid
       :card.card_nr
       :user.first_name
       :user.last_name]
      :left-join :card->user}
     :on-select
     (fn [state! dispatch! e]
       (println (jarman.logic.metadata/convert_flattfields->model e :card))
       (dispatch!
         {:action :switch-plugin-ui-mode :plugin-ui-mode :update
          :debug? true :message "TABLE CLICK UPDATE"
          :model (jarman.logic.metadata/convert_flattfields->model e :card)}))}))

(jarman.logic.view-manager/defview registration
  (table
    :id :registration
    :name "Pracownicy GZWM"
    :permission :ekka-all
    :tables [:registration :user :card]
    :model-configurations
    {:export
     {:filters
      [{:field-qualified :registration.id_user
        :representation "Pracownicy"
        :component
        {:type :jsgl-filter-in
         :component-template
         {:type :dialog-external-table
          :expand-table :user
          :expand-columns [:user.first_name :user.last_name]
          :dialog-plugin-path [:user :dialog-table :user-dialog]
          :on-select (fn [e] e)}
         :on-change
         (fn [filter-seq]
           (when (not-empty filter-seq)
             [:in :user.id filter-seq]))}}
       {:field-qualified :registration.datetime
        :representation "Wybierz daty"
        :component
        {:type :jsgl-filter-between
         :on-change (fn [{:keys [date-from date-to]}]
                      (when (and date-from date-to)
                        [:between :registration.datetime date-from date-to]))}}]
      :additional
      [{:type :jsgl-button :value "Export to EXCEL" :on-click
        (fn [state! dispatch!] (dispatch! {:action :table-export-csv :socket-id :main-table}))}
       {:type :jsgl-button :value "Export to CSV" :on-click
        (fn [state! dispatch!] (dispatch! {:action :table-export-excel :socket-id :main-table}))}]}}
    ;; -------------------------------
    :view-configurations
    {:socket-id :main-table
     :type :jsgl-database-table
     :tables [:registration :card :user]
     :columns
     [:registration.datetime
      :registration.direction
      :user.first_name
      :user.last_name
      :card.card_nr]
     :csv-custom-renderer
     {:registration.datetime
      (fn [value] (.format (java.text.SimpleDateFormat. "YYYY-MM-dd HH:mm:ss") value))
      :registration.direction
      (fn [value] (if (= value "we") "Wejście" "Wyście"))}
     :excel-custom-renderer
     {:registration.datetime
      (fn [value] (.format (java.text.SimpleDateFormat. "YYYY-MM-dd HH:mm:ss") value))
      :registration.direction
      (fn [value] (if (= value "we") "Wejście" "Wyście"))}
     :custom-renderers
     {:registration.datetime
      (fn [^javax.swing.JTable table, ^Object value, isSelected, hasFocus, row, column]
        (cond->
            (seesaw.core/label
              :font {:name "monospaced"}
              :h-text-position :right
              :text (.format (java.text.SimpleDateFormat. "YYYY-MM-dd HH:mm:ss") value))
          isSelected (c/config! :background (.getSelectionBackground table))))
      :registration.direction
      (fn [^javax.swing.JTable table, ^Object value, isSelected, hasFocus, row, column]
        (cond->
            (seesaw.core/label
              :font {:name "monospaced"}
              :h-text-position :right
              :text (if (= value "we") "Wejście" "Wyście"))
          isSelected (seesaw.core/config! :background (.getSelectionBackground table))))}
     :socket-refreshable-query
     {:table_name :registration,
      :column
      [:#as_is
       :registration.id
       :registration.datetime
       :registration.direction
       :card.id
       :card.rfid
       :card.card_nr
       :user.id
       :user.first_name
       :user.last_name
       :user.teta_nr]
      :left-join [:registration->card
                  :registration->user]
      :order [:registration.datetime :desc]
      :limit 100}
     :on-select
     (fn [state! dispatch! e]
       (jarman.org/print-line "Select item from table")
       (jarman.org/print-src :clojure (prn-str (jarman.logic.metadata/convert_flattfields->model e :registration))))}))

(jarman.logic.view-manager/defview jarman_user
  (table
   :id :jarman_user
   :name "Jarman user"
   :permission :ekka-all
   :tables [:jarman_user :permission]
   :model-configurations
   {:insert
    {:fields
     [[:jarman_user.login
       [(fn [m] (assoc-in m [:component :on-change]
                 (fn [e] (jarman.lang/exception-> (seesaw.core/text e)
                          (jarman.gui.gui-components2/check-field-re #"[^\s]{3,100}" "login without space, at least 4 symbols")))))]]
      [:jarman_user.password
       [(fn [m] (assoc-in m [:component :on-change]
                 (fn [e] (jarman.lang/exception-> (seesaw.core/text e)
                          (jarman.gui.gui-components2/check-field-re #"[^\s]{3,30}" "password without space, at least 4 symbols")))))]]
      [:jarman_user.first_name
       [(fn [m] (assoc-in m [:component :on-change]
                 (fn [e] (jarman.lang/exception-> (seesaw.core/text e)
                          (jarman.gui.gui-components2/check-field-re #"[^\s]{3,100}" "first-name without space, at least 4 symbols")))))]]
      [:jarman_user.last_name
       [(fn [m] (assoc-in m [:component :on-change]
                 (fn [e] (jarman.lang/exception-> (seesaw.core/text e)
                          (jarman.gui.gui-components2/check-field-re #"[^\s]{3,100}" "last-name without space, at least 4 symbols")))))]]
      [:jarman_user.configuration [(fn [m] (assoc-in m [:component :on-change] (fn [e] (seesaw.core/text e))))]]
      [:jarman_user.id_jarman_profile [(fn [m] (assoc-in m [:component :plugin-path] [:jarman_profile :dialog-table :jarman_profile-dialog]))]]]
     :additional
     [{:type :jsgl-button, :value "Do insert!", :on-click :insert-action}]}
    :update
    {:fields
     [[:jarman_user.login [(fn [m] (assoc-in m [:component :on-change] (fn [e] (seesaw.core/text e))))]]
      [:jarman_user.password [(fn [m] (assoc-in m [:component :on-change] (fn [e] (seesaw.core/text e))))]]
      [:jarman_user.first_name [(fn [m] (assoc-in m [:component :on-change] (fn [e] (seesaw.core/text e))))]]
      [:jarman_user.last_name [(fn [m] (assoc-in m [:component :on-change] (fn [e] (seesaw.core/text e))))]]
      [:jarman_user.configuration [(fn [m] (assoc-in m [:component :on-change] (fn [e] (seesaw.core/text e))))]]
      [:jarman_user.id_jarman_profile [(fn [m] (assoc-in m [:component :plugin-path] [:jarman_profile :dialog-table :jarman_profile-dialog]))]]]
     :additional
     [{:type :jsgl-button :value "Do delete!" :on-click :delete-action}
      {:type :jsgl-button :value "Do update!" :on-click :update-action}]}}
   :actions
   {:insert-action
    (fn [state! dispatch!]
      ;; (jarman.org/print-line "INSERT!")
      (if-let [exceptions (not-empty (filter (partial instance? Exception) (vals (:model-changes (state!)))))]
        (do
          (doall (map jarman.interaction/error-to-alert exceptions)))
        (do
         (jarman.logic.connection/exec
           (jarman.logic.sql-tool/insert!
             {:table_name :jarman_user
              :set (jarman.logic.metadata/convert_model->flattfields
                     (:model-changes (state!)) :jarman_user)}))
         (dispatch! {:route :main-table :action :refresh-to-bottom})
         (dispatch! {:action :switch-plugin-ui-mode :debug true :plugin-ui-mode :insert :erease-selected? true
                     :model (jarman.logic.metadata/convert_metadata->model
                              (.return-table_name (-> (state!) :plugin-toolkit :meta-obj)))})
         (jarman.interaction/success "Table. User" "Successfully created new user"))))
    :delete-action
    (fn [state! dispatch!]
      ;; (jarman.org/print-line "INSERT!")
      (if (get-in (state!) [:model-changes :jarman_user.id])
        (do
          (jarman.logic.connection/exec
           (jarman.logic.sql-tool/delete!
            {:table_name :jarman_user
             :where [:= :jarman_user.id (get-in (state!) [:model-changes :jarman_user.id])]}))
          (dispatch! {:route :main-table :action :refresh})
          (dispatch! {:action :switch-plugin-ui-mode :debug true :plugin-ui-mode :insert :erease-selected? true
                      :model (jarman.logic.metadata/convert_metadata->model
                              (.return-table_name (-> (state!) :plugin-toolkit :meta-obj)))}))
        (jarman.interaction/danger "Delete user action" "ID is empty for model")))
    :update-action
    (fn [state! dispatch!]
      ;; (println "UPDATE!")
      (if (get-in (state!) [:model-changes :jarman_user.id_jarman_profile])
        (if-let [exceptions (not-empty (filter (partial instance? Exception) (vals (:model-changes (state!)))))]
          (do
            (doall (map jarman.interaction/error-to-alert exceptions)))
          (do
            (jarman.logic.connection/exec
              (jarman.logic.sql-tool/update!
                {:table_name :jarman_user
                 :set (jarman.logic.metadata/convert_model->flattfields
                        (:model-changes (state!)) :jarman_user)
                 :where [:= :jarman_user.id (get-in (state!) [:model-changes :jarman_user.id])]}))
            (dispatch! {:route :main-table :action :refresh})
            (dispatch! {:action :switch-plugin-ui-mode :debug true :plugin-ui-mode :insert :erease-selected? true
                        :model (jarman.logic.metadata/convert_metadata->model
                                 (.return-table_name (-> (state!) :plugin-toolkit :meta-obj)))})
            (jarman.interaction/success "Table. User" "Successfully created new user")))
        (jarman.interaction/danger "Update user action" "ID is empty for model")))}
   :view-configurations
   {:socket-id :main-table
    :type :jsgl-database-table
    :tables  [:jarman_user :jarman_profile]
    :columns
    [:jarman_user.login
     :jarman_user.password
     :jarman_user.first_name
     :jarman_user.last_name
     :jarman_user.configuration
     :jarman_profile.name
     :jarman_profile.configuration]
    :socket-refreshable-query
    {:table_name :jarman_user,
     :column
     [:#as_is
      :jarman_user.id
      :jarman_user.login
      :jarman_user.password
      :jarman_user.first_name
      :jarman_user.last_name
      :jarman_user.configuration
      :jarman_user.id_jarman_profile
      :jarman_profile.name
      :jarman_profile.configuration]
     :inner-join :jarman_user->jarman_profile}
    :on-select
    (fn [state! dispatch! e]
      (dispatch!
       {:action :switch-plugin-ui-mode :plugin-ui-mode :update
        :debug? true :message "TABLE CLICK UPDATE"
        :model (jarman.logic.metadata/convert_flattfields->model e :jarman_user)}))}))

(jarman.logic.view-manager/defview jarman_profile
  (table
   :id :jarman_profile
   :name "Jarman profile"
   :tables [:jarman_profile]
   :permission :ekka-all
   :model-configurations
   {:insert
    {:fields
     [[:jarman_profile.name [(fn [m] (assoc-in m [:component :on-change] (fn [e] (seesaw.core/text e))))]]
      [:jarman_profile.configuration [(fn [m] (assoc-in m [:component :on-change] (fn [e] (seesaw.core/text e))))]]]}
    :update
    {:fields
     [[:jarman_profile.name [(fn [m] (assoc-in m [:component :on-change] (fn [e] (seesaw.core/text e))))]]
      [:jarman_profile.configuration [(fn [m] (assoc-in m [:component :on-change] (fn [e] (seesaw.core/text e))))]]]}}
   :view-configurations
   {:socket-id :main-table
    :type :jsgl-database-table
    :tables  [:jarman_profile]
    :columns [:jarman_profile.name :jarman_profile.configuration]
    :socket-refreshable-query
    {:table_name :jarman_profile,
     :column [:#as_is :jarman_profile.id :jarman_profile.name :jarman_profile.configuration]}
    ;; :on-select
    ;; (fn [state! dispatch! e]
    ;;   (dispatch!
    ;;    {:action :switch-plugin-ui-mode :plugin-ui-mode :update
    ;;     :debug? true :message "TABLE CLICK UPDATE"
    ;;     :model (jarman.logic.metadata/convert_flattfields->model
    ;;             e :jarman_profile)}))
    })

  (dialog-table
   :id :jarman_profile-dialog
   :name "Profile dialog"
   :permission :ekka-all
   :tables [:jarman_profile]
   :view-configurations
   {:type :jsgl-database-table
    :tables  [:jarman_profile]
    :columns [:jarman_profile.name :jarman_profile.configuration]
    :data
    (fn []
      (vec
       (jarman.logic.connection/query
        (jarman.logic.sql-tool/select!
         {:table_name :jarman_profile,
          :column
          [:#as_is
           :jarman_profile.id
           :jarman_profile.name
           :jarman_profile.configuration]}))
       (vec
        (jarman.logic.connection/query
         (jarman.logic.sql-tool/select!
          {:table_name :jarman_profile,
           :column
           [:#as_is
            :jarman_profile.id
            :jarman_profile.name
            :jarman_profile.configuration]})))))}))

(comment
  (jarman.logic.view-manager/defview user
      (table
       :id :jarman_user
       :name "user"
       :permission :ekka-all
       :tables [:jarman_user :permission]
       :model-configurations
       {:insert
        {:fields
         [{:representation "Site",
           :field-qualified :seal.site
           :component {:type :jsgl-url-panel
                       :on-change (fn [e] e)},
           :columns
           [{:field-qualified :seal.site_name
             :constructor-var :text
             :description nil,
             :private? false,
             :default-value nil,
             :editable? true,
             :field :site_name,
             :column-type {:stype "varchar(120)", :tname :varchar, :tspec nil, :tlimit 120, :tnull true, :textra "", :tdefault nil, :tkey ""},
             :component {:type :jsgl-text, :value "Title", :char-limit 120},
             :representation "Site name"}
            {:description nil,
             :default-value nil,
             :constructor-var :link
             :field :site_url,
             :column-type {:stype "varchar(120)", :tname :varchar, :tspec nil, :tlimit 120, :tnull true, :textra "", :tdefault nil, :tkey ""},
             :component {:type :jsgl-text, :value "https://", :char-limit 120},
             :representation "Site url",
             :field-qualified :seal.site_url}]}
          {:representation "Database File"
           :field :db_file
           :field-qualified :seal.db_file
           :component {:type :jsgl-file-panel}
           :columns
           [{:field :file_name
             :field-qualified :seal.file_name
             :representation "File name",
             :constructor-var :file-name
             :column-type {:stype "varchar(120)", :tname :varchar, :tspec nil, :tlimit 120, :tnull true, :textra "", :tdefault nil, :tkey ""},
             :component {:type :jsgl-text, :value "", :char-limit 120}}
            {:field :file
             :field-qualified :seal.file
             :representation "File"
             :constructor-var :file
             :column-type {:stype "blob", :tname :blob, :tspec nil, :tlimit nil, :tnull true, :textra "", :tdefault nil, :tkey ""}
             :component {:type :jsgl-stub}}]}
          :jarman_user.login
          :jarman_user.password
          [:jarman_user.first_name [(fn [m] (assoc-in m [:component :on-change] (fn [e] (seesaw.core/text e))))]]
          [:jarman_user.last_name [(fn [m] (assoc-in m [:component :on-change] (fn [e] (seesaw.core/text e))))]]
          [:jarman_user.configuration [(fn [m] (assoc-in m [:component :on-change] (fn [e] (seesaw.core/text e))))]]
          [:jarman_user.id_jarman_profile [(fn [m] (assoc-in m [:component :plugin-path] [:jarman_profile :dialog-table :jarman_profile-dialog]))]]
          {:field :date-label,
           :field-qualified :jarman_user.date-label
           :representation "Date label"
           :component {:type :jsgl-calendar-label,
                       :on-click (fn [e] e)
                       :value nil}}]
         :additional
         [{:type :jsgl-button
           :value "Custom User insert"
           :on-click :insert-action}
          {:type :jsgl-button
           :value "Second custom action"
           :on-click (fn [state! dispatch!] (println "some function"))}]}
        :update
        {:fields
         [ ;; :jarman_user.login
          ;; :jarman_user.password
          [:jarman_user.first_name [(fn [m] (assoc-in m [:component :on-change] (fn [e] (seesaw.core/text e))))]]
          [:jarman_user.last_name [(fn [m] (assoc-in m [:component :on-change] (fn [e] (seesaw.core/text e))))]]
          [:jarman_user.configuration [(fn [m] (assoc-in m [:component :on-change] (fn [e] (seesaw.core/text e))))]]
          [:jarman_user.id_jarman_profile [(fn [m] (assoc-in m [:component :plugin-path] [:jarman_profile :dialog-table :jarman_profile-dialog]))]]
          {:field :SECRET_UPDATE_FIELD,
           :field-qualified :jarman_user.SECRET_UPDATE_FIELD
           :representation "ONLY FOR UPDATE"
           :component {:type :jsgl-text, :value "SOME TEXT ONLY FOR UPDATE", :char-limit 100 :on-change (fn [e] (seesaw.core/text e))}}]
         :additional
         [{:type :jsgl-button, :value "DELETE ACTION", :on-click :delete-action}
          {:type :jsgl-button, :value "UPDATE ACTION", :on-click :update-action}]}}
       :actions
       {:TMP_ACTION (fn [state! dispatch!] (println "TMP DEBUG ACTION"))
        :insert-action (fn [state! dispatch!]
                         (println "INSERT!")
                         (jarman.logic.sql-tool/insert!
                          {:table_name :jarman_user
                           :set (jarman.logic.metadata/convert_model->flattfields
                                 (:model-changes (state!)) :jarman_user)})
                         (dispatch! {:route :main-table :action :refresh-to-bottom}))
        :delete-action (fn [state! dispatch!]
                         (println "DELETE!")
                         (if (get-in (state!) [:model-changes :jarman_user.id])
                           (do
                             (jarman.logic.sql-tool/delete!
                              {:table_name :jarman_user
                               :where [:= :jarman_user.id (get-in (state!) [:model-changes :jarman_user.id])]})
                             (dispatch! {:route :main-table :action :refresh})
                             (dispatch! {:action :switch-plugin-ui-mode :debug true :plugin-ui-mode :insert :erease-selected? true
                                         :model (jarman.logic.metadata/convert_metadata->model (.return-table_name (-> (state!) :plugin-toolkit :meta-obj)))}))
                           (jarman.interaction/danger "Delete user action" "ID is empty for model")))
        :update-action (fn [state! dispatch!]
                         (println "UPDATE!")
                         (if (get-in (state!) [:model-changes :jarman_user.id_jarman_profile])
                           (do
                             (jarman.logic.sql-tool/update!
                              {:table_name :jarman_user
                               :set (jarman.logic.metadata/convert_model->flattfields
                                     (:model-changes (state!)) :jarman_user)
                               :where [:= :jarman_user.id (get-in (state!) [:model-changes :jarman_user.id])]})
                             (dispatch! {:route :main-table :action :refresh}))
                           (jarman.interaction/danger "Update user action" "ID is empty for model")))}
       :reference-to {:jarman_user.id_jarman_profile [:jarman_profile :dialog-table :jarman_profile-dialog]}
       :view-configurations
       {:socket-id :main-table
        :type :jsgl-database-table
        :tables  [:jarman_user :jarman_profile]
        :columns [:jarman_user.login
                  :jarman_user.password
                  :jarman_user.first_name
                  :jarman_user.last_name
                  :jarman_user.configuration
                  :jarman_profile.name
                  :jarman_profile.configuration]
        :socket-refreshable-query
        {:table_name :jarman_user,
         :column [:#as_is :jarman_user.id :jarman_user.login :jarman_user.password :jarman_user.first_name :jarman_user.last_name :jarman_user.configuration :jarman_user.id_jarman_profile :jarman_profile.name :jarman_profile.configuration]
         :inner-join :jarman_user->jarman_profile}})))
