package ni.edu.uam.proyectofinalprogramacionappescritorio.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Inicializador del esquema y tablas PostgreSQL de Latch.
 * Ejecuta DDL de creación y carga datos semilla de prueba si no existen.
 */
public class DatabaseInitializer {

    public static void inicializarBaseDeDatos() {
        try (Connection conn = ConexionBD.obtenerConexion();
             Statement stmt = conn.createStatement()) {

            // 1. Esquema
            stmt.execute("CREATE SCHEMA IF NOT EXISTS latch;");

            // 2. Tabla rol
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS latch.rol (
                    id UUID NOT NULL DEFAULT gen_random_uuid(),
                    nombre VARCHAR(16) NOT NULL,
                    CONSTRAINT ck_rol_nombre CHECK (nombre IN ('Propietario','Asistente')),
                    CONSTRAINT pk_rol PRIMARY KEY(id),
                    CONSTRAINT uq_rol_nombre UNIQUE(nombre)
                );
            """);

            // 3. Tabla usuario
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS latch.usuario (
                    id UUID NOT NULL DEFAULT gen_random_uuid(),
                    rol_id UUID NOT NULL,
                    nombre_usuario VARCHAR(40) NOT NULL,
                    nombre_visible VARCHAR(120) NOT NULL,
                    hash_clave TEXT NOT NULL,
                    activo BOOLEAN NOT NULL DEFAULT TRUE,
                    creado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    version BIGINT NOT NULL DEFAULT 0,
                    CONSTRAINT pk_usuario PRIMARY KEY(id),
                    CONSTRAINT uq_usuario_nombre UNIQUE(nombre_usuario),
                    CONSTRAINT fk_usuario_rol FOREIGN KEY (rol_id) REFERENCES latch.rol(id) ON DELETE RESTRICT
                );
            """);

            // 4. Tabla propiedad
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS latch.propiedad (
                    id UUID NOT NULL DEFAULT gen_random_uuid(),
                    nombre VARCHAR(120) NOT NULL,
                    direccion VARCHAR(300) NOT NULL,
                    activa BOOLEAN NOT NULL DEFAULT TRUE,
                    creado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    creado_por UUID,
                    version BIGINT NOT NULL DEFAULT 0,
                    CONSTRAINT pk_propiedad PRIMARY KEY(id)
                );
            """);

            // 5. Tabla unidad
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS latch.unidad (
                    id UUID NOT NULL DEFAULT gen_random_uuid(),
                    propiedad_id UUID NOT NULL,
                    codigo VARCHAR(30) NOT NULL,
                    tipo VARCHAR(24) NOT NULL DEFAULT 'Cuarto',
                    estado VARCHAR(24) NOT NULL DEFAULT 'Disponible',
                    canon_referencia NUMERIC(19,2) NOT NULL,
                    moneda VARCHAR(3) NOT NULL DEFAULT 'USD',
                    creado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    creado_por UUID,
                    version BIGINT NOT NULL DEFAULT 0,
                    CONSTRAINT pk_unidad PRIMARY KEY(id),
                    CONSTRAINT uq_unidad_codigo UNIQUE(propiedad_id, codigo),
                    CONSTRAINT fk_unidad_propiedad FOREIGN KEY (propiedad_id) REFERENCES latch.propiedad(id) ON DELETE CASCADE
                );
            """);

