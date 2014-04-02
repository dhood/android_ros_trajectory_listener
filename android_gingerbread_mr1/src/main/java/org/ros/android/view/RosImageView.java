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

package org.ros.android.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;
import org.ros.android.MessageCallable;
import org.ros.message.Duration;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.topic.Subscriber;

import nav_msgs.Path;

/**
 * Displays incoming messages with a bitmap or other Drawable.
 * 
 * @author ethan.rublee@gmail.com (Ethan Rublee)
 * @author damonkohler@google.com (Damon Kohler). Modified by Deanna Hood.
 */
public class RosImageView<T> extends ImageView implements NodeMain {
    private static final java.lang.String TAG = "RosImageView";
  private String topicName;
  private String messageType;
  private MessageCallable<Bitmap, T> bitmapCallable;
  private MessageCallable<Drawable, T> drawableCallable;
    private AnimationDrawable drawable;

    public RosImageView(Context context) {
    super(context);
  }

  public RosImageView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public RosImageView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }

  public void setTopicName(String topicName) {
    this.topicName = topicName;
  }

  public void setMessageType(String messageType) {
    this.messageType = messageType;
  }

  public void setMessageToBitmapCallable(MessageCallable<Bitmap, T> callable) {
    this.bitmapCallable = callable;
  }
  public void setMessageToDrawableCallable(MessageCallable<Drawable, T> callable) {
        this.drawableCallable = callable;
    }


    @Override
  public GraphName getDefaultNodeName() {
    return GraphName.of("ros_image_view");
  }

  @Override
  public void onStart(final ConnectedNode connectedNode) {
    Subscriber<T> subscriber = connectedNode.newSubscriber(topicName, messageType);
    subscriber.addMessageListener(new MessageListener<T>() {
      @Override
      public void onNewMessage(final T message) {
        if (bitmapCallable != null) {
            post(new Runnable() {
              @Override
              public void run() {
                setImageBitmap(bitmapCallable.call(message));
              }
            });
        } else if (drawableCallable != null) {
            post(new Runnable() {
                @Override
                public void run() {
                    Log.e(TAG, "got a message at " + connectedNode.getCurrentTime().toString());

                    drawable = (AnimationDrawable)drawableCallable.call(message);
                    setImageDrawable(drawable);
                    if(message instanceof nav_msgs.Path){ // this does not belong in this class
                        Duration delay = ((Path) message).getHeader().getStamp().subtract(connectedNode.getCurrentTime());
                        try{Thread.sleep(Math.round(delay.totalNsecs() / 1000000.0));}
                        catch(InterruptedException e){
                            Log.e(TAG, "InterruptedException: " + e.getMessage());
                        }
                        Log.e(TAG, "executing message at " + connectedNode.getCurrentTime().toString());
                    }
                    drawable.start();
                }
            });
        }
        postInvalidate();
      }
    });

  }

  @Override
  public void onShutdown(Node node) {
  }

  @Override
  public void onShutdownComplete(Node node) {
  }

  @Override
  public void onError(Node node, Throwable throwable) {
  }
}
