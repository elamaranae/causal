-- Identity Database Seed (generated from seed-data.yml)
-- Do not edit manually — run: python3 generate-seeds.py

SET statement_timeout = 0;
SET client_encoding = 'UTF8';
SELECT pg_catalog.set_config('search_path', 'public', false);

TRUNCATE public.refresh_tokens, public.users RESTART IDENTITY CASCADE;

INSERT INTO public.users VALUES (1, NULL, 'sample@example.dev', '$2a$10$FDq3Mtrrk04f.htulpNj9eDusOo62FZdmFxYdg1MTgdF3ZgRc8Ae.', NULL);

SELECT setval('public.users_id_seq', 1, true);
SELECT setval('public.refresh_tokens_id_seq', 1, true);
