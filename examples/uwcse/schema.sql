CREATE TABLE advisedby (
   student VARCHAR(32),
   advisor VARCHAR(32),
   CONSTRAINT pk_advisedby PRIMARY KEY (student,advisor)
);

CREATE TABLE taughtby (
   course VARCHAR(32),
   professor VARCHAR(32),
   term VARCHAR(32),
   CONSTRAINT pk_taughtby PRIMARY KEY (course,professor,term)
);

CREATE TABLE courselevel (
   course VARCHAR(32),
   level VARCHAR(32),
   CONSTRAINT pk_courselevel PRIMARY KEY (course)   
);

CREATE TABLE hasposition (
   professor VARCHAR(32),
   pos VARCHAR(32),
   CONSTRAINT pk_hasposition PRIMARY KEY (professor)
);

CREATE TABLE inphase (
   student VARCHAR(32),
   phase VARCHAR(32),
   CONSTRAINT pk_inphase PRIMARY KEY (student)
);

CREATE TABLE yearsinprogram (
   student VARCHAR(32),
   years VARCHAR(32),
   CONSTRAINT pk_yearsinprogram PRIMARY KEY (student)
);

CREATE TABLE ta (
   course VARCHAR(32),
   student VARCHAR(32),
   term VARCHAR(32),
   CONSTRAINT pk_ta PRIMARY KEY (course,student,term)
);

CREATE TABLE professor (
   professor VARCHAR(32),
   CONSTRAINT pk_professor PRIMARY KEY (professor)
);

CREATE TABLE student (
   student VARCHAR(32),
   CONSTRAINT pk_student PRIMARY KEY (student)
);

CREATE TABLE publication (
   publication VARCHAR(32),
   author VARCHAR(32),
   CONSTRAINT pk_publication PRIMARY KEY (publication,author)
);