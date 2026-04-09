-- ======================================================================
-- 1. LIMPIEZA DE TABLAS
-- ======================================================================
SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS Socios_Alquiler;
DROP TABLE IF EXISTS Alquiler_Tripulantes;
DROP TABLE IF EXISTS Reserva;
DROP TABLE IF EXISTS Alquiler;
DROP TABLE IF EXISTS Asignacion;
DROP TABLE IF EXISTS Embarcaciones;
DROP TABLE IF EXISTS Acompanantes; 
DROP TABLE IF EXISTS Socios;
DROP TABLE IF EXISTS Inscripciones;
DROP TABLE IF EXISTS Empleado;
SET FOREIGN_KEY_CHECKS = 1;


-- ======================================================================
-- 2. CREACIÓN DE TABLAS
-- ======================================================================

CREATE TABLE Inscripciones (
    id_inscripcion INT AUTO_INCREMENT PRIMARY KEY,
    id_socio_titular INT NULL,
    tipo_inscripcion ENUM('INDIVIDUAL', 'FAMILIAR') NOT NULL,
    cuota_anual DECIMAL(8,2) NOT NULL,
    fecha_creacion DATE NOT NULL
);

-- TABLA SOCIOS (CONSOLIDADA)
CREATE TABLE Socios (
    id_socio INT AUTO_INCREMENT PRIMARY KEY,
    dni VARCHAR(9) UNIQUE NOT NULL,
    nombre VARCHAR(50) NOT NULL,
    apellidos VARCHAR(100) NOT NULL,
    fecha_nacimiento DATE NOT NULL,
    direccion VARCHAR(200),
    fecha_inscripcion DATE NOT NULL,
    tiene_titulo_patron BOOLEAN DEFAULT FALSE,
    -- ELIMINADO: es_titular BOOLEAN DEFAULT FALSE,
    tipo_miembro ENUM('TITULAR', 'CONYUGE', 'HIJO') DEFAULT 'TITULAR' NOT NULL,
    id_socio_titular_fk INT NULL, -- Nueva FK para el titular (auto-referenciada)
    inscripcion_id INT NULL, 
    FOREIGN KEY (inscripcion_id) REFERENCES Inscripciones(id_inscripcion),
    FOREIGN KEY (id_socio_titular_fk) REFERENCES Socios(id_socio),
    CHECK (YEAR(CURDATE()) - YEAR(fecha_nacimiento) >= 0) -- Se relaja la restricción de edad aquí, se controla en Java
);

CREATE TABLE Empleado (
    id_empleado INT AUTO_INCREMENT PRIMARY KEY,
    dni VARCHAR(9) UNIQUE NOT NULL,
    nombre VARCHAR(50) NOT NULL,
    apellidos VARCHAR(100) NOT NULL,
    fecha_nacimiento DATE NOT NULL,
    fecha_expedicion_titulo DATE NOT NULL
);

CREATE TABLE Embarcaciones (
    matricula VARCHAR(10) PRIMARY KEY,
    tipo_embarcacion ENUM(
        'YATE', 'VELERO', 'MOTO', 'LANCHA',
        'CATAMARAN', 'TRIMARAN', 'FERRY', 'FRAGATA', 'GALERA', 'NONE'
    ) NOT NULL,
    nombre VARCHAR(50) UNIQUE NOT NULL,
    numero_plazas INT NOT NULL,
    dimensiones VARCHAR(50),
    id_patron_asignado INT NULL,
    FOREIGN KEY (id_patron_asignado) REFERENCES Empleado(id_empleado)
);

CREATE TABLE Asignacion (
    id_asignacion INT AUTO_INCREMENT PRIMARY KEY,
    matricula_embarcacion VARCHAR(10) NOT NULL,
    id_empleado INT NOT NULL,
    fecha_asignacion DATE NOT NULL,
    fecha_fin_asignacion DATE NULL,
    FOREIGN KEY (id_empleado) REFERENCES Empleado(id_empleado),
    FOREIGN KEY (matricula_embarcacion) REFERENCES Embarcaciones(matricula)
);

