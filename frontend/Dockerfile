# ---- Build Stage ----
FROM node:20-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npx vite build

# ---- Runtime Stage ----
FROM nginx:alpine
# 只替换 HIGRESS_ADDR，不动 nginx 变量和正则中的 $
ENV NGINX_ENVSUBST_FILTER=HIGRESS_ADDR
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/templates/default.conf.template
EXPOSE 80
