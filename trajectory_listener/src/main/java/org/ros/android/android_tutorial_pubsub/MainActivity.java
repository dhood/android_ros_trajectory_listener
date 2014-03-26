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
    import org.ros.node.NodeConfiguration;
    import org.ros.node.NodeMainExecutor;

    import java.util.List;

    import geometry_msgs.Vector3;
    import trajectory_msgs.MultiDOFJointTrajectory;
    import trajectory_msgs.MultiDOFJointTrajectoryPoint;
    /**
     * @author damonkohler@google.com (Damon Kohler). modified by Deanna Hood.
     */
    public class MainActivity extends RosActivity {
      private static final java.lang.String TAG = "trajectoryListener";

      //private RosTextView<MultiDOFJointTrajectory> rosTextView;
      private RosImageView<MultiDOFJointTrajectory> rosImageView;
      //private Talker talker;

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

              Path prevPath = new Path(); //example code says 'final' http://stackoverflow.com/questions/9993030/bezier-curve-and-canvas
              prevPath.moveTo((float)points.get(0).getTransforms().get(0).getTranslation().getX()*5000,(float)points.get(0).getTransforms().get(0).getTranslation().getY()*5000);
              long dt_nsecs = points.get(0).getTimeFromStart().totalNsecs();

              int dt_msecs = (int) Long.rotateRight(dt_nsecs/1000,Integer.SIZE); //lose the nanosecond precision

              for(MultiDOFJointTrajectoryPoint p : points)
              {
                  Path currPath = new Path(prevPath);
                Vector3 tx = p.getTransforms().get(0).getTranslation();
                currPath.lineTo((float)tx.getX()*5000,(float)tx.getY()*5000);
                  PathShape pathShape = new PathShape(currPath,rosImageView.getWidth(),rosImageView.getHeight());

                  ShapeDrawable shapeDrawable = new ShapeDrawable();
                  shapeDrawable.getPaint().setColor(Color.RED);
                  shapeDrawable.getPaint().setStyle(Paint.Style.STROKE);
                  shapeDrawable.getPaint().setStrokeWidth(10);
                  shapeDrawable.setIntrinsicHeight(rosImageView.getHeight());
                  shapeDrawable.setIntrinsicWidth(rosImageView.getWidth());
                  shapeDrawable.setBounds(0, 0, rosImageView.getWidth(), rosImageView.getHeight());
                  shapeDrawable.setShape(pathShape);
                  animationDrawable.addFrame(shapeDrawable,dt_msecs);



                prevPath = currPath;

              }
/*
                // use ovals as trial animation
              ShapeDrawable mDrawable = new ShapeDrawable(new OvalShape());
              mDrawable.getPaint().setColor(0xff74AC23);
              mDrawable.setBounds(0, 0, 500, 500);
              mDrawable.setIntrinsicHeight(rosImageView.getHeight());
              mDrawable.setIntrinsicWidth(rosImageView.getWidth());

              //animationDrawable.addFrame(shapeDrawable.getConstantState().newDrawable(),100);
              animationDrawable.addFrame(mDrawable,1000);

              ShapeDrawable nDrawable = new ShapeDrawable(new OvalShape());
              nDrawable.getPaint().setColor(Color.RED);
              nDrawable.setBounds(0, 0, 500, 500);
              nDrawable.setIntrinsicHeight(rosImageView.getHeight());
              nDrawable.setIntrinsicWidth(rosImageView.getWidth());
              animationDrawable.addFrame(nDrawable,1000);
*/


              animationDrawable.setBounds(0, 0, rosImageView.getWidth(), rosImageView.getHeight());
              animationDrawable.setOneShot(true); //do not auto-restart the animation

              return animationDrawable;//message.getHeader().getFrameId();
          }
        });
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
