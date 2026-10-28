# Front end + web server in one image: Vite builds the static site, Caddy serves it, gets the
# HTTPS certificate and forwards /api/* to the backend. Build context is the repository root.

FROM node:22-alpine AS build
WORKDIR /app
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./

# Empty API address = same origin: the page and the API are served from one host, so the
# browser needs no CORS and the address is not baked into the build.
ARG VITE_API_BASE_URL=
ARG VITE_APP_TIME_ZONE=Europe/Istanbul
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL \
    VITE_APP_TIME_ZONE=$VITE_APP_TIME_ZONE
RUN npm run build

FROM caddy:2-alpine
COPY --from=build /app/dist /srv
COPY deploy/Caddyfile /etc/caddy/Caddyfile
