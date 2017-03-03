package bbc.unibo.it.smartmoblitybbc;

        import android.graphics.Color;
        import android.location.Location;
        import android.os.AsyncTask;
        import android.os.Bundle;
        import android.os.Handler;
        import android.os.Message;
        import android.support.design.widget.FloatingActionButton;
        import android.support.v4.app.FragmentTransaction;
        import android.support.v7.app.AppCompatActivity;
        import android.util.Log;
        import android.view.View;

        import com.google.android.gms.maps.CameraUpdateFactory;
        import com.google.android.gms.maps.GoogleMap;
        import com.google.android.gms.maps.OnMapReadyCallback;
        import com.google.android.gms.maps.SupportMapFragment;
        import com.google.android.gms.maps.model.BitmapDescriptorFactory;
        import com.google.android.gms.maps.model.LatLng;
        import com.google.android.gms.maps.model.MarkerOptions;
        import com.google.android.gms.maps.model.PolylineOptions;
        import com.rabbitmq.client.AMQP;
        import com.rabbitmq.client.Channel;
        import com.rabbitmq.client.Connection;
        import com.rabbitmq.client.ConnectionFactory;
        import com.rabbitmq.client.Consumer;
        import com.rabbitmq.client.DefaultConsumer;
        import com.rabbitmq.client.Envelope;

        import org.json.JSONException;

        import java.io.IOException;
        import java.util.ArrayList;
        import java.util.HashSet;
        import java.util.List;
        import java.util.concurrent.TimeoutException;

        import bbc.unibo.it.smartmoblitybbc.model.Coordinates;
        import bbc.unibo.it.smartmoblitybbc.model.InfrastructureNodeImpl;
        import bbc.unibo.it.smartmoblitybbc.model.NodePath;
        import bbc.unibo.it.smartmoblitybbc.model.Pair;
        import bbc.unibo.it.smartmoblitybbc.model.interfaces.ICoordinates;
        import bbc.unibo.it.smartmoblitybbc.model.interfaces.IInfrastructureNode;
        import bbc.unibo.it.smartmoblitybbc.model.interfaces.IInfrastructureNodeImpl;
        import bbc.unibo.it.smartmoblitybbc.model.interfaces.INodePath;
        import bbc.unibo.it.smartmoblitybbc.model.interfaces.IPair;
        import bbc.unibo.it.smartmoblitybbc.model.interfaces.msg.ICongestionAlarmMsg;
        import bbc.unibo.it.smartmoblitybbc.model.interfaces.msg.IPathAckMsg;
        import bbc.unibo.it.smartmoblitybbc.model.interfaces.msg.IRequestPathMsg;
        import bbc.unibo.it.smartmoblitybbc.model.interfaces.msg.IRequestTravelTimeMsg;
        import bbc.unibo.it.smartmoblitybbc.model.interfaces.msg.IResponsePathMsg;
        import bbc.unibo.it.smartmoblitybbc.model.interfaces.msg.IResponseTravelTimeMsg;
        import bbc.unibo.it.smartmoblitybbc.model.interfaces.msg.ITravelTimeAckMsg;
        import bbc.unibo.it.smartmoblitybbc.model.msg.PathAckMsg;
        import bbc.unibo.it.smartmoblitybbc.model.msg.RequestPathMsg;
        import bbc.unibo.it.smartmoblitybbc.model.msg.RequestTravelTimeMsg;
        import bbc.unibo.it.smartmoblitybbc.model.msg.TravelTimeAckMsg;
        import bbc.unibo.it.smartmoblitybbc.utils.http.HttpUtils;
        import bbc.unibo.it.smartmoblitybbc.utils.json.JSONMessagingUtils;
        import bbc.unibo.it.smartmoblitybbc.utils.messaging.MessagingUtils;
        import bbc.unibo.it.smartmoblitybbc.utils.mom.MomUtils;

