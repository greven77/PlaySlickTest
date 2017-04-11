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
    correct_answer bigint(20),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT creator_fk FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE,
    PRIMARY KEY(id)
);

CREATE TABLE TagsQuestions(
    tag_id bigint(20) NOT NULL,
    question_id bigint(20) NOT NULL,
    CONSTRAINT tag_fk FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE,
    CONSTRAINT question_fk FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE,
    CONSTRAINT tag_question_pk PRIMARY KEY (tag_id, question_id)
);

CREATE TABLE answers(
    id bigint(20) NOT NULL AUTO_INCREMENT,
    content text NOT NULL,
    user_id bigint(20) NOT NULL,
    question_id bigint(20) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT user_fk FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT parent_question_fk FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE,
    PRIMARY KEY (id)
);

ALTER TABLE "questions" CONSTRAINT "answer_fk" FOREIGN KEY (correct_answer) REFERENCES answers(id);
# --- !Downs

DROP TABLE TagsQuestions;
DROP TABLE tags;
DROP TABLE questions;
DROP TABLE answers;
