# --- !Ups

CREATE TABLE tags(
    id bigint(20) NOT NULL AUTO_INCREMENT,
    text varchar(40) UNIQUE NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE questions(
    id bigint(20) NOT NULL AUTO_INCREMENT,
    title varchar(60) UNIQUE NOT NULL,
    content TEXT NOT NULL,
    created_by bigint(20) NOT NULL,
    corrected_answered_by bigint(20),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (correct_answered_by) REFERENCES answers(id),
    PRIMARY KEY(id)
);

CREATE TABLE TagsQuestions(
    tag_id bigint(20) NOT NULL,
    question_id bigint(20) NOT NULL,
    FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE,
    FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE,
    CONSTRAINT tag_question_pk PRIMARY KEY (tag_id, question_id)
);

CREATE TABLE answers(
    id bigint(20) NOT NULL AUTO_INCREMENT,
    content text NOT NULL,
    user_id bigint(20) NOT NULL,
    question_id bigint(20) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE,
    PRIMARY KEY (id)
);
# --- !Downs

DROP TABLE tags;
DROP TABLE TagsQuestions;
DROP TABLE questions;
DROP TABLE answers;
