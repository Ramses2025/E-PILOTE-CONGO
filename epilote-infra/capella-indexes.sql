-- Index Couchbase Capella — à exécuter depuis l'UI Capella ou via cbq
-- Bucket : epilote_prod, Scope : _default

CREATE INDEX idx_users_ecole     ON `epilote_prod`._default.`users`(ecoleId)   WHERE type="user";
CREATE INDEX idx_users_username  ON `epilote_prod`._default.`users`(username)  WHERE type="user";
CREATE INDEX idx_schools_groupe  ON `epilote_prod`._default.`schools`(groupeId) WHERE type="school";
CREATE INDEX idx_profils_groupe  ON `epilote_prod`._default.`profils`(groupeId) WHERE type="profil";
CREATE INDEX idx_modules_type    ON `epilote_prod`._default.`modules`(type);
CREATE INDEX idx_plans_type      ON `epilote_prod`._default.`plans`(type);
CREATE INDEX idx_notes_classe    ON `epilote_prod`._default.`notes`(ecoleId, classeId, periode) WHERE type="note";
CREATE INDEX idx_absences_eleve  ON `epilote_prod`._default.`absences`(ecoleId, eleveId)        WHERE type="absence";
CREATE INDEX idx_eleves_classe   ON `epilote_prod`._default.`eleves`(ecoleId, classeId)         WHERE type="eleve";
CREATE INDEX idx_classes_ecole   ON `epilote_prod`._default.`classes`(ecoleId)                  WHERE type="classe";
CREATE INDEX idx_matieres_classe ON `epilote_prod`._default.`matieres`(ecoleId, classeId)       WHERE type="matiere";
