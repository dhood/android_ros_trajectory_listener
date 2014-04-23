/*
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

package org.ros.android.shape_learner;

import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Publisher;

import geometry_msgs.PointStamped;

/**
 * Publishes touch events.
 *
 * @author Deanna Hood
 */

class TouchPublisher extends AbstractNodeMain {
    private static final java.lang.String TAG = "RosTouchPublisher";
    private String topicName;
    private ConnectedNode connectedNode;
    private Publisher<PointStamped> publisher;

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("touch_publisher");
    }

    @Override
    public void onStart(final ConnectedNode connectedNode) {
         this.connectedNode = connectedNode;
        this.publisher =
                connectedNode.newPublisher(topicName, geometry_msgs.PointStamped._TYPE);

    }

  public void publishMessage(double x, double y) {

    geometry_msgs.PointStamped pointStamped = publisher.newMessage();
      pointStamped.getHeader().setStamp(connectedNode.getCurrentTime());
      pointStamped.getPoint().setX(x);
      pointStamped.getPoint().setY(y);

    publisher.publish(pointStamped);

  }
}