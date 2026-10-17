# Étape 1 : Construction de l'application Angular
FROM node:24-alpine AS builder
WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build -- --configuration production

# Étape 2 : Serveur Nginx léger et optimisé
FROM nginx:alpine
# Configuration personnalisée de Nginx pour SPA et routage Angular
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copie des fichiers compilés depuis l'étape précédente
COPY --from=builder /app/dist/frontend/browser /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
