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
  - Flujo detallado:
  1.  Primero se crea y persiste un pago con los datos de la request del cliente (campos: `importe`, `referencia_externa`, `fecha_vto`, `descripcion`). Este registro inicial se guarda con `estado_interno = PENDING`.
  2.  Si al intentar crear ya existía un pago con la misma `referencia_externa`, el servicio detecta la condición de idempotencia y devuelve el pago existente (evitando duplicados).
  3.  Si no existe, tras persistir el registro PENDING se llama a Helipagos para crear el pago en su sistema y obtener el `id_sp` necesario para posteriores operaciones (`get`, `cancel`).
  4.  La implementación usa dos bloques try-catch encadenados:
  - El primer bloque intenta persistir el pago PENDING. Si se detecta una violación de restricción UNIQUE (otro request creó el mismo `referencia_externa` en paralelo), se captura `DataIntegrityViolationException` y se retorna el pago ya creado (idempotencia por restricción de BD).
  - El segundo bloque llama a Helipagos y luego actualiza el pago local con la respuesta (asignando `id_sp`, `checkout_url`, `estado_externo`).
  5.  Manejo de fallos y códigos HTTP:
  - Si la llamada a Helipagos falla por indisponibilidad (timeout/connection error), el endpoint devuelve 503 (Service Unavailable). El pago ya persistido queda con `estado_interno = ERROR` para indicar que la creación local se realizó pero la confirmación remota no pudo completarse.
  - Si la llamada a Helipagos devuelve éxito pero el intento de actualizar / persistir los datos devueltos por Helipagos falla (por ejemplo error en el update local), el endpoint devuelve 500 (Internal Server Error) y el pago queda con `estado_interno = ERROR`.
  6.  Garantía operativa:
  - Con este diseño queda registrado que se intentó crear el pago aunque fallara la confirmación remota o la actualización local. El servicio deja trazas (logs) del error para facilitar investigación y reconciliación.
  - Cuando Helipagos posteriormente envía un webhook con el cambio de estado (por ejemplo de `GENERADA` a `PROCESADA`), el endpoint de webhook realiza la reconciliación: busca por `referencia_externa`, sincroniza campos (incluido `id_sp`) y actualiza el pago local. Esto permite recuperar pagos cuyo `id_sp` no pudo almacenarse en el momento de la creación.
  - Si la conexión a Helipagos falló y el pago quedó almacenado localmente pero no existe en Helipagos, el registro permanecerá en `ERROR` y el cliente deberá reintentar la creación del pago enviando una nueva referencia externa.
  7. Caso no abordado:
  - El pago quedó almacenado localemnte pero en Helipagos no se creó. El pago queda con estado interno `ERROR` pero no se puede reintentar su creación desde el cliente con la misma referencia externa.
    Solución propuesta, sujeta a mayor investigación:
  - Distinguir los tipos de `ERRORES` (propios o del servicio externo).
  - Antes de crear un pago consultar localmente por referencia externa y ver el estado interno, si es un error de tipo externo permitir la llamada a Helpagos y actualizar el registro.
    Este caso no fue desarrollado por cuestiones de tiempo.

- **getPayment**:
  - Sincroniza el estado local con Helipagos antes de responder.

- **cancelPayment**:
  - Primero sincroniza el estado con Helipagos.
  - Verifica si se puede cancelar (solo se pueden cancelar pagos en estado RECHAZADA (FAIL) y GENERADA (CREATED))
  - Llama a Helipagos para cancelar y actualiza el estado local.
  - Es idempotente: si ya está cancelado, devuelve el mismo resultado.
  - Estrategia de concurrencia: transacción + optimistic locking (columna version).

- **processWebhook**:
  - Valida API Key.
  - Busca el pago por id_sp y si no lo encuentra busca por referencia externa (ambos datos enviados por Helipagos).
  - En caso de que no exista el id_sp pero si la refencia externa, lo que significa que el microservicio no pudo completar exitosamente la creación del pago quedando un registro incompleto; actualiza el registtro completando con los datos falatantes.
  - Actualiza el estado solo si es diferente al actual.
  - Ignora webhooks duplicados o de pagos inexistentes.

---

## CI/CD

El proyecto cuenta con integración y despliegue continuo (CI/CD) configurado para compilar, testear y desplegar automáticamente en los entornos definidos.

---

## Cómo Correr el Proyecto Localmente

1. Clonar el repositorio
   git clone git@github.com:CamilaVHeuer/microservicio_integrador.git
   cd microservicio_integrador

