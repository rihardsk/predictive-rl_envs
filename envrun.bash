# PORT="${PORT:-4096}"

#Path back to rl-library main directory from here
basePath=..
systemPath=$basePath/system

productsPath=$basePath/products
#Source a script that sets all important functions and variables
source $systemPath/scripts/rl-library-includes.sh

#Linux command to start RL_glue
#rl_glue &
startRLGlueInBackGround
java -jar $productsPath/CartPole.jar

waitForRLGlueToDie

# RLGLUE_PORT=$PORT startRLGlueInBackGround
# RLGLUE_PORT=$PORT java -jar $productsPath/CartPole.jar
# 
# RLGLUE_PORT=$PORT waitForRLGlueToDie

