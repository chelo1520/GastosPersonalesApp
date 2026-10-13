-- Se ejecuta en cada arranque: solo inserta el usuario de prueba si todavía no existe
INSERT INTO Usuario(email, password, rol, activo) SELECT 'test@unlam.edu.ar', 'test', 'ADMIN', true FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM Usuario WHERE email = 'test@unlam.edu.ar');

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

-- Gastos de ejemplo del usuario de prueba, siempre los mismos, en el mes en curso. Cada gasto se
-- fecha "dias" antes de hoy, sin salirse del mes. Si ya existe uno con la misma descripción en el
-- mes, no se vuelve a insertar. Arman el ejemplo del doc de gastos hormiga: Café y snacks y Kiosco
-- son hormiga, Delivery y Taxis y apps no (menos de 4), la manicura tampoco (pasa el 2 % del
-- presupuesto) y la leche tampoco (Alimentación es esencial)
INSERT INTO Gasto(importe, fecha, descripcion, usuario_id, categoria_id)
SELECT d.importe,
       GREATEST(DATE_SUB(CURDATE(), INTERVAL (DAYOFMONTH(CURDATE()) - 1) DAY), DATE_SUB(CURDATE(), INTERVAL d.dias DAY)),
       d.descripcion, u.id, c.id
FROM (
  SELECT 'Alquiler' AS descripcion, 250000 AS importe, 'Vivienda' AS categoria, 18 AS dias
  UNION ALL SELECT 'Expensas', 45000, 'Vivienda', 18
  UNION ALL SELECT 'Luz e internet', 38000, 'Servicios', 12
  UNION ALL SELECT 'Carga SUBE', 15000, 'Transporte', 10
  UNION ALL SELECT 'Farmacia', 9800, 'Salud', 6
  UNION ALL SELECT 'Compra semanal 1', 46200, 'Alimentación', 15
  UNION ALL SELECT 'Compra semanal 2', 32000, 'Alimentación', 8
  UNION ALL SELECT 'Leche 1', 1500, 'Alimentación', 17
  UNION ALL SELECT 'Leche 2', 1500, 'Alimentación', 15
  UNION ALL SELECT 'Leche 3', 1500, 'Alimentación', 13
  UNION ALL SELECT 'Leche 4', 1500, 'Alimentación', 11
  UNION ALL SELECT 'Leche 5', 1500, 'Alimentación', 9
  UNION ALL SELECT 'Leche 6', 1500, 'Alimentación', 6
  UNION ALL SELECT 'Leche 7', 1500, 'Alimentación', 3
  UNION ALL SELECT 'Leche 8', 1500, 'Alimentación', 1
  UNION ALL SELECT 'Café 1', 2500, 'Café y snacks', 17
  UNION ALL SELECT 'Café 2', 2500, 'Café y snacks', 15
  UNION ALL SELECT 'Café 3', 2500, 'Café y snacks', 13
  UNION ALL SELECT 'Café 4', 2500, 'Café y snacks', 11
  UNION ALL SELECT 'Café 5', 2500, 'Café y snacks', 9
  UNION ALL SELECT 'Café 6', 2500, 'Café y snacks', 8
  UNION ALL SELECT 'Café 7', 2500, 'Café y snacks', 6
  UNION ALL SELECT 'Café 8', 2500, 'Café y snacks', 5
  UNION ALL SELECT 'Café 9', 2500, 'Café y snacks', 3
  UNION ALL SELECT 'Café 10', 2500, 'Café y snacks', 2
  UNION ALL SELECT 'Café 11', 2500, 'Café y snacks', 1
  UNION ALL SELECT 'Café 12', 2500, 'Café y snacks', 0
  UNION ALL SELECT 'Kiosco 1', 1800, 'Kiosco', 16
  UNION ALL SELECT 'Kiosco 2', 1800, 'Kiosco', 14
  UNION ALL SELECT 'Kiosco 3', 1800, 'Kiosco', 12
  UNION ALL SELECT 'Kiosco 4', 1800, 'Kiosco', 10
  UNION ALL SELECT 'Kiosco 5', 1800, 'Kiosco', 7
  UNION ALL SELECT 'Kiosco 6', 1800, 'Kiosco', 5
  UNION ALL SELECT 'Kiosco 7', 1800, 'Kiosco', 3
  UNION ALL SELECT 'Kiosco 8', 1800, 'Kiosco', 1
  UNION ALL SELECT 'Delivery 1', 4000, 'Delivery', 14
  UNION ALL SELECT 'Delivery 2', 4000, 'Delivery', 7
  UNION ALL SELECT 'Delivery 3', 4000, 'Delivery', 2
  UNION ALL SELECT 'Uber 1', 6500, 'Taxis y apps', 9
  UNION ALL SELECT 'Uber 2', 7200, 'Taxis y apps', 4
  UNION ALL SELECT 'Manicura', 12000, 'Estética y cuidado personal', 8
) d
JOIN Usuario u ON u.email = 'test@unlam.edu.ar'
JOIN Categoria c ON c.nombre = d.categoria
WHERE NOT EXISTS (
  SELECT 1 FROM Gasto g
  WHERE g.usuario_id = u.id
    AND g.descripcion = d.descripcion
    AND g.fecha >= DATE_SUB(CURDATE(), INTERVAL (DAYOFMONTH(CURDATE()) - 1) DAY)
);

