# --- !Ups

CREATE TABLE users (
    id bigint(20) NOT NULL AUTO_INCREMENT,
    username varchar(20) UNIQUE NOT NULL,
    email varchar(255) UNIQUE NOT NULL,
    fullname varchar(255) NOT NULL,
    password varchar(255) NOT NULL,
    PRIMARY KEY (id)
);

# --- !Downs

DROP TABLE users;
