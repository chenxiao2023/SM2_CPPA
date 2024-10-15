package com.example.socketlw;

import static com.example.socketlw.CertificateGenerator.certRecover;
import static com.example.socketlw.CertificateGenerator.makeCert;
import static com.example.socketlw.CertificateGenerator.readPublicKey;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Hex;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

//http请求用的
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.socketlw.MainUse.MessageHolder;
import com.example.socketlw.SM2Utils.SM2Util;
import com.example.socketlw.SM2Utils.User;
import com.example.socketlw.SM2Utils.Util;

import java.util.HashMap;
import java.util.Map;
import java.security.*;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private Handler handler;
    private LayoutInflater inflater;
    private View layout;
    private AlertDialog.Builder builder;
    private TextView titleview;
    private ListView showmsg;
    private TextView showres;
    private EditText sendmsgtext;
    private Button startserver;
    private Button continueserver;
    private Button sendmsgbt;
    private Button mapgettxid;
    private Button getpkcert;
    private Button InitialUser;
    private Button updatePk;
    private Button verifysign;

    private int StartPort;
    private boolean isContinue = true,isServer = false;
    private String message = "",userSendMsg = "",titletext = "";
    private String[] ContinueServerData = new String[2];// 0.ipv4 1.端口号
    private Long mID = 0L;
    private List<MessageInfor> datas = new ArrayList<MessageInfor>();
    private SimpleDateFormat simpleDateFormat;
    private MessageAdapte messageAdapte;
    private static Socket socket = null;//用于与服务端通信的Socket
    private static ServerSocket server;
    private static List<PrintWriter> allOut; //存放所有客户端的输出流的集合，用于广播
    //SM2参数
    private User userMyself;
    private User userOther;
    //从服务器接收的公钥。

    private byte[] sign  = new byte[0];
    private Boolean verifySign  = false;

    //对方消息中的的TxID
    private String TxIDres="";
    private String Timeres="";
    private String Msgres="";
    private byte[] Signres=new byte[0];

    //http参数

    //验证方取证书

    private  String urlinitialPK = "http://192.168.220.20:8080/InitialUser";
    private  String urlUpdatePK = "http://192.168.220.20:8080/updatePk";

    private  String urlUpdatePKtest = "http://192.168.220.20:8080/updatepktest";
    //签名方取TxID
    private  String urlTxID =  "http://192.168.220.20:8080/mapgettxid";
    //从TxID取公钥
    private  String urlTxID2PublicKey = "http://192.168.220.20:8080/getpkcert";
    private  String urlPk2cert = "http://192.168.220.20:8080/certpk";

    private String address1 = "0x4f4072fc87a0833ea924f364e8a2af3546f71279";

    private String address2 = "0xccdee8c8017f64c686fa39c42f883f363714e078";
    private String chain="560AF94CC1C8BB9AE6986502136B425D";
    // 生成证书
    private X509Certificate certificate;

    private static String[] PERMISSIONS_STORAGE = {
            //依次权限申请
            Manifest.permission.INTERNET,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };
    @Override//实现了一个 Android Activity 的初始化逻辑
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().hide();//隐藏标题栏
        applypermission();
        InitView();
        handler = new Handler(){//Handler 可以用来在不同线程之间发送和处理消息，更新 UI 等
            @Override
            public void handleMessage(@NonNull Message msg) {
                if(msg.what == 1){
                    titleview.setText(titletext);
                }else if(msg.what == 2){
                    titleview.setText("当前在线人数["+(allOut.size()+1)+"]");
                }
                super.handleMessage(msg);
            }
        };
        //SM2的初始化
        byte[][] key = SM2Util.generateKeyPair();
        userMyself=new User(key[0],key[1],0);
        userOther=new User();
        System.out.println("公钥为："+ Util.byte2HexStr(userMyself.getPublicKeySM2()));
        System.out.println("私钥为："+Util.byte2HexStr(userMyself.getPrivateKeySM2()));
