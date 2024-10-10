package com.example.socketlw;



import static com.example.socketlw.CertificateGenerator.addBouncyCastleProvider;
import static com.example.socketlw.CertificateGenerator.certRecover;
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

import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
//import org.bouncycastle.openssl.PEMParser;





import org.bouncycastle.asn1.x9.ECNamedCurveTable;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.jcajce.provider.asymmetric.ec.GMCipherSpi;
import org.bouncycastle.util.Arrays;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;

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

import cn.mtjsoft.lib_encryption.SM2.SM2Util;
import cn.mtjsoft.lib_encryption.utils.Util;

//http请求用的
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import java.util.HashMap;
import java.util.Map;
import java.security.*;




public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private Handler handler;
    private LayoutInflater inflater;
    private View layout;
    private AlertDialog.Builder builder;
    private TextView titleview;
    private ListView showmsg;
    private EditText sendmsgtext;
    private Button startserver;
    private Button continueserver;
    private Button sendmsgbt;
    private Button sendimg;
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
    //从服务器接收的公钥。
    private byte[] publicKeySM2F = new byte[0];
    private byte[] sign  = new byte[0];
    private Boolean verifySign  = false;
    private String TxID="";

    //http参数
    private String res="";
    //验证方取证书
    //private  String urlTxID2Sij = "http://jsonplaceholder.typicode.com/todos/1";
    private  String urlUpdatePK = "http://192.168.198.20:8080/updatepktest";
    //签名方取TxID
    private  String urlTxID =     "http://192.168.198.20:8080/postmapget";
    private  String urlPostaddr = "http://192.168.198.20:8080/postaddr";
    //从TxID取公钥
    private  String urlTxID2PublicKey = "http://192.168.198.20:8080/getpublickey";
    //取证书
    private  String urlCert = "http://10.152.57.101:8080/testCert";


    private byte[] CertB = new byte[0];
    private String address = "0x4f4072fc87a0833ea924f364e8a2af3546f71279";
    private String address2 = "0xccdee8c8017f64c686fa39c42f883f363714e078";
    private String chain="560AF94CC1C8BB9AE6986502136B425D";
    private String sk = "your_sk_value";

    private static final String URL = "http://192.168.198.20:8545";
    private static final String TAG = "VolleyRequest";

    //定义证书

    // 生成证书
    private X509Certificate certificate;

    private static final int IMAGE = 1;//调用系统相册-选择图片
    private static String[] PERMISSIONS_STORAGE = {
            //依次权限申请
            Manifest.permission.INTERNET,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };


    @Override//实现了一个 Android Activity 的初始化逻辑
    protected void onCreate(Bundle savedInstanceState) {
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
        publicKeySM2 = key[0];
        privateKeySM2 = key[1];
        System.out.println("公钥为："+Util.byte2HexStr(publicKeySM2));
        System.out.println("私钥为："+Util.byte2HexStr(privateKeySM2));


        //一进来就获取TxID
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("address", address);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
      //  postOne(urlTxID,jsonObject);
      //  TxID=res;

      //  Toast.makeText(MainActivity.this,"服务器获取TxID:"+TxID,Toast.LENGTH_LONG).show();
      //  Log.d("服务器获取TxID:",TxID);
//从文件中读取CA公钥
        try {
            addBouncyCastleProvider(); // 添加 BouncyCastle 提供程序
            String path2="app/src/main/res/account/gm/0x4c26aecee34487d29adff978fd6791578ed8fd28.pem.pub";
            String path1="C:\\Users\\22861\\Desktop\\game\\SocketLW\\app\\src\\main\\java\\com\\example\\account\\gm\\0x4c26aecee34487d29adff978fd6791578ed8fd28.pem.pub";
            PublicKey publicKey = readPublicKey(path2);
            System.out.println("Public Key: " + publicKey);
        } catch (Exception e) {
            e.printStackTrace();
        }
       //从服务器获取证书的字节流
       /* JSONObject jsonObjectC = new JSONObject();
        try {
            jsonObjectC.put("TxID", TxID);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        postOne(urlCert,jsonObjectC);
        try {
            X509Certificate Cert=CertificateGenerator.certRecover(res);*/
            //验证证书
            // 读取CA公钥
          /* PEMParser pemParserpk = new PEMParser(new FileReader("src/main/res/account/gm/0x4c26aecee34487d29adff978fd6791578ed8fd28.pem.pub"));
            Object objectpk = pemParserpk.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");
            PublicKey publicKeyCA = converter.getPublicKey((SubjectPublicKeyInfo) objectpk);
            pemParserpk.close();*/
            //Cert.verify(publicKeyCA, "BC");

           /* String pkInCert=CertificateGenerator.PkRecover(    Util.byte2HexStr(Cert.getEncoded())    );
            System.out.println("pkInCert="+pkInCert);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }*/



     /* //发出更新公钥指令
        JSONObject jsonObjectUPdate = new JSONObject();
        try {
            jsonObjectUPdate.put("address", "0xccdee8c8017f64c686fa39c42f883f363714e078");
            jsonObjectUPdate.put("pkIndex", 1);
           // jsonObjectUPdate.put("key", Util.byte2HexStr(publicKeySM2));
            jsonObjectUPdate.put("key", "FC5B2396034B0C1807EED779B7D20F8C97E22CE4E6BA18156458BBCF76172AB9D0074745EB713CDDCB5C21A95A79631EE626F8F2266EF7BC9D8DF2B8C652D530");
            jsonObjectUPdate.put("chain", chain);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        postOne(urlUpdatePK,jsonObjectUPdate);

        publicKeySM2F=Util.hexStr2Bytes(res);

        //更新本地公钥
       //Log.d("本地推导的公钥为：",Util.byte2HexStr(Update.derivePk(Util.hexStr2Bytes(), BigInteger.valueOf(1))));
        String chain2="560AF94CC1C8BB9AE6986502136B425D";
        byte[] chainroot = Util.hexStr2Bytes(chain2);
        String pk="FC5B2396034B0C1807EED779B7D20F8C97E22CE4E6BA18156458BBCF76172AB9D0074745EB713CDDCB5C21A95A79631EE626F8F2266EF7BC9D8DF2B8C652D530";
        String sk="FF5934D949F3B9FC972EF14123BD8855ECC0BA63D0BDAAD38A7D86690A031C53";
        byte[] hash=Update.Hashpkichain(Util.hexStr2Bytes(pk),1,chainroot);
        System.out.println("hash"+Util.byte2HexStr(hash));
        int midIndex = hash.length / 2;
        byte[] firstHalf = Arrays.copyOfRange(hash, 0, midIndex); // 前半部分
        System.out.println("哈希值"+Util.byte2HexStr(firstHalf));
        BigInteger d2 = new BigInteger(1, firstHalf);
       // byte[] newpk=Update.derivePk(Util.hexStr2Bytes(aa),d2);
        BigInteger d = new BigInteger(1, Util.hexStr2Bytes(sk));
        X9ECParameters x9 = ECNamedCurveTable.getByName("sm2p256v1");
        BigInteger curveOrder = x9.getN();
        byte[] newsk=Update.deriveSk(d,d2,curveOrder);
        System.out.println("本地推的私钥为："+Util.byte2HexStr(newsk));
        byte[] newpk= SM2Util.getPublicKeyFromPrivateKey(newsk);
        System.out.println("本地推的公钥为："+Util.byte2HexStr(newpk));
*/
    }

    /**
     * 初始化控件
     */
    private void InitView() {
        titleview = (TextView) findViewById(R.id.titleview);
        showmsg = (ListView) findViewById(R.id.showmsg);
        sendmsgtext = (EditText) findViewById(R.id.sendmsgtext);
        startserver = (Button) findViewById(R.id.startserver);
        continueserver = (Button) findViewById(R.id.continueserver);
        sendmsgbt = (Button) findViewById(R.id.sendmsgbt);

        simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        messageAdapte = new MessageAdapte();
        showmsg.setAdapter(messageAdapte);

        startserver.setOnClickListener(this);
        continueserver.setOnClickListener(this);
        sendmsgbt.setOnClickListener(this);
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
                if(isServer){//服务器
                    message = sendmsgtext.getText().toString();
                    if(message==null||"".equals(message)){
                        Toast.makeText(MainActivity.this,"发送消息不能为空",Toast.LENGTH_LONG).show();
                        return ;
                    }
                    long Ltimes = System.currentTimeMillis();
                    //String signature="暂时签名";
                    //TxID="到时候再去区块链取";
                    sk=Util.byte2HexStr(privateKeySM2);

                   // sendPostRequest();

                    sign=SM2Util.sign(privateKeySM2,(message+Ltimes+TxID).getBytes() );
                 //   verifySign=SM2Util.verifySign(publicKeySM2, (message+Ltimes+TxID).getBytes(), sign);
                    Log.d("发送消息时","签名为："+ Util.byte2HexStr(sign));
                  //  Log.d("发送消息时","verifySign="+verifySign);
                    //signature=sign.toString();
                    message = sendmsgtext.getText().toString();
                    String base64PublicKey = Base64.encodeToString(publicKeySM2, Base64.NO_WRAP);
                    String base64Signature = Base64.encodeToString(sign, Base64.NO_WRAP);
                    datas.add(new MessageInfor(message,Ltimes,mID,sign,publicKeySM2,TxID,"1"));
                    sendMessage("{\"isimg\":\"1\",\"msg\":\""+message+"\",\"times\":\""+Ltimes+"\",\"id\":\""+mID+"\",\"base64Signature\":\""+base64Signature+"\",\"base64PublicKey\":\""+ base64PublicKey +"\",\"TxID\":\""+TxID+"\",\"peoplen\":\""+"当前在线人数["+(allOut.size()+1)+"]"+"\"}");
                    sendmsgtext.setText("");



                }else {//客户端
                    sendMsgText();
                }


                break;
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
                        if(json.getString("isimg").equals("1")){//不为图片
                            datas.add(new MessageInfor(json.getString("msg"),Long.valueOf(json.getString("times")),Long.valueOf(json.getString("id")),Base64.decode(base64Signature, Base64.NO_WRAP),Base64.decode(base64PublicKey, Base64.NO_WRAP),String.valueOf(json.getString("TxID")),"1"));
                        }else if(json.getString("isimg").equals("0")){//为图片
                            datas.add(new MessageInfor(json.getString("msg"),Long.valueOf(json.getString("times")),Long.valueOf(json.getString("id")),Base64.decode(base64Signature, Base64.NO_WRAP),Base64.decode(base64PublicKey, Base64.NO_WRAP),String.valueOf(json.getString("TxID")),"0"));
                        }
                        titletext = json.getString("peoplen");
                        handler.sendEmptyMessage(1);
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
                        if(json.getLong("id") != mID){
                            String base64PublicKey = json.getString("base64PublicKey"); // 获取 Base64 编码的字符串
                            String base64Signature = json.getString("base64Signature"); // 获取 Base64 编码的字符串
                            if(json.getString("isimg").equals("1")){//不为图片
                                datas.add(new MessageInfor(json.getString("msg"),Long.valueOf(json.getString("times")),Long.valueOf(json.getString("id")),Base64.decode(base64Signature, Base64.NO_WRAP),Base64.decode(base64PublicKey, Base64.NO_WRAP),String.valueOf(json.getString("TxID")),"1"));
                            }else if(json.getString("isimg").equals("0")){//为图片
                                datas.add(new MessageInfor(json.getString("msg"),Long.valueOf(json.getString("times")),Long.valueOf(json.getString("id")),Base64.decode(base64Signature, Base64.NO_WRAP),Base64.decode(base64PublicKey, Base64.NO_WRAP),String.valueOf(json.getString("TxID")),"0"));
                            }
                        }
                        titletext = json.getString("peoplen");
                        TxID=json.getString("TxID");
                        handler.sendEmptyMessage(1);
                        //给出TxID，返回公钥
                        JSONObject jsonObjectT = new JSONObject();
                        try {
                            jsonObjectT.put("txid", TxID);
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                        postOne(urlTxID2PublicKey,jsonObjectT);
                        publicKeySM2=Util.hexStr2Bytes(res);
                        Log.d("接收从服务端的TxID后，获取公钥：",Util.byte2HexStr(publicKeySM2));
                       /* Log.d("接收消息后更新公钥","已更新公钥");
                        updatepk(urlUpdatePK,address);
                        Log.d("更新公钥后获取txid","TxID="+TxID   );*/
                        //messageAdapte.notifyDataSetChanged();//通知数据源发生变化
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
        long Ltimes = System.currentTimeMillis();
       // String signature="暂时签名";
      //  TxID="到时候再去区块链取";
        sk=Util.byte2HexStr(privateKeySM2);

//先转到这来验证公钥
     /*   publicKeySM2F=Util.hexStr2Bytes(res);
        Log.d("传来的公钥为：",Util.byte2HexStr(publicKeySM2F));
        //更新密钥
        String aa="A5B4792C75CDF7D4DF2CA1370F15FAA2FF8ADEB1C7CBBA7CE94FF98EC8F788C9186C3438482D0D027A00A77A78326639649DF283A872DAB3DEE8C5C3877D5099";
        Log.d("本地推导的公钥为：",Util.byte2HexStr(Update.derivePk(Util.hexStr2Bytes(aa), BigInteger.valueOf(1))));
*/

        // Log.d("接收消息后更新公钥","已更新公钥");
       //updatepk(urlUpdatePK,address);
        //g给出地址，获取TxID
        JSONObject jsonObjectA = new JSONObject();
        try {
            jsonObjectA.put("address", address);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        postOne(urlTxID,jsonObjectA);//从Address得到txID
        TxID=res;
        Toast.makeText(MainActivity.this,"客户端获取TxID:"+TxID,Toast.LENGTH_LONG).show();
        Log.d("客户端获取TxID:",TxID);
       // sendPostRequest();

        sign=SM2Util.sign(privateKeySM2,(message+Ltimes+TxID).getBytes() );
       //=SM2Util.verifySign(publicKeySM2, (message+Ltimes+TxID).getBytes(), sign);
        Log.d("客户端发消息","sign="+ Util.byte2HexStr(sign));
       // Log.d("客户端发消息","verifySign="+verifySign);
       // signature=sign.toString();
        MessageInfor m = new MessageInfor(message,Ltimes,mID,sign,publicKeySM2,TxID,"1");//消息 时间戳 id
        String base64PublicKey = Base64.encodeToString(publicKeySM2, Base64.NO_WRAP);
        String base64Signature = Base64.encodeToString(sign, Base64.NO_WRAP);
        userSendMsg = "{\"isimg\":\"1\",\"msg\":\""+sendmsgtext.getText().toString()+"\",\"times\":\""+Ltimes+"\",\"base64Signature\":\""+base64Signature+"\",\"base64PublicKey\":\""+ base64PublicKey +"\",\"TxID\":\""+TxID+"\",\"id\":\""+mID+"\"}";
        datas.add(m);
        messageAdapte.notifyDataSetChanged();//通知数据源发生变化

        sendmsgtext.setText("");
    }

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
                    holder.leftimg.setVisibility(View.GONE);
                    holder.leftimgtime.setVisibility(View.GONE);

                    holder.rightimg.setVisibility(View.VISIBLE);
                    holder.rightimgtime.setVisibility(View.VISIBLE);

                    holder.rightimg.setImageBitmap(convertStringToIcon(mi.getMsg()));
                    holder.rightimgtime.setText(simpleDateFormat.format(new Date(mi.getTime())));

                    holder.left.setVisibility(View.GONE);
                    holder.lefttime.setVisibility(View.GONE);
                    holder.right.setVisibility(View.GONE);
                    holder.righttime.setVisibility(View.GONE);

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
                    holder.rightsign.setText("签名："+Util.byte2HexStr(mi.getSignature())+",公钥："+Util.byte2HexStr(mi.getPublicKey())+",标识："+mi.getTxID());

                }


            }else {//对面发的
                if(mi.getType().equals("0")){//图片
                    holder.leftimg.setVisibility(View.VISIBLE);
                    holder.leftimgtime.setVisibility(View.VISIBLE);
                    holder.rightimg.setVisibility(View.GONE);
                    holder.rightimgtime.setVisibility(View.GONE);
                    holder.leftimg.setImageBitmap(convertStringToIcon(mi.getMsg()));
                    holder.leftimgtime.setText(simpleDateFormat.format(new Date(mi.getTime())));

                    holder.left.setVisibility(View.GONE);
                    holder.lefttime.setVisibility(View.GONE);
                    holder.right.setVisibility(View.GONE);
                    holder.righttime.setVisibility(View.GONE);

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
                    if(verifySign){
                        holder.leftsign.setText("签名通过"+",标识："+mi.getTxID());
                        holder.leftsign.setTextColor(Color.GREEN);
                    }else {
                        holder.leftsign.setText("签名不通过"+",标识："+mi.getTxID());
                        holder.leftsign.setTextColor(Color.RED);
                    }
                  //  holder.leftsign.setText("签名："+Arrays.toString(mi.getSignature())+",公钥："+Arrays.toString(mi.getPublicKey())+",标识："+mi.getTxID()+"签名通通通过了");

                   // holder.leftsign.setText("签名："+mi.getSignature()+",公钥："+mi.getPublicKey()+",标识："+mi.getTxID());

                }
            }
            return view;
        }
    }

    class MessageHolder{
        public TextView left;
        public TextView right;
        public TextView lefttime;
        public TextView righttime;
        public TextView leftsign;
        public TextView rightsign;



        private TextView rightimgtime;
        private TextView leftimgtime;
        private ImageView rightimg;
        private ImageView leftimg;

    }







    /**
     * string转成bitmap
     * @param st
     * @return
     */
    private Bitmap convertStringToIcon(String st){
        Bitmap bitmap = null;
        try
        {
            byte[] bitmapArray;
            bitmapArray = Base64.decode(st, Base64.DEFAULT);
            bitmap = BitmapFactory.decodeByteArray(bitmapArray, 0,bitmapArray.length);
            return bitmap;
        }
        catch (Exception e)
        {
            return null;
        }
    }



   

