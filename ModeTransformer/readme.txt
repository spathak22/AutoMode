Stpes to run the ModeTranform tool:
1) unzip the package 
2) cd to folder ModeTransform
3) ant
4) cd dist
5) the jar will be present in dist folder, from here we can run the tool using command mentioned below
6) java -cp ModeTransform.jar modetransform.main.TransformMain  -inputPath=/Users/SherLock/Desktop/Books/REsearch/DARPA/ -outputPath=/Users/SherLock/Desktop/Books/REsearch/DARPA/test/

- inputPath contains the input mode file and transfomation file. File name convention is:
	Mode file = datamodel-input.json
	tranformation file = transform.txt

-outputpath contains the result file datamodel-output.json

logs can be found under folder ModeTranform/outputlog/transform.log
