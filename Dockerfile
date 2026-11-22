# Default repository-root build serves the Next.js frontend.
# Spring Boot remains a separate service; set BACKEND_URL before building.
FROM node:24-alpine AS build
WORKDIR /app
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ .
ARG BACKEND_URL=http://backend:8080
ENV BACKEND_URL=$BACKEND_URL NEXT_TELEMETRY_DISABLED=1
RUN npm run build
RUN npm prune --omit=dev
FROM node:24-alpine
WORKDIR /app
ARG BACKEND_URL=http://backend:8080
ENV NODE_ENV=production NEXT_TELEMETRY_DISABLED=1 BACKEND_URL=$BACKEND_URL
COPY --from=build --chown=node:node /app ./
USER node
EXPOSE 3000
CMD ["npm","run","start","--","--hostname","0.0.0.0"]
