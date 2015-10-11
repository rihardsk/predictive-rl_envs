# Neurostim Makefile v1

#If RL-Glue Core or the C/C++ Codecs are not installed to default locations
#You may need to use set the CFLAGS and LDFLAGS. 

CC := g++
OCFLAGS := -O2 -DNDEBUG -march=native
INCDIR := -I. -I./include/
OBJS := ./bin/model.o ./bin/kdtree2.o ./bin/matvecops.o 

BOOST:= $(BOOST)
LDFLAGS :=$(LDFLAGS)
CFLAGS :=$(CFLAGS) -I./include 


all: Neurostim NeurostimEnvironment NeurostimAgent NeurostimExperiment

##Experiments
Neurostim: ./bin/NeurostimFixedExperiment.o ./bin/NeurostimFixedAgent.o ./bin/NeurostimEnvironment.o $(OBJS)
	$(CC) $^ $(LDFLAGS) -lrlglue -lrlutils -o Neurostim
	
##C/C++ Codecs:
NeurostimEnvironment: ./bin/NeurostimEnvironment.o $(OBJS)
	$(CC) $^ $(LDFLAGS) -lrlutils -lrlenvironment -lrlgluenetdev -o NeurostimEnvironment
NeurostimAgent: ./bin/NeurostimFixedAgent.o ./bin/matvecops.o
	$(CC) $^ $(LDFLAGS) -lrlutils -lrlagent -lrlgluenetdev -o NeurostimAgent  
NeurostimExperiment: ./bin/NeurostimFixedExperiment.o
	$(CC) $^ $(LDFLAGS) -lrlutils -lrlexperiment -lrlgluenetdev -o NeurostimExperiment  


##System Objects
./bin/model.o: ./src/system/model.cpp
	$(CC) $(OCFLAGS) $(INCDIR) $(BOOST) -c $< -o $@

##Utility Functions
./bin/matvecops.o: ./src/utility/matvecops.cpp
	$(CC) $(OCFLAGS) $(INCDIR) $(BOOST) -c $< -o $@

./bin/kdtree2.o: ./src/utility/kdtree2.cpp
	$(CC) $(OCFLAGS) $(INCDIR) $(BOOST) -c $< -o $@  
	

##Objects
./bin/NeurostimFixedExperiment.o: ./core/NeurostimFixedExperiment.c
	$(CC)  $(CFLAGS) $(BOOST) -c ./core/NeurostimFixedExperiment.c -o ./bin/NeurostimFixedExperiment.o 

./bin/NeurostimFixedAgent.o: core/NeurostimFixedAgent.c
	$(CC)  $(CFLAGS) $(BOOST) -c ./core/NeurostimFixedAgent.c -o ./bin/NeurostimFixedAgent.o

./bin/NeurostimEnvironment.o: core/NeurostimEnvironment.c
	$(CC)  $(CFLAGS) $(BOOST) -c ./core/NeurostimEnvironment.c -o ./bin/NeurostimEnvironment.o 



clean:
	rm -f Neurostim NeurostimEnvironment NeurostimExperiment NeurostimAgent bin/*.o 
	make trash
trash:
	rm -f *~ */*~ */*/*~ 


zip: 
	zip neurostim.zip -r ./core ./include ./src ./params ./Makefile






