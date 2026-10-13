-- Se ejecuta en cada arranque: solo inserta el usuario de prueba si todavía no existe
INSERT INTO Usuario(email, password, rol, activo) SELECT 'test@unlam.edu.ar', 'test', 'ADMIN', true FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM Usuario WHERE email = 'test@unlam.edu.ar');

INSERT INTO Categoria(nombre, descripcion, esencial) SELECT 'Alimentación', 'Comidas y compras de alimentos', true FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM Categoria WHERE nombre = 'Alimentación');
INSERT INTO Categoria(nombre, descripcion, esencial) SELECT 'Transporte', 'Viajes y movilidad', true FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM Categoria WHERE nombre = 'Transporte');
INSERT INTO Categoria(nombre, descripcion, esencial) SELECT 'Vivienda', 'Alquiler, expensas y mantenimiento', true FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM Categoria WHERE nombre = 'Vivienda');
INSERT INTO Categoria(nombre, descripcion, esencial) SELECT 'Servicios', 'Luz, gas, agua, internet y telefonía', true FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM Categoria WHERE nombre = 'Servicios');
INSERT INTO Categoria(nombre, descripcion, esencial) SELECT 'Salud', 'Gastos médicos, farmacia y cuidado personal', true FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM Categoria WHERE nombre = 'Salud');
INSERT INTO Categoria(nombre, descripcion, esencial) SELECT 'Educación', 'Estudios, cursos y materiales', true FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM Categoria WHERE nombre = 'Educación');
INSERT INTO Categoria(nombre, descripcion, esencial) SELECT 'Entretenimiento', 'Salidas, actividades y suscripciones', false FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM Categoria WHERE nombre = 'Entretenimiento');
INSERT INTO Categoria(nombre, descripcion, esencial) SELECT 'Otros', 'Gastos que no corresponden a otra categoría', true FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM Categoria WHERE nombre = 'Otros');
INSERT INTO Categoria(nombre, descripcion, esencial) SELECT 'Comida afuera', 'Restaurantes, bares y almuerzos fuera de casa', false FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM Categoria WHERE nombre = 'Comida afuera');
INSERT INTO Categoria(nombre, descripcion, esencial) SELECT 'Café y snacks', 'Cafés, golosinas y colaciones', false FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM Categoria WHERE nombre = 'Café y snacks');
INSERT INTO Categoria(nombre, descripcion, esencial) SELECT 'Delivery', 'Pedidos de comida a domicilio', false FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM Categoria WHERE nombre = 'Delivery');
INSERT INTO Categoria(nombre, descripcion, esencial) SELECT 'Kiosco', 'Compras chicas en el kiosco', false FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM Categoria WHERE nombre = 'Kiosco');
INSERT INTO Categoria(nombre, descripcion, esencial) SELECT 'Suscripciones', 'Streaming, apps y servicios digitales', false FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM Categoria WHERE nombre = 'Suscripciones');
INSERT INTO Categoria(nombre, descripcion, esencial) SELECT 'Ocio', 'Juegos, hobbies y gustos personales', false FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM Categoria WHERE nombre = 'Ocio');

-- Categorías creadas antes de existir la columna esencial: Entretenimiento es prescindible, el resto esencial
UPDATE Categoria SET esencial = (nombre <> 'Entretenimiento') WHERE esencial IS NULL;