public class MainActivity extends AppCompatActivity implements
        GoogleMap.OnMapClickListener,
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
    private int pointsCounter = 0;
    private static final float colorsArray[] = {BitmapDescriptorFactory.HUE_AZURE, BitmapDescriptorFactory.HUE_GREEN,
            BitmapDescriptorFactory.HUE_MAGENTA, BitmapDescriptorFactory.HUE_YELLOW, BitmapDescriptorFactory.HUE_ORANGE};
    private static final double LAT_CENTER = 43.773663;
    private static final double LONG_CENTER = -79.401956;
    private GoogleMap mMap;
    private FloatingActionButton buttonStart;
    private boolean channelInitialized;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        SupportMapFragment mapFragment = new SupportMapFragment();
        transaction.add(R.id.frameMap, mapFragment);
        this.chosenPath = new NodePath(new ArrayList<IInfrastructureNode>());
        this.pathsWithTravelID = new ArrayList<>();
        this.travelTimes = new ArrayList<>();
        this.currentIndex = 0;
        this.userID = "newuser";
        this.channelInitialized = false;
        transaction.commit();
        mapFragment.getMapAsync(this);
    }

    /** Called when the map is ready. */
    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(LAT_CENTER,LONG_CENTER) , 14.0f) );
        mMap.setOnMapClickListener(this);
        FloatingActionButton buttonCancel = (FloatingActionButton) findViewById(R.id.resetButton);
        buttonCancel.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                mMap.clear();
                                                pointsCounter = 0;
                                                buttonStart.setVisibility(View.GONE);
                                            }
                                        }

        );
        buttonStart = (FloatingActionButton) findViewById(R.id.startButton);
        buttonStart.setOnClickListener(new View.OnClickListener() {
                                           @Override
                                           public void onClick(View v) {
                                               requestPaths(start, end);
                                           }
                                       }

        );
    }

    private Channel initChannel() throws IOException, TimeoutException {
        this.factory = new ConnectionFactory();
        this.factory.setHost(brokerAddress);
        Connection connection = this.factory.newConnection();
        Channel channel = connection.createChannel();
        channel.queueDeclare(this.userID, false, false, false, null);
        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");
        return channel;
    }

    private void requestPaths(final IInfrastructureNode start, final IInfrastructureNode end) {
        this.pathsWithTravelID.clear();
        this.travelTimes.clear();
        this.mMap.clear();
        new AsyncTask<Void,Void,Void>(){

            @Override
            protected Void doInBackground(Void... voids) {
                IRequestPathMsg requestMsg = new RequestPathMsg(MessagingUtils.REQUEST_PATH, start, end, userID);
                try {
                    String requestPathString = JSONMessagingUtils.getStringfromRequestPathMsg(requestMsg);
                    handleResponsePathMsg(HttpUtils.POST(requestPathString));
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (TimeoutException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                int j = 0;
                for(IPair<INodePath,Integer> pair : pathsWithTravelID){
                    drawMarkersForPath(pair.getFirst(), colorsArray[j%5]);
                    j++;
                }
            }
        }.execute();
    }

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
        this.timerValue = System.currentTimeMillis();
        this.travelID = minTravelID;
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
            channel.basicConsume(this.userID, true, consumer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void switchArrivedMsg(String msg) throws IOException, TimeoutException {
        try {
            int n = MessagingUtils.getIntId(msg);
            Log.i("> Id "+n, msg);
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
        //mMap.clear();
        //drawMarkersForPath(this.chosenPath, colorsArray[4]);
        /*GpsMock gps = new GpsMock(this.chosenPath, new ArrayList<>());  //TODO: we have to find a way to create a mock path with mock times
        gps.attachObserver(this);
        gps.start();*/
    }

    private void handleResponsePathMsg(String msg)
            throws JSONException, IOException, TimeoutException {
        System.out.println(msg);
        IResponsePathMsg message = JSONMessagingUtils.getResponsePathMsgFromString(msg);
        List<INodePath> paths;
        paths = message.getPaths();
        this.userID = message.getUserID();
        this.brokerAddress = message.getBrokerAddress();
        System.out.println(""+paths.size());
        for (int j = 0; j < paths.size(); j++) {
            this.pathsWithTravelID.add(new Pair<>(paths.get(j), j));
            Log.i("TRAVEL ID "+j, paths.get(j).getStringPath());
        }
        if(!this.channelInitialized){
            this.startReceiving();
            this.channelInitialized=true;
        }

        for (int i = 0; i < paths.size(); i++) {
            IRequestTravelTimeMsg requestMsg = new RequestTravelTimeMsg(userID, MessagingUtils.REQUEST_TRAVEL_TIME, 0,
                    paths.get(i), i, false);
            String toSend = JSONMessagingUtils.getStringfromRequestTravelTimeMsg(requestMsg);
            MomUtils.sendMsg(factory, paths.get(i).getPathNodes().get(0).getNodeID(), toSend);
        }
        /*List<IInfrastructureNode> path = new ArrayList<>();
        path.add(this.start);
        path.add(this.end);
        this.chosenPath.setPath(path);*/
    }

    private void handleResponseTravelTimeMsg(String msg) throws JSONException {
        IResponseTravelTimeMsg message = JSONMessagingUtils.getResponseTravelTimeMsgFromString(msg);
        this.travelID = message.getTravelID();
        int time = message.getTravelTime();
        if(message.frozenDanger()){
            System.out.println("Frozen Danger on path number "+message.getTravelID());
        }
        this.travelTimes.add(new Pair<Integer, Integer>(this.travelID, time));
        if(this.travelTimes.size()==this.pathsWithTravelID.size()){
            this.chosenPath = this.evaluateBestPath();
            this.requestCoordinates();
            try {
                    this.sendAckToNode();
            } catch (IOException | TimeoutException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendAckToNode() throws JSONException, IOException, TimeoutException{
        IPathAckMsg ackMsgToNode = new PathAckMsg(this.userID, MessagingUtils.PATH_ACK, this.chosenPath, this.travelID);
        String ackToSend = JSONMessagingUtils.getStringfromPathAckMsg(ackMsgToNode);
        MomUtils.sendMsg(this.factory, this.chosenPath.getPathNodes().get(0).getNodeID(), ackToSend);
    }

    private void drawMarkersForPath(INodePath path, float color){
        for(int i = 0; i < path.getPathNodes().size()-1; i++){
            IInfrastructureNode src = path.getPathNodes().get(i);
            IInfrastructureNode dest = path.getPathNodes().get(i+1);
            LatLng coord = new LatLng(src.getCoordinates().getLatitude(), src.getCoordinates().getLongitude());
            mMap.addMarker(new MarkerOptions()
                    .position(coord)
                    .icon(BitmapDescriptorFactory.defaultMarker(color)));
            mMap.addPolyline(new PolylineOptions()
                        .add(new LatLng(src.getCoordinates().getLatitude(), src.getCoordinates().getLongitude()), new LatLng(dest.getCoordinates().getLatitude(), dest.getCoordinates().getLongitude()))
                        .width(5)
                        .color(Color.BLUE).geodesic(true));
        }
        IInfrastructureNode in = path.getPathNodes().get(path.getPathNodes().size()-1);
        mMap.addMarker(new MarkerOptions()
                .position(new LatLng(in.getCoordinates().getLatitude(),in.getCoordinates().getLongitude()))
                .icon(BitmapDescriptorFactory.defaultMarker(color)));

    }

    @Override
    public void onMapClick(LatLng latLng) {
        if(pointsCounter==0){
            mMap.addMarker(new MarkerOptions()
                    .position(latLng));
            this.start = new InfrastructureNodeImpl("start", new HashSet<IInfrastructureNodeImpl>());
            this.start.setCoordinates(new Coordinates(latLng.latitude, latLng.longitude));
            pointsCounter = 1;
        } else if(pointsCounter==1){
            mMap.addMarker(new MarkerOptions()
                    .position(latLng));
            this.end = new InfrastructureNodeImpl("end", new HashSet<IInfrastructureNodeImpl>());
            this.end.setCoordinates(new Coordinates(latLng.latitude, latLng.longitude));
            pointsCounter = 2;
            this.buttonStart.setVisibility(View.VISIBLE);
        }
    }

    private void requestCoordinates(){
        new AsyncTask<Void,Void,Void>(){

            @Override
            protected Void doInBackground(Void... voids) {
                IPathAckMsg pathAckMsg = new PathAckMsg(userID, MessagingUtils.PATH_ACK, chosenPath, travelID);
                String pathAckString = "";
                try {

                    pathAckString = JSONMessagingUtils.getStringfromPathAckMsg(pathAckMsg);
                    handlePathAckMsg(HttpUtils.POST(pathAckString));
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                mMap.clear();
                drawMarkersForPath(chosenPath, colorsArray[4]);
            }
        }.execute();
    }


    public class GPSServiceHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            if(msg.what==0){
                Log.i("HANDLER", "GPS POS CHANGED");
                Location location = (Location) msg.obj;
                ICoordinates coordinates = new Coordinates(location.getLatitude(), location.getLongitude());
                if(chosenPath.getPathNodes().get(currentIndex+1).getCoordinates().isCloseEnough(coordinates)){
                    int time = (int) (System.currentTimeMillis()-timerValue);
                    timerValue = System.currentTimeMillis();
                    ICoordinates c = chosenPath.getPathNodes().get(currentIndex+1).getCoordinates();
                    LatLng latLng = new LatLng(c.getLatitude(), c.getLongitude());
                    mMap.addMarker(new MarkerOptions().position(latLng)
                            .icon(BitmapDescriptorFactory.defaultMarker(colorsArray[0])));
                    try {
                        nearNextNode(time);
                    } catch (JSONException | IOException | TimeoutException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void nearNextNode(int time) throws JSONException, IOException, TimeoutException{
        ITravelTimeAckMsg msg = new TravelTimeAckMsg(this.userID, MessagingUtils.TRAVEL_TIME_ACK,
                chosenPath.getPathNodes().get(this.currentIndex), this.chosenPath.getPathNodes().get(this.currentIndex + 1), time);
        String travelTimeAck = JSONMessagingUtils.getStringfromTravelTimeAckMsg(msg);
        MomUtils.sendMsg(this.factory, this.chosenPath.getPathNodes().get(this.currentIndex).getNodeID(), travelTimeAck);
        this.currentIndex++;
    }
}
