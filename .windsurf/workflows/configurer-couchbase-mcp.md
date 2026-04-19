---
description: Configurer le serveur MCP Couchbase pour Windsurf avec la configuration officielle
---
1. Vérifier les prérequis du serveur MCP Couchbase officiel : Python 3.10+ et `uv` disponibles sur la machine.
2. Ouvrir la configuration MCP brute de Windsurf depuis les paramètres MCP.
3. Fusionner le contenu de `epilote-infra/couchbase-mcp-config.json` dans l’objet `mcpServers` existant.
4. Définir les variables d’environnement `CB_CONNECTION_STRING`, `CB_USERNAME` et `CB_PASSWORD` avec les accès Couchbase/Capella valides.
5. Sauvegarder la configuration MCP puis rafraîchir la liste des serveurs MCP dans Windsurf.
6. Vérifier que le serveur `couchbase` apparaît comme actif avant de l’utiliser.
7. Si le serveur ne démarre pas, contrôler d’abord la présence de `uv`, puis les identifiants Couchbase et enfin la portée réseau vers le cluster Capella.
