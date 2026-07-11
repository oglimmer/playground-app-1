# syntax=docker/dockerfile:1

FROM node:22-alpine AS build
WORKDIR /app
# Build metadata (passed by oglimmer.sh). Vite exposes VITE_* to import.meta.env.
ARG VITE_APP_VERSION=0.0.0
ARG VITE_GIT_COMMIT=unknown
ARG VITE_BUILD_TIME=unknown
ENV VITE_APP_VERSION=$VITE_APP_VERSION \
    VITE_GIT_COMMIT=$VITE_GIT_COMMIT \
    VITE_BUILD_TIME=$VITE_BUILD_TIME
COPY package.json package-lock.json* ./
RUN npm ci || npm install
COPY . .
RUN npm run build

FROM nginx:alpine AS run
RUN rm -rf /usr/share/nginx/html/*
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
