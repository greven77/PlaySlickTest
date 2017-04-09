# --- !Ups
ALTER TABLE users
ADD token varchar(255);

# --- !Downs
ALTER TABLE users
DROP COLUMN token;
