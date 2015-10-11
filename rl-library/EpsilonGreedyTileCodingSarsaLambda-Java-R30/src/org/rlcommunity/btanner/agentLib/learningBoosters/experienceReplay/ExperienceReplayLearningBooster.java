package org.rlcommunity.btanner.agentLib.learningBoosters.experienceReplay;

import org.rlcommunity.btanner.agentLib.dataStructures.DataPoint;
import org.rlcommunity.btanner.agentLib.actionSelectors.ActionSelectorInterface;
import java.util.Random;
import java.util.Vector;

import org.rlcommunity.btanner.agentLib.learningBoosters.LearningBoosterInterface;
import org.rlcommunity.btanner.agentLib.learningModules.LearningModuleInterface;
import rlVizLib.general.ParameterHolder;
import org.rlcommunity.rlglue.codec.taskspec.TaskSpec;
import org.rlcommunity.rlglue.codec.types.Observation;

public class ExperienceReplayLearningBooster implements LearningBoosterInterface {

    private Vector<Lesson> theLessons;
    private Lesson currentLesson = null;
    Observation lastObservation;
    int lastAction;
    int maxLessonSize = 512;
    int lessonMemoryLimit = 65536;
    int currentMemoryUsed = 0;
    boolean forgetRandom = true;
    private int replayLessonCount = 15;
    private boolean uniformSampleDistribution = false;
    private boolean discardNonPolicyActions = false;
    //Limit the number of replays to the total number of lessons
    private boolean limitReplayToNumLessons=true;
    private LearningModuleInterface theLearningModule = null;
    private ActionSelectorInterface theActionSelector = null;
    Random theRand = new Random();

    public static void addToParameterHolder(ParameterHolder p) {
        p.addIntegerParam("e-replay-lesson-size", 1);
        p.addIntegerParam("e-replay-replay-count", 16);
        p.addIntegerParam("e-replay-memlimit", 65536);
        p.addBooleanParam("e-replay-uniform-dist", true);
        p.addBooleanParam("e-replay-off-policy", true);
        p.addBooleanParam("e-replay-forget-random", true);
        p.addBooleanParam("e-replay-limit-replay", true);
    }

    ExperienceReplayLearningBooster(TaskSpec theTaskObject, ParameterHolder theParameterHolder, LearningModuleInterface theLearningModule, ActionSelectorInterface theActionSelector) {
        this.theLearningModule = theLearningModule;
        this.theActionSelector = theActionSelector;

        //        this.alpha = theParameterHolder.getDoubleParam("e-replay-alpha");
        this.maxLessonSize = theParameterHolder.getIntegerParam("e-replay-lesson-size");
        this.replayLessonCount = theParameterHolder.getIntegerParam("e-replay-replay-count");
        this.lessonMemoryLimit = theParameterHolder.getIntegerParam("e-replay-memlimit");
        this.uniformSampleDistribution = theParameterHolder.getBooleanParam("e-replay-uniform-dist");
        this.discardNonPolicyActions = !theParameterHolder.getBooleanParam("e-replay-off-policy");
        this.forgetRandom = theParameterHolder.getBooleanParam("e-replay-forget-random");
        this.limitReplayToNumLessons=theParameterHolder.getBooleanParam("e-replay-limit-replay");
        theLessons = new Vector<Lesson>();
    }

    public void init() {
    }

    public void start(Observation theObservation, int theAction) {
        currentLesson = new Lesson();
        lastObservation = theObservation;
        lastAction = theAction;
    }

    public void step(Observation theObservation, double r, int theAction) {
        if (currentLesson.size() >= maxLessonSize) {
            capLesson();
            learnFromLessons();
        }
        DataPoint d = new DataPoint(lastObservation, lastAction, r, theObservation);
        currentLesson.addStep(d);

        lastObservation = theObservation;
        lastAction = theAction;
    }

    private void forgetSomething() {
        if (theLessons.size() == 0) {
            System.err.println("Experience replay told to forget something but there is nothing");
            System.err.println("Current lesson size is: " + currentLesson.size());
            System.err.println("Current Memory used is: " + currentMemoryUsed);
            System.err.println("lesson Memory Limit is: " + lessonMemoryLimit);
            System.exit(1);
        }

        int indexToForget = 0;
        if (forgetRandom) {
            indexToForget = theRand.nextInt(theLessons.size());
        }
        Lesson removedLesson = theLessons.remove(indexToForget);
        currentMemoryUsed -= removedLesson.size();
    }

