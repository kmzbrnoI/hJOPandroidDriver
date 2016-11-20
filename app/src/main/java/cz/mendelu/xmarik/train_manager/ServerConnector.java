 package cz.mendelu.xmarik.train_manager;

 import android.app.Activity;
 import android.app.Dialog;
 import android.content.Intent;
 import android.os.Bundle;
 import android.util.Log;
 import android.view.View;
 import android.widget.Button;
 import android.widget.CheckBox;
 import android.widget.EditText;
 import android.widget.ListView;
 import android.widget.Toast;

 import cz.mendelu.xmarik.train_manager.events.AreasEvent;
 import cz.mendelu.xmarik.train_manager.events.HandShakeEvent;

 import java.util.ArrayList;

 import org.greenrobot.eventbus.EventBus;


 public class ServerConnector extends Activity
 {
     private static String [] messges = {};//dodelat vsechno retezce co budou treba
     private static int i=0;
     private ListView mList;
     private ArrayList<String> arrayList;
     private MyCustomAdapter mAdapter;
     private TCPClient mTcpClient;
     private Button send;
     private ServerConnector classObject;
     String user = null;
     String passwd = null;
     boolean ok;

     private Server server;

     @Override
     public void onCreate(Bundle savedInstanceState)
     {
         classObject = this;
         EventBus.getDefault().register(this);



         super.onCreate(savedInstanceState);
         setContentView(R.layout.activity_server_connector);
         Bundle extras = getIntent().getExtras();
         messges = new String[3];
         if (extras != null) {
             String value = extras.getString("server");
             String [] tmp = value.split("\t");

             server = ServerList.getInstance().getServer(tmp[0]);
             Log.e("","server"+server.getDnsName());
             Log.e("services","ulozene jmeno a heslo: "+ server.getUserName() +"  "+server.getUserPassword());


         }else finish();


         messges[0] = "-;HELLO;1.0\n";


         if(server.getUserName()!=null && server.getUserPassword()!=null) {
             user = server.getUserName();
             passwd = server.getUserPassword();
             messges[1] = "-;LOK;G;AUTH;{" + user + "};" + passwd + "\n";
         }
         else {

             final Dialog dialog = new Dialog(this);
             dialog.setContentView(R.layout.user_dialog);
             dialog.setTitle("Title...");

             //set dialog component
             final EditText mName   = (EditText)dialog.findViewById(R.id.dialogName);
             final EditText mPasswd   = (EditText)dialog.findViewById(R.id.dialogPasswd);
             Button dialogButton = (Button) dialog.findViewById(R.id.dialogButtonOK);
             final CheckBox save = (CheckBox) dialog.findViewById(R.id.dialogSaveData);

             mName.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                 @Override
                 public void onFocusChange(View view, boolean b) {
                     if(mName.isFocused()) mName.setText("");
                 }
             });
             mPasswd.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                 @Override
                 public void onFocusChange(View view, boolean b) {
                     if(mPasswd.isFocused()) mPasswd.setText("");
                 }
             });

             // if button is clicked, close the custom dialog
             dialogButton.setOnClickListener(new View.OnClickListener() {
                 @Override
                 public void onClick(View v) {

                     user = mName.getText().toString();
                     passwd = HelpServices.hashPasswd(mPasswd.getText().toString());
                     if(save.isChecked())
                     {
                         setData(user, passwd,true);
                     }else setData(user, passwd, false);
                     messges[1] = "-;LOK;G;AUTH;{" + user + "};" + passwd + "\n";

                     dialog.dismiss();
                 }
             });

             dialog.show();

         }

         messges[2] = "-;OR-LIST;" + "\n";
         arrayList = new ArrayList<String>();

         send = (Button)findViewById(R.id.send_button);

         //relate the listView from java to the one created in xml
         mList = (ListView)findViewById(R.id.list);
         mAdapter = new MyCustomAdapter(this, arrayList);
         mList.setAdapter(mAdapter);

         //new connectTask().execute("");
         TCPClientApplication tcp = TCPClientApplication.startNewServer();
         tcp.server = server;
         tcp.auth = true;
         tcp.start();

         send.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View view) {

                 initialize();

                 sendNext();
             }

         });


     }

    public void setData(String name, String tmpPass, boolean save)
    {
        name = name.replaceAll("\n","");
        tmpPass = tmpPass.replaceAll("\n","");
        name = name.replaceAll("\\{","");
        tmpPass = tmpPass.replaceAll("\\{","");
        name = name.replaceAll("\\}","");
        tmpPass = tmpPass.replaceAll("\\}","");

        this.user = name;
        this.passwd = tmpPass;

        if(save)
        {
            this.server.setUserName(name);
            this.server.setUserPassword(tmpPass);
            ServerList.getInstance().setPassword(server);
        }


        Log.e("","user a heslo:"+name+" \n"+ tmpPass);
    }
     @Override
     protected void onStop() {
         super.onStop();
         EventBus.getDefault().unregister(this);
         // Unbind from service
         /*if (mTcpClient!=null) {
             mTcpClient.stopClient();
         }*/
     }

     public String getMessage()
     {
        if(messges.length>i)
        {   String tmp = messges[i];
            if(i==0)
            {
                arrayList.add("Navazuji komunikaci");
            }else if(i==1){
                arrayList.add("Autorizuji");
            }else arrayList.add("Získávám oblasti řízení");
            i++;
            return tmp;
        }

         return null;
     }

     @Override
     public void onPause(){
         super.onPause();

         EventBus.getDefault().unregister(this);
     }

     @Override
     public void onResume(){
         super.onResume();


         if(!EventBus.getDefault().isRegistered(this))EventBus.getDefault().register(this);
     }



    /* public class connectTask extends AsyncTask<String,String,TCPClient> {




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
             },server.ipAdr,server.port);
             String answer = mTcpClient.run();

             if(answer != null && answer.equals("server is unreachable"))
             {
                 raiseErrorState("client error, please check your connection");
             }

             return null;
         }

         @Override
         protected void onProgressUpdate(String... values) {
             super.onProgressUpdate(values);

             String serverMessage = values[0];
             //parsovat odpoved od serveru je ve values 0
             //TODO tady tohle smazat
             if(serverMessage.startsWith("-;OR-LIST;")){
                 addControlAreas(serverMessage.substring("-;OR-LIST;".length()));
                 arrayList.add("Data načteny, aktivace serveru dokončena");
                 Intent returnIntent = new Intent();
                 returnIntent.putExtra("result","Data načteny, aktivace serveru dokončena");
                 setResult(RESULT_OK, returnIntent);

                 Log.d("services","tady ta prvni blbost no");

                 server.setTcpClient(mTcpClient);
                 TCPClientApplication.getInstance().setClient(mTcpClient);
                 server.setActive(true);
                 ServerList.getInstance().setActive(server);
                 finish();
             }
            if(serverMessage.startsWith("-;MOD-CAS")||serverMessage.startsWith("-;DCC")){
                Log.e("","");
            }else
            if(i==1)
            {
               if(serverMessage.startsWith("-;HELLO;"))
               {
                   arrayList.add("komunikace navázána");
                   sendNext();
                   if(!serverMessage.substring("-;HELLO;".length()).equals("1.0"))
                   {
                       arrayList.add("Server využívá jiný komunikační protokol než 1.0, v komunikaci může docházet k chybám");

                   }

               }else raiseErrorState("handshake selhal");
            }else if(i==2)
            {
                Log.e("reload", "auth message'" + serverMessage+ "'");
                if(serverMessage.startsWith("-;LOK;G;AUTH;ok;"))
                {
                    arrayList.add("Strojvůdce autorizován");
                    sendNext();

                }else raiseErrorState("autorizace se nezdarila");
            }else if (i==3)
            {
                if(serverMessage.startsWith("-;OR-LIST;"))
                {

                    addControlAreas(serverMessage.substring("-;OR-LIST;".length()));
                    arrayList.add("Data načteny, aktivace serveru dokončena");
                    Intent returnIntent = new Intent();
                    returnIntent.putExtra("result","Data načteny, aktivace serveru dokončena");
                    setResult(RESULT_OK, returnIntent);

                    Log.d("services","načítám data v klasice");

                    server.setTcpClient(mTcpClient);
                    TCPClientApplication.getInstance().setClient(mTcpClient);
                    server.setActive(true);
                    ServerList.getInstance().setActive(server);
                    finish();

                }else raiseErrorState("nepodařilo se načíst oblasti řízení");
            }


             mAdapter.notifyDataSetChanged();
         }


         private void raiseErrorState(final String error)
         {
             runOnUiThread(new Runnable() {
                 @Override
                 public void run() {
                     i=0;
                     send.setClickable(true);
                     send.setText("opakovat");
                     new connectTask().execute("");
                     arrayList.add(error);
                     mAdapter.notifyDataSetChanged();
                 }
             });

         }



         private void addControlAreas(String data)
         {

             String [] tmp = data.split("\\[*\\]");

             for (String s : tmp)
             {
                 s = s.replace("[","");
                 s = s.replace("]","");
                 s = s.replace("];","");

                 String[] area = s.split(",");
                 if (area.length > 1)
                     Log.e("","area : "+area.toString());
                     server.addArea(new ControlArea(area[0],area[1]));
             }

         }
     }*/
     private void sendNext()
     {
         String message = getMessage();

         Log.e("","odeslano:"+message);

         //sends the message to the server
         if (TCPClientApplication.getInstance().getClient() != null) {
             TCPClientApplication.getInstance().getClient().sendMessage(message);
         }

         //refresh the list
         mAdapter.notifyDataSetChanged();;
     }

     public void changeUserData(View view)
     {
         final Dialog dialog = new Dialog(this);
         dialog.setContentView(R.layout.user_dialog);
         dialog.setTitle("Title...");

         //set dialog component
         final EditText mName   = (EditText)dialog.findViewById(R.id.dialogName);
         final EditText mPasswd   = (EditText)dialog.findViewById(R.id.dialogPasswd);
         Button dialogButton = (Button) dialog.findViewById(R.id.dialogButtonOK);
         final CheckBox savebox = (CheckBox) dialog.findViewById(R.id.dialogSaveData);

         mName.setOnFocusChangeListener(new View.OnFocusChangeListener() {
             @Override
             public void onFocusChange(View view, boolean b) {
                 if(mName.isFocused()) mName.setText("");
             }
         });
         mPasswd.setOnFocusChangeListener(new View.OnFocusChangeListener() {
             @Override
             public void onFocusChange(View view, boolean b) {
                 if(mPasswd.isFocused()) mPasswd.setText("");
             }
         });

         // if button is clicked, close the custom dialog
         dialogButton.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View v) {

                 user = mName.getText().toString();
                 passwd = HelpServices.hashPasswd(mPasswd.getText().toString());
                 setData(user, passwd,savebox.isChecked());
                 messges[1] = "-;LOK;G;AUTH;{" + user + "};" + passwd + "\n";
                 initialize();
                 dialog.dismiss();
             }
         });

         dialog.show();
     }

     private void initialize()
     {
         send.setClickable(false);
         send.setText("autorizuji");
         arrayList.clear();
         arrayList.add("připojuji k serveru");
         mAdapter.notifyDataSetChanged();

         sendNext();
     }

    // @Subscribe
     public void onEvent(AreasEvent event){
         // your implementation
         addControlAreas(event.getMessage().substring("-;OR-LIST;".length()));


             arrayList.add("Data načteny, aktivace serveru dokončena");
             Intent returnIntent = new Intent();


//TODO dodelat nejakou chybu
             server.setTcpClient(TCPClientApplication.getInstance().getClient());
             server.setActive(true);
             ServerList.getInstance().setActive(server);
             TCPClientApplication.getInstance().auth = false;

             if(ok){
                 Toast.makeText(getApplicationContext(),
                         "Server authorized and connected", Toast.LENGTH_LONG)
                         .show();
                 Intent intent = new Intent(this, Trains_box.class);
                 startActivity(intent);
             }else{
                 returnIntent.putExtra("result","authorization failed");
                 setResult(RESULT_CANCELED, returnIntent);
                 finish();
             }


     }

     public void onEvent(HandShakeEvent event){
         // your implementation

             if(event.getMessage().startsWith("-;HELLO;"))
             {
                 arrayList.add("komunikace navázána");
                 sendNext();
                 if(!event.getMessage().substring("-;HELLO;".length()).equals("1.0"))
                 {
                     arrayList.add("Server využívá jiný komunikační protokol než 1.0, v komunikaci může docházet k chybám");

                 }
             }else if(event.getMessage().startsWith("-;LOK;G;AUTH;ok;"))
             {
                 ok=true;
                 arrayList.add("Strojvůdce autorizován");
                 sendNext();

             }else if(event.getMessage().startsWith("-;LOK;G;AUTH;"))
             {
                 ok=false;
                 raiseErrorState("autorizace se nezdarila");
             }else raiseErrorState("handshake selhal");

     }

     private void addControlAreas(String data)
     {

         String [] tmp = data.split("\\[*\\]");
         server.areas = new ArrayList<ControlArea>();

         for (String s : tmp)
         {
             s = s.replace("[","");
             s = s.replace("]","");
             s = s.replace("];","");

             String[] area = s.split(",");
             if (area.length > 1)
                 Log.e("","area : "+area.toString());
             server.addArea(new ControlArea(area[0],area[1]));
         }

     }

     private void raiseErrorState(final String error)
     {
         runOnUiThread(new Runnable() {
             @Override
             public void run() {
                 i=0;
                 send.setClickable(true);
                 send.setText("opakovat");
                 arrayList.add(error);
                 mAdapter.notifyDataSetChanged();
             }
         });

     }

 }