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
    import android.graphics.Paint;
    import android.graphics.Path;
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

    import java.util.Iterator;
    import java.util.List;

    import geometry_msgs.Vector3;
    import trajectory_msgs.MultiDOFJointTrajectory;
    import trajectory_msgs.MultiDOFJointTrajectoryPoint;
    /**
     * @author damonkohler@google.com (Damon Kohler). modified by Deanna Hood.
     */
    public class MainActivity extends RosActivity {
      private static final java.lang.String TAG = "trajectoryListener";
        private int timeoutDuration_mSecs = 1000; //time in ms to leave the trajectory displayed before removing it (negative displays indefinitely)

      private RosImageView<MultiDOFJointTrajectory> rosImageView;

      public MainActivity() {
        // The RosActivity constructor configures the notification title and ticker
        // messages.
        super("Trajectory listener", "Trajectory listener");
          Paint paint = new Paint() {
              {
                  setStyle(Paint.Style.STROKE);
                  setStrokeCap(Paint.Cap.ROUND);
                  setStrokeWidth(3.0f);
                  setAntiAlias(true);
              }
          };
      }

      @SuppressWarnings("unchecked")
      @Override
      public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        rosImageView = (RosImageView<MultiDOFJointTrajectory>) findViewById(R.id.image);
        rosImageView.setTopicName("write_traj");

        rosImageView.setMessageType(MultiDOFJointTrajectory._TYPE);

        rosImageView.setMessageToDrawableCallable(new MessageCallable<Drawable, MultiDOFJointTrajectory>() {
          @Override
          public Drawable call(MultiDOFJointTrajectory message) {
              Log.e(TAG, "got a message");
              List<MultiDOFJointTrajectoryPoint> points = message.getPoints();

              AnimationDrawable animationDrawable = new AnimationDrawable();

              Path trajPath = new Path(); //example code says 'final' http://stackoverflow.com/questions/9993030/bezier-curve-and-canvas
              trajPath.moveTo((float)points.get(0).getTransforms().get(0).getTranslation().getX()*5000,(float)points.get(0).getTransforms().get(0).getTranslation().getY()*5000);

              for(int i = 0; i < points.size()-1; i++) //special case for first and last point/frame of trajectory
              {
                  //add new trajectory point onto path and create ShapeDrawable to pass as a frame for animation
                  MultiDOFJointTrajectoryPoint p = points.get(i);
                  Vector3 tx = p.getTransforms().get(0).getTranslation();
                  ShapeDrawable shapeDrawable = addPointToShapeDrawablePath((float) tx.getX() * 5000, (float) tx.getY() * 5000, trajPath);

                  //determine the duration of the frame for the animation
                  Duration frameDuration = points.get(i + 1).getTimeFromStart().subtract(p.getTimeFromStart()); // take difference between times to get appropriate duration for frame to be displayed

                  long dt_msecs = frameDuration.totalNsecs()/1000000;
                  animationDrawable.addFrame(shapeDrawable,(int)dt_msecs); //unless the duration is over 2mil seconds the cast is ok

              }
              //cover end case
              MultiDOFJointTrajectoryPoint p = points.get(points.size()-1);
              Vector3 tx = p.getTransforms().get(0).getTranslation();
              ShapeDrawable shapeDrawable = addPointToShapeDrawablePath((float) tx.getX() * 5000, (float) tx.getY() * 5000, trajPath);

              if(timeoutDuration_mSecs >= 0)//only display the last frame until timeoutDuration has elapsed
              {
                animationDrawable.addFrame(shapeDrawable, timeoutDuration_mSecs);
                animationDrawable.addFrame(new ShapeDrawable(new PathShape(new Path(),0,0)),0); //stop displaying
              } else { //display last frame indefinitely
                  animationDrawable.addFrame(shapeDrawable, 1000 ); //think it will be left there until something clears it so time shouldn't matter
              }

              animationDrawable.setBounds(0, 0, rosImageView.getWidth(), rosImageView.getHeight());
              animationDrawable.setOneShot(true); //do not auto-restart the animation

              return animationDrawable;//message.getHeader().getFrameId();
          }
        });
      }
private ShapeDrawable addPointToShapeDrawablePath(float x, float y, Path path){
    // add point to path
    path.lineTo(x,y);

    // make local copy of path and store in new ShapeDrawable
    Path currPath = new Path(path);

    ShapeDrawable shapeDrawable = new ShapeDrawable();
    shapeDrawable.getPaint().setColor(Color.RED);
    shapeDrawable.getPaint().setStyle(Paint.Style.STROKE);
    shapeDrawable.getPaint().setStrokeWidth(10);
    shapeDrawable.setIntrinsicHeight(rosImageView.getHeight());
    shapeDrawable.setIntrinsicWidth(rosImageView.getWidth());
    shapeDrawable.setBounds(0, 0, rosImageView.getWidth(), rosImageView.getHeight());

    shapeDrawable.setShape(new PathShape(currPath,rosImageView.getWidth(),rosImageView.getHeight()));

    return shapeDrawable;
}
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
