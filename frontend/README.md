# UITMerch

## API mode

The frontend is demo-safe by default:

```env
VITE_USE_MOCK=true
```

To test against the real Spring Boot API, run the backend dev profile on a port
that is not occupied by IIS. In this workspace, `localhost:8080` is IIS, so use
`8081`:

```powershell
cd ..\backend
$env:SERVER_PORT="8081"
mvn.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

Then set:

```env
VITE_USE_MOCK=false
VITE_API_BASE_URL=http://localhost:8081
```

Public catalog routes are served by the backend under `/api/v1/public/...`.