            // 6. Tabla inquilino
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS latch.inquilino (
                    id UUID NOT NULL DEFAULT gen_random_uuid(),
                    nombre VARCHAR(160) NOT NULL,
                    identificacion VARCHAR(40),
                    telefono VARCHAR(30),
                    correo VARCHAR(254),
                    contacto_emergencia VARCHAR(200),
                    activo BOOLEAN NOT NULL DEFAULT TRUE,
                    creado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    creado_por UUID,
                    version BIGINT NOT NULL DEFAULT 0,
                    CONSTRAINT pk_inquilino PRIMARY KEY(id)
                );
            """);

            // 7. Tabla contrato
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS latch.contrato (
                    id UUID NOT NULL DEFAULT gen_random_uuid(),
                    unidad_id UUID NOT NULL,
                    inquilino_id UUID NOT NULL,
                    inicio DATE NOT NULL,
                    fin DATE NOT NULL,
                    terminado_el DATE,
                    canon NUMERIC(19,2) NOT NULL,
                    moneda VARCHAR(3) NOT NULL DEFAULT 'USD',
                    dia_pago SMALLINT NOT NULL DEFAULT 5,
                    deposito_pactado NUMERIC(19,2) NOT NULL DEFAULT 0.00,
                    estado VARCHAR(24) NOT NULL DEFAULT 'Vigente',
                    creado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    creado_por UUID,
                    version BIGINT NOT NULL DEFAULT 0,
                    CONSTRAINT pk_contrato PRIMARY KEY(id),
                    CONSTRAINT fk_contrato_unidad FOREIGN KEY (unidad_id) REFERENCES latch.unidad(id) ON DELETE RESTRICT,
                    CONSTRAINT fk_contrato_inquilino FOREIGN KEY (inquilino_id) REFERENCES latch.inquilino(id) ON DELETE RESTRICT
                );
            """);

            // 8. Tabla cargo
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS latch.cargo (
                    id UUID NOT NULL DEFAULT gen_random_uuid(),
                    contrato_id UUID NOT NULL,
                    tipo VARCHAR(24) NOT NULL DEFAULT 'Renta',
                    periodo DATE NOT NULL,
                    fecha_emision DATE NOT NULL,
                    vence_el DATE NOT NULL,
                    importe NUMERIC(19,2) NOT NULL,
                    concepto VARCHAR(300) NOT NULL,
                    estado VARCHAR(24) NOT NULL DEFAULT 'Pendiente',
                    creado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    creado_por UUID,
                    version BIGINT NOT NULL DEFAULT 0,
                    CONSTRAINT pk_cargo PRIMARY KEY(id),
                    CONSTRAINT fk_cargo_contrato FOREIGN KEY (contrato_id) REFERENCES latch.contrato(id) ON DELETE RESTRICT
                );
            """);

            // 9. Tabla pago
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS latch.pago (
                    id UUID NOT NULL DEFAULT gen_random_uuid(),
                    contrato_id UUID NOT NULL,
                    fecha_pago DATE NOT NULL,
                    moneda_original VARCHAR(3) NOT NULL DEFAULT 'USD',
                    monto_original NUMERIC(19,2) NOT NULL,
                    monto_contrato NUMERIC(19,2) NOT NULL,
                    metodo VARCHAR(24) NOT NULL DEFAULT 'Efectivo',
                    referencia VARCHAR(100),
                    notas VARCHAR(255),
                    estado VARCHAR(24) NOT NULL DEFAULT 'Registrado',
                    creado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    creado_por UUID,
                    CONSTRAINT pk_pago PRIMARY KEY(id),
                    CONSTRAINT fk_pago_contrato FOREIGN KEY (contrato_id) REFERENCES latch.contrato(id) ON DELETE RESTRICT
                );
            """);

            // 10. Datos semilla de roles
            stmt.execute("""
                INSERT INTO latch.rol (id, nombre) VALUES
                    ('11111111-1111-1111-1111-111111111111', 'Propietario'),
                    ('22222222-2222-2222-2222-222222222222', 'Asistente')
                ON CONFLICT (nombre) DO NOTHING;
            """);

            // 11. Datos semilla de usuarios: admin / admin123 (Propietario), asistente / asistente123 (Asistente)
            stmt.execute("""
                INSERT INTO latch.usuario (id, rol_id, nombre_usuario, nombre_visible, hash_clave, activo) VALUES
                    ('33333333-3333-3333-3333-333333333333', 
                     (SELECT id FROM latch.rol WHERE nombre = 'Propietario'),
                     'admin', 'Administrador Principal', 'admin123', TRUE),
                    ('44444444-4444-4444-4444-444444444444', 
                     (SELECT id FROM latch.rol WHERE nombre = 'Asistente'),
                     'asistente', 'Asistente Inmobiliario', 'asistente123', TRUE)
                ON CONFLICT (nombre_usuario) DO NOTHING;
            """);

            // 12. Datos semilla de propiedades
            stmt.execute("""
                INSERT INTO latch.propiedad (id, nombre, direccion, activa) VALUES
                    ('55555555-5555-5555-5555-555555555555', 'Condominio Villa Fontana', 'Km 12 Carretera a Masaya, Managua', TRUE),
                    ('66666666-6666-6666-6666-666666666666', 'Residencial Los Robles', 'Semáforos Plaza Cuba 2c al Sur, Managua', TRUE)
                ON CONFLICT (id) DO NOTHING;
            """);

            // 13. Datos semilla de unidades
            stmt.execute("""
                INSERT INTO latch.unidad (id, propiedad_id, codigo, tipo, estado, canon_referencia, moneda) VALUES
                    ('77777777-7777-7777-7777-777777777777', '55555555-5555-5555-5555-555555555555', 'A-101', 'Apartamento', 'Disponible', 450.00, 'USD'),
                    ('88888888-8888-8888-8888-888888888888', '55555555-5555-5555-5555-555555555555', 'A-102', 'Apartamento', 'Disponible', 500.00, 'USD'),
                    ('99999999-9999-9999-9999-999999999999', '66666666-6666-6666-6666-666666666666', 'C-01', 'Casa', 'Disponible', 750.00, 'USD')
                ON CONFLICT (id) DO NOTHING;
            """);

            // 14. Datos semilla de inquilinos
            stmt.execute("""
                INSERT INTO latch.inquilino (id, nombre, identificacion, telefono, correo, contacto_emergencia, activo) VALUES
                    ('a1b2c3d4-e5f6-4a5b-8c9d-0e1f2a3b4c5d', 'Carlos Alberto Mendoza Ruiz', '001-150392-0004X', '+505 8899-7711', 'carlos.mendoza@email.com', 'Elena Ruiz (Madre) - +505 8811-2233', TRUE),
                    ('b2c3d4e5-f6a7-4b5c-9d0e-1f2a3b4c5d6e', 'Lucía Sofía Morales Gómez', '281-220795-0002Y', '+505 7744-1122', 'lucia.morales@empresa.com', 'Roberto Morales (Hermano) - +505 8833-4455', TRUE),
                    ('c3d4e5f6-a7b8-4c5d-0e1f-2a3b4c5d6e7f', 'Marvin José Castillo Blandón', '441-051188-0001Z', '+505 8234-5678', 'marvin.castillo@correo.ni', 'Carmen Blandón - +505 8456-7890', TRUE)
                ON CONFLICT (id) DO NOTHING;
            """);

        } catch (SQLException e) {
            System.err.println("[DatabaseInitializer] Advertencia al verificar/inicializar BD: " + e.getMessage());
        }
    }
}