-- Historial del usuario de prueba: gastos de los 5 meses anteriores al actual, para el gráfico
-- por mes. "meses" = cuántos meses atrás, "dia" = día de ese mes. Los alquileres y servicios
-- suben con los meses, y los cafés, el kiosco y el delivery cambian para que la parte hormiga varíe.
-- Si ya existe un gasto con la misma descripción en ese mes, no se vuelve a insertar
INSERT INTO Gasto(importe, fecha, descripcion, usuario_id, categoria_id)
SELECT d.importe,
       DATE_ADD(DATE_SUB(DATE_SUB(CURDATE(), INTERVAL (DAYOFMONTH(CURDATE()) - 1) DAY), INTERVAL d.meses MONTH), INTERVAL (d.dia - 1) DAY),
       d.descripcion, u.id, c.id
FROM (
  SELECT 'Alquiler' AS descripcion, 220000 AS importe, 'Vivienda' AS categoria, 5 AS meses, 1 AS dia
  UNION ALL SELECT 'Expensas', 38000, 'Vivienda', 5, 5
  UNION ALL SELECT 'Luz e internet', 30000, 'Servicios', 5, 10
  UNION ALL SELECT 'Compra del mes', 120000, 'Alimentación', 5, 3
  UNION ALL SELECT 'Carga SUBE', 12000, 'Transporte', 5, 2
  UNION ALL SELECT 'Café 1', 2200, 'Café y snacks', 5, 2
  UNION ALL SELECT 'Café 2', 2200, 'Café y snacks', 5, 4
  UNION ALL SELECT 'Café 3', 2200, 'Café y snacks', 5, 6
  UNION ALL SELECT 'Café 4', 2200, 'Café y snacks', 5, 8
  UNION ALL SELECT 'Café 5', 2200, 'Café y snacks', 5, 10
  UNION ALL SELECT 'Café 6', 2200, 'Café y snacks', 5, 12
  UNION ALL SELECT 'Cena afuera', 15000, 'Comida afuera', 5, 20
  UNION ALL SELECT 'Alquiler', 220000, 'Vivienda', 4, 1
  UNION ALL SELECT 'Expensas', 39000, 'Vivienda', 4, 5
  UNION ALL SELECT 'Luz e internet', 31000, 'Servicios', 4, 10
  UNION ALL SELECT 'Compra del mes', 128000, 'Alimentación', 4, 3
  UNION ALL SELECT 'Carga SUBE', 12500, 'Transporte', 4, 2
  UNION ALL SELECT 'Café 1', 2300, 'Café y snacks', 4, 2
  UNION ALL SELECT 'Café 2', 2300, 'Café y snacks', 4, 4
  UNION ALL SELECT 'Café 3', 2300, 'Café y snacks', 4, 6
  UNION ALL SELECT 'Café 4', 2300, 'Café y snacks', 4, 8
  UNION ALL SELECT 'Kiosco 1', 1500, 'Kiosco', 4, 3
  UNION ALL SELECT 'Kiosco 2', 1500, 'Kiosco', 4, 6
  UNION ALL SELECT 'Kiosco 3', 1500, 'Kiosco', 4, 9
  UNION ALL SELECT 'Kiosco 4', 1500, 'Kiosco', 4, 12
  UNION ALL SELECT 'Kiosco 5', 1500, 'Kiosco', 4, 15
  UNION ALL SELECT 'Cena afuera', 22000, 'Comida afuera', 4, 20
  UNION ALL SELECT 'Alquiler', 235000, 'Vivienda', 3, 1
  UNION ALL SELECT 'Expensas', 41000, 'Vivienda', 3, 5
  UNION ALL SELECT 'Luz e internet', 34000, 'Servicios', 3, 10
  UNION ALL SELECT 'Compra del mes', 135000, 'Alimentación', 3, 3
  UNION ALL SELECT 'Carga SUBE', 13500, 'Transporte', 3, 2
  UNION ALL SELECT 'Café 1', 2400, 'Café y snacks', 3, 2
  UNION ALL SELECT 'Café 2', 2400, 'Café y snacks', 3, 4
  UNION ALL SELECT 'Café 3', 2400, 'Café y snacks', 3, 6
  UNION ALL SELECT 'Café 4', 2400, 'Café y snacks', 3, 8
  UNION ALL SELECT 'Café 5', 2400, 'Café y snacks', 3, 10
  UNION ALL SELECT 'Café 6', 2400, 'Café y snacks', 3, 12
  UNION ALL SELECT 'Café 7', 2400, 'Café y snacks', 3, 14
  UNION ALL SELECT 'Café 8', 2400, 'Café y snacks', 3, 16
  UNION ALL SELECT 'Kiosco 1', 1600, 'Kiosco', 3, 3
  UNION ALL SELECT 'Kiosco 2', 1600, 'Kiosco', 3, 6
  UNION ALL SELECT 'Kiosco 3', 1600, 'Kiosco', 3, 9
  UNION ALL SELECT 'Kiosco 4', 1600, 'Kiosco', 3, 12
  UNION ALL SELECT 'Kiosco 5', 1600, 'Kiosco', 3, 15
  UNION ALL SELECT 'Kiosco 6', 1600, 'Kiosco', 3, 18
  UNION ALL SELECT 'Delivery 1', 3800, 'Delivery', 3, 6
  UNION ALL SELECT 'Delivery 2', 3800, 'Delivery', 3, 12
  UNION ALL SELECT 'Delivery 3', 3800, 'Delivery', 3, 18
  UNION ALL SELECT 'Delivery 4', 3800, 'Delivery', 3, 24
  UNION ALL SELECT 'Alquiler', 235000, 'Vivienda', 2, 1
  UNION ALL SELECT 'Expensas', 42000, 'Vivienda', 2, 5
  UNION ALL SELECT 'Luz e internet', 35000, 'Servicios', 2, 10
  UNION ALL SELECT 'Compra del mes', 140000, 'Alimentación', 2, 3
  UNION ALL SELECT 'Carga SUBE', 14000, 'Transporte', 2, 2
  UNION ALL SELECT 'Café 1', 2400, 'Café y snacks', 2, 2
  UNION ALL SELECT 'Café 2', 2400, 'Café y snacks', 2, 4
  UNION ALL SELECT 'Café 3', 2400, 'Café y snacks', 2, 6
  UNION ALL SELECT 'Café 4', 2400, 'Café y snacks', 2, 8
  UNION ALL SELECT 'Café 5', 2400, 'Café y snacks', 2, 10
  UNION ALL SELECT 'Café 6', 2400, 'Café y snacks', 2, 12
  UNION ALL SELECT 'Café 7', 2400, 'Café y snacks', 2, 14
  UNION ALL SELECT 'Café 8', 2400, 'Café y snacks', 2, 16
  UNION ALL SELECT 'Café 9', 2400, 'Café y snacks', 2, 18
  UNION ALL SELECT 'Café 10', 2400, 'Café y snacks', 2, 20
  UNION ALL SELECT 'Kiosco 1', 1700, 'Kiosco', 2, 3
  UNION ALL SELECT 'Kiosco 2', 1700, 'Kiosco', 2, 6
  UNION ALL SELECT 'Kiosco 3', 1700, 'Kiosco', 2, 9
  UNION ALL SELECT 'Kiosco 4', 1700, 'Kiosco', 2, 12
  UNION ALL SELECT 'Kiosco 5', 1700, 'Kiosco', 2, 15
  UNION ALL SELECT 'Kiosco 6', 1700, 'Kiosco', 2, 18
  UNION ALL SELECT 'Kiosco 7', 1700, 'Kiosco', 2, 21
  UNION ALL SELECT 'Delivery 1', 3800, 'Delivery', 2, 6
  UNION ALL SELECT 'Delivery 2', 3800, 'Delivery', 2, 12
  UNION ALL SELECT 'Cena afuera', 18000, 'Comida afuera', 2, 20
  UNION ALL SELECT 'Alquiler', 250000, 'Vivienda', 1, 1
  UNION ALL SELECT 'Expensas', 45000, 'Vivienda', 1, 5
  UNION ALL SELECT 'Luz e internet', 37000, 'Servicios', 1, 10
  UNION ALL SELECT 'Compra del mes', 150000, 'Alimentación', 1, 3
  UNION ALL SELECT 'Carga SUBE', 15000, 'Transporte', 1, 2
  UNION ALL SELECT 'Café 1', 2500, 'Café y snacks', 1, 2
  UNION ALL SELECT 'Café 2', 2500, 'Café y snacks', 1, 4
  UNION ALL SELECT 'Café 3', 2500, 'Café y snacks', 1, 6
  UNION ALL SELECT 'Kiosco 1', 1800, 'Kiosco', 1, 3
  UNION ALL SELECT 'Kiosco 2', 1800, 'Kiosco', 1, 6
  UNION ALL SELECT 'Kiosco 3', 1800, 'Kiosco', 1, 9
  UNION ALL SELECT 'Kiosco 4', 1800, 'Kiosco', 1, 12
  UNION ALL SELECT 'Cena afuera', 30000, 'Comida afuera', 1, 20
) d
JOIN Usuario u ON u.email = 'test@unlam.edu.ar'
JOIN Categoria c ON c.nombre = d.categoria
WHERE NOT EXISTS (
  SELECT 1 FROM Gasto g
  WHERE g.usuario_id = u.id
    AND g.descripcion = d.descripcion
    AND g.fecha BETWEEN DATE_SUB(DATE_SUB(CURDATE(), INTERVAL (DAYOFMONTH(CURDATE()) - 1) DAY), INTERVAL d.meses MONTH)
                    AND LAST_DAY(DATE_SUB(CURDATE(), INTERVAL d.meses MONTH))
);

