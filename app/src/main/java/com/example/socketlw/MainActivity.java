package com.example.socketlw;

import static com.example.socketlw.SM2Utils.SM2Util.SM2_GenerateKeyPair;
import static com.example.socketlw.SM2Utils.SM2Util.SM2_Sign;
import static com.example.socketlw.SM2Utils.SM2Util.SM2_Verifysign;
import static com.example.socketlw.SM2Utils.Update.SM2_Getpkfromsk;
import static com.example.socketlw.SM2Utils.Update.SM2_PrivateKeyDerive;
import static com.example.socketlw.SM2Utils.Update.Sm3Hash;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
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

import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

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
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.socketlw.MainUse.JsonPut;
import com.example.socketlw.MainUse.MessageHolder;
import com.example.socketlw.MainUse.MessageInfor;
import com.example.socketlw.SM2Utils.CertificateGenerator;
import com.example.socketlw.SM2Utils.Update;
import com.example.socketlw.SM2Utils.Util;

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
    private Button updateSk;
    private Button verifysign;
    private Button showFile;

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
    private byte[] publicKeySM2 = new byte[0];
    private byte[] privateKeySM2 = new byte[0];
    private int keyIndex=0;
    private String chain="560AF94CC1C8BB9AE6986502136B425D";
    //从服务器接收的公钥。
    private byte[] publicKeySM2InCert= new byte[0];
    private byte[] sign  = new byte[0];
    private Boolean verifySign  = false;
    private String TxID="";
    //对方消息中的的TxID
    private String TxIDres="";
    private String Timeres="";
    private String Msgres="";
    private byte[] Signres=new byte[0];


    //时间测试参数
    private long startTime;
    private long endTime;

    //初始化用户
    private  String urlinitialPK = "http://192.168.220.20:8080/InitialUser";
    //签名方取TxID
    private  String urlTxID =  "http://192.168.220.20:8080/getTxID";
    //验证方从TxID取公钥
    private  String urlTxID2PublicKey = "http://192.168.220.20:8080/getPublickey";

    private String pkHash = "";

   private String address = "0xccdee8c8017f64c686fa39c42f883f363714e078";//地址1
    //private String address = "0x4f4072fc87a0833ea924f364e8a2af3546f71279";//地址2

    private static String[] PERMISSIONS_STORAGE = {
            //依次权限申请
            Manifest.permission.INTERNET,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
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

        //清空文件内容
        clearFileOnStartup();
    }

    /**
     * 初始化控件
     */
    private void InitView() {
        titleview = (TextView) findViewById(R.id.titleview);
        showmsg = (ListView) findViewById(R.id.showmsg);
        showres = (TextView) findViewById(R.id.showres);
        showres.setMovementMethod(new ScrollingMovementMethod());//设置为能划的
        sendmsgtext = (EditText) findViewById(R.id.sendmsgtext);
        startserver = (Button) findViewById(R.id.startserver);
        continueserver = (Button) findViewById(R.id.continueserver);
        sendmsgbt = (Button) findViewById(R.id.sendmsgbt);
        mapgettxid = (Button) findViewById(R.id.mapgettxid);
        getpkcert = (Button) findViewById(R.id.getpkcert);
        verifysign = (Button) findViewById(R.id.verifysign);
        InitialUser = (Button) findViewById(R.id.InitialUser);
        updateSk = (Button) findViewById(R.id.updateSk);
        showFile = (Button) findViewById(R.id.showfile);

        simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        messageAdapte = new MessageAdapte();
        showmsg.setAdapter(messageAdapte);

        startserver.setOnClickListener(this);
        continueserver.setOnClickListener(this);
        sendmsgbt.setOnClickListener(this);
        mapgettxid.setOnClickListener(this);
        getpkcert.setOnClickListener(this);
        verifysign.setOnClickListener(this);
        updateSk.setOnClickListener(this);
        InitialUser.setOnClickListener(this);
        showFile.setOnClickListener(this);

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
                    if(TxID==null||"".equals(TxID)){
                        Toast.makeText(MainActivity.this,"请先获取TxID",Toast.LENGTH_LONG).show();
                        return ;
                    }
                    long Ltimes = System.currentTimeMillis();
                    startTime = System.nanoTime();
                    sign=SM2_Sign(privateKeySM2,(message+Ltimes+TxID).getBytes() );
                    endTime =System.nanoTime();
                    double duration = (endTime - startTime) / 1_000_0000.0;

                    writeToInternalStorage("------------SM2_Sign-------------");
                    writeToInternalStorage("SM2_Sign duration:"+duration+"ms");
                    writeToInternalStorage("SM2_Signature="+Util.byte2HexStr(sign)+"\nSM2_Privatekey="+Util.byte2HexStr(privateKeySM2));
                    writeToInternalStorage("\n");

                    /*//函数时间测试
                    for(int i=0;i<12;i++){
                        long startTime = System.nanoTime();
                        sign=SM2_Sign(privateKeySM2,(message+Ltimes+TxID).getBytes() );
                        long endTime =  System.nanoTime();
                        duration = (endTime - startTime) / 1_000_000.0;

                        writeToInternalStorage("第"+i+"次签名耗时="+duration +"ms");
                    }*/
                    Log.d("发送消息时","签名为："+ Util.byte2HexStr(sign));
                    message = sendmsgtext.getText().toString();
                    String base64PublicKey = Base64.encodeToString(publicKeySM2, Base64.NO_WRAP);
                    String base64Signature = Base64.encodeToString(sign, Base64.NO_WRAP);
                    datas.add(new MessageInfor(message,Ltimes,mID,sign,publicKeySM2,TxID,"1"));
                    sendMessage("{\"isimg\":\"1\",\"msg\":\""+message+"\",\"times\":\""+Ltimes+"\",\"id\":\""+mID+"\",\"base64Signature\":\""+base64Signature+"\",\"base64PublicKey\":\""+ base64PublicKey +"\",\"TxID\":\""+TxID+"\",\"peoplen\":\""+"当前在线人数["+(allOut.size()+1)+"]"+"\"}");
                    sendmsgtext.setText("");
                    showres.setText("SM2_Sign duration:"+duration+"ms"+"\nSM2_Signature="+Util.byte2HexStr(sign)+"\nSM2_Privatekey="+Util.byte2HexStr(privateKeySM2));
                }else {//客户端
                    sendMsgText();
                }
                break;
            case R.id.mapgettxid://发送Address获取TxID
                postOne(urlTxID);
                break;
            case R.id.getpkcert://发送TxID，获取公钥
                postOne(urlTxID2PublicKey);
                break;
            case R.id.verifysign://发送TxID，获取公钥

                //判断是否超时
                long Ltimes = System.currentTimeMillis();
                long nowTime = Long.parseLong(Timeres);
                if((nowTime-Ltimes)/1000>60){
                    System.out.println("超时");
                    break;
                }

                startTime = System.nanoTime();
                verifySign=SM2_Verifysign(publicKeySM2InCert, (Msgres+Timeres+TxIDres).getBytes(), Signres);//正式的
                endTime =System.nanoTime();
                double duration = (endTime - startTime) / 1_000_0000.0;
                /*//函数时间测试
                for(int i=0;i<12;i++){
                long startTime = System.nanoTime();
                verifySign=SM2_Verifysign(publicKeySM2InCert, (Msgres+Timeres+TxIDres).getBytes(), Signres);
                long endTime =System.nanoTime();
                duration = (endTime - startTime) / 1_000_0000.0;
                writeToInternalStorage("第"+i+"次验证签名耗时="+duration +"ms");
            }*/
                if(verifySign){
                    System.out.println("签名验证成功");
                    showres.setText("SM2_VerifySign duration:"+duration+"ms"+"\nSM2_Signature="+Util.byte2HexStr(Signres)+"\nSM2_Publickey="+Util.byte2HexStr(publicKeySM2InCert)+"\n验证结果="+"签名通过");
                    writeToInternalStorage("----------SM2_VerifySign---------");
                    writeToInternalStorage("SM2_VerifySign duration:"+duration+"ms");
                    writeToInternalStorage("SM2_Signature="+Util.byte2HexStr(Signres)+"\nSM2_Publickey="+Util.byte2HexStr(publicKeySM2InCert)+"\n结果="+"签名通过");
                    writeToInternalStorage("\n");
                    //showres.setTextColor(Color.GREEN);
                }else{
                    System.out.println("签名验证失败");
                    showres.setText("SM2_VerifySign duration:"+duration+"ms"+"\nSM2_Signature="+Util.byte2HexStr(Signres)+"\nSM2_Publickey="+Util.byte2HexStr(publicKeySM2InCert)+"\n验证结果="+"签名不通过");
                    writeToInternalStorage("----------SM2_VerifySign---------");
                    writeToInternalStorage("SM2_VerifySign duration:"+duration+"ms");
                    writeToInternalStorage("SM2_Signature="+Util.byte2HexStr(Signres)+"\nSM2_Publickey="+Util.byte2HexStr(publicKeySM2InCert)+"\n结果="+"签名不通过");
                    writeToInternalStorage("\n");
                    // showres.setTextColor(Color.RED);
                }
                break;
            case R.id.updateSk://更新私钥
                //postOne(urlUpdatePK);
                new Update(publicKeySM2,privateKeySM2,chain);
                byte[] chainbyte=Util.hexStr2Bytes(chain);

                startTime = System.nanoTime();
                byte[][] skandchain=SM2_PrivateKeyDerive(privateKeySM2,keyIndex,chainbyte);
                endTime =System.nanoTime();
                duration = (endTime - startTime) / 1_000_0000.0;

                privateKeySM2=skandchain[0];
                System.out.println("newsk= "+Util.byte2HexStr(privateKeySM2));
                chain=Util.byte2HexStr(skandchain[1]);
                System.out.println("newchain="+chain);
                publicKeySM2=SM2_Getpkfromsk(privateKeySM2);
                pkHash=Util.byte2HexStr(Sm3Hash(publicKeySM2)).toLowerCase();
                System.out.println("newPk: "+Util.byte2HexStr(publicKeySM2));
                keyIndex =keyIndex+1;
                writeToInternalStorage("-------SM2_PrivateKeyDerive------");
                writeToInternalStorage("SM2_PrivateKeyDerive duration:"+duration+"ms");
                writeToInternalStorage("keyIndex="+keyIndex+"\nSM2_privateKey="+Util.byte2HexStr(privateKeySM2)+"\nSM2_publicKey="+Util.byte2HexStr(publicKeySM2)+"\nchain="+chain);
                writeToInternalStorage("\n");

                showres.setText("SM2_PrivateKeyDerive duration:"+duration+"ms"+"\nkeyIndex="+keyIndex+"\nSM2_privateKey="+Util.byte2HexStr(privateKeySM2)+"\nSM2_publicKey="+Util.byte2HexStr(publicKeySM2)+"\nchain="+chain);
                /*//函数时间测试
                    for(int i=0;i<12;i++){
                        long startTime = System.nanoTime();
                        skandchain=Update.SM2_PrivateKeyDerive(privateKeySM2,keyIndex,chainbyte);
                        long endTime =  System.nanoTime();
                        duration = (endTime - startTime) / 1_000_000.0;
                        writeToInternalStorage("第"+i+"次密钥派生耗时="+duration +"ms");
                    }*/
                break;
            case R.id.InitialUser://发送初始公钥
                postOne(urlinitialPK);
                break;
            case R.id.showfile://展示日志
                showFileContentDialog();
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
                        showres.setText("SM2_Signature="+Util.byte2HexStr(Base64.decode(base64Signature, Base64.NO_WRAP))+"\nTxID="+json.getString("TxID"));
                        TxIDres=json.getString("TxID");
                        Timeres=json.getString("times");
                        Msgres=json.getString("msg");
                        Signres=Base64.decode(base64Signature, Base64.NO_WRAP);
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
                    try {
                        JSONObject json = new  JSONObject(message1);
                        String base64PublicKey = json.getString("base64PublicKey"); // 获取 Base64 编码的字符串
                        String base64Signature = json.getString("base64Signature"); // 获取 Base64 编码的字符串
                        if(json.getLong("id") != mID){
                            datas.add(new MessageInfor(json.getString("msg"),Long.valueOf(json.getString("times")),Long.valueOf(json.getString("id")),Base64.decode(base64Signature, Base64.NO_WRAP),Base64.decode(base64PublicKey, Base64.NO_WRAP),String.valueOf(json.getString("TxID")),"1"));
                        }
                        TxIDres=json.getString("TxID");
                        Timeres=json.getString("times");
                        Msgres=json.getString("msg");
                        Signres=Base64.decode(base64Signature, Base64.NO_WRAP);
                        handler.sendEmptyMessage(1);
                        showres.setText("SM2_Signature="+Util.byte2HexStr(Base64.decode(base64Signature, Base64.NO_WRAP))+"\nTxID="+json.getString("TxID"));
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
        if(TxID==null||TxID.equals("")){
            Toast.makeText(MainActivity.this,"请先获取TxID",Toast.LENGTH_LONG).show();
            return ;
        }
        long Ltimes = System.currentTimeMillis();
        startTime = System.nanoTime();
        sign=SM2_Sign(privateKeySM2,(message+Ltimes+TxID).getBytes() );
        endTime =System.nanoTime();
        double duration = (endTime - startTime) / 1_000_0000.0;
        writeToInternalStorage("------------SM2_Sign-------------");
        writeToInternalStorage("SM2_Sign duration:"+duration+"ms");
        writeToInternalStorage("SM2_Signature="+Util.byte2HexStr(Signres)+"\nSM2_Privatekey="+Util.byte2HexStr(privateKeySM2));
        writeToInternalStorage("\n");
        showres.setText("SM2_Sign duration:"+duration+"ms"+"\nSM2_Signature="+"\nSM2_Privatekey="+Util.byte2HexStr(privateKeySM2));
        /*//函数时间测试
        for(int i=0;i<12;i++){
            long startTime = System.nanoTime();
            sign=SM2_Sign(privateKeySM2,(message+Ltimes+TxID).getBytes() );
            long endTime =  System.nanoTime();
            duration = (endTime - startTime) / 1_000_000.0;
            writeToInternalStorage("第"+i+"次签名耗时="+duration +"ms");
        }*/
        MessageInfor m = new MessageInfor(message,Ltimes,mID,sign,publicKeySM2,TxID,"1");//消息 时间戳 id
        String base64PublicKey = Base64.encodeToString(publicKeySM2, Base64.NO_WRAP);
        String base64Signature = Base64.encodeToString(sign, Base64.NO_WRAP);
        userSendMsg = "{\"isimg\":\"1\",\"msg\":\""+sendmsgtext.getText().toString()+"\",\"times\":\""+Ltimes+"\",\"base64Signature\":\""+base64Signature+"\",\"base64PublicKey\":\""+ base64PublicKey +"\",\"TxID\":\""+TxID+"\",\"id\":\""+mID+"\"}";
        datas.add(m);
        messageAdapte.notifyDataSetChanged();//通知数据源发生变化
        sendmsgtext.setText("");
    }
    //传1个json参数
    private void postOne(String url) {
        RequestQueue queue = Volley.newRequestQueue(this);
        StringRequest jsonRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d("测试PostOne", "url = " + url);
                        Log.d("测试PostOne", "response =" + response);
                        switch (url){
                            case "http://192.168.220.20:8080/getTxID"://返回时间
                                JSONObject jsonObject = null;
                                try {
                                    jsonObject = new JSONObject(response);
                                    String runtime = jsonObject.getString("runtime");
                                    String txid = jsonObject.getString("txid");
                                    TxID=txid;
                                    if(txid.equals("")){
                                        showres.setText("publicKey not found");
                                        writeToInternalStorage("--------------GetTxID------------");
                                        writeToInternalStorage("publicKey not found");
                                        writeToInternalStorage("\n");
                                    }else{
                                        showres.setText("mapPkToTx.get duration:"+runtime+"\ntxid="+txid);
                                        writeToInternalStorage("--------------GetTxID------------");
                                        writeToInternalStorage("mapPkToTx.get duration:"+runtime+"\ntxid="+txid);
                                        writeToInternalStorage("\n");
                                    }
                                } catch (JSONException e) {
                                    throw new RuntimeException(e);
                                }
                                break;
                            case "http://192.168.220.20:8080/getPublickey"://返回时间
                                try {
                                    jsonObject = new JSONObject(response);
                                    String runtime = jsonObject.getString("runtime");
                                    String publicKey = jsonObject.getString("publicKey");
                                    String transaction = jsonObject.getString("transaction");
                                    publicKeySM2InCert=Util.hexStr2Bytes(CertificateGenerator.verifyCert(publicKey));
                                    showres.setText("Get_PublicKey duration:"+runtime+"\npublicKey="+Util.byte2HexStr(publicKeySM2InCert)+"\ntransaction="+transaction);
                                    writeToInternalStorage("---------Get_Publickey--------");
                                    writeToInternalStorage("Get_PublicKey duration:"+runtime+"\npublicKey="+Util.byte2HexStr(publicKeySM2InCert)+"\ntransaction="+transaction);
                                    writeToInternalStorage("\n");
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                                break;
                            case "http://192.168.220.20:8080/InitialUser"://返回时间
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
                    case "http://192.168.220.20:8080/getTxID":
                        jsonObject= JsonPut.PutJson(jsonObject,"address",address);
                        jsonObject=JsonPut.PutJson(jsonObject,"pkhash",pkHash);
                        break;
                    case "http://192.168.220.20:8080/getPublickey"://给txID返回证书
                        jsonObject=JsonPut.PutJson(jsonObject,"txid",TxIDres);
                        break;
                    case "http://192.168.220.20:8080/InitialUser":
                        try {
                            startTime = System.nanoTime();
                            byte[][] key = SM2_GenerateKeyPair();
                            endTime =System.nanoTime();
                            double duration = (endTime - startTime) / 1_000_0000.0;
                            /*//函数时间测试
                            for(int i=0;i<12;i++){
                                long startTime = System.nanoTime();
                                 key = SM2_GenerateKeyPair();
                                long endTime =System.nanoTime();
                                duration = (endTime - startTime) / 1_000_0000.0;
                                writeToInternalStorage("第"+i+"次生成密钥对耗时="+duration +"ms");
                            } */
                            publicKeySM2 = key[0];
                            pkHash=Util.byte2HexStr(Sm3Hash(publicKeySM2)).toLowerCase();
                            privateKeySM2 = key[1];
                            System.out.println("SM2_Publickey:"+ Util.byte2HexStr(publicKeySM2));
                            System.out.println("SM2_Privatekey:"+Util.byte2HexStr(privateKeySM2));
                            showres.setText("SM2_GenerateKeyPair duration:"+duration+"ms"+"\nSM2_publickey="+Util.byte2HexStr(key[0])+"\nSM2_privatekey="+Util.byte2HexStr(key[1])+"\naddress="+address+"\nkeyIndex="+keyIndex+"\nchain="+chain);
                            writeToInternalStorage("------------InitialUser----------");
                            writeToInternalStorage("SM2_GenerateKeyPair duration:"+duration+"ms");
                            writeToInternalStorage("SM2_publickey="+Util.byte2HexStr(key[0]));
                            writeToInternalStorage("SM2_privatekey="+Util.byte2HexStr(key[1]));
                            writeToInternalStorage("address="+address+"\nkeyIndex="+keyIndex+"\nchain="+chain);
                            writeToInternalStorage("\n");
                            System.out.println("SM2_Publickey:"+Util.byte2HexStr(publicKeySM2));
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        jsonObject=JsonPut.PutJson(jsonObject,"address",address);
                        jsonObject=JsonPut.PutJson(jsonObject,"keyIndex",keyIndex);
                        jsonObject=JsonPut.PutJson(jsonObject,"key",Util.byte2HexStr(publicKeySM2));
                        jsonObject=JsonPut.PutJson(jsonObject,"chain",chain);
                        break;
                    default:
                        System.out.println("Url错误！！");
                        return new byte[0]; // 返回空字节数组
                }
                System.out.println(jsonObject.toString());
                return jsonObject.toString().getBytes();
            }
            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }
        };
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

            verifySign= SM2_Verifysign(mi.getPublicKey(), (mi.getMsg()+mi.getTime()+mi.getTxID()).getBytes(), mi.getSignature());
            //显示
            if (mi.getUserID() == mID){//id相等,自己发的
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
            }else {//对面发的
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

    // 写入文件到内部存储
    private void writeToInternalStorage(String data) {
        try (FileOutputStream fos = openFileOutput("logg.txt", MODE_APPEND)) {
            fos.write(data.getBytes());
            fos.write("\n".getBytes()); // 添加换行符，确保每行数据占一行
            Log.d("logg", "File written to internal storage.");
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("Error", "Error writing file to internal storage: " + e.getMessage());
        }
    }

    //清空logg文件
    private void clearFileOnStartup() {
        try (FileOutputStream fos = openFileOutput("logg.txt", MODE_PRIVATE)) {
            // 写入空字符串以清空文件内容
            fos.write("".getBytes());
            Log.d("MyFile", "File cleared on startup.");
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("Error", "Error clearing file on startup: " + e.getMessage());
        }
    }

    private void showFileContentDialog() {
        // 读取文件内容
        String fileContent = readFromInternalStorage();

        // 创建对话框
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("文件内容");
        builder.setMessage(fileContent);

        // 添加按钮
        builder.setPositiveButton("关闭", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        // 添加"复制"按钮
        builder.setNegativeButton("复制", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // 获取剪贴板服务
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("文件内容", fileContent);
                clipboard.setPrimaryClip(clip);  // 设置剪贴板内容
                Toast.makeText(getApplicationContext(), "内容已复制", Toast.LENGTH_SHORT).show();  // 提示用户
            }
        });
        // 显示对话框
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private String readFromInternalStorage() {
        StringBuilder stringBuilder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(openFileInput("logg.txt")))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("Error", "Error reading file from internal storage: " + e.getMessage());
        }
        return stringBuilder.toString();
    }
}