//        postOne(urlUpdatePKtest);
//        String pkS="56D70FF6E674089C2641176D805FAC31977272BC83598B348DD25FA251965CCE570EB42A852BD60306E853E1BC9F249EE0362888BEC5C9D4762096AFB34829DE";
//        byte[] pk=Util.hexStr2Bytes(pkS);
//        new Update(pk,privateKeySM2,"560AF94CC1C8BB9AE6986502136B425D");
//        Update.updateAll(0);
           //公钥对应AAAE9
        // String encodecert="0x3082013d3081e5a00302010202060192867a79d7300a06082a811ccf55018375302b310f300d06035504030c0649737375657231183016060355040a0c0f4d79204f7267616e697a6174696f6e301e170d3234313031323135323334365a170d3235313031333135323334365a302c3110300e06035504030c075375626a65637431183016060355040a0c0f4d79204f7267616e697a6174696f6e304f300a06082a811ccf5501822d034100d59f38f85b3f03c48f0b1a5f98a8c12d50f6d8b1a4194f27ad3c78710c76ca60ae597ebdb31e16934be2bc6d87d9207ff4688ff31310d97c8cb80d684dd2e05f300a06082a811ccf55018375034700304402206e67164ff9d08f53ac5d19518e5b16e5a779751d9168440014fd5a16b1a5ed6f02207e7e90614df3aac8e9d006761884dfae8f02d5ff5ab23982189e7705ac8e0c93";
            //
       // String encodecert=   "0x3082013e3081e5a003020102020601928a0968f4300a06082a811ccf55018375302b310f300d06035504030c0649737375657231183016060355040a0c0f4d79204f7267616e697a6174696f6e301e170d3234313031333037353834355a170d3235313031343037353834355a302c3110300e06035504030c075375626a65637431183016060355040a0c0f4d79204f7267616e697a6174696f6e304f300a06082a811ccf5501822d034100e9229eb61c759a5b3484d7741a9352559f6066f25c0ca818e093b325fe2cb8b295a97883a8d047a427a0211e83e2934454bb1dbe12fd3072810e4179eaf6ac9f300a06082a811ccf550183750348003045022100ca892445bfb3b81075534aa85c3f6b65925219260b449003be1be146b371ce4c02206baea51db96e0cb4b7773e1a86ebd04a416ee308dc2da9fb5b98994b0219991f";
         //String encodecert="0x3082013d3081e5a003020102020601928033abe2300a06082a811ccf55018375302b310f300d06035504030c0649737375657231183016060355040a0c0f4d79204f7267616e697a6174696f6e301e170d3234313031313130303834325a170d3235313031323130303834325a302c3110300e06035504030c075375626a65637431183016060355040a0c0f4d79204f7267616e697a6174696f6e304f300a06082a811ccf5501822d034100a5b4792c75cdf7d4df2ca1370f15faa2ff8adeb1c7cbba7ce94ff98ec8f788c9186c3438482d0d027a00a77a78326639649df283a872dab3dee8c5c3877d5099300a06082a811ccf550183750347003044022038ad66a734221094f7bdd3b34efa00d05c29c3b2a2b37d79271bf428fd5575cd02200bd17e6cec99a02900b314c1df9b922626fe54a2b56509864bd3b713de94a2be";
           // String encodecert=makeCert("FC5B2396034B0C1807EED779B7D20F8C97E22CE4E6BA18156458BBCF76172AB9D0074745EB713CDDCB5C21A95A79631EE626F8F2266EF7BC9D8DF2B8C652D530");
          //  System.out.println("输出的证书为："+encodecert);