-- Más gastos del historial (mismos 5 meses), para que el gráfico tenga variedad: salud, curso,
-- suscripciones, salidas, estética, verdulería y viajes en Uber. Los Uber llegan a 4 o más en
-- algunos meses y ahí también cuentan como hormiga. Algunos meses quedan debajo del presupuesto
-- y otros arriba. Mismas reglas: no se duplican
INSERT INTO Gasto(importe, fecha, descripcion, usuario_id, categoria_id)
SELECT d.importe,
       DATE_ADD(DATE_SUB(DATE_SUB(CURDATE(), INTERVAL (DAYOFMONTH(CURDATE()) - 1) DAY), INTERVAL d.meses MONTH), INTERVAL (d.dia - 1) DAY),
       d.descripcion, u.id, c.id
FROM (
  SELECT 'Farmacia' AS descripcion, 8500 AS importe, 'Salud' AS categoria, 5 AS meses, 9 AS dia
  UNION ALL SELECT 'Netflix', 6500, 'Suscripciones', 5, 7
  UNION ALL SELECT 'Spotify', 3500, 'Suscripciones', 5, 7
  UNION ALL SELECT 'Cine', 9000, 'Entretenimiento', 5, 16
  UNION ALL SELECT 'Uber 1', 5200, 'Taxis y apps', 5, 4
  UNION ALL SELECT 'Uber 2', 5200, 'Taxis y apps', 5, 8
  UNION ALL SELECT 'Peluquería', 14000, 'Estética y cuidado personal', 5, 12
  UNION ALL SELECT 'Verdulería 1', 4500, 'Alimentación', 5, 7
  UNION ALL SELECT 'Verdulería 2', 4800, 'Alimentación', 5, 14
  UNION ALL SELECT 'Verdulería 3', 5100, 'Alimentación', 5, 21
  UNION ALL SELECT 'Farmacia', 12000, 'Salud', 4, 9
  UNION ALL SELECT 'Netflix', 6500, 'Suscripciones', 4, 7
  UNION ALL SELECT 'Spotify', 3500, 'Suscripciones', 4, 7
  UNION ALL SELECT 'Uber 1', 5600, 'Taxis y apps', 4, 4
  UNION ALL SELECT 'Uber 2', 5600, 'Taxis y apps', 4, 8
  UNION ALL SELECT 'Uber 3', 5600, 'Taxis y apps', 4, 12
  UNION ALL SELECT 'Uber 4', 5600, 'Taxis y apps', 4, 16
  UNION ALL SELECT 'Uber 5', 5600, 'Taxis y apps', 4, 20
  UNION ALL SELECT 'Uñas', 9500, 'Estética y cuidado personal', 4, 12
  UNION ALL SELECT 'Verdulería 1', 4700, 'Alimentación', 4, 7
  UNION ALL SELECT 'Verdulería 2', 5000, 'Alimentación', 4, 14
  UNION ALL SELECT 'Verdulería 3', 5300, 'Alimentación', 4, 21
  UNION ALL SELECT 'Farmacia', 6400, 'Salud', 3, 9
  UNION ALL SELECT 'Curso de inglés', 28000, 'Educación', 3, 4
  UNION ALL SELECT 'Netflix', 6500, 'Suscripciones', 3, 7
  UNION ALL SELECT 'Spotify', 3500, 'Suscripciones', 3, 7
  UNION ALL SELECT 'Cine', 14000, 'Entretenimiento', 3, 16
  UNION ALL SELECT 'Uber 1', 5900, 'Taxis y apps', 3, 4
  UNION ALL SELECT 'Uber 2', 5900, 'Taxis y apps', 3, 8
  UNION ALL SELECT 'Uber 3', 5900, 'Taxis y apps', 3, 12
  UNION ALL SELECT 'Cejas', 7000, 'Estética y cuidado personal', 3, 12
  UNION ALL SELECT 'Verdulería 1', 4800, 'Alimentación', 3, 7
  UNION ALL SELECT 'Verdulería 2', 5100, 'Alimentación', 3, 14
  UNION ALL SELECT 'Verdulería 3', 5400, 'Alimentación', 3, 21
  UNION ALL SELECT 'Libro', 12000, 'Ocio', 3, 22
  UNION ALL SELECT 'Farmacia', 15800, 'Salud', 2, 9
  UNION ALL SELECT 'Netflix', 6500, 'Suscripciones', 2, 7
  UNION ALL SELECT 'Spotify', 3500, 'Suscripciones', 2, 7
  UNION ALL SELECT 'Uber 1', 6100, 'Taxis y apps', 2, 4
  UNION ALL SELECT 'Uber 2', 6100, 'Taxis y apps', 2, 8
  UNION ALL SELECT 'Uber 3', 6100, 'Taxis y apps', 2, 12
  UNION ALL SELECT 'Uber 4', 6100, 'Taxis y apps', 2, 16
  UNION ALL SELECT 'Uber 5', 6100, 'Taxis y apps', 2, 20
  UNION ALL SELECT 'Uber 6', 6100, 'Taxis y apps', 2, 24
  UNION ALL SELECT 'Uñas', 9500, 'Estética y cuidado personal', 2, 12
  UNION ALL SELECT 'Verdulería 1', 5000, 'Alimentación', 2, 7
  UNION ALL SELECT 'Verdulería 2', 5300, 'Alimentación', 2, 14
  UNION ALL SELECT 'Verdulería 3', 5600, 'Alimentación', 2, 21
  UNION ALL SELECT 'Farmacia', 9200, 'Salud', 1, 9
  UNION ALL SELECT 'Netflix', 6500, 'Suscripciones', 1, 7
  UNION ALL SELECT 'Spotify', 3500, 'Suscripciones', 1, 7
  UNION ALL SELECT 'Uber 1', 6400, 'Taxis y apps', 1, 4
  UNION ALL SELECT 'Uber 2', 6400, 'Taxis y apps', 1, 8
  UNION ALL SELECT 'Uber 3', 6400, 'Taxis y apps', 1, 12
  UNION ALL SELECT 'Uber 4', 6400, 'Taxis y apps', 1, 16
  UNION ALL SELECT 'Verdulería 1', 5300, 'Alimentación', 1, 7
  UNION ALL SELECT 'Verdulería 2', 5600, 'Alimentación', 1, 14
  UNION ALL SELECT 'Verdulería 3', 5900, 'Alimentación', 1, 21
) d
JOIN Usuario u ON u.email = 'test@unlam.edu.ar'
JOIN Categoria c ON c.nombre = d.categoria
WHERE NOT EXISTS (
  SELECT 1 FROM Gasto g
  WHERE g.usuario_id = u.id
    AND g.descripcion = d.descripcion
    AND g.fecha BETWEEN DATE_SUB(DATE_SUB(CURDATE(), INTERVAL (DAYOFMONTH(CURDATE()) - 1) DAY), INTERVAL d.meses MONTH)
                    AND LAST_DAY(DATE_SUB(CURDATE(), INTERVAL d.meses MONTH))
);
