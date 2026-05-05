CREATE TABLE payments (
                          id BIGSERIAL PRIMARY KEY,
                          referencia_externa VARCHAR(255) NOT NULL UNIQUE,
                          id_sp VARCHAR(255),
                          importe BIGINT NOT NULL,
                          descripcion TEXT,
                          fecha_vto DATE NOT NULL ,
                          estado_interno VARCHAR(50) NOT NULL,
                          estado_externo VARCHAR(50),
                          checkout_url TEXT,
                          created_at TIMESTAMP NOT NULL,
                          updated_at TIMESTAMP NOT NULL,
                          version BIGINT
  );