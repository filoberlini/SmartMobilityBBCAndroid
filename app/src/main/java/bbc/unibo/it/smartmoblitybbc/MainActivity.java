package bbc.unibo.it.smartmoblitybbc;

        import android.location.Location;
        import android.os.Bundle;
        import android.os.Handler;
        import android.os.Message;
        import android.support.v4.app.FragmentTransaction;
        import android.support.v7.app.AppCompatActivity;
        import android.util.Log;
        import android.widget.Toast;

        import com.google.android.gms.maps.GoogleMap;
        import com.google.android.gms.maps.OnMapReadyCallback;
        import com.google.android.gms.maps.SupportMapFragment;
        import com.google.android.gms.maps.model.LatLng;
        import com.google.android.gms.maps.model.Marker;
        import com.google.android.gms.maps.model.MarkerOptions;
        import com.rabbitmq.client.AMQP;
        import com.rabbitmq.client.Channel;
        import com.rabbitmq.client.Connection;
        import com.rabbitmq.client.ConnectionFactory;
        import com.rabbitmq.client.Consumer;
        import com.rabbitmq.client.DefaultConsumer;
        import com.rabbitmq.client.Envelope;

        import org.json.JSONException;

        import java.io.IOException;
        import java.io.UnsupportedEncodingException;
        import java.util.ArrayList;
        import java.util.List;
        import java.util.concurrent.TimeoutException;

        import bbc.unibo.it.smartmoblitybbc.model.Coordinates;
        import bbc.unibo.it.smartmoblitybbc.model.InfrastructureNodeImpl;
        import bbc.unibo.it.smartmoblitybbc.model.Pair;
        import bbc.unibo.it.smartmoblitybbc.model.interfaces.ICoordinates;
        import bbc.unibo.it.smartmoblitybbc.model.interfaces.IInfrastructureNode;
        import bbc.unibo.it.smartmoblitybbc.model.interfaces.INodePath;
        import bbc.unibo.it.smartmoblitybbc.model.interfaces.msg.ICongestionAlarmMsg;
        import bbc.unibo.it.smartmoblitybbc.model.interfaces.msg.IPathAckMsg;
        import bbc.unibo.it.smartmoblitybbc.model.interfaces.msg.IRequestPathMsg;
        import bbc.unibo.it.smartmoblitybbc.model.interfaces.msg.IRequestTravelTimeMsg;
        import bbc.unibo.it.smartmoblitybbc.model.interfaces.msg.IResponsePathMsg;
        import bbc.unibo.it.smartmoblitybbc.model.interfaces.msg.IResponseTravelTimeMsg;
        import bbc.unibo.it.smartmoblitybbc.model.interfaces.msg.ITravelTimeAckMsg;
        import bbc.unibo.it.smartmoblitybbc.model.msg.RequestTravelTimeMsg;
        import bbc.unibo.it.smartmoblitybbc.model.msg.TravelTimeAckMsg;
        import bbc.unibo.it.smartmoblitybbc.utils.json.JSONMessagingUtils;
        import bbc.unibo.it.smartmoblitybbc.utils.messaging.MessagingUtils;
        import bbc.unibo.it.smartmoblitybbc.utils.mom.MomUtils;

