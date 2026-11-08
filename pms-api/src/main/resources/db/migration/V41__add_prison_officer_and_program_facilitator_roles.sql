ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;
UPDATE users
SET role = 'Program Facilitator'
WHERE role = 'Rehabilitation Officer';

ALTER TABLE users ADD CONSTRAINT users_role_check CHECK (role IN (
    'Super Admin', 'Records Officer', 'Disciplinary Officer', 'Case Officer',
    'Visitor Officer', 'Instructor', 'Medical Officer', 'Duty Officer',
    'Prison Officer', 'Program Facilitator'
));