2. **Configura la base de datos PostgreSQL** y las variables de entorno necesarias en el `application-dev.yml` (perfil para desarrollo).
   Para poder usar las variables de entorno, define sus valores en un .env.local y luego lo exportas con export $(grep -v '^#' .env | xargs)
   También se pueden cargar en el entorno del IDE (IntelliJIdea) y setear el perfil "dev" para ejecutar más facilmente la app
3. **Ejecuta migraciones**: Flyway lo hace automáticamente al levantar la app.
4. **Compila y ejecuta**:
   ```bash
   ./mvnw clean package
   ./mvnw spring-boot:run
   ```
   The API will be available at: http://localhost:8080
5. **Health check**:  
   Accede a `http://localhost:8080/actuator/health`

### Docker

Para levantar el microservicio y la base de datos con Docker:

1. Crear un application-docker.yml
2. Indicar en el docker-compose.yml que se utilizará el perfil docker.
3. Definir las variables de entorno en un .env

```bash
docker-compose up --build
```

---

## Cómo Correr los Tests

- **Unitarios y de integración:**
  ```bash
  ./mvnw test
  ```

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

### Ejemplos (request / response)

1. Crear pago - Happy path (201 Created)

Request POST /api/v1/payments

```json
{
  "importe": 10000,
  "descripcion": "Pago de servicios",
  "fechaVto": "2026-12-31",
  "referenciaExterna": "REF123456"
}
```

Response 201

```json
{
  "paymentId": 1,
  "id_sp": "123",
  "referencia_externa": "REF123456",
  "estado_interno": "GENERATED",
  "estado_externo": "GENERADA",
  "checkout_url": "https://checkout.helipagos/..."
}
```

2. Crear pago - Helipagos no disponible (503)

Si la llamada a Helipagos falla por indisponibilidad, el servicio devuelve 503 y el pago previamente persistido queda con `estado_interno = ERROR`.

Response 503

```json
{
  "status": 503,
  "error": "SERVICE_UNAVAILABLE",
  "message": "Helipagos service is currently unavailable",
  "timestamp": "2026-05-11T12:34:56Z"
}
```

En la base de datos el pago creado inicialmente permanece con:

```sql
estado_interno = 'ERROR'
```

3. Crear pago - fallo al actualizar después de crear en Helipagos (500)

Si Helipagos responde correctamente pero el update local falla (por ejemplo error inesperado al persistir `id_sp`), el endpoint devuelve 500 y el pago queda en `estado_interno = ERROR`.

Response 500

```json
{
  "status": 500,
  "error": "INTERNAL_SERVER_ERROR",
  "message": "Failed to update payment after Helipagos response",
  "timestamp": "2026-05-11T12:34:56Z"
}
```

4. Consultar pago (GET) - Happy path (200 OK)

Request GET /api/v1/payments/123

Response 200

```json
{
  "paymentId": 1,
  "id_sp": "123",
  "referencia_externa": "REF123456",
  "estado_interno": "GENERATED",
  "estado_externo": "PROCESADA",
  "checkout_url": "https://checkout.helipagos/..."
}
```

5. Cancelar pago (DELETE) - Happy path (200 OK)

Request DELETE /api/v1/payments/123

Response 200

```json
{
  "paymentId": 1,
  "id_sp": "123",
  "referencia_externa": "REF123456",
  "estado_interno": "CANCELLED",
  "estado_externo": "VENCIDA",
  "checkout_url": "https://checkout.helipagos/..."
}
```

6. Webhook de Helipagos (reconciliación)

Helipagos envía un POST a `/api/v1/payments/webhook` con el siguiente body (ejemplo):

```json
{
  "id_sp": 123,
  "estado": "PROCESADA",
  "referencia_externa": "REF123456",
  "medio_pago": "VISA",
  "importe_abonado": "10000",
  "fecha_importe": "2026-05-11"
}
```

El servicio valida la `api-key` recibida en el header, busca el pago local (por `id_sp` o por `referencia_externa` si no existe) y actualiza el estado local. Si el pago estaba en `ERROR` porque falló la actualización previa, la reconciliación rellena `id_sp` y actualiza `estado_externo`, resolviendo el caso donde la creación parcial quedó pendiente.

---

## Documentación OpenAPI

- Accesible en `/swagger-ui.html` al levantar el servicio.
- ***

## Despliegue

- Deployada en Render: https://microservicio-integrador-1.onrender.com
- Swagger UI (deploy): https://microservicio-integrador-1.onrender.com/swagger-ui/index.html
- Health check https://microservicio-integrador-1.onrender.com/actuator/health

## Autor

- Camila Villalba Heuer
