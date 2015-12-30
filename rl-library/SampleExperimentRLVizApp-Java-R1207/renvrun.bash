#Path back to rl-library main directory from here
basePath=..
systemPath=$basePath/system

productsPath=$basePath/products
#Source a script that sets all important functions and variables
source $systemPath/scripts/rl-library-includes.sh

#Linux command to start RL_glue
#rl_glue &
startRLGlueInBackGround

#Linux Command for EnvironmentShell
#java -ea -Xmx128M -jar $systemPath/common/libs/rl-viz/EnvironmentShell.jar environment-jar-path=$productsPath &
startEnvShellInBackGround

startNetGuiTrainerDynamicEnvironmentStandardAgent

waitForAgentShellToDie
waitForRLGlueToDie

