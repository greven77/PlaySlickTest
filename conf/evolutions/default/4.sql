# --- !Ups
CREATE TABLE votes(
    answer_id bigint(20) NOT NULL,
    user_id bigint(20) NOT NULL,
    vote_value int NOT NULL,
    CONSTRAINT vote_answer_fk FOREIGN KEY (answer_id) REFERENCES answers(id) ON DELETE CASCADE,
    CONSTRAINT vote_user_fk FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_vote_value CHECK (vote = -1 OR vote = 1),
    CONSTRAINT vote_pk PRIMARY KEY (answer_id, user_id)
);

# --- !Downs
DROP TABLE votes;
