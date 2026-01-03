# Falcon Airlines — Frontend (React)

This folder contains a React frontend (Vite) and an Express + Mongoose backend scaffold to run a MERN stack locally.

Quick start (development):

```bash
cd frontend
npm install
# create a .env with MONGODB_URI (see .env.example)
npm run dev
```

This runs the Vite dev server and the Express server concurrently. The dev server proxies `/api` to the local Express server.

Running production build (frontend only):

```bash
cd frontend
npm run build
npm run preview
```

Server only:

```bash
cd frontend
npm run server
```

Notes:
- The Express server is in `server/` and exposes REST endpoints under `/api/*` for `flights`, `passengers`, `bookings`, `cancellations`, and `predict`.
- By default the server will connect to MongoDB at `MONGODB_URI` (see `.env.example`). If you don't have MongoDB, you can run a local Mongo instance or provide a connection string to a hosted MongoDB.
- Tailwind CSS is installed and used via `src/index.css` (Tailwind directives). Remove or adapt components to use Tailwind utilities as desired.

Next steps I can do for you (choose any):
- Convert all components to use Tailwind utility classes and improve UI.
- Wire the Express endpoints to your existing Python backend instead of Mongoose.
- Add authentication and protected routes.

