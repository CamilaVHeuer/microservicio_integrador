# Microservicio Integrador de Pagos - Helipagos

## Descripción General

Este proyecto es un microservicio integrador de pagos que actúa como intermediario entre clientes internos y la plataforma Helipagos. Su objetivo es exponer una API REST segura y robusta para la creación, consulta y cancelación de pagos, gestionando la idempotencia, la concurrencia y la actualización de estados mediante webhooks.

El microservicio está diseñado para ser fácilmente integrable en arquitecturas modernas, soportando buenas prácticas de seguridad, documentación OpenAPI, migraciones automáticas de base de datos y health checks para monitoreo.

---

## Estructura del Proyecto

- **src/main/java/com/camicompany/microserviciointegrador/**
  - **controller/**: Controladores REST (`PaymentController`, `AuthController`)
  - **service/**: Lógica de negocio y acceso a datos (`PaymentServiceImp`, `AuthServiceImp`)
  - **dto/**: Objetos de transferencia de datos (DTOs) para requests y responses
  - **repository/**: Interfaces JPA para acceso a base de datos
  - **mapper/**: Conversión entre entidades y DTOs
  - **exception/**: Excepciones personalizadas y manejo global de errores
  - **client/**: Cliente HTTP para integración con Helipagos

- **src/main/resources/db/migration/**: Migraciones SQL gestionadas por Flyway
- **src/test/**: Tests unitarios y de integración

---

## Seguridad

- **API Key**: Todos los endpoints de pagos requieren autenticación mediante API Key, que se genera y asocia a cada usuario registrado.
- **Validación**: El filtro `ApiKeyFilter` valida la API Key en cada request.
- **Contraseñas**: Se almacenan hasheadas y nunca en texto plano.
- **API Keys**: Las API Keys también se almacenan hasheadas en la base de datos y se buscan por prefijo, nunca por el valor completo.
- **Endpoints de autenticación**: Permiten registrar usuarios y regenerar API Keys.

---

## Endpoints Principales

### Pagos

- `POST /api/v1/payments`  
  Crea un nuevo pago.  
  **Body:** `CreatePaymentRequest`  
  **Seguridad:** API Key  
  **Respuestas:**
  - 201: Pago creado y si la referencia externa ya existe (idempotencia)
  - 400: Request inválido

- `GET /api/v1/payments/{idSp}`  
  Consulta el estado de un pago por su identificador externo provisto por Helipagos.  
  **Seguridad:** API Key
  - 200: Detalle del pago
  - 404: No encontrado

- `DELETE /api/v1/payments/{idSp}`  
  Cancela un pago si está en estado permitido.  
  **Seguridad:** API Key
  - 200: Cancelado
  - 404: No encontrado
  - 409: Estado no permite cancelación

- `POST /api/v1/payments/webhook`  
  Recibe notificaciones de Helipagos para actualizar el estado de un pago.  
  **Header:** `api-key`
  - 200: Procesado
  - 400: API Key inválida o datos inválidos

### Autenticación

- `POST /api/v1/auth/register`  
  Registra un usuario y genera una API Key.

- `POST /api/v1/auth/regenerate-api-key`  
  Regenera la API Key para un usuario existente.

### Health Check

- `GET /actuator/health`  
  Endpoint estándar de Spring Boot Actuator para monitoreo de salud del microservicio.

---

## Dependencias y Justificación

- **Spring Boot Starter Web / WebFlux**: Exposición de API REST y cliente HTTP reactivo.
- **Spring Boot Starter Data JPA**: Persistencia con PostgreSQL.
- **Spring Boot Starter Validation**: Validación de DTOs.
- **Spring Boot Starter Security**: Seguridad y autenticación.
- **Flyway**: Migraciones automáticas de base de datos.
- **PostgreSQL Driver**: Base de datos productiva.
- **H2**: Base de datos en memoria para tests.
- **Springdoc OpenAPI**: Documentación automática de la API.
- **Actuator**: Health checks y métricas.
- **Lombok**: Reducción de boilerplate (opcional).
- **Testing**: JUnit, Mockito, Spring Security Test, Reactor Test.

---

## Decisiones de Diseño Importantes

- **Idempotencia**:
  - La creación de pagos es idempotente por `referencia_externa`. Si se recibe dos veces la misma referencia, siempre se devuelve el mismo pago.
  - La cancelación de un pago: si se recibe más de una vez la misma solicitud de cancelación de un pago, siempre se devuelve el mismo pago con estado cancelado. 
  - El webhook también es idempotente: si el estado recibido es igual al actual, se ignora.

- **Concurrencia**:
  - Para la creación de un pago se usa un try-catch: se valida que la referencia externa no exista y se intenta guardar. Si otra request guardó primero, se captura el `DataIntegrityViolationException` y se devuelve el pago ya creado.
  - Para la cancelación de un pago, se usa el 'optimisctic locking' implementando la columna `version` al actualizar el estado.
  - En ambos casos, los métodos del servicio son transaccionales (`@Transactional`), por lo que la estrategia es:
    - **Creación**: transacción + restricción UNIQUE en BD para concurrencia.
    - **Cancelación**: transacción + optimistic locking.

- **Validación y Seguridad**:
  - Validaciones exhaustivas en DTOs.
  - Manejo centralizado de errores.
  - API Key obligatoria en todos los endpoints de operaciones del servicio. 

- **Migraciones**:
  - Flyway gestiona la evolución de la base de datos con scripts versionados.

- **Separación de Tests**:
  - Los tests de integración con el sandbox de Helipagos están en un grupo separado y no se ejecutan por defecto en CI (`excludedGroups=sandbox` en Surefire).

---

## Descripción de Métodos y Estrategias

- **createPayment**:
  - Verifica idempotencia por referencia externa.
  - Llama a Helipagos, valida respuesta, guarda el pago.
  - Estrategia de concurrencia: transacción + manejo de violación de restricción UNIQUE en BD (no optimistic locking).
  - Devuelve siempre el mismo pago para la misma referencia.

- **getPayment**:
  - Sincroniza el estado local con Helipagos antes de responder.

- **cancelPayment**:
  - Primero sincroniza el estado con Helipagos.
  - Verifica si se puede cancelar.
  - Llama a Helipagos para cancelar y actualiza el estado local.
  - Es idempotente: si ya está cancelado, devuelve el mismo resultado.
  - Estrategia de concurrencia: transacción + optimistic locking (columna version).

- **processWebhook**:
  - Valida API Key.
  - Actualiza el estado solo si es diferente al actual.
  - Ignora webhooks duplicados o de pagos inexistentes.

---

## CI/CD

El proyecto cuenta con integración y despliegue continuo (CI/CD) configurado para compilar, testear y desplegar automáticamente en los entornos definidos.

---

## Cómo Correr el Proyecto

1. **Configura la base de datos PostgreSQL** y las variables de entorno necesarias (`application.yml`).
2. **Ejecuta migraciones**: Flyway lo hace automáticamente al levantar la app.
3. **Compila y ejecuta**:
   ```bash
   ./mvnw clean package
   java -jar target/microserviciointegrador-0.0.1.jar
   ```
4. **Health check**:  
    Accede a `http://localhost:8080/actuator/health`

### Docker

Para levantar el microservicio y la base de datos con Docker:

```bash
docker-compose up --build
```

Para solo construir la imagen:

```bash
docker build -t microservicio-integrador .
```

---

## Cómo Correr los Tests

- **Unitarios y de integración (excepto sandbox):**
  ```bash
  ./mvnw test
  ```
- **Tests de integración con sandbox (manual):**
  ```bash
  ./mvnw verify -Dgroups=sandbox
  ```
  > Estos tests están excluidos del CI por configuración en `pom.xml`.

---

## Base de Datos y Migraciones

- **PostgreSQL** como base de datos principal.
- **H2** en memoria para tests.
- **Flyway** gestiona migraciones en `src/main/resources/db/migration/`.
  - `V1__create_payments_table.sql`: Tabla de pagos.
  - `V2__create_user_and_api_keys_tables.sql`: Tablas de usuarios y API Keys.

---

## Flujo Principal

1. El cliente envía una request de pago con una referencia externa y datos requeridos.
2. El microservicio valida, reenvía a Helipagos y responde con el estado.
3. Los cambios de estado se sincronizan por webhook.
4. El cliente puede consultar o cancelar pagos en cualquier momento.

---

## Documentación OpenAPI

- Accesible en `/swagger-ui.html` al levantar el servicio.

---


