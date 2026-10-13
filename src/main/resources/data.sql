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
