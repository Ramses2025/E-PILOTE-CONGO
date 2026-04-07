# Comment trouver les IDs Capella pour le script PowerShell

## ORGANIZATION_ID (Tenant ID)
Capella → clic sur ton profil en haut à droite → **Organization Settings**
→ copier le champ **Organization ID**

## PROJECT_ID
Capella → **Projects** → clic sur ton projet → regarder l'URL :
`https://cloud.couchbase.com/projects/XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX/...`
→ copier l'UUID dans l'URL

## CLUSTER_ID
Capella → ton cluster → **Settings** → **Cluster ID** (ou dans l'URL)

## API_KEY + API_SECRET
Capella → ton profil → **API Keys** → **Create API Key**
- Access Level : **Organization Owner** ou **Project Creator**
- Copier la clé ET le secret (le secret n'est affiché qu'une seule fois)
