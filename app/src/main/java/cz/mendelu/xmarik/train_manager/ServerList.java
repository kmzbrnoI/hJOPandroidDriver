package cz.mendelu.xmarik.train_manager;

import android.os.AsyncTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by ja on 15. 6. 2016.
 */
public class ServerList {

    private static ServerList instance;
    private ArrayList<Server> servers;
    private ArrayList<Server> storedServers;
    private TCPClient mTcpClient;

    public void setPassword(Server server) {
        if(storedServers.contains(server))
        {
            for(Server s : storedServers)
            {
                if(s.equals(server))
                {
                    s.setUserPassword(server.getUserPassword());
                    s.setUserName(server.getUserName());
                }
            }
        }else storedServers.add(server);
    }

    public String getSaveString() {
        String saveString = "";

        for(Server s : this.storedServers)
        {
            saveString =  saveString+s.getSaveDataString()+"|";
        }

        return saveString;
    }

    public void loadServers(String servers) {
        String[] serverString = servers.split("\\|");

        for(String tmpS : serverString)
        {
            String[] attributes = tmpS.split(";");
            if(tmpS.length()>5){

            Server tmpServer = new Server(attributes[0],attributes[1],attributes[2],attributes[3],
                    attributes[4],attributes[5]);
            if(!storedServers.contains(servers))storedServers.add(tmpServer);
            }

        }
    }

    public void clearCustomServer() {
        Server tmp = this.getActiveServer();
        this.storedServers.clear();
        if(tmp != null) this.storedServers.add(tmp);
    }

    public static enum TRAINTYPE {
                                    PARNI, DIESEL, MOTOROVÁ, ELEKTRICKÁ
                                    }
    public static String [] FUNCTION = {"F0", "F1", "F2", "F3",
            "F4", "F5", "F6", "F7",
            "F8", "F9", "F10", "F11",
            "F12", "F13", "F14", "F15",
            "F16", "F17", "F18", "F19",
            "F20", "F21", "F22", "F23",
            "F24", "F25", "F26", "F27",};

    protected ServerList()
    {

        this.servers=new ArrayList<Server>();
        this.storedServers=new ArrayList<Server>();

        //just fot testing
        this.servers.add(new Server("test1","127.1.2.2",1448,false, ""));
        this.storedServers.add(new Server("test2","192.168.2.101",4444,true, ""));
        this.storedServers.add(new Server("test3","192.168.0.114",4444,true, ""));
        this.storedServers.get(0).setActive(true);
    }

    public void addServer(Server server)
    {
        this.servers.add(server);
    }

    public void addServer(ArrayList<Server> servers)
    {

        this.servers.addAll(servers);

    }

    public void clear()
    {

        Set<Server> hs = new HashSet<>();
        hs.addAll(servers);
        servers.clear();
        servers.addAll(hs);
    }

    public void addCustomServer(Server server)
    {
        this.storedServers.add(server);
        Collections.sort(storedServers, new CustomServerComparator());
    }

    public ArrayList<Server> getServers()
    {
        return this.servers;
    }

    public  ArrayList <String> getServersString()
    {
        ArrayList <String> tmp = new ArrayList <>();
        for(Server s : this.servers)
        {
            tmp.add(s.getStringData());
        }

        return tmp;
    }

    public   ArrayList <String> getStoredServersString()
    {
        ArrayList <String> tmp = new ArrayList <>();
        int i =0;
        for(Server s : this.storedServers)
        {
            tmp.add(s.getStringData());
        }

        return tmp;
    }

    public ArrayList<Server> getCustomServers()
    {
        return this.storedServers;
    }

    public static ServerList getInstance() {
        if(instance == null) {
            instance = new ServerList();
        }
        //nacist stored servery
        return instance;
    }

    public Server getActiveServer()
    {
        for (Server s : this.servers)
        {
            if(s.getActive()) return s;
        }

        for (Server s : this.storedServers)
        {
            if(s.getActive()) return s;
        }

        return null;
    }

    public void deleteAllData()
    {
        storedServers = null;
        servers = null;
    }

    public void deleteAllUserData()
    {
        for(Server s : this.storedServers)
        {
            s.setUserName(null);
            s.setUserPassword(null);
        }

        for(Server s : this.servers)
        {
            s.setUserName(null);
            s.setUserPassword(null);
        }
    }

   /* public boolean AuthorizeLocalServe(String itemValue) {
        //osetrit
        String serverName= itemValue.split("--")[1];
        for (Server s : servers) {

            if(s.name.equals(serverName))
            {

                // connect to the server
                new connectTask(s.ipAdr,s.port).execute("");

                Client c = new Client(s.port,s.ipAdr);
                //TCPClient c = new TCPClient(s.ipAdr,s.port);
                String answer = c.sendMessage(s.Autorizace(User.getUser().getName(), User.getUser().getPassword()));
                if (mTcpClient != null) {
                    mTcpClient.sendMessage(s.Autorizace(User.getUser().getName(), User.getUser().getPassword()));
                }

                deactivateAll();
                s.setActive(true);

                return true;
            }

        }
        return false;
    }

    public boolean AuthorizeFarServe(String itemValue) {
        //osetrit
        String serverName= itemValue.split("--")[1];
        for (Server s : storedServers) {

            if(s.name.equals(serverName))
            {

                Client c = new Client(s.port,s.ipAdr);
                String answer = c.sendMessage(s.Autorizace(User.getUser().getName(), User.getUser().getPassword()));

                //0TODO if bude authorizovan
                deactivateAll();
                s.setActive(true);

                return true;
            }

        }
        return false;
    }*/

    private void deactivateAll()
    {
        for (Server s : servers)
        {
            if(s.getActive())s.setActive(false);
        }
        for (Server s : this.storedServers)
        {
            if(s.getActive())s.setActive(false);
        }
    }



    public Server getServer(String itemValue) {


        for (Server s : this.storedServers)
        {
            if(s.name.equals(itemValue))return s;
        }
        for (Server s : servers)
        {
            if(s.name.equals(itemValue))return s;
        }

        return null;
    }

    public void removeServer(int position) {
        if(position<=storedServers.size())
            this.storedServers.remove(position);

    }

    public String getServerStoreString() {
        String serverStoreString="";
        for (Server s : this.storedServers)
        {
            serverStoreString = serverStoreString +"--"+s.getStringData();
        }

        return serverStoreString;
    }

    public void setActive(Server server) {
        for (Server s : this.servers){
            if((s.getActive())  ){
                if(!s.equals(server)){
                    s.setActive(false);
                }

            }
        }
        for (Server s : this.storedServers){
            if((s.getActive())  ){
                if(!s.equals(server)){
                    s.setActive(false);
                }

            }
        }
    }

    public class connectTask extends AsyncTask<String,String,TCPClient> {

        String ipAdr;
        int port;
        public connectTask(String ipAdr, int port) {
            this.ipAdr=ipAdr;
            this.port=port;
        }


        @Override
        protected TCPClient doInBackground(String... message) {

            //we create a TCPClient object and
            mTcpClient = new TCPClient(new TCPClient.OnMessageReceived() {
                @Override
                //here the messageReceived method is implemented
                public void messageReceived(String message) {
                    //this method calls the onProgressUpdate
                    publishProgress(message);
                }
            },ipAdr,port);
            mTcpClient.run();

            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);

            //in the arrayList we add the messaged received from server
            String serverAnswer = values[0];
            //parser odpovedi ze serveru
        }
    }
}
