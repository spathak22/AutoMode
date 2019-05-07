#!/bin/bash

PORT=21212

# create schema
sqlcmd --port=$PORT < ddl-schema.sql
sqlcmd --port=$PORT < ddl-indexes.sql

# load database

csvloader --port $PORT --separator "," --skip 1 --file large_data_set/source.csv source -r ./log
csvloader --port $PORT --separator "," --skip 1 --file large_data_set/cot_event.csv cot_event -r ./log
csvloader --port $PORT --separator "," --skip 1 --file large_data_set/cot_event_position.csv cot_event_position -r ./log


FILES=target/*.csv
for f in $FILES
do
  echo "Loading file $f..."
  # take action on each file. $f store current file name
  filename=$(basename "$f")
  # extension="${filename##*.}"
  filename="${filename%.*}"
  csvloader --skip 1 --file $f $filename -r ./log
done