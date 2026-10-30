# Build with: docker build --build-arg SERVICE=log-service -t log-service .
FROM node:22-alpine
ARG SERVICE
ENV SERVICE=${SERVICE}
WORKDIR /app
COPY package.json ./
COPY services ./services
RUN npm install --omit=dev --workspaces --include-workspace-root=false
CMD node services/${SERVICE}/src/index.js
