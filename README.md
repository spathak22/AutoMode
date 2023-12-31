# Automode: An automatic language bias generator system 
Find more detail about the project on Arxiv : https://arxiv.org/pdf/1710.01420v1.pdf

Following is the more generalized steps to run Automode for any dataset such as Immortals, UWCSE, IMDB, HIV, CORA,
WebKB.

## Installation
Install VoltDB

## Download and install VoltDB Community Edition. Instructions available.
Set environment variables
Set VOLTDB_HOME environment variable to installation directory of VoltDB.
Add $VOLTDB_HOME/bin to PATH environment variable.

Compile Automode by running:
```sh
ant
```
It will generate a dist folder, containing Automode.jar.


## Steps to run on UWCSE dataset:

Setup the dataset in Voltdb
```sh
    $ sh examples/uwcse/load.sh
```
    
Run: examples/uwcse/run.sh to run the Automode. This script does two things: 

1.  It runs the profiling algorithm to generate the inclusion dependency(DB tables and target query tables included)
2.  It runs AutoMode setup client to generate the modes. The script uses inds generated in step-1 
    
For examples in database:

```sh
    $ sh run.sh -examplesRelation advisedby -target advisedby -- approximate 0.5 automode-input/setup_inds.txt out/dataModel_advisedby.json abs 5 UWCSEProcedure 
```      
For examples in file:

```sh
    $ sh run.sh -examplesFile target/advisedby.csv -target advisedby -- approximate 0.5 automode-input/setup_inds.txt out/dataModel_advisedby.json abs 5 UWCSEProcedure 
```      

Script Parameters:

```sh       
            -examplesRelation       :   Provides the name of relation having examples      
            -examplesFile           :   Example file location
            -target                 :   target query(advisedby), specifying target will exclude all the example tables except the exampleRelation while IND extraction. 
            -algorithm              :   exact/approximate
            -maxIndError            :   This is to specify exact or approximation inclusion dependency 0 for exact
            -inputIndFilePath       :   Path to save intermediate ind file generated by Profiling algorithm
            -outModeFilePath        :   Path to save output Mode file generated by AutoMode
            -constThresholdType     :   Specify threshold type (abs or pctg)
            -constThresholdValue    :   Threshold value
            -storedProcedure        :   stored procedure name to be used by Castor   
            -dbUrl                  :   voltdb server url(optional argument, default : localhost)
            -port                   :   voltdb server port(optional argument, default : 21212)
            -manualTunedConstants   :   Path to manual constants language file (Not required for uwcse)
```     

Mode is generated and stored under "out" directory and other intermediate results such as setup_inds.txt are stored under automode-input directory           


## Steps to run on Immortals dataset:

Setup the dataset in Voltdb with all the examples relations

```sh
    $ sh examples/immortals/load.sh
```

### One step method    

Run: examples/immortals/run.sh to run the Automode. run.sh script will generate modes for all queries from query1 - query9. This script does two things:

1.  It calls the setup.sh script to generate the base modes 
2.  It calls the run_script.sh to generate the query specific modes 

```sh
    $ sh run.sh -target query -- exact 0 automode-input/setup_inds.txt automode-input/setup_modes.json pctg 18 automode-input/manual-constants 
```    

Script Parameters:
```sh
            -target                 :   target query(query), specifying target will exclude all the example tables except the exampleRelation while IND extraction.
            -algorithm              :   exact/approximate
            -maxIndError            :   This is to specify exact or approximation inclusion dependency 0 for exact
            -inputIndFilePath       :   Path to save intermediate ind file generated by Profiling algorithm            
            -inputModeFilePath      :   Path to save intermediate Mode file generated by AutoMode
            -constThresholdType     :   Specify threshold type (abs or pctg)
            -constThresholdValue    :   Threshold value
            -manualTunedConstants   :   Path to manual constants language file 
```       

Mode is generated and stored under "out" directory and other intermediate results such as setup_inds.txt are stored under automode-input directory           
    

### Two step method 

#### Run: examples/immortals/setup.sh to generate the base modes. 
The script does two things:

1.  It runs the profiling algorithm to generate the inclusion dependency(target query tables will be omitted)
2.  It runs AutoMode setup client to generate the base modes. The script uses inds generated in previous step 

```sh     
    sh setup.sh -target query -- approximate 0.0 automode-input/setup_inds.txt automode-input/setup_modes.json pctg 18 automode-input/manual-constants 
```
    
Script Parameters:

```sh
            -target                 :   target query(query), specifying target will exclude all the example tables except the exampleRelation while IND extraction.
            -algorithm              :   exact/approximate
            -maxIndError            :   This is to specify exact or approximation inclusion dependency 0 for exact
            -inputIndFilePath       :   Path to save intermediate ind file generated by Profiling algorithm            
            -outModeFilePath        :   Path to save intermediate Mode file generated by AutoMode
            -constThresholdType     :   Specify threshold type (abs or pctg)
            -constThresholdValue    :   Threshold value
            -manualTunedConstants   :   Path to manual constants language file 
            -storedProcedure        :   stored procedure name to be used by Castor(Optional while immortals setup)   
            -dbUrl                  :   voltdb server url(optional argument, default : localhost)
            -port                   :   voltdb server port(optional argument, default : 21212)            
```     

             
    
#### Run: examples/immortals/run_query.sh to generate the final modes. 
The script does two things:

1.  It runs the profiling algorithm to generate the one way inclusion dependency. 
    This step will extract the inclusion dependency between target query table and rest of database schema        
2.  The one way inds and setup modes generated in step-1 will now be used to generate the final modes to learn target query.
    It runs AutoMode per query client to generate the final modes for given target query
    
For examples in database

```sh
    $ sh run_query.sh -examplesRelation query1_all_pos -target query  --   automode-input/query1_inds.txt automode-input/setup_modes.json out/dataModel_query1.json 0 CastorProcedure_query1
```  

For examples in file
```sh
    $ sh run_query.sh -examplesFile target/query1_all_pos.csv -target query  --   automode-input/query1_inds.txt automode-input/setup_modes.json out/dataModel_query1.json 0 CastorProcedure_query1
```   
   
Script Parameters:

```sh
            -target                 :   target query(query), specifying target will exclude all the example tables except the exampleRelation while IND extraction.
            -examplesRelation       :   Provides the name of relation having examples      
            -examplesFile           :   Example file location
            -inputIndFile           :   Path to save intermediate ind file generated by Profiling algorithm            
            -inputModeFile          :   Path to save intermediate Mode file generated by AutoMode
            -outputModeFile         :   Path to save final Mode file generally stored in out directory
            -maxIndError            :   This is to specify exact or approximation inclusion dependency 0 for exact
            -storedProcedure        :   stored procedure name to be used by Castor
            -dbUrl                  :   voltdb server url(optional argument, default : localhost)
            -port                   :   voltdb server port(optional argument, default : 21212)                                    
```      

This step will generate the final mode for given target which is now ready to be used by castor

NOTE: 
```sh
The headmode will only be created only when target is provided with examplesRelation/examplesFile
```

 
 
 
 
 
 