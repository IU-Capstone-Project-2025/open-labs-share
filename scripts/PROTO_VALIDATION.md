# Protocol Buffer Validation

## Quick Reference

### Validate all proto files
```bash
./scripts/validate-proto.sh
```

### Sync proto files after editing specifying edited proto file as a single source of truth in parameters
```bash
./scripts/sync-proto.sh services/users-service/src/main/proto/users_service.proto
```

## What it validates
- ✅ **Syntax**: All proto files compile with `protoc`
- ✅ **Naming**: 
  - Files must end with `_service.proto` (e.g., `users_service.proto`)
  - Services/messages/enums use `PascalCase`
  - Fields use `snake_case`
- ✅ **Consistency**: Duplicate proto files across services are semantically identical (ignoring language-specific options)

## Development Workflow

1. **Edit** the proto file in its "home" service (e.g., `users-service` for `users_service.proto`)
2. **Sync** to update all copies:
   ```bash
   ./scripts/sync-proto.sh services/users-service/src/main/proto/users_service.proto
   ```
3. **Validate** everything is consistent:
   ```bash
   ./scripts/validate-proto.sh
   ```
4. **Commit** all changes together

## Troubleshooting

- **Syntax errors**: Check proto file syntax and imports
- **Naming violations**: Files must end with `_service.proto`, use `PascalCase` for services/messages/enums, `snake_case` for fields
- **Semantic differences**: Use sync script to update all copies from the source file

## Notes

- **Language options preserved**: Sync script keeps `java_package`, `go_package`, etc. specific to each service
- **Source of truth**: Keep the original proto in its "home" service (e.g., `users_service.proto` in `users-service`)

## Requirements
- `protoc` (Protocol Buffer Compiler)
- `bash` 