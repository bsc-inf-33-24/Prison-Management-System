ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;

UPDATE users
SET role = 'Records Officer',
    authentication_version = authentication_version + 1
WHERE role = 'Prison Officer';

ALTER TABLE users ADD CONSTRAINT users_role_check CHECK (role IN (
    'Super Admin', 'Commissioner', 'Records Officer', 'Disciplinary Officer',
    'Case Officer', 'Visitor Officer', 'Instructor', 'Medical Officer',
    'Duty Officer', 'Program Facilitator'
));
