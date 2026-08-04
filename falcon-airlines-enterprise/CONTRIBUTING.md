# Contributing

## Branching

- `main` — production-ready
- `develop` — integration branch
- `feature/*` — new work
- `fix/*` — bug fixes

## Commit Messages

Use conventional commits:

```
feat(auth): add JWT login endpoint
fix(booking): correct inventory hold logic
docs(readme): update setup guide
```

## Pull Request Checklist

- [ ] Tests pass (`mvn test`)
- [ ] Docker build succeeds
- [ ] No secrets committed
- [ ] Follows package structure

## Code Style

- Java 21 idioms
- Constructor injection
- Lombok and MapStruct for DTOs
- Standard REST conventions