public class MainActivity extends AppCompatActivity implements
        GoogleMap.OnMarkerClickListener,
        OnMapReadyCallback {


    private String userID;
    private String brokerAddress;
    private ConnectionFactory factory;
    private Integer travelID;
    private List<Pair<INodePath, Integer>> pathsWithTravelID;
    private List<Pair<Integer, Integer>> travelTimes;
    private INodePath chosenPath;
    private InfrastructureNodeImpl start;
    private InfrastructureNodeImpl end;
    private int currentIndex;
    private long timerValue;







    private static final LatLng PERTH = new LatLng(-31.952854, 115.857342);
    private static final LatLng SYDNEY = new LatLng(-33.87365, 151.20689);
    private static final LatLng BRISBANE = new LatLng(-27.47093, 153.0235);

    private Marker mPerth;
    private Marker mSydney;
    private Marker mBrisbane;

    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        SupportMapFragment mapFragment = new SupportMapFragment();
        transaction.add(R.id.frameMap, mapFragment);
        transaction.commit();
        mapFragment.getMapAsync(this);
    }

    /** Called when the map is ready. */
    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;

        // Add some markers to the map, and add a data object to each marker.
        mPerth = mMap.addMarker(new MarkerOptions()
                .position(PERTH)
                .title("Perth"));
        mPerth.setTag(0);

        mSydney = mMap.addMarker(new MarkerOptions()
                .position(SYDNEY)
                .title("Sydney"));
        mSydney.setTag(0);

        mBrisbane = mMap.addMarker(new MarkerOptions()
                .position(BRISBANE)
                .title("Brisbane"));
        mBrisbane.setTag(0);

        // Set a listener for marker click.
        mMap.setOnMarkerClickListener(this);
    }

    /** Called when the user clicks a marker. */
    @Override
    public boolean onMarkerClick(final Marker marker) {

        // Retrieve the data from the marker.
        Integer clickCount = (Integer) marker.getTag();

        // Check if a click count was set, then display the click count.
        if (clickCount != null) {
            clickCount = clickCount + 1;
            marker.setTag(clickCount);

            Toast.makeText(this,
                    marker.getTitle() +
                            " has been clicked " + clickCount + " times.",
                    Toast.LENGTH_SHORT).show();
        }

        // Return false to indicate that we have not consumed the event and that we wish
        // for the default behavior to occur (which is for the camera to move such that the
        // marker is centered and for the marker's info window to open, if it has one).
        return false;
    }

    private Channel initChannel() throws IOException, TimeoutException {
        this.factory = new ConnectionFactory();
        this.factory.setHost(brokerAddress);
        Connection connection = this.factory.newConnection();
        Channel channel = connection.createChannel();
        channel.queueDeclare("receiveQueue", false, false, false, null);
        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");
        return channel;
    }

    /*private void requestPaths(IInfrastructureNode start, IInfrastructureNode end) {
        Vertx vertx = Vertx.vertx();
        HttpClient client = vertx.createHttpClient();
        client.websocket(8080, "localhost", "/some-uri", ws -> {
            ws.handler(data -> {
                System.out.println("Received data " + data.toString("ISO-8859-1"));
                try {
                    this.handleResponsePathMsg(data.toString());
                } catch (JSONException | IOException | TimeoutException e) {
                    e.printStackTrace();
                }
            });
            IRequestPathMsg requestMsg = new RequestPathMsg(MessagingUtils.REQUEST_PATH, this.start, this.end);
            try {
                String requestPathString = JSONMessagingUtils.getStringfromRequestPathMsg(requestMsg);
                ws.writeBinaryMessage(Buffer.buffer(requestPathString));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });
    }*/

    private INodePath evaluateBestPath() {
        int min = Integer.MAX_VALUE;
        int minTravelID = -1;
        INodePath bestPath = null;
        for (Pair<Integer, Integer> p : this.travelTimes) {
            if (p.getSecond() < min) {
                min = p.getSecond();
                minTravelID = p.getFirst();
            }
        }
        for (Pair<INodePath, Integer> p : this.pathsWithTravelID) {
            if (p.getSecond() == minTravelID) {
                bestPath = p.getFirst();
            }
        }
        return bestPath;
    }

    private void startReceiving() throws IOException, TimeoutException {
        Channel channel = null;
        try {
            channel = initChannel();
        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
        }
        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
                                       byte[] body) throws IOException {
                String message = new String(body, "UTF-8");
                try {
                    switchArrivedMsg(message);
                } catch (TimeoutException e) {
                    e.printStackTrace();
                }
                System.out.println(" [x] Received A '" + message + "'");
            }
        };

        try {
            channel.basicConsume("receiveQueue", true, consumer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void switchArrivedMsg(String msg) throws UnsupportedEncodingException, IOException, TimeoutException {
        try {
            int n = MessagingUtils.getIntId(msg);
            switch (n) {
                case 0:
                    handleCongestionAlarmMsg(msg);
                    break;
                case 1:
                    handlePathAckMsg(msg);
                    break;
                case 4:
                    handleResponsePathMsg(msg);
                    break;
                case 5:
                    handleResponseTravelTimeMsg(msg);
                    break;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void handleCongestionAlarmMsg(String msg) throws JSONException {
        ICongestionAlarmMsg message = JSONMessagingUtils.getCongestionAlarmMsgFromString(msg);
    }

    private void handlePathAckMsg(String msg) throws JSONException {
        IPathAckMsg message = JSONMessagingUtils.getPathAckWithCoordinatesMsgFromString(msg);
        this.chosenPath = message.getPath();
        /*GpsMock gps = new GpsMock(this.chosenPath, new ArrayList<>());  //TODO: we have to find a way to create a mock path with mock times
        gps.attachObserver(this);
        gps.start();*/
    }

    private void handleResponsePathMsg(String msg)
            throws JSONException, UnsupportedEncodingException, IOException, TimeoutException {
        System.out.println(msg);
        IResponsePathMsg message = JSONMessagingUtils.getResponsePathMsgFromString(msg);
        List<INodePath> paths;
        paths = message.getPaths();
        this.userID = message.getUserID();
        this.brokerAddress = message.getBrokerAddress();
        for (int j = 0; j < paths.size(); j++) {
            this.pathsWithTravelID.add(new Pair<INodePath, Integer>(paths.get(j), j));
        }
        for (int i = 0; i < paths.size(); i++) {
            IRequestTravelTimeMsg requestMsg = new RequestTravelTimeMsg(userID, MessagingUtils.REQUEST_TRAVEL_TIME, 0,
                    paths.get(i), i, false);
            String toSend = JSONMessagingUtils.getStringfromRequestTravelTimeMsg(requestMsg);
            MomUtils.sendMsg(factory, userID, toSend);
        }
        List<IInfrastructureNode> path = new ArrayList<>();
        path.add(this.start);
        path.add(this.end);
        this.chosenPath.setPath(path);
    }

    private void handleResponseTravelTimeMsg(String msg) throws JSONException {
        IResponseTravelTimeMsg message = JSONMessagingUtils.getResponseTravelTimeMsgFromString(msg);
        this.travelID = message.getTravelID();
        int time = message.getTravelTime();
        if(message.frozenDanger()){
            System.out.println("Frozen Danger on path number "+message.getTravelID());
        }
        this.travelTimes.add(new Pair<Integer, Integer>(this.travelID, time));
    }

    /*private void requestCoordinates(){
        Vertx vertx = Vertx.vertx();
        HttpClient client = vertx.createHttpClient();

        client.websocket(8080, "localhost", "/some-uri", ws -> {
            ws.handler(data -> {
                System.out.println("Received data " + data.toString("ISO-8859-1"));
                try {
                    this.handlePathAckMsg(data.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            });
            IPathAckMsg pathAckMsg = new PathAckMsg(this.userID, MessagingUtils.PATH_ACK, this.chosenPath, this.travelID);
            try {
                String pathAckString = JSONMessagingUtils.getStringfromPathAckMsg(pathAckMsg);
                ws.writeBinaryMessage(Buffer.buffer(pathAckString));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });
    }*/


    public class GPSServiceHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            if(msg.what==0){
                Log.i("HANDLER", "GPS POS CHANGED");
                Location location = (Location) msg.obj;
                ICoordinates coordinates = new Coordinates(location.getLatitude(), location.getLongitude());
                if(chosenPath.getPathNodes().get(currentIndex+1).getCoordinates().isCloseEnough(coordinates)){
                    int time = (int) (System.currentTimeMillis()-timerValue);
                    try {
                        nearNextNode(time);
                    } catch (JSONException | IOException | TimeoutException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void nearNextNode(int time) throws JSONException, UnsupportedEncodingException, IOException, TimeoutException{
        ITravelTimeAckMsg msg = new TravelTimeAckMsg(this.userID, MessagingUtils.TRAVEL_TIME_ACK,
                chosenPath.getPathNodes().get(this.currentIndex), this.chosenPath.getPathNodes().get(this.currentIndex + 1), time);
        String travelTimeAck = JSONMessagingUtils.getStringfromTravelTimeAckMsg(msg);
        MomUtils.sendMsg(this.factory, this.chosenPath.getPathNodes().get(this.currentIndex).getNodeID(), travelTimeAck);
    }
}
