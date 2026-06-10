# Causal

Services: `identity`, `product`, `cart`, `profile`, `order`, `inventory`

## Skills

- [Project Overview](.claude/observability-skill.md) — Architecture, services, networking, and observability stack

## Testing

After making any code change, check if there will be any impact to test suite.

```bash
cd <service> && ./gradlew test
```
