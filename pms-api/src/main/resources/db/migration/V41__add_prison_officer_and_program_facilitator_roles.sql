ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;
ALTER TABLE users ADD CONSTRAINT users_role_check CHECK (role IN (
    'Super Admin', 'Records Officer', 'Disciplinary Officer', 'Case Officer',
    'Visitor Officer', 'Rehabilitation Officer', 'Instructor', 'Medical Officer', 'Duty Officer',
    'Prison Officer', 'Program Facilitator'
));