//        try {
//            CertificateGenerator.verifyCert(encodecert);
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }


        //更新本地公钥
       /*
        System.out.println("更新后的公钥："+Util.byte2HexStr(Update.publicKeySM2));
        System.out.println("更新后的私钥："+Util.byte2HexStr(Update.privateKeySM2));*/
    }

    /**
     * 初始化控件
     */
    private void InitView() {
        titleview = (TextView) findViewById(R.id.titleview);
        showmsg = (ListView) findViewById(R.id.showmsg);
        showres = (TextView) findViewById(R.id.showres);
        showres.setMovementMethod(new ScrollingMovementMethod());
        sendmsgtext = (EditText) findViewById(R.id.sendmsgtext);
        startserver = (Button) findViewById(R.id.startserver);
        continueserver = (Button) findViewById(R.id.continueserver);
        sendmsgbt = (Button) findViewById(R.id.sendmsgbt);
        mapgettxid = (Button) findViewById(R.id.mapgettxid);
        getpkcert = (Button) findViewById(R.id.getpkcert);
        verifysign = (Button) findViewById(R.id.verifysign);
        InitialUser = (Button) findViewById(R.id.InitialUser);
        updatePk = (Button) findViewById(R.id.updatePk);

        simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        messageAdapte = new MessageAdapte();
        showmsg.setAdapter(messageAdapte);

        startserver.setOnClickListener(this);
        continueserver.setOnClickListener(this);
        sendmsgbt.setOnClickListener(this);
        mapgettxid.setOnClickListener(this);
        getpkcert.setOnClickListener(this);
        verifysign.setOnClickListener(this);
        updatePk.setOnClickListener(this);
        InitialUser.setOnClickListener(this);
       // sendimg.setOnClickListener(this);

    }
    //定义判断权限申请的函数，在onCreat中调用就行
    public void applypermission(){
        if(Build.VERSION.SDK_INT>=23){
            boolean needapply=false;
            for(int i=0;i<PERMISSIONS_STORAGE.length;i++){
                int chechpermission= ContextCompat.checkSelfPermission(getApplicationContext(),
                        PERMISSIONS_STORAGE[i]);
                if(chechpermission!= PackageManager.PERMISSION_GRANTED){
                    needapply=true;
                }
            }
            if(needapply){
                ActivityCompat.requestPermissions(MainActivity.this,PERMISSIONS_STORAGE,1);
            }
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.startserver:
                //加载布局
                inflater = LayoutInflater.from(this);
                layout = inflater.inflate(R.layout.start_server,null);
                //通过对 AlertDialog.Builder 对象调用 setView()
                builder =  new AlertDialog.Builder(MainActivity.this);
                builder.setView(R.layout.start_server);
                builder.setCancelable(false);//是否为可取消
                //加载控件
                EditText editprot = (EditText) layout.findViewById(R.id.editprot);

                new AlertDialog.Builder(MainActivity.this)
                        .setView(layout)  //设置显示内容
                        .setPositiveButton("开启", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                StartPort = Integer.valueOf(editprot.getText().toString());
                                mID = System.currentTimeMillis();
                                ServerInit();
                            }
                        })
                        .setNegativeButton("取消", null)
                        .setCancelable(false)  //按回退键不可取消该对话框
                        .show();
                break;
            case R.id.continueserver:
                //加载布局
                inflater = LayoutInflater.from(this);
                layout = inflater.inflate(R.layout.continue_server,null);
                //通过对 AlertDialog.Builder 对象调用 setView()
                builder =  new AlertDialog.Builder(MainActivity.this);
                builder.setView(R.layout.continue_server);
                builder.setCancelable(false);//是否为可取消
                //加载控件
                EditText editipv4text = (EditText) layout.findViewById(R.id.editipv4text);
                EditText editprottext = (EditText) layout.findViewById(R.id.editprottext);

                new AlertDialog.Builder(MainActivity.this)
                        .setView(layout)  //设置显示内容
                        .setPositiveButton("连接", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ContinueServerData[0] = editipv4text.getText().toString();
                                ContinueServerData[1] = editprottext.getText().toString();
                                ContinueSever();
                            }
                        })
                        .setNegativeButton("取消", null)
                        .setCancelable(false)  //按回退键不可取消该对话框
                        .show();

                break;
            case R.id.sendmsgbt://发送消息
                if(isServer)//服务器
                {
                    message = sendmsgtext.getText().toString();
                    if(message==null||"".equals(message)){
                        Toast.makeText(MainActivity.this,"发送消息不能为空",Toast.LENGTH_LONG).show();
                        return ;
                    }
                    if(userMyself.getTxID()==null||"".equals(userMyself.getTxID())){
                        Toast.makeText(MainActivity.this,"请先获取TxID",Toast.LENGTH_LONG).show();
                        return ;
                    }
                    long Ltimes = System.currentTimeMillis();
                    sign=SM2Util.sign(userMyself.getPrivateKeySM2(),(message+Ltimes+userMyself.getTxID()).getBytes() );
                    Log.d("发送消息时","签名为："+ Util.byte2HexStr(sign));
                    message = sendmsgtext.getText().toString();
                    String base64PublicKey = Base64.encodeToString(userMyself.getPublicKeySM2(), Base64.NO_WRAP);
                    String base64Signature = Base64.encodeToString(sign, Base64.NO_WRAP);
                    datas.add(new MessageInfor(message,Ltimes,mID,sign,userMyself.getPublicKeySM2(),userMyself.getTxID(),"1"));
                    sendMessage("{\"isimg\":\"1\",\"msg\":\""+message+"\",\"times\":\""+Ltimes+"\",\"id\":\""+mID+"\",\"base64Signature\":\""+base64Signature+"\",\"base64PublicKey\":\""+ base64PublicKey +"\",\"TxID\":\""+userMyself.getTxID()+"\",\"peoplen\":\""+"当前在线人数["+(allOut.size()+1)+"]"+"\"}");
                    sendmsgtext.setText("");
                    showres.setText("发送过去的：签名："+Util.byte2HexStr(sign)+",公钥："+Util.byte2HexStr(userMyself.getPublicKeySM2())+",标识："+userMyself.getTxID());
                }else {//客户端
                    sendMsgText();
                    showres.setText("发送过去的：签名："+Util.byte2HexStr(sign)+",公钥："+Util.byte2HexStr(userMyself.getPublicKeySM2())+",标识："+userMyself.getTxID());
                }
                break;
            case R.id.mapgettxid://发送Address获取TxID
                postOne(urlTxID);
                break;
            case R.id.getpkcert://发送TxID，获取公钥
                // String pk="FC5B2396034B0C1807EED779B7D20F8C97E22CE4E6BA18156458BBCF76172AB9D0074745EB713CDDCB5C21A95A79631EE626F8F2266EF7BC9D8DF2B8C652D530";
                // publicKeySM2F=Util.hexStr2Bytes(pk);
                // showres.setText(Util.byte2HexStr(publicKeySM2F));
                postOne(urlTxID2PublicKey);
                break;
            case R.id.verifysign://发送TxID，获取公钥

                verifySign=SM2Util.verifySign(userOther.getPublicKeySM2Cert(), (Msgres+Timeres+TxIDres).getBytes(), Signres);//正式的
                //verifySign=SM2Util.verifySign(publicKeySM2Res, (Msgres+Timeres+TxIDres).getBytes(), Signres);
                if(verifySign){
                    System.out.println("签名验证成功");
                    showres.setText("签名通过");
                    showres.setTextColor(Color.GREEN);
                }else{
                    System.out.println("签名验证失败");
                    showres.setText("签名不通过");
                    showres.setTextColor(Color.RED);
                }
                break;
            case R.id.updatePk://更新公钥
                postOne(urlUpdatePK);
                break;
            case R.id.InitialUser://发送初始公钥
                postOne(urlinitialPK);
                break;
            default:

        }
    }
    /**
     * 服务器端
     * @param out
     */
    //将给定的输出流放入集合
    private synchronized void addOut(PrintWriter out){
        allOut.add(out);
    }
    //将给定的输出流移出集合
    private synchronized void removeOut(PrintWriter out){
        allOut.remove(out);
    }
    //将给定的消息发给客户端
    private void sendMessage(String message) {
        Thread sendmsg = new Thread(new Runnable() {
            @Override
            public void run() {
                for(PrintWriter out:allOut) {
                    out.println(message);
                }
            }
        });
        sendmsg.start();
    }
    //服务器初始化
    public void ServerInit() {
        try {
            server = new ServerSocket(StartPort);
            allOut = new ArrayList<PrintWriter>();
            isServer = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        // 无限循环监听客户端连接，所以才要整一个新线程来防止阻碍主线程
                        while(true) {
                            Socket socket1 = null;
                            try {
                                socket1 = server.accept();
                            } catch (IOException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                            // 当有客户端连接时，创建一个新的ClientHandler实例来处理该客户端
                            ClientHandler hander = new ClientHandler(socket1);

                            // 为每个客户端启动一个新线程来执行ClientHandler
                            Thread t = new Thread(hander);
                            t.start();
                        }
                    }
                }).start();
    }

    //该线程类是与指定的客户端进行交互工作
    class ClientHandler implements Runnable{
        //当前线程客户端的Socket
        private Socket socket;

        //该客户端的地址
        private String host;

        public ClientHandler(Socket socket) {
            this.socket=socket;
            InetAddress address = socket.getInetAddress();
            //获取ip地址
            host = address.getHostAddress();
        }

        @Override
        public void run() {
            PrintWriter pw = null;
            try {
                //有用户加入
                sendMessage("["+host+"]加入聊天!");

                OutputStream out = socket.getOutputStream();
                OutputStreamWriter osw = new OutputStreamWriter(out,"UTF-8");
                pw = new PrintWriter(osw,true);

                //将该客户的输出流存入共享集合，以便消息可以广播给该客户端
                addOut(pw);

                handler.sendEmptyMessage(2);

                //处理来自客户端的数据
                InputStream in = socket.getInputStream();
                InputStreamReader isr = new InputStreamReader(in,"utf-8");
                BufferedReader br = new BufferedReader(isr);

                String message = null;
                while((message = br.readLine())!=null) {

                    try {//接收到客户端发送的消息后，使用 JSONObject 解析消息。
                        JSONObject json = new  JSONObject(message);
                        String base64PublicKey = json.getString("base64PublicKey"); // 获取 Base64 编码的字符串
                        String base64Signature = json.getString("base64Signature"); // 获取 Base64 编码的字符串
                        datas.add(new MessageInfor(json.getString("msg"),Long.valueOf(json.getString("times")),Long.valueOf(json.getString("id")),Base64.decode(base64Signature, Base64.NO_WRAP),Base64.decode(base64PublicKey, Base64.NO_WRAP),String.valueOf(json.getString("TxID")),"1"));
                        //titletext = json.getString("peoplen");
                        handler.sendEmptyMessage(1);
                        showres.setText("接收的消息：签名："+Util.byte2HexStr(Base64.decode(base64Signature, Base64.NO_WRAP))+",公钥："+Util.byte2HexStr(Base64.decode(base64PublicKey, Base64.NO_WRAP))+",标识："+json.getString("TxID"));
                        TxIDres=json.getString("TxID");
                        Timeres=json.getString("times");
                        Msgres=json.getString("msg");
                        Signres=Base64.decode(base64Signature, Base64.NO_WRAP);

                        //messageAdapte.notifyDataSetChanged();//通知数据源发生变化
                    }catch (JSONException e){
                        e.printStackTrace();
                    }

                    //将解析后的消息广播给所有客户端
                    sendMessage(message);
                }

            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }finally {
                //将该客户端的输出流从共享集合中删除，以避免后续广播给已断开的客户端
                removeOut(pw);

                //有用户退出
                sendMessage("["+host+"]退出聊天!");

                handler.sendEmptyMessage(2);

                try {
                    socket.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }
    /**
     * 客户端
     * @return
     */
    public boolean ContinueSever(){
        Thread continuethread = new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        try {
                            //localhost 127.0.0.1
                            socket = new Socket(ContinueServerData[0],Integer.valueOf(ContinueServerData[1]));
                            mID = System.currentTimeMillis();
                        } catch (Exception e) {
                            isContinue = false;
                            isServer = false;
                            e.printStackTrace();
                        }
                    }
                }
        );
        continuethread.start();
        while(isContinue){
            if(socket != null){
                break;
            }
        }
        if(isContinue) {
            new Thread(
                    new Runnable() {
                        @Override
                        public void run() {
                            /*
                             * 客户端开始工作的方法
                             */
                            try {
                                //启动用于读取服务端发送消息的线程
                                ServerHandler handler = new ServerHandler();
                                //ServerHandler是自己写的类，实现Runnable接口,有多线程功能
                                Thread t = new Thread(handler);
                                t.start();

                                //将数据发送到服务端
                                OutputStream out = socket.getOutputStream();//获取输出流对象
                                OutputStreamWriter osw = new OutputStreamWriter(out,"utf-8");//转化成utf-8格式
                                PrintWriter pw = new PrintWriter(osw,true);
                                while(true) {
                                    if(userSendMsg != "" && userSendMsg!=null){
                                        pw.println(userSendMsg);//把信息输出到服务端
                                        userSendMsg = "";
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                        }
                    }).start();
        }
        return isContinue;
    }

    class ServerHandler implements Runnable{
        /**
         * 读取服务端发送过来的消息
         */
        @Override
        public void run() {
            try {
                InputStream in = socket.getInputStream();//输入流
                InputStreamReader isr = new InputStreamReader(in,"UTF-8");//以utf-8读
                BufferedReader br = new BufferedReader(isr);
                String message1=br.readLine();
                while(message1!=null) {
                  //  Log.i("测试4",message1);
                    try {
                        JSONObject json = new  JSONObject(message1);
                        String base64PublicKey = json.getString("base64PublicKey"); // 获取 Base64 编码的字符串
                        String base64Signature = json.getString("base64Signature"); // 获取 Base64 编码的字符串
                        if(json.getLong("id") != mID){
                                datas.add(new MessageInfor(json.getString("msg"),Long.valueOf(json.getString("times")),Long.valueOf(json.getString("id")),Base64.decode(base64Signature, Base64.NO_WRAP),Base64.decode(base64PublicKey, Base64.NO_WRAP),String.valueOf(json.getString("TxID")),"1"));
                        }
                      //  titletext = json.getString("peoplen");
                        TxIDres=json.getString("TxID");
                        Timeres=json.getString("times");
                        Msgres=json.getString("msg");
                        Signres=Base64.decode(base64Signature, Base64.NO_WRAP);
                        handler.sendEmptyMessage(1);
                        showres.setText("接收的消息：签名："+Util.byte2HexStr(Base64.decode(base64Signature, Base64.NO_WRAP))+",公钥："+Util.byte2HexStr(Base64.decode(base64PublicKey, Base64.NO_WRAP))+",标识："+json.getString("TxID"));
                     //   messageAdapte.notifyDataSetChanged();//通知数据源发生变化
                    }catch (JSONException e){
                        e.printStackTrace();
                    }
                    message1=br.readLine();

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    /**
     * 发送消息
     */
    private void sendMsgText(){
        message = sendmsgtext.getText().toString();
        if(message==null||"".equals(message)){
            Toast.makeText(MainActivity.this,"发送消息不能为空",Toast.LENGTH_LONG).show();
            return ;
        }
        if(userMyself.getTxID()==null||"".equals(userMyself.getTxID())){
            Toast.makeText(MainActivity.this,"请先获取TxID",Toast.LENGTH_LONG).show();
            return ;
        }
        long Ltimes = System.currentTimeMillis();
        sign=SM2Util.sign(userMyself.getPrivateKeySM2(),(message+Ltimes+userMyself.getTxID()).getBytes() );
       //=SM2Util.verifySign(userMyself.getPublicKeySM2(), (message+Ltimes+userMyself.getTxID()).getBytes(), sign);
        Log.d("客户端发消息","sign="+ Util.byte2HexStr(sign));
       // Log.d("客户端发消息","verifySign="+verifySign);
       // signature=sign.toString();
        MessageInfor m = new MessageInfor(message,Ltimes,mID,sign,userMyself.getPublicKeySM2(),userMyself.getTxID(),"1");//消息 时间戳 id
        String base64PublicKey = Base64.encodeToString(userMyself.getPublicKeySM2(), Base64.NO_WRAP);
        String base64Signature = Base64.encodeToString(sign, Base64.NO_WRAP);
        userSendMsg = "{\"isimg\":\"1\",\"msg\":\""+sendmsgtext.getText().toString()+"\",\"times\":\""+Ltimes+"\",\"base64Signature\":\""+base64Signature+"\",\"base64PublicKey\":\""+ base64PublicKey +"\",\"TxID\":\""+userMyself.getTxID()+"\",\"id\":\""+mID+"\"}";
        datas.add(m);
        messageAdapte.notifyDataSetChanged();//通知数据源发生变化
        sendmsgtext.setText("");

    }

//传1个参数
private void postOne(String url) {
    RequestQueue queue = Volley.newRequestQueue(this);
    StringRequest jsonRequest = new StringRequest(Request.Method.POST, url,
            new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    Toast.makeText(MainActivity.this, "请求成功: response = " + response, Toast.LENGTH_SHORT).show();
                    Log.d("测试PostOne", "url = " + url);
                    Log.d("测试PostOne", "response =" + response);
                    switch (url){
                        case "http://192.168.220.20:8080/mapgettxid":
                            userMyself.setTxID(response);
                            showres.setText(response);
                            break;
                        case "http://192.168.220.20:8080/getpkcert":
                            //publicKeySM2F=Util.hexStr2Bytes(response);
                            try {
                                userOther.setPublicKeySM2Cert(Util.hexStr2Bytes(CertificateGenerator.verifyCert(response)));
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                            showres.setText(response);
                            break;
                        case "http://192.168.220.20:8080/InitialUser":

                            showres.setText(response);
                            System.out.println("初始化公钥为："+Util.byte2HexStr(userMyself.getPublicKeySM2()));
                            break;
                        case "http://192.168.220.20:8080/updatePk":

                            showres.setText(response);

                            break;
                        case "http://192.168.220.20:8080/certpk":

                            showres.setText(response);
                            try {
                                String pkInCert=CertificateGenerator.verifyCert(response);
                                System.out.println("pkInCert="+pkInCert);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                            break;
                        case "http://192.168.220.20:8080/updatepktest":

                            showres.setText(response);
                            System.out.println("收到服务器的公钥为："+response);
                            break;
                        default:
                            System.out.println("Url错误！！");

                    }

                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    if (error.networkResponse != null) {
                        Log.e("TAG", "Response code: " + error.networkResponse.statusCode);
                        Log.e("TAG", "Response data: " + new String(error.networkResponse.data));
                    } else {
                        Log.e("TAG", "Network response is null");
                    }
                    String errorMessage = error.getMessage();
                    if (errorMessage == null) {
                        Log.e("TAG", "Error message is null");
                    } else {
                        Log.e("TAG", "Error message: " + errorMessage);
                    }

                }
            }) {
        @Override
        public byte[] getBody() {
            JSONObject jsonObject = new JSONObject();
           // jsonObject=JsonPut.PutJson(jsonObject,"address","0x4f4072fc87a0833ea924f364e8a2af3546f71279");
            switch (url){
                case "http://192.168.220.20:8080/mapgettxid":
                    jsonObject=JsonPut.PutJson(jsonObject,"address",address2);
                    break;
                case "http://192.168.220.20:8080/getpkcert"://给txID返回证书
                   // jsonObject=JsonPut.PutJson(jsonObject,"txid",TxID);//测试用的，正常情况下是TxIDres
                    jsonObject=JsonPut.PutJson(jsonObject,"txid",TxIDres);
                    break;
                case "http://192.168.220.20:8080/InitialUser":
                    jsonObject=JsonPut.PutJson(jsonObject,"address",address2);
                    jsonObject=JsonPut.PutJson(jsonObject,"pkIndex",userMyself.getPkIndex());
                    jsonObject=JsonPut.PutJson(jsonObject,"key",Util.byte2HexStr(userMyself.getPublicKeySM2()));
                    jsonObject=JsonPut.PutJson(jsonObject,"chain",chain);
                    break;
                case "http://192.168.220.20:8080/updatePk":
                    jsonObject=JsonPut.PutJson(jsonObject,"address",address2);
                    jsonObject=JsonPut.PutJson(jsonObject,"pkIndex",userMyself.getPkIndex());
                    new Update(userMyself.getPublicKeySM2(),userMyself.getPrivateKeySM2(),chain);
                    Update.updateAll(userMyself.getPkIndex());
                    userMyself.setPublicKeySM2(Update.publicKeySM2);
                    userMyself.setPrivateKeySM2(Update.privateKeySM2);
                    chain=Update.chainS;
                    userMyself.setPkIndex(userMyself.getPkIndex()+1);
                    break;
                case "http://192.168.220.20:8080/certpk":
                    jsonObject=JsonPut.PutJson(jsonObject,"key",Util.byte2HexStr(userMyself.getPublicKeySM2()));
                    break;
                case "http://192.168.220.20:8080/updatepktest":
                    jsonObject=JsonPut.PutJson(jsonObject,"address",address2);
                    jsonObject=JsonPut.PutJson(jsonObject,"pkIndex",0);
                    jsonObject=JsonPut.PutJson(jsonObject,"key","56D70FF6E674089C2641176D805FAC31977272BC83598B348DD25FA251965CCE570EB42A852BD60306E853E1BC9F249EE0362888BEC5C9D4762096AFB34829DE");
                    jsonObject=JsonPut.PutJson(jsonObject,"chain","560AF94CC1C8BB9AE6986502136B425D");
                    break;
                default:
                    System.out.println("Url错误！！");
                    return new byte[0]; // 返回空字节数组
            }
            // 返回 JSON 格式的请求体
          //  jsonObject=JsonPut.PutJson(jsonObject,"address","0x4f4072fc87a0833ea924f364e8a2af3546f71279");
            System.out.println(jsonObject.toString());
            return jsonObject.toString().getBytes();

        }
        @Override
        public String getBodyContentType() {
            return "application/json; charset=utf-8";
        }
    };
    // Set a custom retry policy
    int socketTimeout = 10000; // 10 seconds
    RetryPolicy policy = new DefaultRetryPolicy(socketTimeout, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
    jsonRequest.setRetryPolicy(policy);


    queue.add(jsonRequest);
}

//消息适应
    class MessageAdapte extends BaseAdapter {

        @Override
        public int getCount() {
            return datas.size();
        }

        @Override
        public MessageInfor getItem(int i) {
            return datas.get(i);
        }

        @Override
        public long getItemId(int i) {
            Long id = datas.get(i).getUserID();
            return id==null?0:id;
        }
        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            MessageHolder holder = null;
            if(view == null){
                view = LayoutInflater.from(MainActivity.this).inflate(R.layout.chart_item,null);
                holder = new MessageHolder();
                holder.left = (TextView) view.findViewById(R.id.itemleft);
                holder.right = (TextView) view.findViewById(R.id.itemright);

                holder.leftsign = (TextView) view.findViewById(R.id.itemsignleft);
                holder.rightsign = (TextView) view.findViewById(R.id.itemsignright);
                holder.lefttime = (TextView) view.findViewById(R.id.itemtimeleft);
                holder.righttime = (TextView) view.findViewById(R.id.itemtimeright);
                holder.rightimgtime = (TextView) view.findViewById(R.id.rightimgtime);
                holder.leftimgtime = (TextView) view.findViewById(R.id.leftimgtime);
                holder.rightimg = (ImageView) view.findViewById(R.id.rightimg);
                holder.leftimg = (ImageView) view.findViewById(R.id.leftimg);

                view.setTag(holder);
            }else {
                holder = (MessageHolder) view.getTag();
            }
            MessageInfor mi = getItem(i);
          /*  boolean isok=SM2Util.verifySign(mi.getPublicKey().getBytes(), (mi.getMsg()+mi.getTime()+mi.getTxID()).getBytes(), mi.getSignature().getBytes());
            Log.d("sss","isok="+isok);*/
            //verifySign=SM2Util.verifySign(mi.getPublicKey().getBytes(), (mi.getMsg()+mi.getTime().toString()+mi.getTxID()).getBytes(), mi.getSignature().getBytes());
            verifySign=SM2Util.verifySign(mi.getPublicKey(), (mi.getMsg()+mi.getTime()+mi.getTxID()).getBytes(), mi.getSignature());
            Log.d("测试6","verifySign="+verifySign);
            Log.d("测试6","公钥为："+ Util.byte2HexStr(mi.getPublicKey()));
            Log.d("测试6","数据为："+ mi.getMsg()+mi.getTime()+mi.getTxID());
            Log.d("测试6","签名为："+ Util.byte2HexStr(mi.getSignature()));
            //显示
            if (mi.getUserID() == mID){//id相等,自己发的
                if(mi.getType().equals("0")){//图片
                }else if(mi.getType().equals("1")){//消息
                    holder.leftimg.setVisibility(View.GONE);
                    holder.leftimgtime.setVisibility(View.GONE);
                    holder.rightimg.setVisibility(View.GONE);
                    holder.rightimgtime.setVisibility(View.GONE);

                    holder.left.setVisibility(View.GONE);
                    holder.lefttime.setVisibility(View.GONE);
                    holder.leftsign.setVisibility(View.GONE);
                    holder.right.setVisibility(View.VISIBLE);
                    holder.righttime.setVisibility(View.VISIBLE);
                    holder.rightsign.setVisibility(View.VISIBLE);
                    holder.right.setText(mi.getMsg());
                    holder.righttime.setText(simpleDateFormat.format(new Date(mi.getTime())));


                }


            }else {//对面发的
                if(mi.getType().equals("0")){//图片
                }else if(mi.getType().equals("1")){//消息
                    holder.leftimg.setVisibility(View.GONE);
                    holder.leftimgtime.setVisibility(View.GONE);
                    holder.rightimg.setVisibility(View.GONE);
                    holder.rightimgtime.setVisibility(View.GONE);

                    holder.left.setVisibility(View.VISIBLE);
                    holder.lefttime.setVisibility(View.VISIBLE);
                    holder.leftsign.setVisibility(View.VISIBLE);
                    holder.right.setVisibility(View.GONE);
                    holder.righttime.setVisibility(View.GONE);
                    holder.rightsign.setVisibility(View.GONE);
                    holder.left.setText(mi.getMsg());
                    holder.lefttime.setText(simpleDateFormat.format(new Date(mi.getTime())));
                   /* if(verifySign){
                        holder.leftsign.setText("签名通过"+",标识："+mi.getTxID());
                        holder.leftsign.setTextColor(Color.GREEN);
                    }else {
                        holder.leftsign.setText("签名不通过"+",标识："+mi.getTxID());
                        holder.leftsign.setTextColor(Color.RED);
                    }*/
                    //  holder.leftsign.setText("签名："+Arrays.toString(mi.getSignature())+",公钥："+Arrays.toString(mi.getPublicKey())+",标识："+mi.getTxID()+"签名通通通过了");

                    // holder.leftsign.setText("签名："+mi.getSignature()+",公钥："+mi.getPublicKey()+",标识："+mi.getTxID());
                }
            }
            return view;
        }
    }

    //获取公钥文件路径
    public String getpathPk() throws IOException {

            InputStream inputStream = getResources().openRawResource(R.raw.x);
            File outFile = new File(getFilesDir(), "pub"); // 确保使用正确的文件扩展名
            FileOutputStream outputStream = new FileOutputStream(outFile);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            outputStream.close();
            inputStream.close();
            String filePath = outFile.getAbsolutePath(); // 现在你可以获取文件路径
        return filePath;
    }

    //获取私钥文件路径
    public String getpathSk() throws IOException {

        InputStream inputStream = getResources().openRawResource(R.raw.x2);
        File outFile = new File(getFilesDir(), "pem"); // 确保使用正确的文件扩展名
        FileOutputStream outputStream = new FileOutputStream(outFile);
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, length);
        }
        outputStream.close();
        inputStream.close();
        String filePath = outFile.getAbsolutePath(); // 现在你可以获取文件路径
        return filePath;
    }
}
