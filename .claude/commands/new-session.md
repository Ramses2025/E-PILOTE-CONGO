# new-session

Crée manuellement une nouvelle session propre avec sauvegarde préalable.

## Ce que cette commande fait

1. Met à jour CLAUDE.md avec l'état actuel
2. Commit Git : `chore: save context before new session`
3. Nomme la nouvelle session : `epilote-YYYY-MM-DD-HH:MM`
4. Lance `claude -n [nom]` en arrière-plan
5. Confirme avec le nom de la session créée

## Règles de nommage des sessions

- Nouvelle feature → `feature-[nom]`
- Correction de bug → `fix-[description]`
- Exploration/test → `explore-[sujet]`
- Session principale → `e-pilote-main`
- Session auto → `epilote-YYYYMMDD-HHMM`

## Quand utiliser

- Changement complet de domaine de travail
- Début d'une nouvelle journée (vérifier `/resume` d'abord)
- Avant toute expérimentation risquée
- Manuellement avant d'atteindre 90% de contexte

## Usage

```
/new-session
```
