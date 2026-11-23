-- Inventario inicial para las pruebas y la demostracion
INSERT INTO medicamentos (id_medicamento, codigo, nombre, presentacion, stock, activo) VALUES
 (gen_random_uuid(), 'MDT-0001', 'Acetaminofen', 'Tabletas 500 mg', 500, TRUE),
 (gen_random_uuid(), 'MDT-0002', 'Ibuprofeno', 'Tabletas 400 mg', 300, TRUE),
 (gen_random_uuid(), 'MDT-0003', 'Amoxicilina', 'Capsulas 500 mg', 200, TRUE),
 (gen_random_uuid(), 'MDT-0004', 'Loratadina', 'Tabletas 10 mg', 150, TRUE),
 (gen_random_uuid(), 'MDT-0005', 'Omeprazol', 'Capsulas 20 mg', 250, TRUE),
 (gen_random_uuid(), 'MDT-0006', 'Losartan', 'Tabletas 50 mg', 180, TRUE),
 (gen_random_uuid(), 'MDT-0007', 'Metformina', 'Tabletas 850 mg', 220, TRUE),
 (gen_random_uuid(), 'MDT-0008', 'Salbutamol', 'Inhalador 100 mcg', 5, TRUE);
SELECT setval('medicamento_codigo_seq', 8);
