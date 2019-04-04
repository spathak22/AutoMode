CREATE TABLE advisedby (
   student VARCHAR(32),
   advisor VARCHAR(32),
   CONSTRAINT pk_advisedby PRIMARY KEY (student,advisor)
);