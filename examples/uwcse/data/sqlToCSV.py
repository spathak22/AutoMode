#!/usr/bin/env python

from __future__ import print_function

import pymysql

# host = '127.0.0.1'
# port = 3306
# user = 'root'
# passwd = ''
# db = 'uwcse'

# Create connection
conn = pymysql.connect(host='127.0.0.1', port=3306, user='root', passwd='', db='uwcse')

# SQL statement
statements = {
	"student": "SELECT * FROM student_set4_schema1_sigmod17;",
	"yearsinprogram": "SELECT * FROM yearsinprogram_set4_schema1_sigmod17;",
	"inphase": "SELECT * FROM inphase_set4_schema1_sigmod17;",
	"professor": "SELECT * FROM professor_set4_schema1_sigmod17;",
	"hasposition": "SELECT * FROM hasposition_set4_schema1_sigmod17;",
	"courselevel": "SELECT * FROM courselevel_set4_schema1_sigmod17;",
	"taughtby": "SELECT * FROM taughtby_set4_schema1_sigmod17;",
	"ta": "SELECT * FROM ta_set4_schema1_sigmod17;",
	"publication": "SELECT * FROM publication_set4_schema1_sigmod17;"
}

for table, statement in statements.iteritems():
	print(statement)
	cur = conn.cursor()
	cur.execute(statement)
	file = open(table+".csv", "w")
	for row in cur:
		tuple = str(row).replace("'", "").lower()
		if len(row) == 1:
			tuple = tuple.replace(",", "").lower()
		file.write(tuple[1:-1]+"\n")
	# Close files
	cur.close()
	file.close()


# Close connection
conn.close()
