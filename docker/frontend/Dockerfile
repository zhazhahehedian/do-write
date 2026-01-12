FROM node:20-alpine AS build

WORKDIR /app

RUN corepack enable

COPY frontend/package.json frontend/pnpm-lock.yaml ./frontend/

WORKDIR /app/frontend
RUN pnpm install --frozen-lockfile

COPY frontend/ ./
RUN pnpm build

FROM node:20-alpine AS runner

WORKDIR /app/frontend

ENV NODE_ENV=production
ENV PORT=3000

RUN addgroup -S nextjs && adduser -S nextjs -G nextjs

COPY --from=build /app/frontend/.next ./.next
COPY --from=build /app/frontend/public ./public
COPY --from=build /app/frontend/package.json ./package.json
COPY --from=build /app/frontend/node_modules ./node_modules
COPY --from=build /app/frontend/next.config.ts ./next.config.ts

USER nextjs

EXPOSE 3000

CMD ["node", "node_modules/next/dist/bin/next", "start", "-p", "3000"]
