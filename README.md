# Consulta SRI + ANT

Proyecto de Diseño y Arquitectura de Software (ISWZ2202) - UDLA

El usuario ingresa su correo y RUC, se valida en el SRI que sea contribuyente y persona natural, despues ingresa la placa del vehiculo y se muestran los datos del vehiculo y los puntos de la licencia de la ANT.

La pagina de la ANT casi nunca responde, por eso se usa el patron Cache-Aside con Redis en la nube (Upstash).

Diagrama C4: ver [docs/arquitectura-c4.md](docs/arquitectura-c4.md)

## Estructura

- `frontend` - React + Vite (puerto 5173)
- `sri-service` - Spring Boot, consulta RUC y vehiculo (puerto 8081)
- `ant-service` - Spring Boot, puntos de licencia + cache Redis (puerto 8082)

## Endpoints

- `GET /api/sri/contribuyente/{ruc}`
- `GET /api/sri/vehiculo/{placa}`
- `GET /api/ant/puntos/{cedula}?placa=`

## Como correrlo

Crear una base Redis gratis en https://upstash.com y copiar la URL (`rediss://default:...@...upstash.io:6379`).

```
cd sri-service
mvn spring-boot:run
```

```
cd ant-service
set REDIS_URL=rediss://default:CLAVE@HOST.upstash.io:6379
mvn spring-boot:run
```

```
cd frontend
npm install
npm run dev
```

## Cache

- Primera consulta: se trae de la ANT y se guarda en Redis (`fuente: ANT`)
- Si se repite dentro de 60 min: sale del cache (`fuente: CACHE`)
- Si la ANT esta caida: se devuelve la ultima respuesta guardada (`fuente: CACHE_RESPALDO`)
