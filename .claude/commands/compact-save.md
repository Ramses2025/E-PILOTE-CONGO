# compact-save

Sauvegarde le contexte dans CLAUDE.md, crée un commit Git, puis compacte la conversation.

## Étapes exécutées automatiquement

1. **Mise à jour CLAUDE.md** — section "État actuel" mise à jour avec :
   - Ce qui fonctionne / ce qui est cassé
   - Tâches en cours et décisions techniques prises
   - Bugs connus

2. **Commit Git** :
   ```bash
   git add CLAUDE.md
   git commit -m "chore: save context before compact"
   ```

3. **Compactage** avec instruction personnalisée :
   ```
   /compact "Garde : architecture, décisions techniques, tâches en cours, bugs connus. Résume le reste."
   ```

## Usage

```
/compact-save
```

Utilise cette commande dès que le contexte atteint 70-80% pour ne rien perdre.