CREATE TABLE Alquiler (
    id_alquiler INT AUTO_INCREMENT PRIMARY KEY,
    id_socio_titular INT NOT NULL,
    matricula_embarcacion VARCHAR(10) NOT NULL,
    fecha_inicio DATE NOT NULL,
    fecha_fin DATE NOT NULL,
    plazas_solicitadas INT NOT NULL,
    precio_total DECIMAL(8,2) NOT NULL,
    FOREIGN KEY (id_socio_titular) REFERENCES Socios(id_socio),
    FOREIGN KEY (matricula_embarcacion) REFERENCES Embarcaciones(matricula)
);

CREATE TABLE Alquiler_Tripulantes (
    id INT AUTO_INCREMENT PRIMARY KEY,
    id_alquiler INT NOT NULL,
    dni_tripulante VARCHAR(9) NOT NULL,
    FOREIGN KEY (id_alquiler) REFERENCES Alquiler(id_alquiler) ON DELETE CASCADE,
    UNIQUE KEY unique_tripulante_alquiler (id_alquiler, dni_tripulante)
);

CREATE TABLE Socios_Alquiler (
    id_alquiler INT,
    id_socio INT,
    PRIMARY KEY (id_alquiler, id_socio),
    FOREIGN KEY (id_alquiler) REFERENCES Alquiler(id_alquiler),
    FOREIGN KEY (id_socio) REFERENCES Socios(id_socio)
);

CREATE TABLE Reserva (
    id_reserva INT AUTO_INCREMENT PRIMARY KEY,
    id_socio_solicitante INT NOT NULL,
    plazas_solicitadas INT NOT NULL,
    proposito_actividad VARCHAR(200),
    precio_total DECIMAL(8,2) NOT NULL,
    matricula_embarcacion VARCHAR(10) NOT NULL,
    fecha_reserva DATE NOT NULL,
    id_empleado INT NULL,
    FOREIGN KEY (id_socio_solicitante) REFERENCES Socios(id_socio),
    FOREIGN KEY (matricula_embarcacion) REFERENCES Embarcaciones(matricula),
    FOREIGN KEY (id_empleado) REFERENCES Empleado(id_empleado)
);
ALTER TABLE Inscripciones ADD CONSTRAINT fk_socio_titular
    FOREIGN KEY (id_socio_titular) REFERENCES Socios(id_socio);


-- ======================================================================
-- 5. DATOS DE EJEMPLO (ACTUALIZADOS)
-- ======================================================================

-- 1. TITULARES (CON id_socio_titular_fk = id_socio)
INSERT INTO Socios (dni, nombre, apellidos, fecha_nacimiento, direccion, fecha_inscripcion, tiene_titulo_patron, tipo_miembro, inscripcion_id, id_socio_titular_fk) VALUES
('12345678A', 'Juan', 'García Pérez', '1980-05-15', 'Calle Principal 123', '2023-01-15', TRUE, 'TITULAR', 1, 1), -- ID 1
('87654321B', 'María', 'López Martínez', '1985-08-22', 'Avenida Central 456', '2023-02-20', FALSE, 'TITULAR', 2, 2), -- ID 2
('31027587D', 'Carlos', 'Rodríguez Sánchez', '1990-03-10', 'Plaza Mayor 789', '2023-03-15', TRUE, 'TITULAR', 3, 3), -- ID 3
('44556677E', 'Ana', 'Martín Fernández', '1988-11-25', 'Calle Secundaria 321', '2023-04-10', FALSE, 'TITULAR', 4, 4); -- ID 4

ALTER TABLE Socios AUTO_INCREMENT = 5; 

-- 2. MIEMBROS FAMILIARES (Antiguos Acompañantes)
INSERT INTO Socios (dni, nombre, apellidos, fecha_nacimiento, direccion, fecha_inscripcion, tiene_titulo_patron, tipo_miembro, inscripcion_id, id_socio_titular_fk) VALUES
('11223344C', 'Laura', 'García López', '1982-07-20', 'Calle Principal 123', '2023-01-15', TRUE, 'CONYUGE', 1, 1), -- Titular ID 1 (Juan)
('99887766D', 'Sofía', 'García Rodríguez', '2010-05-15', 'Calle Principal 123', '2023-01-15', FALSE, 'HIJO', 1, 1), -- Titular ID 1 (Juan)
('66778899F', 'Elena', 'Rodríguez Martín', '1992-09-12', 'Plaza Mayor 789', '2023-03-15', FALSE, 'CONYUGE', 3, 3); -- Titular ID 3 (Carlos)

