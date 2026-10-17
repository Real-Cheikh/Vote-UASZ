# Guide de Déploiement et d'Exploitation VoteUASZ

## 1. Environnements d'Exécution

La plateforme VoteUASZ est conçue pour fonctionner de manière reproductible selon trois configurations :
1. **Local / Développement** : Exécution directe dans Antigravity avec PostgreSQL via Docker ou profil test mémoire.
2. **Conteneurisé (Docker Compose)** : Déploiement complet en un clic (PostgreSQL + Backend Spring Boot + Frontend Nginx).
3. **Cloud Séparé (Production Sécurisée)** :
   - **Frontend Angular** : Hébergement statique optimisé sur **Vercel** ou **Netlify**.
   - **Backend Spring Boot** : Service conteneurisé sur **Render**, **Railway** ou VPS universitaire.
   - **Base de données** : PostgreSQL managé persistant avec sauvegardes automatiques.

---

## 2. Déploiement Local avec Docker Compose

Pour démarrer l'ensemble des services en local :

```powershell
# Cloner ou se placer à la racine du projet
cd "c:\Users\hp\Desktop\Vote UASZ"

# Démarrer tous les conteneurs en arrière-plan
docker compose up -d --build

# Vérifier l'état de santé des services
docker compose ps
```

- Frontend disponible sur : `http://localhost:4200`
- API Backend disponible sur : `http://localhost:8080/api/v1`
- Swagger UI disponible sur : `http://localhost:8080/api/v1/swagger-ui.html`
- Health check : `http://localhost:8080/api/v1/actuator/health`

---

## 3. Variables d'Environnement Obligatoires (Production)

| Variable | Description | Exemple |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Profil Spring actif | `prod` |
| `SERVER_PORT` | Port d'écoute du serveur | `8080` |
| `POSTGRES_HOST` | Hôte du serveur PostgreSQL | `ep-sparkling-db.render.com` |
| `POSTGRES_PORT` | Port PostgreSQL | `5432` |
| `POSTGRES_DB` | Nom de la base de données | `voteuasz_db` |
| `POSTGRES_USER` | Utilisateur PostgreSQL | `voteuasz_admin` |
| `POSTGRES_PASSWORD` | Mot de passe de la base | `[SECRET_COMPLEXE]` |
| `JWT_SECRET` | Clé secrète HMAC-SHA256 (>= 32 car.) | `[CLE_ALEATOIRE_256_BITS]` |
| `CORS_ALLOWED_ORIGINS` | Origines frontend autorisées | `https://voteuasz.vercel.app` |
| `INITIAL_ADMIN_EMAIL` | Email du Super Admin initial | `admin.electoral@uasz.sn` |
| `INITIAL_ADMIN_PASSWORD` | Mot de passe initial du Super Admin | `[SECRET_ADMIN_ROBUSTE]` |

---

## 4. Sauvegarde et Restauration de la Base de Données

### Sauvegarde (Dump sécurisé)
```bash
# Sauvegarde complète de la base de données PostgreSQL
docker exec -t voteuasz-postgres pg_dump -U voteuasz_user -F c -b -v -f /tmp/voteuasz_backup.dump voteuasz_db
docker cp voteuasz-postgres:/tmp/voteuasz_backup.dump ./backups/voteuasz_$(date +%Y%m%d_%H%M%S).dump
```

### Restauration en cas d'incident
```bash
# Restauration à partir d'un dump certifié
docker cp ./backups/voteuasz_backup_point.dump voteuasz-postgres:/tmp/restore.dump
docker exec -t voteuasz-postgres pg_restore -U voteuasz_user -d voteuasz_db -v /tmp/restore.dump
```

---

## 5. Procédure de Retour Arrière (Rollback)

En cas d'anomalie détectée lors du déploiement d'une nouvelle version :
1. **Frontend** : Dans le tableau de bord Vercel / Netlify, rétablir instantanément le déploiement précédent en 1 clic ("Instant Rollback").
2. **Backend** :
   - Redéployer l'image Docker du tag stable précédent : `docker tag voteuasz-backend:previous voteuasz-backend:latest`.
   - Redémarrer le service avec l'image certifiée.
3. **Migrations Flyway** : Les migrations ont été conçues de façon incrémentale et non destructive pour garantir la rétro-compatibilité entre versions consécutives.
