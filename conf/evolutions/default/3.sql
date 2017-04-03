# --- !Ups

CREATE TABLE FavouriteQuestions(
    question_id bigint(20) NOT NULL,
    user_id bigint(20) NOT NULL,
    CONSTRAINT fq_question_fk FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE,
    CONSTRAINT fq_user_fk FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT favouritequestion_pk PRIMARY KEY (question_id, user_id)
);

# --- !Downs
DROP TABLE FavouriteQuestions;
