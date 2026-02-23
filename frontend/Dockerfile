# ---- Build Stage ----
FROM node:20-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm config set registry https://registry.npmmirror.com && npm ci
COPY . .
RUN npx vite build

# ---- Runtime Stage ----
FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/nginx-template.conf
EXPOSE 80
CMD ["/bin/sh", "-c", "envsubst '${HIGRESS_ADDR}' < /etc/nginx/nginx-template.conf > /etc/nginx/conf.d/default.conf && nginx -g 'daemon off;'"]
