-- Identity Database Seed
-- Dump of current single-user database

SET statement_timeout = 0;
SET lock_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);

-- Clear existing data
TRUNCATE public.refresh_tokens, public.users RESTART IDENTITY CASCADE;

-- Users
INSERT INTO public.users VALUES (1, NULL, 'sample@example.dev', '$2a$10$FDq3Mtrrk04f.htulpNj9eDusOo62FZdmFxYdg1MTgdF3ZgRc8Ae.', NULL);

-- Reset sequences
SELECT setval('public.users_id_seq', 1, true);
SELECT setval('public.refresh_tokens_id_seq', 1, true);