ALTER TABLE Socios AUTO_INCREMENT = 8; 

INSERT INTO Empleado (dni, nombre, apellidos, fecha_nacimiento, fecha_expedicion_titulo) VALUES
('55566677D', 'Pedro', 'Martín Gómez', '1975-12-01', '2010-06-15'),
('88899900E', 'Ana', 'Fernández Ruiz', '1982-07-30', '2015-09-20'),
('99988877F', 'Luis', 'González López', '1988-03-25', '2018-11-10');

ALTER TABLE Empleado AUTO_INCREMENT = 4;

INSERT INTO Embarcaciones (matricula, tipo_embarcacion, nombre, numero_plazas, dimensiones, id_patron_asignado) VALUES
('ABC123', 'VELERO', 'María del Mar', 8, '12m x 4m', 1),
('DEF456', 'YATE', 'Neptuno', 12, '18m x 5m', 2),
('GHI789', 'LANCHA', 'Rápido', 6, '8m x 3m', 3), 
('JKL012', 'CATAMARAN', 'Océano', 10, '15m x 6m', NULL),
('MNO345', 'VELERO', 'Brisa Marina', 6, '10m x 3m', NULL);

INSERT INTO Asignacion (matricula_embarcacion, id_empleado, fecha_asignacion, fecha_fin_asignacion) VALUES
('ABC123', 1, '2023-06-15', NULL),
('DEF456', 2, '2023-06-20', NULL),
('GHI789', 3, '2023-07-01', NULL);

ALTER TABLE Asignacion AUTO_INCREMENT = 4;

-- INSCRIPCIONES (Mantienen sus IDs originales)
INSERT INTO Inscripciones (id_socio_titular, tipo_inscripcion, cuota_anual, fecha_creacion) VALUES
(1, 'FAMILIAR', 550.00, '2023-01-15'),
(2, 'INDIVIDUAL', 300.00, '2023-02-20'),
(3, 'FAMILIAR', 550.00, '2023-03-15'),
(4, 'INDIVIDUAL', 300.00, '2023-04-10');

ALTER TABLE Inscripciones AUTO_INCREMENT = 5;

-- Alquileres de ejemplo (Los id_socio_titular referencian a los IDs de Socios)
INSERT INTO Alquiler (id_socio_titular, matricula_embarcacion, fecha_inicio, fecha_fin, plazas_solicitadas, precio_total) VALUES
(1, 'ABC123', '2024-12-01', '2024-12-07', 4, 560.00), 
(2, 'DEF456', '2024-12-15', '2024-12-20', 6, 900.00),
(3, 'JKL012', '2024-11-28', '2024-11-29', 2, 80.00),
(4, 'GHI789', '2024-12-10', '2024-12-12', 3, 180.00);

ALTER TABLE Alquiler AUTO_INCREMENT = 5;

-- DATOS PARA ALQUILER_TRIPULANTES (usando DNI de los miembros consolidados en Socios)
INSERT INTO Alquiler_Tripulantes (id_alquiler, dni_tripulante) VALUES
(1, '87654321B'), -- María (Titular ID 2) como tripulante del alquiler de Juan (ID 1)
(1, '31027587D'), -- Carlos (Titular ID 3) como tripulante del alquiler de Juan (ID 1)
(2, '12345678A'), -- Juan (Titular ID 1) como tripulante del alquiler de María (ID 2)
(3, '44556677E'); -- Ana (Titular ID 4) como tripulante del alquiler de Carlos (ID 3)

ALTER TABLE Alquiler_Tripulantes AUTO_INCREMENT = 5;

INSERT INTO Socios_Alquiler (id_alquiler, id_socio) VALUES
(1, 1), 
(2, 2),
(3, 3),
(4, 4);

INSERT INTO Reserva (id_socio_solicitante, plazas_solicitadas, proposito_actividad, precio_total, matricula_embarcacion, fecha_reserva, id_empleado) VALUES
(1, 4, 'Paseo familiar', 200.00, 'ABC123', '2024-12-05', 1),
(2, 6, 'Celebración evento', 300.00, 'DEF456', '2024-12-18', 2),
(3, 3, 'Excursión', 150.00, 'JKL012', '2024-12-12', 3);

ALTER TABLE Reserva AUTO_INCREMENT = 4;