    private void capLesson() {
        int thisLessonSize = currentLesson.size();

        //If we have to forget something, do it
        while (currentMemoryUsed + thisLessonSize >= lessonMemoryLimit) {
            forgetSomething();
        }

        currentMemoryUsed += thisLessonSize;
        theLessons.add(currentLesson);

        currentLesson = new Lesson();
    }

    public void end(double r) {
        DataPoint d = new DataPoint(lastObservation, lastAction, r);

        currentLesson.addStep(d);
        capLesson();
        learnFromLessons();
    }

    /**
     * This can be made so much faster with caching loop values
     * @param theLesson
     */
    private void learnFromLesson(Lesson theLesson) {
        for (int t = theLesson.size() - 1; t >= 0; t--) {
            DataPoint thisStep = theLesson.getData(t);

            //Remember ShivaramReplay is code word for don't replay off policy actions
            //If the action in this lesson is not the greedy action, move on
            if (discardNonPolicyActions) {
                double[] actionProbs = theActionSelector.getActionProbabilities(thisStep.getS(), theLearningModule);
                if (actionProbs[thisStep.getAction()] <= .1d) {
                    continue;
                }
            }


            double nextValue = 0.0d;
            if (thisStep.getSPrime() != null) {
                //We'll get the next state value by sampling an action
                int sampledNextAction = theActionSelector.sampleAction(thisStep.getSPrime(), theLearningModule);
                nextValue = theLearningModule.query(thisStep.getSPrime(), sampledNextAction);
            }

            double thisValue = theLearningModule.query(thisStep.getS(), thisStep.getAction());
            double r = thisStep.getReward();

            double target = r + nextValue;
            double delta = target - thisValue;

            theLearningModule.update(thisStep.getS(), thisStep.getAction(), delta);
        }
    }

    /**
     * This is an implementation that faithfully (I think) implements the strategy
     * Lin used for deciding which lesson's to learn from in the paper:
     * Self-Improving Reactive Agents Based On Reinforcement Learning, Planning and Teaching 
     *
     * @return k : 0 <= k < n where n is the number of lessons available
     */
    private int pickLessonToLearn() {

        if (uniformSampleDistribution) {
            return theRand.nextInt(theLessons.size());
        }

        return sampleLin(theLessons.size());
    }

    private static int sampleLin(int theSize) {
        double n = (double) theSize;
        double w = Math.min(3.0d, 1.0d + .02d * n);
        double r = Math.random();
        int k = (int) (n * Math.log(1.0 + r * (Math.exp(w) - 1.0d)) / w);
        return k;

    }
    //Test the distribution
    public static void main(String[] args) {

        System.out.print("nums=[");

        for (int i = 0; i < 10000; i++) {
            System.out.print(sampleLin(100000) + " ");
        }

        System.out.println("];");


    }

    /**
     * Go through all of the lessons backwards replayLessonCount times
     */
//    private void doShivaramReplay() {
//        for (int k = 0; k < Math.min(theLessons.size(), replayLessonCount); k++) {
//            for(int l=theLessons.size()-1;l>=0;l--){
//            learnFromLesson(theLessons.get(l));
//            }
//        }
//            
//    }
    private void learnFromLessons() {
        int replayCountToUse=replayLessonCount;

        if(limitReplayToNumLessons){
            replayCountToUse=Math.min(theLessons.size(), replayCountToUse);
        }
        for (int i = 0; i < replayCountToUse; i++) {
            int whichLesson = pickLessonToLearn();
            learnFromLesson(theLessons.get(whichLesson));
        }
    }

    public void cleanup() {
        if (theLessons != null) {
            for (Lesson thisLesson : theLessons) {
                thisLesson.cleanup();
            }
            theLessons.clear();
            theLessons = null;
        }
        currentLesson = null;
        lastObservation = null;
        /*this may have been cleaned up already, hopefully they are robust to it*/
        theLearningModule.cleanup();
        theActionSelector.cleanup();
    }
}
