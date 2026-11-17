# Arquitectura C4

## Nivel 1 – Contexto

| Elemento | Tipo | Descripción |
|---|---|---|
| **Usuario** | Actor | Ingresa correo, RUC y placa del vehículo |
| **Sistema de Consulta SRI-ANT** | Sistema | Verifica contribuyente, vehículo y puntos de licencia |
| **API REST SRI – Catastro** | Sistema externo | `existePorNumeroRuc` y `obtenerPorNumerosRuc` |
| **API REST SRI – Matriculación** | Sistema externo | `BaseVehiculo/obtenerPorNumeroPlaca...` |
| **Portal web ANT** | Sistema externo (baja disponibilidad) | `clp_grid_citaciones.jsp` (puntos / citaciones) |

```mermaid
C4Context
  title Nivel 1 - Contexto
  Person(user, "Usuario", "Persona natural que consulta sus datos")
  System(sys, "Sistema Consulta SRI-ANT", "Verifica RUC, vehículo y puntos de licencia")
  System_Ext(sriCat, "SRI - Catastro (REST)", "Contribuyentes")
  System_Ext(sriVeh, "SRI - Matriculación (REST)", "Vehículos")
  System_Ext(ant, "Portal web ANT", "Puntos y citaciones. Baja disponibilidad")
  Rel(user, sys, "Ingresa correo, RUC y placa", "HTTPS")
  Rel(sys, sriCat, "Consulta RUC", "HTTPS/JSON")
  Rel(sys, sriVeh, "Consulta placa", "HTTPS/JSON")
  Rel(sys, ant, "Consulta puntos", "HTTPS/HTML")
```

## Nivel 2 – Contenedores

| Contenedor | Tecnología | Responsabilidad | Repo / carpeta |
|---|---|---|---|
| **Web App** | React + Vite | Formulario correo/RUC → info persona → placa → vehículo + puntos | `frontend/` |
| **SRI Service** (microservicio) | Java 17, Spring Boot | Verifica contribuyente, persona natural y vehículo | `sri-service/` |
| **ANT Service** (microservicio) | Java 17, Spring Boot | Puntos de licencia con patrón Cache-Aside | `ant-service/` |
| **Cache Cloud** | Redis (Upstash / Redis Cloud) | Guarda las respuestas buenas de la ANT | servicio en la nube |

```mermaid
C4Container
  title Nivel 2 - Contenedores
  Person(user, "Usuario")
  System_Boundary(b, "Sistema Consulta SRI-ANT") {
    Container(web, "Web App", "React", "Interfaz de consulta")
    Container(sri, "SRI Service", "Java / Spring Boot", "Contribuyente y vehículo")
    Container(antS, "ANT Service", "Java / Spring Boot", "Puntos de licencia (Cache-Aside)")
    ContainerDb(cache, "Cache Cloud", "Redis (Upstash)", "Respuestas de la ANT")
  }
  System_Ext(sriCat, "SRI - Catastro")
  System_Ext(sriVeh, "SRI - Matriculación")
  System_Ext(ant, "Portal web ANT")
  Rel(user, web, "Usa", "HTTPS")
  Rel(web, sri, "GET /api/sri/...", "JSON")
  Rel(web, antS, "GET /api/ant/puntos/{cedula}", "JSON")
  Rel(sri, sriCat, "existePorNumeroRuc / obtenerPorNumerosRuc")
  Rel(sri, sriVeh, "obtenerPorNumeroPlaca...")
  Rel(antS, cache, "Lee / escribe", "TLS")
  Rel(antS, ant, "Consulta HTML")
```

## Nivel 3 – Componentes (ANT Service)

| Componente | Clase | Qué hace |
|---|---|---|
| AntController | `AntController.java` | Expone `GET /api/ant/puntos/{cedula}?placa=` |
| PuntosService | `PuntosService.java` | Lógica del cache (Cache-Aside) |
| AntClient | `AntClient.java` | Llama a la web ANT y lee el HTML (Jsoup) |
| Redis Template | `StringRedisTemplate` | Acceso al cache cloud |

### Patrón Cache-Aside

```mermaid
flowchart TD
  A[Petición puntos] --> B{¿Está en cache fresco? 60 min}
  B -- Sí --> R1[Devolver desde CACHE]
  B -- No --> C[Llamar a la ANT]
  C -- OK --> D[Guardar en cache fresco + respaldo 30 días] --> R2[Devolver dato ANT]
  C -- Falla / timeout --> E{¿Hay respaldo en cache?}
  E -- Sí --> R3[Devolver CACHE_RESPALDO]
  E -- No --> R4[503 ANT no disponible]
```
