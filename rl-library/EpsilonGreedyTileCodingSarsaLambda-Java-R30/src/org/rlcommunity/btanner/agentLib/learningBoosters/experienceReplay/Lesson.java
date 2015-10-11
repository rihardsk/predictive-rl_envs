/*
* This program is free software; you can redistribute it and/or
* modify it under the terms of the GNU General Public License
* as published by the Free Software Foundation; either version 2
* of the License, or (at your option) any later version.
* 
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
* 
* You should have received a copy of the GNU General Public License
* along with this program; if not, write to the Free Software
* Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.rlcommunity.btanner.agentLib.learningBoosters.experienceReplay;

import org.rlcommunity.btanner.agentLib.dataStructures.DataPoint;
import java.util.Vector;

/**
 * This will store a trajectory of experience, known in the experience replay
 * literature as a lesson.
 * @author Brian Tanner
 */
public class Lesson {
    private Vector<DataPoint> theLesson=null;
    
    
    
    public Lesson(){
        theLesson=new Vector<DataPoint>();
    }
    
    public void addStep(DataPoint newStep){
        theLesson.add(newStep);
    }
    
    public int size(){return theLesson.size();}

    void cleanup() {
        if(theLesson!=null){
            theLesson.clear();
            theLesson=null;
        }
    }

    DataPoint getData(int i) {
        return theLesson.get(i);
    }
    
}