//传1个参数
private void postOne(String url,JSONObject jsonObject) {
    RequestQueue queue = Volley.newRequestQueue(this);

    // 在函数内部生成 JSON 对象
   // JSONObject jsonObject = new JSONObject();

 /*   try {
        // 添加键值对到 JSON 对象
        jsonObject.put(key, value);

    } catch (JSONException e) {
        e.printStackTrace();
    }*/

    StringRequest jsonRequest = new StringRequest(Request.Method.POST, url,
            new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    // 假设服务器返回一个含有TXID的JSON字符串
                    // 直接将响应的字符串作为 TXID

                    res=response;
                    Toast.makeText(MainActivity.this, "请求成功: response = " + res, Toast.LENGTH_SHORT).show();
                    Log.d("测试PostOne", "url = " + url);
                    Log.d("测试PostOne", "response =" + res);

                    Log.d("测试PostOne", "执行成功");
                }

            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Toast.makeText(MainActivity.this, "请求失败: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.d("ppp", error.getMessage());
                }
            }) {
        @Override
        public byte[] getBody() {
            // 返回 JSON 格式的请求体
            return jsonObject.toString().getBytes();
        }

        @Override
        public String getBodyContentType() {
            return "application/json; charset=utf-8";
        }
    };

    queue.add(jsonRequest);
}




}

























