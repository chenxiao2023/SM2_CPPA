package com.example.socketlw;

import static com.example.socketlw.CertificateGenerator.certRecover;
import static com.example.socketlw.CertificateGenerator.certRecoverfromtx;
import static com.example.socketlw.CertificateGenerator.makeCert;
import static com.example.socketlw.CertificateGenerator.readPublicKey;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
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
import com.example.socketlw.SM2Utils.Util;

import java.util.HashMap;
import java.util.Map;
import java.security.*;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private LayoutInflater inflater;
    private View layout;
    private AlertDialog.Builder builder;
    private TextView showres;


    private Button deployContractButton;
    private Button publishPublicKeyButton;
    private Button publishTxIDButton;
    private Button updatePublicKeyButton;
    private Button showFile;



    private List<MessageInfor> datas = new ArrayList<MessageInfor>();
    private SimpleDateFormat simpleDateFormat;
    private static Socket socket = null;//用于与服务端通信的Socket
    private static ServerSocket server;
    private static List<PrintWriter> allOut; //存放所有客户端的输出流的集合，用于广播

    //http参数
    private String res="";

    //部署合约
    private  String urlDeployContract = "http://192.168.220.20:8080/deployContract";
    //更新公钥
    private  String urlUpdatePK = "http://192.168.220.20:8080/keyDerive";
    //发布公钥
    private  String urlPublishPublicKey =  "http://192.168.220.20:8080/publishKey";
    //发布TxID
    private  String urlPublishTxID = "http://192.168.220.20:8080/publishTxID";



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

        //清空文件内容
        clearFileOnStartup();

    }

    /**
     * 初始化控件
     */
    private void InitView() {

        showres = (TextView) findViewById(R.id.showres);
        showres.setMovementMethod(new ScrollingMovementMethod());

        deployContractButton = (Button) findViewById(R.id.deployContractButton);
        updatePublicKeyButton = (Button) findViewById(R.id.updatePublicKeyButton);
        showFile = (Button) findViewById(R.id.showfile);
        publishPublicKeyButton = (Button) findViewById(R.id.updatePublicKeyButton);
        publishTxIDButton = (Button) findViewById(R.id.publishTxIDButton);

        simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

        deployContractButton.setOnClickListener(this);
        updatePublicKeyButton.setOnClickListener(this);
        publishPublicKeyButton.setOnClickListener(this);
        publishTxIDButton.setOnClickListener(this);
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
            case R.id.deployContractButton://部署公钥
                postOne(urlDeployContract);
                break;
            case R.id.updatePublicKeyButton://更新公钥
                postOne(urlUpdatePK);
                break;
            case R.id.publishPublicKeyButton://发布公钥
                postOne(urlPublishPublicKey);
                break;
            case R.id.publishTxIDButton://发布TxID
                postOne(urlPublishTxID);
                break;
            case R.id.showfile://展示日志
                showFileContentDialog();
                break;
            default:

        }
    }

//传1个参数
private void postOne(String url) {
    RequestQueue queue = Volley.newRequestQueue(this);
    StringRequest jsonRequest = new StringRequest(Request.Method.POST, url,
            new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                   // res=response;
                    Toast.makeText(MainActivity.this, "请求成功: response = " + res, Toast.LENGTH_SHORT).show();
                    Log.d("测试PostOne", "url = " + url);
                    Log.d("测试PostOne", "response =" + response);

                    switch (url){
                        case "http://192.168.220.20:8080/deployContract"://返回时间
                            try {
                                JSONArray jsonArray = new JSONArray(response);
                                JSONObject jsonObject = jsonArray.getJSONObject(0);
                                String time = jsonObject.getString("time");
                                int value = jsonObject.getInt("value");
                                String name = jsonObject.getString("name");
                                // 输出或者使用这些数据
                                Log.d("JSON解析", "时间: " + time + ", 值: " + value + ", 名称: " + name);
                                showres.setText("response="+response);
                                writeToInternalStorage("--------------DeployContract------------");
                                writeToInternalStorage("response="+response);
                                writeToInternalStorage("---------------END---------------");
                            } catch (JSONException e) {
                                Log.e("JSON解析错误", "解析失败: " + e.getMessage());
                            }

                            break;
                        case "http://192.168.220.20:8080/keyDerive":
                            showres.setText("response="+response);
                            writeToInternalStorage("---------KeyDerive--------");
                            writeToInternalStorage("response="+response);
                            writeToInternalStorage("---------------END---------------");

                            break;
                        case "http://192.168.220.20:8080/publishKey":
                            showres.setText("response="+response);
                            writeToInternalStorage("------------PublishKey----------");
                            writeToInternalStorage("response="+response);
                            writeToInternalStorage("---------------END---------------");
                            break;
                        case "http://192.168.220.20:8080/publishTxID":
                            showres.setText("response="+response);
                            writeToInternalStorage("-----------PublishTxID---------");
                            writeToInternalStorage("response="+response);
                            writeToInternalStorage("---------------END---------------");
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
           /* switch (url){
                case "http://192.168.220.20:8080/deployContract":
                    break;
                case "http://192.168.220.20:8080/keyDerive"://给txID返回证书

                    break;
                case "http://192.168.220.20:8080/publishKey":
                    break;
                case "http://192.168.220.20:8080/publishTxID":
                    break;
                default:
                    System.out.println("Url错误！！");
                    return new byte[0]; // 返回空字节数组
            }*/
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
    int socketTimeout = 10000; // 10 seconds
    RetryPolicy policy = new DefaultRetryPolicy(socketTimeout, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
    jsonRequest.setRetryPolicy(policy);


    queue.add(jsonRequest);
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
    //展示文件内容
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

        // 显示对话框
        AlertDialog dialog = builder.create();
        dialog.show();
    }


    //读取文件
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
