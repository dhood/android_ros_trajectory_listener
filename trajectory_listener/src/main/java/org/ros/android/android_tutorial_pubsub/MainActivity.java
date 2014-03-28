/*
 * Copyright (C) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.ros.android.android_tutorial_pubsub;

import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.graphics.drawable.shapes.PathShape;
import android.os.Bundle;
import android.util.Log;

import org.ros.address.InetAddressFactory;
import org.ros.android.MessageCallable;
import org.ros.android.RosActivity;
import org.ros.android.view.RosImageView;
import org.ros.message.Duration;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

import java.util.List;

import geometry_msgs.PoseStamped;
import nav_msgs.Path;
/**
 * @author damonkohler@google.com (Damon Kohler). modified by Deanna Hood.
 */


public class MainActivity extends RosActivity {
  private static final java.lang.String TAG = "trajectoryListener";
    private int timeoutDuration_mSecs = 1000; //time in ms to leave the trajectory displayed before removing it (negative displays indefinitely)
    private double PPI_tablet = 298.9; //pixels per inch of android tablet
    private int[] resolution_tablet = {2560, 1600};
    private double MM2INCH = 0.0393701; //number of millimetres in one inch (for conversions)
  private RosImageView<nav_msgs.Path> rosImageView;


  public MainActivity() {
    // The RosActivity constructor configures the notification title and ticker
    // messages.
    super("Trajectory listener", "Trajectory listener");
  }

  @SuppressWarnings("unchecked")
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    rosImageView = (RosImageView<nav_msgs.Path>) findViewById(R.id.image);
    rosImageView.setTopicName("write_traj");
    rosImageView.setMessageType(nav_msgs.Path._TYPE);

    rosImageView.setMessageToDrawableCallable(new MessageCallable<Drawable, nav_msgs.Path>() {
      @Override
      public Drawable call(nav_msgs.Path message) {
          Log.e(TAG, "got a message");

          ShapeDrawable blankShapeDrawable=new ShapeDrawable(new PathShape(new android.graphics.Path(),0,0));
          blankShapeDrawable.setIntrinsicHeight(rosImageView.getHeight());
          blankShapeDrawable.setIntrinsicWidth(rosImageView.getWidth());
          blankShapeDrawable.setBounds(0, 0, rosImageView.getWidth(), rosImageView.getHeight());

          List<PoseStamped> points = message.getPoses();
          AnimationDrawable animationDrawable = new AnimationDrawable();

          android.graphics.Path trajPath = new android.graphics.Path();
          trajPath.moveTo((float)M2PX(points.get(0).getPose().getPosition().getX()),resolution_tablet[0] - (float)M2PX(points.get(0).getPose().getPosition().getY()));

          long timeUntilFirstFrame_msecs = points.get(0).getHeader().getStamp().totalNsecs()/1000000;
          animationDrawable.addFrame(blankShapeDrawable,(int)timeUntilFirstFrame_msecs);


          for(int i = 0; i < points.size()-1; i++) //special case for first and last point/frame of trajectory
          {
              //add new trajectory point onto path and create ShapeDrawable to pass as a frame for animation
              PoseStamped p = points.get(i);
              geometry_msgs.Point tx = p.getPose().getPosition();

              ShapeDrawable shapeDrawable = addPointToShapeDrawablePath((float) M2PX(tx.getX()), resolution_tablet[0] - (float) M2PX(tx.getY()), trajPath);

              //determine the duration of the frame for the animation
              Duration frameDuration = points.get(i + 1).getHeader().getStamp().subtract(p.getHeader().getStamp()); // take difference between times to get appropriate duration for frame to be displayed

              long dt_msecs = frameDuration.totalNsecs()/1000000;
              animationDrawable.addFrame(shapeDrawable,(int)dt_msecs); //unless the duration is over 2mil seconds the cast is ok

          }
          //cover end case
          PoseStamped p = points.get(points.size()-1);
          geometry_msgs.Point tx = p.getPose().getPosition();

          ShapeDrawable shapeDrawable = addPointToShapeDrawablePath((float) M2PX(tx.getX()), resolution_tablet[0] - (float) M2PX(tx.getY()), trajPath);


          if(timeoutDuration_mSecs >= 0)//only display the last frame until timeoutDuration has elapsed
          {
            animationDrawable.addFrame(shapeDrawable, timeoutDuration_mSecs);
            animationDrawable.addFrame(blankShapeDrawable,0); //stop displaying
          } else { //display last frame indefinitely
              animationDrawable.addFrame(shapeDrawable, 1000 ); //think it will be left there until something clears it so time shouldn't matter
          }

          animationDrawable.setBounds(0, 0, rosImageView.getWidth(), rosImageView.getHeight());
          animationDrawable.setOneShot(true); //do not auto-restart the animation

          return animationDrawable;//message.getHeader().getFrameId();
      }
    });
  }
private ShapeDrawable addPointToShapeDrawablePath(float x, float y, android.graphics.Path path){
// add point to path
    path.lineTo(x,y);

    // make local copy of path and store in new ShapeDrawable
    android.graphics.Path currPath = new android.graphics.Path(path);

    ShapeDrawable shapeDrawable = new ShapeDrawable();
    shapeDrawable.getPaint().setColor(Color.RED);
    shapeDrawable.getPaint().setStyle(Paint.Style.STROKE);
    shapeDrawable.getPaint().setStrokeWidth(10);
    shapeDrawable.getPaint().setStrokeJoin(Paint.Join.ROUND);
    shapeDrawable.getPaint().setStrokeCap(Paint.Cap.ROUND);
    shapeDrawable.getPaint().setPathEffect(new CornerPathEffect(30));
    shapeDrawable.getPaint().setAntiAlias(true);          // set anti alias so it smooths
    shapeDrawable.setIntrinsicHeight(rosImageView.getHeight());
    shapeDrawable.setIntrinsicWidth(rosImageView.getWidth());
    shapeDrawable.setBounds(0, 0, rosImageView.getWidth(), rosImageView.getHeight());

    shapeDrawable.setShape(new PathShape(currPath,rosImageView.getWidth(),rosImageView.getHeight()));

    return shapeDrawable;
}



private double MM2PX(double x){ return x*MM2INCH*PPI_tablet; }
private double PX2MM(double x){return x/(PPI_tablet*MM2INCH);}

private double M2PX(double x){return (MM2PX(x)*1000.0);}
private double PX2M(double x){return PX2MM(x)/1000.0;}



    @Override
  protected void init(NodeMainExecutor nodeMainExecutor) {
    NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostAddress());
    // At this point, the user has already been prompted to either enter the URI
    // of a master to use or to start a master locally.
    nodeConfiguration.setMasterUri(getMasterUri());
    // The RosTextView is a NodeMain that must be executed in order to
    // start displaying incoming messages.
      Log.e(TAG, "Ready to execute");
    nodeMainExecutor.execute(rosImageView, nodeConfiguration.setNodeName("android_gingerbread/trajectory_listener"));

  }
}
