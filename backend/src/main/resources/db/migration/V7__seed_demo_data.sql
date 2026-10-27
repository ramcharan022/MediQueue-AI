-- V7: Demo hospital data (branches, departments, doctors, schedules)
-- All data is clearly labeled [DEMO] — for local development only.
-- Demo user accounts are created on first startup by DataInitializer.java

-- Hospital branches
INSERT INTO hospital_branch (name, address, city, timezone, phone) VALUES
('[DEMO] City Medical Centre — Main',  '42 Health Avenue, Koramangala', 'Bengaluru', 'Asia/Kolkata', '+91-80-2244-0001'),
('[DEMO] City Medical Centre — North', '15 Wellness Road, Yelahanka',   'Bengaluru', 'Asia/Kolkata', '+91-80-2244-0002');

-- Departments
INSERT INTO department (branch_id, name, description) VALUES
(1, 'General Medicine', 'Primary care and general health consultations'),
(1, 'Cardiology',       'Heart and cardiovascular disease management'),
(1, 'Orthopedics',      'Bone, joint, and musculoskeletal care'),
(1, 'Pediatrics',       'Child and adolescent healthcare'),
(2, 'General Medicine', 'Primary care and general health consultations'),
(2, 'Cardiology',       'Heart and cardiovascular disease management');

-- Doctors  (branch_id, department_id, full_name, specialization, qualification, consultation_minutes)
INSERT INTO doctor (branch_id, department_id, full_name, specialization, qualification, consultation_minutes) VALUES
(1, 1, '[DEMO] Dr. Priya Sharma',  'General Medicine', 'MBBS, MD (Internal Medicine)', 15),
(1, 1, '[DEMO] Dr. Ravi Kumar',    'General Medicine', 'MBBS, MD (General Practice)',  15),
(1, 2, '[DEMO] Dr. Ananya Rao',    'Cardiology',       'MBBS, DM (Cardiology)',         20),
(1, 3, '[DEMO] Dr. Suresh Nair',   'Orthopedics',      'MBBS, MS (Orthopedics)',        20),
(1, 4, '[DEMO] Dr. Meera Iyer',    'Pediatrics',       'MBBS, MD (Pediatrics)',         15),
(2, 5, '[DEMO] Dr. Kiran Mehta',   'General Medicine', 'MBBS, MD (Internal Medicine)', 15),
(2, 6, '[DEMO] Dr. Deepa Pillai',  'Cardiology',       'MBBS, DM (Cardiology)',         20);

-- Doctor schedules  (doctor_id, day_of_week 1=Mon…7=Sun, starts_at, ends_at)
INSERT INTO doctor_schedule (doctor_id, day_of_week, starts_at, ends_at) VALUES
-- Dr. Priya Sharma: Mon–Fri 09:00–13:00
(1,1,'09:00:00','13:00:00'),(1,2,'09:00:00','13:00:00'),(1,3,'09:00:00','13:00:00'),(1,4,'09:00:00','13:00:00'),(1,5,'09:00:00','13:00:00'),
-- Dr. Ravi Kumar: Mon–Fri 14:00–18:00
(2,1,'14:00:00','18:00:00'),(2,2,'14:00:00','18:00:00'),(2,3,'14:00:00','18:00:00'),(2,4,'14:00:00','18:00:00'),(2,5,'14:00:00','18:00:00'),
-- Dr. Ananya Rao: Mon–Thu 10:00–14:00
(3,1,'10:00:00','14:00:00'),(3,2,'10:00:00','14:00:00'),(3,3,'10:00:00','14:00:00'),(3,4,'10:00:00','14:00:00'),
-- Dr. Suresh Nair: Tue–Sat 09:00–12:00
(4,2,'09:00:00','12:00:00'),(4,3,'09:00:00','12:00:00'),(4,4,'09:00:00','12:00:00'),(4,5,'09:00:00','12:00:00'),(4,6,'09:00:00','12:00:00'),
-- Dr. Meera Iyer: Mon–Fri 10:00–15:00
(5,1,'10:00:00','15:00:00'),(5,2,'10:00:00','15:00:00'),(5,3,'10:00:00','15:00:00'),(5,4,'10:00:00','15:00:00'),(5,5,'10:00:00','15:00:00'),
-- Dr. Kiran Mehta: Mon–Fri 09:00–13:00
(6,1,'09:00:00','13:00:00'),(6,2,'09:00:00','13:00:00'),(6,3,'09:00:00','13:00:00'),(6,4,'09:00:00','13:00:00'),(6,5,'09:00:00','13:00:00'),
-- Dr. Deepa Pillai: Mon–Fri 15:00–19:00
(7,1,'15:00:00','19:00:00'),(7,2,'15:00:00','19:00:00'),(7,3,'15:00:00','19:00:00'),(7,4,'15:00:00','19:00:00'),(7,5,'15:00:00','19:00:00');
