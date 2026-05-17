# monitor-status

Affiche le statut du service de surveillance du contexte Claude.

## Ce que cette commande fait

1. Vérifie le statut systemd du service `claude-monitor`
2. Affiche les 20 dernières lignes de log
3. Résume en 1 ligne si tout est OK

## Commandes exécutées

```bash
systemctl --user status claude-monitor.service --no-pager
echo "---"
tail -20 ~/.claude/logs/context-monitor.log
```

## Usage

```
/monitor-status
```

Si le service est `inactive` ou `failed`, relancer avec :
```bash
systemctl --user restart claude-monitor.service
```
