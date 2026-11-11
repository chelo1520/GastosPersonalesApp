-- Datos maestros: se ejecuta en cada arranque y no pisa nada. Solo inserta el usuario de prueba si todavía no existe.
-- Los datos ficticios para la demo (usuarios alto, medio y bajo con su historia) están en demo.sql, que se genera.
INSERT INTO Usuario(email, password, rol) SELECT 'test@unlam.edu.ar', 'test', 'ADMIN' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM Usuario WHERE email = 'test@unlam.edu.ar');

-- Categorías. fija = básica y siempre esencial: no se puede marcar como hormiga
INSERT INTO Categoria(nombre, descripcion, esencial, fija) SELECT 'Alimentación', 'Supermercado y compras de alimentos', true, true FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM Categoria WHERE nombre = 'Alimentación');
INSERT INTO Categoria(nombre, descripcion, esencial, fija) SELECT 'Transporte', 'Colectivo, tren, subte y combustible', true, true FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM Categoria WHERE nombre = 'Transporte');
INSERT INTO Categoria(nombre, descripcion, esencial, fija) SELECT 'Vivienda', 'Alquiler, expensas y mantenimiento', true, true FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM Categoria WHERE nombre = 'Vivienda');
INSERT INTO Categoria(nombre, descripcion, esencial, fija) SELECT 'Servicios', 'Luz, gas, agua, internet y telefonía', true, true FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM Categoria WHERE nombre = 'Servicios');
INSERT INTO Categoria(nombre, descripcion, esencial, fija) SELECT 'Salud', 'Médico, farmacia y obra social', true, true FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM Categoria WHERE nombre = 'Salud');
INSERT INTO Categoria(nombre, descripcion, esencial, fija) SELECT 'Educación', 'Estudios, cursos y materiales', true, true FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM Categoria WHERE nombre = 'Educación');
INSERT INTO Categoria(nombre, descripcion, esencial, fija) SELECT 'Otros', 'Gastos que no corresponden a otra categoría', true, false FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM Categoria WHERE nombre = 'Otros');
INSERT INTO Categoria(nombre, descripcion, esencial, fija) SELECT 'Entretenimiento', 'Salidas, actividades y suscripciones', false, false FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM Categoria WHERE nombre = 'Entretenimiento');
INSERT INTO Categoria(nombre, descripcion, esencial, fija) SELECT 'Comida afuera', 'Restaurantes, bares y almuerzos fuera de casa', false, false FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM Categoria WHERE nombre = 'Comida afuera');
INSERT INTO Categoria(nombre, descripcion, esencial, fija) SELECT 'Café y snacks', 'Cafés, golosinas y colaciones', false, false FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM Categoria WHERE nombre = 'Café y snacks');
INSERT INTO Categoria(nombre, descripcion, esencial, fija) SELECT 'Delivery', 'Pedidos de comida a domicilio', false, false FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM Categoria WHERE nombre = 'Delivery');
INSERT INTO Categoria(nombre, descripcion, esencial, fija) SELECT 'Kiosco', 'Compras chicas en el kiosco', false, false FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM Categoria WHERE nombre = 'Kiosco');
INSERT INTO Categoria(nombre, descripcion, esencial, fija) SELECT 'Suscripciones', 'Streaming, apps y servicios digitales', false, false FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM Categoria WHERE nombre = 'Suscripciones');
INSERT INTO Categoria(nombre, descripcion, esencial, fija) SELECT 'Ocio', 'Juegos, hobbies y gustos personales', false, false FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM Categoria WHERE nombre = 'Ocio');
INSERT INTO Categoria(nombre, descripcion, esencial, fija) SELECT 'Estética y cuidado personal', 'Uñas, cejas, peluquería y tratamientos', false, false FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM Categoria WHERE nombre = 'Estética y cuidado personal');
INSERT INTO Categoria(nombre, descripcion, esencial, fija) SELECT 'Taxis y apps', 'Uber, Cabify, taxis y remises', false, false FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM Categoria WHERE nombre = 'Taxis y apps');

-- Bases creadas antes de existir las columnas esencial y fija: se completan con los mismos valores
-- de arriba. Las básicas quedan fijas y esenciales aunque alguien las haya marcado como hormiga
UPDATE Categoria SET esencial = (nombre <> 'Entretenimiento') WHERE esencial IS NULL;
UPDATE Categoria SET fija = (nombre IN ('Alimentación', 'Transporte', 'Vivienda', 'Servicios', 'Salud', 'Educación')) WHERE fija IS NULL;
UPDATE Categoria SET esencial = true WHERE fija = true;
-- Estética y Taxis y apps salieron de Salud y Transporte: las descripciones viejas las mezclaban
UPDATE Categoria SET descripcion = 'Médico, farmacia y obra social' WHERE nombre = 'Salud';
UPDATE Categoria SET descripcion = 'Colectivo, tren, subte y combustible' WHERE nombre = 'Transporte';
UPDATE Categoria SET descripcion = 'Supermercado y compras de alimentos' WHERE nombre = 'Alimentación';
